package vn.edu.utc.hotel_booking.modules.organization.service;
import org.junit.jupiter.api.Test;
import vn.edu.utc.hotel_booking.modules.organization.repository.HotelRepository;
import vn.edu.utc.hotel_booking.modules.organization.service.impl.PublicHotelEligibilityReaderImpl;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class PublicHotelEligibilityReaderTest {
 @Test void readsOnlyActiveNonDeletedHotel() {
  var repository=mock(HotelRepository.class); var reader=new PublicHotelEligibilityReaderImpl(repository);
  when(repository.existsByIdAndStatusAndIsDeletedFalse((short)1,"ACTIVE")).thenReturn(true);
  assertThat(reader.isActivePublicHotel((short)1)).isTrue();
  assertThat(reader.isActivePublicHotel((short)2)).isFalse();
  assertThat(reader.isActivePublicHotel((short)0)).isFalse();
  verify(repository).existsByIdAndStatusAndIsDeletedFalse((short)1,"ACTIVE");
  verify(repository).existsByIdAndStatusAndIsDeletedFalse((short)2,"ACTIVE");
  verifyNoMoreInteractions(repository);
 }
}
