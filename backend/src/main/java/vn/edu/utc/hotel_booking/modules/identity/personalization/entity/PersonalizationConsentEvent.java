package vn.edu.utc.hotel_booking.modules.identity.personalization.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "personalization_consent_events")
@Getter
@Setter
@NoArgsConstructor
public class PersonalizationConsentEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "binding_id", nullable = false)
    private Long bindingId;

    @Column(name = "binding_version", nullable = false)
    private Long bindingVersion;

    @Column(name = "hotel_id", nullable = false)
    private Short hotelId;

    @Column(nullable = false, length = 50)
    private String purpose;

    @Column(nullable = false)
    private Long revision;

    @Column(nullable = false)
    private boolean granted;

    @Column(name = "policy_version", length = 100)
    private String policyVersion;

    @CreationTimestamp
    @Column(name = "event_at", nullable = false, updatable = false)
    private OffsetDateTime eventAt;
}
