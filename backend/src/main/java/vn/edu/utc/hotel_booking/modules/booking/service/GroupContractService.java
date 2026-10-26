package vn.edu.utc.hotel_booking.modules.booking.service;

import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.GroupContractCancelRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.GroupContractCreateRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.GroupContractDepositRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.GroupContractSearchDto;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.GroupContractResponse;

import vn.edu.utc.hotel_booking.modules.booking.entity.GroupContract;

import java.time.OffsetDateTime;
import java.util.List;

public interface GroupContractService {

    GroupContractResponse createContract(GroupContractCreateRequest request, Integer currentStaffId);

    PageResponse<GroupContractResponse> filterContracts(GroupContractSearchDto searchDto);

    GroupContractResponse getContractById(Long id);

    GroupContractResponse confirmDeposit(Long id, GroupContractDepositRequest request);

    GroupContractResponse cancelContract(Long id, GroupContractCancelRequest request);

    GroupContract getEntityById(Long id);

    boolean existsById(Long id);

    List<GroupContract> findContractsPastCutoff(OffsetDateTime cutoff);

    List<GroupContract> findContractsApproachingCutoff(OffsetDateTime now, OffsetDateTime threshold);

    int cancelExpiredTentativeContracts(OffsetDateTime now);

    GroupContract saveEntity(GroupContract contract);
}
