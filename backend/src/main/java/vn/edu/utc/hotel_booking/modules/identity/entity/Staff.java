package vn.edu.utc.hotel_booking.modules.identity.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.common.entity.BaseEntity;
import vn.edu.utc.hotel_booking.modules.organization.entity.Department;

import java.util.UUID;

@Entity
@Table(name = "staffs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Staff extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @Column(name = "keycloak_id", nullable = false, unique = true)
    UUID keycloakId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    Role role;

    @Column(name = "scope_type", nullable = false, length = 20)
    String scopeType; // 'CHAIN', 'REGION', 'PROPERTY'

    @Column(name = "scope_entity_id")
    Integer scopeEntityId; // hotel_id if PROPERTY, region_id if REGION, null if CHAIN

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    Department department;

    @Column(nullable = false, unique = true, length = 100)
    String username;

    @Column(unique = true, length = 255)
    String email;

    @Column(unique = true, length = 20)
    String phone;

    @Column(name = "first_name", nullable = false, length = 100)
    String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    String lastName;

    @Column(name = "full_name", nullable = false, length = 250)
    String fullName;

    @Column(length = 50)
    @Builder.Default
    String status = "ACTIVE";
}
