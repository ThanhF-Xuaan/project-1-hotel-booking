package vn.edu.utc.hotel_booking.modules.identity.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.identity.entity.KeycloakSyncDeadLetter;

import java.util.List;
import java.util.UUID;

@Repository
public interface KeycloakSyncDeadLetterRepository extends JpaRepository<KeycloakSyncDeadLetter, Long> {

    List<KeycloakSyncDeadLetter> findByStatusOrderByCreatedAtDesc(String status);

    boolean existsByKeycloakIdAndActionAndStatus(UUID keycloakId, String action, String status);
}
