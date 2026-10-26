package vn.edu.utc.hotel_booking.modules.identity.personalization.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class PersonalizationConsentId implements Serializable {
    private Long bindingId;
    private Short hotelId;
    private String purpose;
}
