package vn.edu.utc.hotel_booking.modules.operation.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.identity.entity.Staff;
import vn.edu.utc.hotel_booking.modules.identity.repository.StaffRepository;
import vn.edu.utc.hotel_booking.modules.operation.entity.*;
import vn.edu.utc.hotel_booking.modules.operation.mapper.*;
import vn.edu.utc.hotel_booking.modules.operation.repository.*;
import vn.edu.utc.hotel_booking.modules.operation.service.impl.*;
import vn.edu.utc.hotel_booking.modules.organization.entity.*;
import vn.edu.utc.hotel_booking.modules.organization.repository.HotelRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class UtilityScopeReviewRegressionTest {
    @AfterEach void clearAuthentication() { SecurityContextHolder.clearContext(); }
    static Stream<Arguments> invalidScopes() {
        return Stream.of("PROPERTY_MANAGER", "ENGINEERING", "REGION_MANAGER").flatMap(role ->
                Stream.of((Integer)null, 0, -1, 32768, 65537).flatMap(id ->
                        Stream.of(false, true).map(reading -> Arguments.of(role, id, reading))));
    }

    @ParameterizedTest @MethodSource("invalidScopes")
    void invalidScopeCannotReadUtilityData(String role, Integer id, boolean reading) {
        SecurityContextHolder.setContext(SecurityContextHolder.createEmptyContext());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "synthetic-scope", "", AuthorityUtils.createAuthorityList("ROLE_"+role)));
        var staff = mock(StaffRepository.class);
        when(staff.findByUsernameAndIsDeletedFalse("synthetic-scope")).thenReturn(Optional.of(Staff.builder()
                .scopeType(role.equals("REGION_MANAGER") ? "REGION" : "PROPERTY").scopeEntityId(id).build()));
        var hotel = Hotel.builder().id((short)1).region(Region.builder().id((short)1).build()).build();
        var meter = UtilityMeter.builder().id(10).hotel(hotel).build();
        var meters = mock(UtilityMeterRepository.class);
        var hotels = mock(HotelRepository.class);
        when(meters.findByIdWithHotelAndRegion(10)).thenReturn(Optional.of(meter));
        var readings = mock(UtilityReadingRepository.class);
        when(readings.findByIdWithMeterAndHotelAndRegion(20L)).thenReturn(Optional.of(UtilityReading.builder()
                .id(20L).meter(meter).readingDate(LocalDate.of(2026,11,1)).readingValue(BigDecimal.TEN).build()));
        var meterService = new UtilityMeterServiceImpl(meters, new UtilityMeterMapperImpl(), hotels, staff);
        var readingService = new UtilityReadingServiceImpl(readings, meters, staff, hotels, new UtilityReadingMapperImpl());
        assertThatThrownBy(() -> { if (reading) readingService.getById(20L); else meterService.getById(10); })
                .isInstanceOfSatisfying(AppException.class, error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
    }
}
