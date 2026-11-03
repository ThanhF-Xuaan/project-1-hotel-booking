package vn.edu.utc.hotel_booking.modules.operation.service;

import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import vn.edu.utc.hotel_booking.common.exception.*;
import vn.edu.utc.hotel_booking.modules.identity.entity.Staff;
import vn.edu.utc.hotel_booking.modules.identity.repository.StaffRepository;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.*;
import vn.edu.utc.hotel_booking.modules.operation.mapper.UtilityReadingMapperImpl;
import vn.edu.utc.hotel_booking.modules.operation.repository.*;
import vn.edu.utc.hotel_booking.modules.operation.service.impl.UtilityReadingServiceImpl;
import vn.edu.utc.hotel_booking.modules.organization.repository.HotelRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class UtilityConcurrencyPostgresIT extends OperationPostgresSupport {
    UtilityReadingServiceImpl service;
    int meter;
    final ExecutorService pool=Executors.newFixedThreadPool(3);
    @BeforeEach void prepare() {
        meter=jdbc.queryForObject("INSERT INTO utility_meters(hotel_id,meter_code,meter_type,is_deleted) VALUES(1,?,'ELECTRICITY',false) RETURNING id",Integer.class,"SYNTH-F04-"+UUID.randomUUID().toString().substring(0,8));
        var staff=mock(StaffRepository.class);
        when(staff.findByUsernameAndIsDeletedFalse("synthetic-operation-it")).thenReturn(Optional.of(Staff.builder().id(1).build()));
        service=new UtilityReadingServiceImpl(repositories.getRepository(UtilityReadingRepository.class),repositories.getRepository(UtilityMeterRepository.class),staff,
                repositories.getRepository(HotelRepository.class),new UtilityReadingMapperImpl());
        // Optional before the fix so the red case exercises behavior, not a missing field.
        if (org.springframework.util.ReflectionUtils.findField(service.getClass(),"entityManager")!=null) ReflectionTestUtils.setField(service,"entityManager",em);
    }
    @AfterEach void cleanup() {
        pool.shutdownNow();
        try { assertThat(pool.awaitTermination(10,TimeUnit.SECONDS)).isTrue(); } catch(InterruptedException e) { Thread.currentThread().interrupt();throw new AssertionError(e); }
        jdbc.update("DELETE FROM utility_readings WHERE meter_id=?",meter);jdbc.update("DELETE FROM utility_meters WHERE id=?",meter);
    }
    UtilityReadingCreateRequest request(int day,int value) { var request=new UtilityReadingCreateRequest(); request.setMeterId(meter);
            request.setReadingDate(LocalDate.of(2026,1,day)); request.setReadingValue(BigDecimal.valueOf(value)); request.setIsMeterReset(false); return request; }
    Object create(int day,int value) {
        try { return run(()->service.create(request(day,value))); } catch(AppException e) { return e.getErrorCode(); }
    }
    @Test void invertedConcurrentCreatesSerializeBeforeNeighborValidation() throws Exception {
        try(var blocker=jdbc.getDataSource().getConnection()) {
            blocker.setAutoCommit(false);
            try(var statement=blocker.prepareStatement("SELECT id FROM utility_meters WHERE id=? FOR UPDATE")) { statement.setInt(1,meter);statement.executeQuery().close(); }
            var first=pool.submit(()->create(20,200));var second=pool.submit(()->create(21,100));
            try {
                awaitLockWait(2);
                var waiting=jdbc.queryForList("SELECT query FROM pg_stat_activity WHERE datname=current_database() AND wait_event_type='Lock'",String.class);
                assertThat(waiting).hasSize(2).allSatisfy(query->assertThat(query.toLowerCase()).contains("utility_meters","for","update"));
            } finally { blocker.rollback(); }
            var results=List.of(first.get(10,TimeUnit.SECONDS),second.get(10,TimeUnit.SECONDS));
            assertThat(results.stream().filter(ErrorCode.INVALID_UTILITY_READING_VALUE::equals).count()).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM utility_readings WHERE meter_id=? AND NOT is_deleted",Integer.class,meter)).isEqualTo(1);
        }
    }
    @Test void deletingResetCannotCreateNegativeSurvivingDelta() {
        jdbc.update("INSERT INTO utility_readings(meter_id,reading_date,reading_value,is_meter_reset,recorded_by,is_deleted) VALUES(?, '2026-01-01',100,false,1,false),(?, '2026-01-02',0,true,1,false),(?, '2026-01-03',10,false,1,false)",meter,meter,meter);
        Long reset=jdbc.queryForObject("SELECT id FROM utility_readings WHERE meter_id=? AND is_meter_reset",Long.class,meter);
        assertThatThrownBy(()->run(()->{service.delete(List.of(reset));return null;})).isInstanceOfSatisfying(AppException.class,e->assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_UTILITY_READING_VALUE));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM utility_readings WHERE meter_id=? AND NOT is_deleted",Integer.class,meter)).isEqualTo(3);
    }
    Long reading(int day,int value,boolean reset) {
        return jdbc.queryForObject("INSERT INTO utility_readings(meter_id,reading_date,reading_value,is_meter_reset,recorded_by,is_deleted) VALUES(?,?,?,?,1,false) RETURNING id",
                Long.class,meter,LocalDate.of(2026,1,day),value,reset);
    }
    @Test void staleUpdateRechecksDeletedReadingAfterMeterLock() throws Exception {
        Long id=reading(1,100,false);
        var update=new UtilityReadingUpdateRequest();update.setReadingValue(BigDecimal.valueOf(101));
        try(var blocker=jdbc.getDataSource().getConnection()) {
            blocker.setAutoCommit(false);
            try(var q=blocker.prepareStatement("SELECT id FROM utility_meters WHERE id=? FOR UPDATE")) {q.setInt(1,meter);q.executeQuery().close();}
            var writer=pool.submit(()->{
                try {return run(()->service.update(id,update));}catch(AppException error){return error.getErrorCode();}
            });
            try { awaitLockWait(1);
                try(var q=blocker.prepareStatement("UPDATE utility_readings SET is_deleted=true WHERE id=?")){q.setLong(1,id);q.executeUpdate();}
                blocker.commit();
            }finally{blocker.rollback();}
            assertThat(writer.get(10,TimeUnit.SECONDS)).isEqualTo(ErrorCode.UTILITY_READING_NOT_FOUND);
            assertThat(jdbc.queryForObject("SELECT reading_value FROM utility_readings WHERE id=?",BigDecimal.class,id)).isEqualByComparingTo("100");
        }
    }
    @Test void lockTimeoutIsBoundedAndReturnsSanitizedConflict() throws Exception {
        try(var blocker=jdbc.getDataSource().getConnection()) {
            blocker.setAutoCommit(false);
            try(var q=blocker.prepareStatement("SELECT id FROM utility_meters WHERE id=? FOR UPDATE")) {q.setInt(1,meter);q.executeQuery().close();}
            long started=System.nanoTime();var writer=pool.submit(()->create(1,100));
            try {awaitLockWait(1);assertThat(writer.get(8,TimeUnit.SECONDS)).isEqualTo(ErrorCode.UTILITY_WRITE_CONFLICT);
                assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-started)).isBetween(4500L,8000L);
            }finally{blocker.rollback();}
        }
    }
    @Test void differentMeterDoesNotWaitForLockedMeter() throws Exception {
        int other=jdbc.queryForObject("INSERT INTO utility_meters(hotel_id,meter_code,meter_type,is_deleted) VALUES(1,?,'WATER',false) RETURNING id",Integer.class,"SYNTH-F04-"+UUID.randomUUID().toString().substring(0,8));
        try(var blocker=jdbc.getDataSource().getConnection()) {
            blocker.setAutoCommit(false);
            try(var q=blocker.prepareStatement("SELECT id FROM utility_meters WHERE id=? FOR UPDATE")){q.setInt(1,meter);q.executeQuery().close();}
            var request=request(1,100);request.setMeterId(other);
            var writer=pool.submit(()->run(()->service.create(request)));
            try{assertThat(writer.get(3,TimeUnit.SECONDS).getMeterId()).isEqualTo(other);}finally{blocker.rollback();}
        }finally{jdbc.update("DELETE FROM utility_readings WHERE meter_id=?",other);jdbc.update("DELETE FROM utility_meters WHERE id=?",other);}
    }
    @Test void bulkDeleteChecksFinalSurvivorsRatherThanIntermediateReset() {
        reading(1,100,false);Long reset=reading(2,0,true);Long after=reading(3,10,false);
        run(()->{service.delete(List.of(after,reset,reset));return null;});
        assertThat(jdbc.queryForObject("SELECT count(*) FROM utility_readings WHERE meter_id=? AND NOT is_deleted",Integer.class,meter)).isEqualTo(1);
    }

}
