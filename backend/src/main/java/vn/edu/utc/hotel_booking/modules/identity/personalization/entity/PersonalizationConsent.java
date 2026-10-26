package vn.edu.utc.hotel_booking.modules.identity.personalization.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "personalization_consents")
@IdClass(PersonalizationConsentId.class)
@Getter
@Setter
@NoArgsConstructor
public class PersonalizationConsent {

    @Id
    @Column(name = "binding_id", nullable = false)
    private Long bindingId;

    @Id
    @Column(name = "hotel_id", nullable = false)
    private Short hotelId;

    @Id
    @Column(nullable = false, length = 50)
    private String purpose;

    @Column(nullable = false)
    private boolean granted;

    @Column(nullable = false)
    private Long revision;

    @Column(name = "policy_version", length = 100)
    private String policyVersion;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
