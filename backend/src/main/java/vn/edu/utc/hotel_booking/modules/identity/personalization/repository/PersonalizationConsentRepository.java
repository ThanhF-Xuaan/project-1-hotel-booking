package vn.edu.utc.hotel_booking.modules.identity.personalization.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.identity.personalization.entity.PersonalizationConsent;
import vn.edu.utc.hotel_booking.modules.identity.personalization.entity.PersonalizationConsentId;

import java.util.Optional;

@Repository
public interface PersonalizationConsentRepository
        extends JpaRepository<PersonalizationConsent, PersonalizationConsentId> {

    Optional<PersonalizationConsent> findByBindingIdAndHotelIdAndPurpose(
            Long bindingId,
            Short hotelId,
            String purpose
    );
}
