package vn.edu.utc.hotel_booking.modules.booking.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.booking.entity.GroupContractStatus;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GroupContractSearchDto {

    Short hotelId;
    String keyword;
    String contractNumber;
    String groupName;
    String companyName;
    GroupContractStatus status;
    LocalDate checkInDateFrom;
    LocalDate checkInDateTo;

    @Builder.Default
    Integer page = 0;

    @Builder.Default
    Integer pageSize = 10;

    @Builder.Default
    String sortBy = "id";

    @Builder.Default
    String sortDirection = "DESC";

    public void setSize(Integer size) {
        if (size != null) {
            setPageSize(size);
        }
    }
}
