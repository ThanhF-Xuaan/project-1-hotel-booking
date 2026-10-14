package vn.edu.utc.hotel_booking.modules.ai.advisor.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CriteriaPatchDto {
    Integer hotelId;
    String locationKeyword;

    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate checkInDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate checkOutDate;

    Integer adults;
    Integer children;
    Integer infants;
    Integer extraBeds;
    Integer roomsCount;
    List<String> requiredFeatureCodes;
    List<String> preferredFeatureCodes;
    BudgetDto budget;
}
