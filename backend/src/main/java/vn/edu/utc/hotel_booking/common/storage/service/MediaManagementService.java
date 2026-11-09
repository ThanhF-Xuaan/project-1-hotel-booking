package vn.edu.utc.hotel_booking.common.storage.service;

import org.springframework.web.multipart.MultipartFile;
import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.common.storage.domain.StorageCategory;
import vn.edu.utc.hotel_booking.common.storage.dto.CategoryMetaResponse;
import vn.edu.utc.hotel_booking.common.storage.dto.MediaItemResponse;
import vn.edu.utc.hotel_booking.common.storage.dto.MediaSearchDto;

import java.util.List;

public interface MediaManagementService {

    PageResponse<MediaItemResponse> filterMedia(MediaSearchDto criteria);

    List<MediaItemResponse> uploadMedia(StorageCategory category, Short hotelId, Long referenceId, List<MultipartFile> files, String uploaderId);

    void setPrimary(Long mediaId);

    void deleteMedia(Long mediaId);

    List<CategoryMetaResponse> getAvailableCategories();
}

