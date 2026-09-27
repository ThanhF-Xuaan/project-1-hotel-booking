package vn.edu.utc.hotel_booking.modules.identity.mapper;

import org.mapstruct.*;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.CompanyCreateRequest;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.CompanyUpdateRequest;
import vn.edu.utc.hotel_booking.modules.identity.dto.response.CompanyResponse;
import vn.edu.utc.hotel_booking.modules.identity.entity.Company;

import java.util.List;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface CompanyMapper {

    CompanyResponse toResponse(Company entity);

    List<CompanyResponse> toResponseList(List<Company> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Company toEntity(CompanyCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(@MappingTarget Company entity, CompanyUpdateRequest request);
}
