package vn.edu.utc.hotel_booking.modules.identity.personalization.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.identity.personalization.entity.PersonalizationConsentEvent;

@Repository
public interface PersonalizationConsentEventRepository extends JpaRepository<PersonalizationConsentEvent, Long> {
}
