package vn.edu.utc.hotel_booking.modules.pricing.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.pricing.entity.TaxCategory;
import vn.edu.utc.hotel_booking.modules.pricing.repository.TaxCategoryRepository;
import vn.edu.utc.hotel_booking.modules.pricing.service.TaxCategoryService;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class TaxCategoryServiceImpl implements TaxCategoryService {

    TaxCategoryRepository taxCategoryRepository;

    @Override
    public TaxCategory getEntityById(Integer id) {
        if (id == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "ID nhóm thuế không được để trống");
        }
        return taxCategoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.TAX_CATEGORY_NOT_FOUND, "Không tìm thấy nhóm thuế ID: " + id));
    }
}
