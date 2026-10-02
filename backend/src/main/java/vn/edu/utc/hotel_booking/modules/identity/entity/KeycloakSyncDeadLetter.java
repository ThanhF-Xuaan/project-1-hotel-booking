package vn.edu.utc.hotel_booking.modules.identity.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.common.entity.BaseEntity;

import java.util.UUID;

@Entity
@Table(name = "keycloak_sync_dead_letters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class KeycloakSyncDeadLetter extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "keycloak_id", nullable = false)
    UUID keycloakId;

    @Column(name = "action", nullable = false, length = 50)
    String action; // 'DISABLE_AND_LOGOUT', 'DELETE_USER', 'UPDATE_USER', 'CREATE_USER'

    @Column(name = "payload", columnDefinition = "TEXT")
    String payload;

    @Column(name = "error_message", columnDefinition = "TEXT")
    String errorMessage;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    Integer retryCount = 3;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    String status = "PENDING"; // 'PENDING', 'PROCESSED', 'FAILED', 'IGNORED'
}
