package vn.edu.utc.hotel_booking.modules.booking.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.utc.hotel_booking.common.storage.domain.StorageCategory;
import vn.edu.utc.hotel_booking.common.storage.dto.MediaUploadResponse;
import vn.edu.utc.hotel_booking.common.storage.service.InternalMediaDomainService;
import vn.edu.utc.hotel_booking.modules.booking.service.ContractStorageService;

import java.io.IOException;

/**
 * Triển khai ContractStorageService tích hợp trực tiếp với S3-Compatible Object Storage và Database metadata.
 */
@Slf4j
@Service
@Primary
@RequiredArgsConstructor
@ConditionalOnProperty(name = "hotel.storage.type", havingValue = "s3", matchIfMissing = true)
public class ObjectStorageContractStorageService implements ContractStorageService {

    private final InternalMediaDomainService internalMediaDomainService;

    @Override
    public String storeContractFile(MultipartFile file, Short hotelId, Long contractId) throws IOException {
        log.info("Lưu trữ tệp hợp đồng đoàn qua S3-Compatible Storage: hotelId={}, contractId={}", hotelId, contractId);
        MediaUploadResponse response = internalMediaDomainService.upload(
                file,
                StorageCategory.SECURE_CONTRACT,
                hotelId,
                contractId,
                "contract-service"
        );
        return response.getObjectKey();
    }

    @Override
    public byte[] loadContractFile(String relativeFilePath) throws IOException {
        log.debug("Đọc tệp hợp đồng từ S3 Storage: key={}", relativeFilePath);
        return internalMediaDomainService.loadMediaBytes(relativeFilePath);
    }

    @Override
    public void deleteContractFile(String relativeFilePath) throws IOException {
        log.info("Xóa tệp hợp đồng từ S3 Storage: key={}", relativeFilePath);
        internalMediaDomainService.deleteMedia(relativeFilePath);
    }
}
