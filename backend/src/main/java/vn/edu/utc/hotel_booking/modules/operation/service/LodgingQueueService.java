package vn.edu.utc.hotel_booking.modules.operation.service;

import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.LodgingQueueSearchDto;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.LodgingQueueResponse;

public interface LodgingQueueService {

    PageResponse<LodgingQueueResponse> search(LodgingQueueSearchDto request);

    LodgingQueueResponse retryValidate(Long queueId);
}
