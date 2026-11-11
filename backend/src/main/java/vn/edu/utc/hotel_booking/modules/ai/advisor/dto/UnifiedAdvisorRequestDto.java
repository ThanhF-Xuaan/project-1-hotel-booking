package vn.edu.utc.hotel_booking.modules.ai.advisor.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UnifiedAdvisorRequestDto {

    @NotBlank(message = "Message cannot be blank")
    @Size(max = 500, message = "Message must not exceed 500 characters")
    String message;

    UUID sessionId;

    Long expectedSessionVersion;

    @Min(value = 1, message = "Hotel ID must be positive")
    @Max(value = 32767, message = "Hotel ID must be a valid short")
    Integer policyHotelId;

    @Size(min = 2, max = 3, message = "compareRoomTypeIds must contain between 2 and 3 room type IDs")
    List<@NotNull(message = "Room type ID must not be null")
         @Min(value = 1, message = "Room type ID must be positive") Integer> compareRoomTypeIds;
}
