package vn.edu.utc.hotel_booking.modules.operation.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.operation.entity.MasterItem;
import vn.edu.utc.hotel_booking.modules.operation.repository.MasterItemRepository;
import vn.edu.utc.hotel_booking.modules.operation.service.MasterItemService;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class MasterItemServiceImpl implements MasterItemService {

    MasterItemRepository masterItemRepository;

    @Override
    public MasterItem getEntityById(Integer id) {
        if (id == null) {
            return null;
        }
        return masterItemRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST_DATA, "Không tìm thấy danh mục chuỗi MasterItem ID: " + id));
    }

    @Override
    @Transactional
    public MasterItem saveEntity(MasterItem masterItem) {
        return masterItemRepository.save(masterItem);
    }
}
