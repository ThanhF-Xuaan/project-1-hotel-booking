package vn.edu.utc.hotel_booking.modules.booking.service;

import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.GroupInquiryCreateRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.GroupInquirySearchDto;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.GroupInquiryResponse;

import vn.edu.utc.hotel_booking.modules.booking.entity.GroupContract;

public interface GroupInquiryService {

    GroupInquiryResponse createInquiry(GroupInquiryCreateRequest request);

    PageResponse<GroupInquiryResponse> filterInquiries(GroupInquirySearchDto searchDto);

    GroupInquiryResponse getInquiryById(Long id);

    GroupInquiryResponse rejectInquiry(Long id, String reason);

    /**
     * Đánh dấu yêu cầu đặt phòng đoàn đã được lập báo giá hợp đồng dự kiến (Quotation / Tentative Contract).
     *
     * @param inquiryId ID yêu cầu đặt phòng
     * @param contract Hợp đồng đoàn liên kết
     */
    void markQuoted(Long inquiryId, GroupContract contract);

    /**
     * Đánh dấu yêu cầu đặt phòng đoàn đã chuyển đổi thành công (Khách đã đặt cọc 50%).
     *
     * @param contractId ID hợp đồng đoàn đã đặt cọc
     */
    void markConvertedByContractId(Long contractId);
}
