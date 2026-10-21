package vn.edu.utc.hotel_booking.modules.booking.service;

import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.PropertyPolicyOverrideCreateRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.PropertyPolicyOverrideSearchDto;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.PropertyPolicyOverrideUpdateRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.PropertyPolicyOverrideResponse;
import vn.edu.utc.hotel_booking.modules.booking.entity.PropertyPolicyOverride;

import java.util.List;
import java.util.Optional;

public interface PropertyPolicyOverrideService {

    PropertyPolicyOverrideResponse create(PropertyPolicyOverrideCreateRequest request);

    PropertyPolicyOverrideResponse update(Integer id, PropertyPolicyOverrideUpdateRequest request);

    void delete(List<Integer> ids);

    PropertyPolicyOverrideResponse getById(Integer id);

    PropertyPolicyOverride getEntityById(Integer id);

    Optional<PropertyPolicyOverride> findActiveOverride(Short hotelId, Integer hotelRoomTypeId);

    PageResponse<PropertyPolicyOverrideResponse> filter(PropertyPolicyOverrideSearchDto searchDto);

    /**
     * Kiểm tra xem có cấu hình override nào đang sử dụng chính sách hủy này hay không
     *
     * @param cancellationPolicyId ID chính sách hủy
     * @return true nếu đang được sử dụng trong override chưa bị xóa
     */
    boolean existsByCancellationPolicyId(Integer cancellationPolicyId);

    /**
     * Kiểm tra xem có cấu hình override nào đang sử dụng chính sách vắng mặt này hay không
     *
     * @param noShowPolicyId ID chính sách vắng mặt
     * @return true nếu đang được sử dụng trong override chưa bị xóa
     */
    boolean existsByNoShowPolicyId(Integer noShowPolicyId);
}
