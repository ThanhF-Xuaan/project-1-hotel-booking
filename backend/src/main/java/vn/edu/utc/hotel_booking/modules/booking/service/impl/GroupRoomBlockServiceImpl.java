package vn.edu.utc.hotel_booking.modules.booking.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.GroupRoomBlockDailyResponse;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.GroupRoomBlockResponse;
import vn.edu.utc.hotel_booking.modules.booking.entity.*;
import vn.edu.utc.hotel_booking.modules.booking.repository.GroupRoomBlockDailyRepository;
import vn.edu.utc.hotel_booking.modules.booking.repository.GroupRoomBlockRepository;
import vn.edu.utc.hotel_booking.modules.booking.service.GroupContractService;
import vn.edu.utc.hotel_booking.modules.booking.service.GroupRoomBlockService;
import vn.edu.utc.hotel_booking.modules.identity.service.AuditLogService;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GroupRoomBlockServiceImpl implements GroupRoomBlockService {

    GroupRoomBlockRepository groupRoomBlockRepository;
    GroupRoomBlockDailyRepository groupRoomBlockDailyRepository;
    GroupContractService groupContractService;
    AuditLogService auditLogService;

    @Override
    @Transactional(readOnly = true)
    public int getActiveBlockedRooms(Integer hotelRoomTypeId, LocalDate stayDate) {
        if (hotelRoomTypeId == null || stayDate == null) {
            return 0;
        }
        Integer count = groupRoomBlockDailyRepository.sumActiveBlockedRooms(hotelRoomTypeId, stayDate);
        return count != null ? count : 0;
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupRoomBlockResponse> getBlocksByContractId(Long contractId) {
        if (!groupContractService.existsById(contractId)) {
            throw new AppException(ErrorCode.GROUP_CONTRACT_NOT_FOUND);
        }

        List<GroupRoomBlock> blocks = groupRoomBlockRepository.findByContractId(contractId);
        List<GroupRoomBlockResponse> responses = new ArrayList<>();

        for (GroupRoomBlock block : blocks) {
            responses.add(mapToBlockResponse(block));
        }

        return responses;
    }

    @Override
    @Transactional
    public int processCutoffAutoRelease() {
        OffsetDateTime now = OffsetDateTime.now();
        List<GroupContract> contracts = groupContractService.findContractsPastCutoff(now);

        if (contracts.isEmpty()) {
            return 0;
        }

        int totalReleasedOverall = 0;

        for (GroupContract contract : contracts) {
            int totalReleasedForContract = 0;

            for (GroupRoomBlock block : contract.getRoomBlocks()) {
                if (block.getStatus() == GroupRoomBlockStatus.DEFINITE) {
                    int releasedForBlock = 0;

                    for (GroupRoomBlockDaily daily : block.getDailyBlocks()) {
                        if (daily.getStatus() == GroupRoomBlockDailyStatus.ACTIVE) {
                            int unpickedUp = daily.getBlockedQuantity() - daily.getPickedUpQuantity();
                            if (unpickedUp > 0) {
                                daily.setReleasedQuantity(daily.getReleasedQuantity() + unpickedUp);
                                daily.setBlockedQuantity(daily.getPickedUpQuantity());
                                if (daily.getPickedUpQuantity() == 0) {
                                    daily.setStatus(GroupRoomBlockDailyStatus.RELEASED);
                                }
                                groupRoomBlockDailyRepository.save(daily);
                                releasedForBlock += unpickedUp;
                            }
                        }
                    }

                    if (releasedForBlock > 0) {
                        block.setReleasedRooms(block.getReleasedRooms() + releasedForBlock);
                        if (block.getAllocatedRooms() == 0) {
                            block.setStatus(GroupRoomBlockStatus.RELEASED);
                        }
                        groupRoomBlockRepository.save(block);
                        totalReleasedForContract += releasedForBlock;
                    }
                }
            }

            if (totalReleasedForContract > 0) {
                groupContractService.saveEntity(contract);
                totalReleasedOverall += totalReleasedForContract;

                // Ghi vết nhật ký kiểm toán bất biến theo Luật Kế toán 2015 & Luật Giao dịch điện tử 2023
                auditLogService.recordLog(
                        null,
                        "GROUP_CUTOFF_AUTO_RELEASE",
                        "GROUP_CONTRACT",
                        String.valueOf(contract.getId()),
                        "Active Blocked Rooms",
                        "Auto-released " + totalReleasedForContract + " unpicked rooms back to Public Inventory due to Cut-off Date expiry",
                        "SYSTEM_SCHEDULER",
                        "GroupCutoffScheduler"
                );

                log.info("Hợp đồng {} (ID: {}) đã tự động giải phóng {} phòng dôi dư về Public Inventory",
                        contract.getContractNumber(), contract.getId(), totalReleasedForContract);
            }
        }

        return totalReleasedOverall;
    }

    @Override
    @Transactional
    public int processDepositExpiredAutoCancel() {
        OffsetDateTime now = OffsetDateTime.now();
        int cancelledCount = groupContractService.cancelExpiredTentativeContracts(now);
        if (cancelledCount > 0) {
            auditLogService.recordLog(
                    null,
                    "GROUP_CONTRACT_DEPOSIT_EXPIRED_AUTO_CANCEL",
                    "GROUP_CONTRACT",
                    "BATCH",
                    "TENTATIVE",
                    "CANCELLED_NO_DEPOSIT",
                    "SYSTEM_SCHEDULER",
                    "GroupCutoffScheduler"
            );
            log.info("Đã tự động hủy {} hợp đồng đoàn do quá hạn đặt cọc 3 ngày giữ chỗ", cancelledCount);
        }
        return cancelledCount;
    }

    @Override
    @Transactional(readOnly = true)
    public int processCutoffReminders() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime threshold = now.plusHours(48);

        List<GroupContract> approachingContracts = groupContractService.findContractsApproachingCutoff(now, threshold);

        for (GroupContract contract : approachingContracts) {
            String staffInfo = contract.getSalesStaff() != null ? contract.getSalesStaff().getFullName() : "Chưa phân công";
            log.warn("[CẢNH BÁO HẠN CHÓT] Hợp đồng {} (Đoàn: '{}') sẽ đến mốc Cut-off Date vào lúc {}. Nhân viên phụ trách: {}",
                    contract.getContractNumber(), contract.getGroupName(), contract.getCutoffDate(), staffInfo);
        }

        return approachingContracts.size();
    }

    @Override
    @Transactional
    public GroupRoomBlockResponse releaseUnpickedUpRoomsManually(Long contractId, Long blockId) {
        GroupContract contract = groupContractService.getEntityById(contractId);

        GroupRoomBlock block = groupRoomBlockRepository.findById(blockId)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_ROOM_BLOCK_NOT_FOUND));

        if (!block.getContract().getId().equals(contractId)) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Room block không thuộc về hợp đồng này");
        }

        int releasedForBlock = 0;
        for (GroupRoomBlockDaily daily : block.getDailyBlocks()) {
            if (daily.getStatus() == GroupRoomBlockDailyStatus.ACTIVE) {
                int unpickedUp = daily.getBlockedQuantity() - daily.getPickedUpQuantity();
                if (unpickedUp > 0) {
                    daily.setReleasedQuantity(daily.getReleasedQuantity() + unpickedUp);
                    daily.setBlockedQuantity(daily.getPickedUpQuantity());
                    if (daily.getPickedUpQuantity() == 0) {
                        daily.setStatus(GroupRoomBlockDailyStatus.RELEASED);
                    }
                    groupRoomBlockDailyRepository.save(daily);
                    releasedForBlock += unpickedUp;
                }
            }
        }

        if (releasedForBlock > 0) {
            block.setReleasedRooms(block.getReleasedRooms() + releasedForBlock);
            if (block.getAllocatedRooms() == 0) {
                block.setStatus(GroupRoomBlockStatus.RELEASED);
            }
            groupRoomBlockRepository.save(block);

            auditLogService.recordLog(
                    null,
                    "GROUP_CUTOFF_MANUAL_RELEASE",
                    "GROUP_ROOM_BLOCK",
                    String.valueOf(block.getId()),
                    "Active Blocked Rooms",
                    "Manually released " + releasedForBlock + " unpicked rooms back to Public Inventory",
                    "MANUAL_STAFF_ACTION",
                    "ManagementConsole"
            );
        }

        return mapToBlockResponse(block);
    }

    private GroupRoomBlockResponse mapToBlockResponse(GroupRoomBlock block) {
        List<GroupRoomBlockDailyResponse> dailyResponses = new ArrayList<>();
        if (block.getDailyBlocks() != null) {
            for (GroupRoomBlockDaily daily : block.getDailyBlocks()) {
                dailyResponses.add(GroupRoomBlockDailyResponse.builder()
                        .id(daily.getId())
                        .roomBlockId(block.getId())
                        .hotelRoomTypeId(daily.getHotelRoomType() != null ? daily.getHotelRoomType().getId() : null)
                        .roomTypeName(daily.getHotelRoomType() != null && daily.getHotelRoomType().getRoomType() != null
                                ? daily.getHotelRoomType().getRoomType().getName() : null)
                        .stayDate(daily.getStayDate())
                        .blockedQuantity(daily.getBlockedQuantity())
                        .pickedUpQuantity(daily.getPickedUpQuantity())
                        .releasedQuantity(daily.getReleasedQuantity())
                        .status(daily.getStatus())
                        .createdAt(daily.getCreatedAt())
                        .build());
            }
        }

        return GroupRoomBlockResponse.builder()
                .id(block.getId())
                .contractId(block.getContract() != null ? block.getContract().getId() : null)
                .hotelRoomTypeId(block.getHotelRoomType() != null ? block.getHotelRoomType().getId() : null)
                .roomTypeName(block.getHotelRoomType() != null && block.getHotelRoomType().getRoomType() != null
                        ? block.getHotelRoomType().getRoomType().getName() : null)
                .blockedRooms(block.getTotalBlockedRooms())
                .pickedUpRooms(block.getAllocatedRooms())
                .releasedRooms(block.getReleasedRooms())
                .negotiatedRatePerNight(block.getAgreedRate())
                .status(block.getStatus())
                .dailyBlocks(dailyResponses)
                .build();
    }
}
