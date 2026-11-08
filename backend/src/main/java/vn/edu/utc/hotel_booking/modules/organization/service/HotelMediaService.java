package vn.edu.utc.hotel_booking.modules.organization.service;

import org.springframework.web.multipart.MultipartFile;
import vn.edu.utc.hotel_booking.modules.organization.dto.response.HotelImageResponse;

import java.util.List;

public interface HotelMediaService {
    List<HotelImageResponse> uploadImages(Short hotelId, List<MultipartFile> files, String uploaderId);
    List<HotelImageResponse> getHotelImages(Short hotelId);
    void setPrimaryImage(Short hotelId, Long imageId);
    void deleteImage(Short hotelId, Long imageId);
}

