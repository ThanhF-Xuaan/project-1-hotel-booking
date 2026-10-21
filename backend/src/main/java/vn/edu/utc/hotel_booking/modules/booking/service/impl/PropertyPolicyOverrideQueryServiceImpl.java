package vn.edu.utc.hotel_booking.modules.booking.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.hotel_booking.modules.booking.repository.PropertyPolicyOverrideRepository;
import vn.edu.utc.hotel_booking.modules.booking.service.PropertyPolicyOverrideQueryService;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class PropertyPolicyOverrideQueryServiceImpl implements PropertyPolicyOverrideQueryService {

    PropertyPolicyOverrideRepository propertyPolicyOverrideRepository;

    @Override
    public boolean existsByCancellationPolicyId(Integer cancellationPolicyId) {
        if (cancellationPolicyId == null) {
            return false;
        }
        return propertyPolicyOverrideRepository.existsByCancellationPolicyIdAndIsDeletedFalse(cancellationPolicyId);
    }

    @Override
    public boolean existsByNoShowPolicyId(Integer noShowPolicyId) {
        if (noShowPolicyId == null) {
            return false;
        }
        return propertyPolicyOverrideRepository.existsByNoShowPolicyIdAndIsDeletedFalse(noShowPolicyId);
    }
}
