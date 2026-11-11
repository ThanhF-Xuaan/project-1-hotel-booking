package vn.edu.utc.hotel_booking.modules.inventory.service;

import org.springframework.web.multipart.MultipartFile;
import vn.edu.utc.hotel_booking.modules.organization.dto.response.HotelImageResponse;

import java.util.List;

public interface HotelRoomTypeMediaService {

    List<HotelImageResponse> getImages(Integer hotelRoomTypeId);

    List<HotelImageResponse> uploadImages(Integer hotelRoomTypeId, List<MultipartFile> files, String uploaderId);

    void setPrimaryImage(Integer hotelRoomTypeId, Long imageId);

    void deleteImage(Integer hotelRoomTypeId, Long imageId);
}

