package vn.edu.utc.hotel_booking.modules.identity.personalization.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.identity.personalization.entity.PersonalizationAccountBinding;

import java.util.Optional;

@Repository
public interface PersonalizationAccountBindingRepository
        extends JpaRepository<PersonalizationAccountBinding, Long> {

    @Query(value = """
            SELECT binding.*
            FROM personalization_account_bindings binding
            JOIN booking_guests guest ON guest.id = binding.guest_id
            WHERE binding.issuer = :issuer
              AND binding.subject = :subject
              AND binding.status = 'ACTIVE'
              AND guest.is_deleted = false
            """, nativeQuery = true)
    Optional<PersonalizationAccountBinding> findActiveForAccount(
            @Param("issuer") String issuer,
            @Param("subject") String subject
    );

    @Query(value = """
            SELECT binding.*
            FROM personalization_account_bindings binding
            JOIN booking_guests guest ON guest.id = binding.guest_id
            WHERE binding.issuer = :issuer
              AND binding.subject = :subject
              AND binding.status = 'ACTIVE'
              AND guest.is_deleted = false
            FOR UPDATE OF binding, guest
            """, nativeQuery = true)
    Optional<PersonalizationAccountBinding> findActiveForAccountForUpdate(
            @Param("issuer") String issuer,
            @Param("subject") String subject
    );
}
