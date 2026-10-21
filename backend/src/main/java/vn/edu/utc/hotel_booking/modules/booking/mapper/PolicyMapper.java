package vn.edu.utc.hotel_booking.modules.booking.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;
import vn.edu.utc.hotel_booking.modules.booking.dto.policy.CancellationTierDto;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.CancellationPolicyCreateRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.NoShowPolicyCreateRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.CancellationPolicyResponse;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.NoShowPolicyResponse;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.PropertyPolicyOverrideResponse;
import vn.edu.utc.hotel_booking.modules.booking.entity.CancellationPolicy;
import vn.edu.utc.hotel_booking.modules.booking.entity.NoShowPolicy;
import vn.edu.utc.hotel_booking.modules.booking.entity.PropertyPolicyOverride;

import java.util.Collections;
import java.util.List;

@Mapper(componentModel = "spring", builder = @org.mapstruct.Builder(disableBuilder = true))
public abstract class PolicyMapper {

    @Autowired
    protected ObjectMapper objectMapper;

    @Mapping(target = "tiers", source = "tiers", qualifiedByName = "jsonToTiers")
    public abstract CancellationPolicyResponse toResponse(CancellationPolicy entity);

    @Mapping(target = "tiers", source = "tiers", qualifiedByName = "tiersToJson")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    public abstract CancellationPolicy toEntity(CancellationPolicyCreateRequest request);

    public abstract NoShowPolicyResponse toResponse(NoShowPolicy entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    public abstract NoShowPolicy toEntity(NoShowPolicyCreateRequest request);

    @Mapping(target = "hotelId", source = "hotel.id")
    @Mapping(target = "hotelName", source = "hotel.name")
    @Mapping(target = "hotelRoomTypeId", source = "hotelRoomType.id")
    @Mapping(target = "roomTypeName", source = "hotelRoomType.roomType.name")
    @Mapping(target = "cancellationPolicyId", source = "cancellationPolicy.id")
    @Mapping(target = "cancellationPolicyCode", source = "cancellationPolicy.policyCode")
    @Mapping(target = "cancellationPolicyName", source = "cancellationPolicy.name")
    @Mapping(target = "noShowPolicyId", source = "noShowPolicy.id")
    @Mapping(target = "noShowPolicyCode", source = "noShowPolicy.policyCode")
    @Mapping(target = "noShowPolicyName", source = "noShowPolicy.name")
    public abstract PropertyPolicyOverrideResponse toResponse(PropertyPolicyOverride entity);

    @Named("jsonToTiers")
    public List<CancellationTierDto> jsonToTiers(String json) {
        if (json == null || json.isBlank() || "[]".equals(json.trim())) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<CancellationTierDto>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Named("tiersToJson")
    public String tiersToJson(List<CancellationTierDto> tiers) {
        if (tiers == null || tiers.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(tiers);
        } catch (Exception e) {
            return "[]";
        }
    }
}
