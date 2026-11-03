package vn.edu.utc.hotel_booking.modules.operation.service;

import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import vn.edu.utc.hotel_booking.modules.booking.entity.*;
import vn.edu.utc.hotel_booking.modules.booking.repository.StayGuestRepository;
import vn.edu.utc.hotel_booking.modules.booking.service.BookingLookupService;
import vn.edu.utc.hotel_booking.modules.identity.repository.StaffRepository;
import vn.edu.utc.hotel_booking.modules.inventory.repository.RoomInstanceRepository;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.*;
import vn.edu.utc.hotel_booking.modules.operation.entity.enums.*;
import vn.edu.utc.hotel_booking.modules.operation.mapper.*;
import vn.edu.utc.hotel_booking.modules.operation.repository.LodgingQueueRepository;
import vn.edu.utc.hotel_booking.modules.operation.service.impl.StayGuestServiceImpl;
import vn.edu.utc.hotel_booking.modules.organization.repository.HotelRepository;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class StayGuestDedupPostgresIT extends OperationPostgresSupport {
    StayGuestServiceImpl service;
    StayGuestMapper mapper;
    String marker;
    final ExecutorService pool=Executors.newFixedThreadPool(3);
    @BeforeEach void prepare() {
        marker="SYNTH-F03-"+UUID.randomUUID().toString().substring(0,8);
        mapper=spy(new StayGuestMapperImpl());
        var bookings=mock(BookingLookupService.class);
        when(bookings.getBookingRoomEntityById(1L)).thenAnswer(call->em.find(BookingRoom.class,1L));
        service=new StayGuestServiceImpl(repositories.getRepository(StayGuestRepository.class),repositories.getRepository(LodgingQueueRepository.class),
                repositories.getRepository(HotelRepository.class),bookings,repositories.getRepository(RoomInstanceRepository.class),mock(StaffRepository.class),mapper);
        ReflectionTestUtils.setField(service,"entityManager",em);
    }
    @AfterEach void cleanup() throws Exception {
        pool.shutdownNow();assertThat(pool.awaitTermination(10,TimeUnit.SECONDS)).isTrue();
        jdbc.update("DELETE FROM lodging_queues WHERE stay_guest_id IN(SELECT id FROM stay_guests WHERE full_name=?)",marker);
        jdbc.update("DELETE FROM stay_guests WHERE full_name=?",marker);
    }
    StayGuestCreateRequest request(String document) {
        return StayGuestCreateRequest.builder().hotelId((short)1).bookingRoomId(1L).roomNumber("101")
                .fullName(marker).documentNumber(document).dateOfBirth(LocalDate.of(2000,1,1)).gender(Gender.OTHER)
                .nationality("Vietnam").permanentAddress("SYNTHETIC")
                .checkInTime(OffsetDateTime.parse("2026-11-03T12:00:00+07:00"))
                .expectedCheckOutTime(OffsetDateTime.parse("2026-11-04T12:00:00+07:00")).build();
    }
    Long create(StayGuestCreateRequest request) {return run(()->service.createOrCheckIn(request).getId());}
    @Test void preCheckInRowIsNotClaimedByActiveStayRetry() {
        Long planned=jdbc.queryForObject("INSERT INTO stay_guests(hotel_id,booking_id,booking_room_id,room_number,full_name,document_type,document_number,guest_type,nationality,is_deleted) VALUES(1,1,1,'101',?,'CCCD',?,'ADULT','Vietnam',false) RETURNING id",Long.class,marker,marker);
        Long active=create(request(marker));
        assertThat(active).isNotEqualTo(planned);
        assertThat(jdbc.queryForObject("SELECT check_in_time FROM stay_guests WHERE id=?",OffsetDateTime.class,planned)).isNull();
    }
    @Test void unknownIdentityNeverMergesGuests() {
        Long first=create(request("N/A"));Long second=create(request("N/A"));
        assertThat(second).isNotEqualTo(first);
    }
    @Test void concurrentSameKeyRetriesReturnOneGuestAndOneQueue() throws Exception {
        var entered=new CountDownLatch(1);var release=new CountDownLatch(1);var calls=new java.util.concurrent.atomic.AtomicInteger();
        doAnswer(call->{if(calls.incrementAndGet()==1){entered.countDown();if(!release.await(10,TimeUnit.SECONDS))throw new AssertionError("Synthetic first insert barrier");}return call.callRealMethod();}).when(mapper).toEntity(any());
        var first=pool.submit(()->create(request(marker)));assertThat(entered.await(5,TimeUnit.SECONDS)).isTrue();
        var second=pool.submit(()->create(request(marker)));
        try {
            long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
            while(calls.get()<2 && jdbc.queryForObject("SELECT count(*) FROM pg_stat_activity WHERE datname=current_database() AND wait_event_type='Lock'",Integer.class)==0 && System.nanoTime()<deadline) Thread.sleep(25);
        } finally {release.countDown();}
        assertThat(first.get(12,TimeUnit.SECONDS)).isEqualTo(second.get(12,TimeUnit.SECONDS));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM stay_guests WHERE full_name=?",Integer.class,marker)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM lodging_queues WHERE stay_guest_id IN(SELECT id FROM stay_guests WHERE full_name=?)",Integer.class,marker)).isEqualTo(1);
    }
    @Test void checkoutPreservesHistoryAndAllowsNewStay() {
        Long first=create(request(marker));
        jdbc.update("UPDATE stay_guests SET actual_check_out_time=expected_check_out_time WHERE id=?",first);
        Long second=create(request(marker));assertThat(second).isNotEqualTo(first);
        assertThat(jdbc.queryForObject("SELECT actual_check_out_time IS NOT NULL FROM stay_guests WHERE id=?",Boolean.class,first)).isTrue();
    }
    @Test void legacyBookingFallbackRetryPreservesExportedNoopAndNullForeignKeys() {
        var request=request(marker);request.setBookingRoomId(1L);request.setRoomNumber("101");
        Long id=create(request);
        jdbc.update("UPDATE stay_guests SET booking_id=null,room_id=null WHERE id=?",id);
        jdbc.update("UPDATE lodging_queues SET status='EXPORTED',batch_reference='SYNTHETIC_BATCH',exported_at=CURRENT_TIMESTAMP WHERE stay_guest_id=?",id);
        var guestBefore=jdbc.queryForMap("SELECT * FROM stay_guests WHERE id=?",id);
        var queueBefore=jdbc.queryForMap("SELECT * FROM lodging_queues WHERE stay_guest_id=?",id);
        assertThat(create(request)).isEqualTo(id);
        assertThat(jdbc.queryForMap("SELECT * FROM stay_guests WHERE id=?",id)).containsAllEntriesOf(guestBefore);
        assertThat(jdbc.queryForMap("SELECT * FROM lodging_queues WHERE stay_guest_id=?",id)).containsAllEntriesOf(queueBefore);
    }
    @Test void walkinKeysAreScopedByHotelAndRoomAndTrimAsciiWhitespace() {
        var first=request(marker);first.setBookingRoomId(null);first.setRoomNumber("SYNTHETIC");
        Long id=create(first);
        var retry=request(" \t"+marker+"\r\n");retry.setBookingRoomId(null);retry.setRoomNumber(" SYNTHETIC ");
        assertThat(create(retry)).isEqualTo(id);
        var otherRoom=request(marker);otherRoom.setBookingRoomId(null);otherRoom.setRoomNumber("SYNTHETIC2");
        assertThat(create(otherRoom)).isNotEqualTo(id);
        var otherHotel=request(marker);otherHotel.setBookingRoomId(null);otherHotel.setRoomNumber("SYNTHETIC");otherHotel.setHotelId((short)2);
        assertThat(create(otherHotel)).isNotEqualTo(id);
        assertThat(create(request(marker))).isNotEqualTo(id); // BOOKING is distinct from WALKIN.
    }
    @Test void identityComparisonRemainsCaseSensitive() {
        Long plain=create(request(marker));
        assertThat(create(request(marker.toLowerCase(Locale.ROOT)))).isNotEqualTo(plain);
        Long control=create(request("\u001f"+marker));
        assertThat(control).isNotEqualTo(plain);
        assertThat(create(request("\u001f"+marker))).isEqualTo(control);
    }
    @Test void rawWriterDuplicateCannotBypassUniqueTriggerAndCannotTamperWithKey() {
        Long id=create(request(marker));byte[] key=jdbc.queryForObject("SELECT active_dedup_key FROM stay_guests WHERE id=?",byte[].class,id);
        jdbc.update("UPDATE stay_guests SET active_dedup_key=decode(repeat('00',32),'hex') WHERE id=?",id);
        assertThat(jdbc.queryForObject("SELECT active_dedup_key FROM stay_guests WHERE id=?",byte[].class,id)).containsExactly(key);
        assertThatThrownBy(()->jdbc.update("INSERT INTO stay_guests(hotel_id,booking_room_id,room_number,full_name,document_type,document_number,guest_type,nationality,check_in_time,is_deleted) VALUES(1,1,'101',?,'CCCD',?,'ADULT','Vietnam',CURRENT_TIMESTAMP,false)",marker,marker))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM stay_guests WHERE full_name=?",Integer.class,marker)).isEqualTo(1);
    }
    @Test void activeIdentityPatchCollisionIs409BeforeBusinessMutation() {
        Long first=create(request(marker));Long second=create(request(marker+"-OTHER"));
        assertThatThrownBy(()->run(()->service.update(second,StayGuestUpdateRequest.builder().documentNumber(marker).build())))
                .isInstanceOfSatisfying(vn.edu.utc.hotel_booking.common.exception.AppException.class,
                        e->assertThat(e.getErrorCode()).isEqualTo(vn.edu.utc.hotel_booking.common.exception.ErrorCode.STAY_GUEST_CONFLICT));
        assertThat(jdbc.queryForObject("SELECT document_number FROM stay_guests WHERE id=?",String.class,second)).isEqualTo(marker+"-OTHER");
        assertThat(first).isNotEqualTo(second);
    }
    @Test void staleActiveMatchCheckedOutWhileWaitingForQueueBecomesANewStay() throws Exception {
        Long first=create(request(marker));
        try(var blocker=jdbc.getDataSource().getConnection()) {
            blocker.setAutoCommit(false);
            try(var q=blocker.prepareStatement("SELECT id FROM lodging_queues WHERE stay_guest_id=? FOR UPDATE")){q.setLong(1,first);q.executeQuery().close();}
            var retry=pool.submit(()->create(request(marker)));
            try {awaitLockWait(1);
                try(var q=blocker.prepareStatement("UPDATE stay_guests SET actual_check_out_time=expected_check_out_time WHERE id=?")){q.setLong(1,first);q.executeUpdate();}
                blocker.commit();
            }finally{blocker.rollback();}
            assertThat(retry.get(10,TimeUnit.SECONDS)).isNotEqualTo(first);
            assertThat(jdbc.queryForObject("SELECT actual_check_out_time IS NOT NULL FROM stay_guests WHERE id=?",Boolean.class,first)).isTrue();
        }
    }
    @Test void softDeleteReleasesKeyAndPreservesHistory() {
        Long first=create(request(marker));run(()->{service.delete(List.of(first));return null;});
        assertThat(jdbc.queryForObject("SELECT active_dedup_key FROM stay_guests WHERE id=?",byte[].class,first)).isNull();
        assertThat(create(request(marker))).isNotEqualTo(first);
        assertThat(jdbc.queryForObject("SELECT is_deleted FROM stay_guests WHERE id=?",Boolean.class,first)).isTrue();
    }
    @Test void missingIdentityCoreChildrenAreDistinctAndHaveNoActiveKey() {
        for(int i=0;i<2;i++) jdbc.update("INSERT INTO stay_guests(hotel_id,room_number,full_name,document_type,document_number,guest_type,nationality,check_in_time,is_deleted) VALUES(1,'SYNTHETIC',?,'OTHER',null,'CHILD','Vietnam',CURRENT_TIMESTAMP,false)",marker);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM stay_guests WHERE full_name=? AND active_dedup_key IS NULL",Integer.class,marker)).isEqualTo(2);
    }

    @Test void twoKeyIdentityEditsLockInSameOrderWithoutDeadlock() throws Exception {
        Long first=create(request(marker));Long second=create(request(marker+"-OTHER"));
        byte[] firstKey=jdbc.queryForObject("SELECT active_dedup_key FROM stay_guests WHERE id=?",byte[].class,first);
        byte[] secondKey=jdbc.queryForObject("SELECT active_dedup_key FROM stay_guests WHERE id=?",byte[].class,second);
        byte[] smallest=HexFormat.of().formatHex(firstKey).compareTo(HexFormat.of().formatHex(secondKey))<0?firstKey:secondKey;
        try(var blocker=jdbc.getDataSource().getConnection()) {
            blocker.setAutoCommit(false);
            try(var q=blocker.prepareStatement("SELECT pg_advisory_xact_lock(hashtextextended(encode(CAST(? AS bytea),'hex'),0))")){q.setBytes(1,smallest);q.executeQuery().close();}
            var left=pool.submit(()->editIdentity(first,marker+"-OTHER"));var right=pool.submit(()->editIdentity(second,marker));
            try{awaitLockWait(2);}finally{blocker.rollback();}
            assertThat(left.get(10,TimeUnit.SECONDS)).isEqualTo(vn.edu.utc.hotel_booking.common.exception.ErrorCode.STAY_GUEST_CONFLICT);
            assertThat(right.get(10,TimeUnit.SECONDS)).isEqualTo(vn.edu.utc.hotel_booking.common.exception.ErrorCode.STAY_GUEST_CONFLICT);
        }
    }
    Object editIdentity(Long id,String document) {
        try{return run(()->service.update(id,StayGuestUpdateRequest.builder().documentNumber(document).build()));}
        catch(vn.edu.utc.hotel_booking.common.exception.AppException error){return error.getErrorCode();}
    }
    @Test void duplicatePlannedCheckInsRollBackWholeBatchWithoutMerging() {
        Long detail=jdbc.queryForObject("INSERT INTO booking_details(booking_id,hotel_room_type_id,room_type_name,quantity,check_in_date,check_out_date) SELECT 1,hotel_room_type_id,'SYNTHETIC_F03',1,CURRENT_DATE,CURRENT_DATE+1 FROM booking_details WHERE id=1 RETURNING id",Long.class);
        Long room=jdbc.queryForObject("INSERT INTO booking_rooms(booking_detail_id,room_instance_id) SELECT ?,room_instance_id FROM booking_rooms WHERE id=1 RETURNING id",Long.class,detail);
        try {
            for(int i=0;i<2;i++)jdbc.update("INSERT INTO stay_guests(hotel_id,booking_room_id,room_number,full_name,document_type,document_number,guest_type,nationality,expected_check_out_time,is_deleted) VALUES(1,?,'101',?,'CCCD',?,'ADULT','Vietnam',CURRENT_TIMESTAMP+INTERVAL '2 day',false)",room,marker,marker);
            assertThatThrownBy(()->run(()->{service.syncLodgingQueueForBookingRoom(room);return null;}))
                    .isInstanceOfSatisfying(vn.edu.utc.hotel_booking.common.exception.AppException.class,e->assertThat(e.getErrorCode()).isEqualTo(vn.edu.utc.hotel_booking.common.exception.ErrorCode.STAY_GUEST_CONFLICT));
            assertThat(jdbc.queryForObject("SELECT count(*) FROM stay_guests WHERE full_name=? AND check_in_time IS NULL",Integer.class,marker)).isEqualTo(2);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM lodging_queues WHERE stay_guest_id IN(SELECT id FROM stay_guests WHERE full_name=?)",Integer.class,marker)).isZero();
        } finally {
            jdbc.update("DELETE FROM lodging_queues WHERE stay_guest_id IN(SELECT id FROM stay_guests WHERE booking_room_id=?)",room);
            jdbc.update("DELETE FROM stay_guests WHERE booking_room_id=?",room);
            jdbc.update("DELETE FROM booking_rooms WHERE id=?",room);jdbc.update("DELETE FROM booking_details WHERE id=?",detail);
        }
    }

}
