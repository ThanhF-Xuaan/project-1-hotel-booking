package vn.edu.utc.hotel_booking.modules.operation.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.identity.repository.StaffRepository;
import vn.edu.utc.hotel_booking.modules.operation.entity.LodgingQueue;
import vn.edu.utc.hotel_booking.modules.operation.entity.enums.LodgingQueueStatus;
import vn.edu.utc.hotel_booking.modules.operation.mapper.LodgingQueueMapperImpl;
import vn.edu.utc.hotel_booking.modules.operation.repository.LodgingQueueRepository;
import vn.edu.utc.hotel_booking.modules.operation.service.impl.LodgingQueueServiceImpl;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class LodgingQueueReviewRegressionTest {
    final LodgingQueueRepository repository = mock(LodgingQueueRepository.class);
    final LodgingQueueMapperImpl mapper = new LodgingQueueMapperImpl();
    final LodgingQueueService service = new LodgingQueueServiceImpl(repository, mock(StaffRepository.class), mapper);
    @BeforeEach void authenticate() {
        org.springframework.test.util.ReflectionTestUtils.setField(mapper, "stayGuestMapper", new vn.edu.utc.hotel_booking.modules.operation.mapper.StayGuestMapperImpl());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "synthetic-admin", "", AuthorityUtils.createAuthorityList("ROLE_CHAIN_ADMIN")));
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    LodgingQueue queue(LodgingQueueStatus status) {
        var guest = vn.edu.utc.hotel_booking.modules.booking.entity.StayGuest.builder().id(21L).fullName("SYNTHETIC")
                .hotel(vn.edu.utc.hotel_booking.modules.organization.entity.Hotel.builder().id((short)1).name("SYNTHETIC").build()).build();
        var queue = LodgingQueue.builder().id(31L).hotel(guest.getHotel()).stayGuest(guest)
                .status(status).batchReference("SYNTHETIC_BATCH").build();
        when(repository.findByIdForUpdate(31L)).thenReturn(Optional.of(queue));
        return queue;
    }
    @Test void exportedRetryIsReadOnlyNoop() {
        var queue = queue(LodgingQueueStatus.EXPORTED);
        var result = service.retryValidate(31L);
        assertThat(result.getStatus()).isEqualTo(LodgingQueueStatus.EXPORTED);
        assertThat(queue.getBatchReference()).isEqualTo("SYNTHETIC_BATCH");
        verify(repository, never()).save(any());
    }
    @ParameterizedTest @EnumSource(value=LodgingQueueStatus.class, names={"PROCESSING","CANCELLED"})
    void retryCannotReactivateProtectedState(LodgingQueueStatus status) {
        var queue = queue(status);
        assertThatThrownBy(() -> service.retryValidate(31L)).isInstanceOfSatisfying(AppException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.LODGING_QUEUE_STATE_CONFLICT));
        assertThat(queue.getStatus()).isEqualTo(status);
        verify(repository, never()).save(any());
    }
}
