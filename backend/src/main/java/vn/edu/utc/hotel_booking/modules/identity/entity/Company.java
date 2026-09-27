package vn.edu.utc.hotel_booking.modules.identity.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.common.entity.BaseEntity;

@Entity
@Table(name = "companies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Company extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, length = 255)
    String name;

    @Column(name = "tax_code", unique = true, length = 50)
    String taxCode;

    @Column(columnDefinition = "TEXT")
    String address;

    @Column(name = "contact_name", length = 100)
    String contactName;

    @Column(name = "contact_phone", length = 20)
    String contactPhone;

    @Column(name = "contact_email", length = 150)
    String contactEmail;

    @Column(length = 50)
    @Builder.Default
    String status = "ACTIVE";
}
