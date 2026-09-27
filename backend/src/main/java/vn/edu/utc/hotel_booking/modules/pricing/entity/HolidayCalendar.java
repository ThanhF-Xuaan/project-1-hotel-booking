package vn.edu.utc.hotel_booking.modules.pricing.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.common.entity.BaseEntity;

import java.time.LocalDate;

@Entity
@Table(name = "holiday_calendars", uniqueConstraints = {
        @UniqueConstraint(name = "uq_holiday_name_date", columnNames = {"name", "date"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HolidayCalendar extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @Column(nullable = false, length = 150)
    String name;

    @Column(nullable = false)
    LocalDate date;

    @Column(columnDefinition = "TEXT")
    String description;

    @Column(length = 50)
    @Builder.Default
    String status = "ACTIVE";
}
