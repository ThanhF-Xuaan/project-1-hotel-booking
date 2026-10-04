package vn.edu.utc.hotel_booking.modules.finance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorRowDto {
    private Integer row;
    private String error;
}
