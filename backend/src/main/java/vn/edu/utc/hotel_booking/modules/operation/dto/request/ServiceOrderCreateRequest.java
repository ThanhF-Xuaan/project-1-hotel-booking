package vn.edu.utc.hotel_booking.modules.operation.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ServiceOrderCreateRequest {

    @NotNull(message = "ID đơn đặt phòng không được để trống")
    Long bookingId;

    @NotNull(message = "ID phòng vật lý không được để trống")
    Integer roomInstanceId;

    @NotEmpty(message = "Danh sách món ăn/dịch vụ không được để trống")
    @Valid
    @Builder.Default
    List<OrderItemRequest> items = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class OrderItemRequest {
        @NotNull(message = "ID món/dịch vụ không được để trống")
        Integer menuId;

        @NotNull(message = "Số lượng không được để trống")
        @Min(value = 1, message = "Số lượng tối thiểu là 1")
        @Builder.Default
        Integer quantity = 1;
    }
}
