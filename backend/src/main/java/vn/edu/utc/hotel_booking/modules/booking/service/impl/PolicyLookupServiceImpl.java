package vn.edu.utc.hotel_booking.modules.booking.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.hotel_booking.modules.booking.entity.CancellationPolicy;
import vn.edu.utc.hotel_booking.modules.booking.entity.NoShowPolicy;
import vn.edu.utc.hotel_booking.modules.booking.service.CancellationPolicyService;
import vn.edu.utc.hotel_booking.modules.booking.service.NoShowPolicyService;
import vn.edu.utc.hotel_booking.modules.booking.service.PolicyLookupService;
import vn.edu.utc.hotel_booking.modules.booking.service.PropertyPolicyOverrideQueryService;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class PolicyLookupServiceImpl implements PolicyLookupService {

    PropertyPolicyOverrideQueryService propertyPolicyOverrideQueryService;
    CancellationPolicyService cancellationPolicyService;
    NoShowPolicyService noShowPolicyService;

    @Override
    public boolean existsOverrideByCancellationPolicyId(Integer cancellationPolicyId) {
        if (cancellationPolicyId == null) {
            return false;
        }
        return propertyPolicyOverrideQueryService.existsByCancellationPolicyId(cancellationPolicyId);
    }

    @Override
    public boolean existsOverrideByNoShowPolicyId(Integer noShowPolicyId) {
        if (noShowPolicyId == null) {
            return false;
        }
        return propertyPolicyOverrideQueryService.existsByNoShowPolicyId(noShowPolicyId);
    }

    @Override
    public CancellationPolicy getCancellationPolicyEntityById(Integer id) {
        return cancellationPolicyService.getEntityById(id);
    }

    @Override
    public NoShowPolicy getNoShowPolicyEntityById(Integer id) {
        return noShowPolicyService.getEntityById(id);
    }
}
