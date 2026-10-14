package vn.edu.utc.hotel_booking.modules.ai.advisor.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StayIntervalDto {
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate checkInDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate checkOutDate;

    @Builder.Default
    int roomsCount = 1;
}
