package vn.edu.utc.hotel_booking.modules.identity.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.common.entity.BaseEntity;

@Entity
@Table(name = "permissions", uniqueConstraints = {
        @UniqueConstraint(name = "uq_permission_action_resource", columnNames = {"action", "resource"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Permission extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Short id;

    @Column(nullable = false, length = 50)
    String action;

    @Column(nullable = false, length = 100)
    String resource;

    @Column(length = 50)
    @Builder.Default
    String status = "ACTIVE";
}
