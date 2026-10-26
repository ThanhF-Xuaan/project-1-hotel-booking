package vn.edu.utc.hotel_booking.modules.booking.service.impl;

import jakarta.persistence.criteria.Predicate;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.GroupContractCancelRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.GroupContractCreateRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.GroupContractDepositRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.GroupContractSearchDto;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.GroupContractResponse;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.GroupRoomBlockResponse;
import vn.edu.utc.hotel_booking.modules.booking.entity.*;
import vn.edu.utc.hotel_booking.modules.booking.repository.GroupContractRepository;
import vn.edu.utc.hotel_booking.modules.booking.service.GroupContractService;
import vn.edu.utc.hotel_booking.modules.booking.service.GroupInquiryService;
import vn.edu.utc.hotel_booking.modules.finance.entity.MasterFolio;
import vn.edu.utc.hotel_booking.modules.finance.service.MasterFolioService;
import vn.edu.utc.hotel_booking.modules.identity.entity.Company;
import vn.edu.utc.hotel_booking.modules.identity.entity.Staff;
import vn.edu.utc.hotel_booking.modules.identity.service.CompanyService;
import vn.edu.utc.hotel_booking.modules.identity.service.StaffLookupService;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomType;
import vn.edu.utc.hotel_booking.modules.inventory.service.HotelRoomTypeService;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;
import vn.edu.utc.hotel_booking.modules.organization.service.HotelService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GroupContractServiceImpl implements GroupContractService {

    GroupContractRepository groupContractRepository;
    GroupInquiryService groupInquiryService;
    HotelService hotelService;
    HotelRoomTypeService hotelRoomTypeService;
    CompanyService companyService;
    StaffLookupService staffLookupService;
    MasterFolioService masterFolioService;

    @Override
    @Transactional
    public GroupContractResponse createContract(GroupContractCreateRequest request, Integer currentStaffId) {
        if (!request.getCheckOutDate().isAfter(request.getCheckInDate())) {
            throw new AppException(ErrorCode.INVALID_BOOKING_DATES);
        }

        if (request.getTotalRooms() < 6) {
            throw new AppException(ErrorCode.GROUP_INQUIRY_MIN_ROOMS_INVALID);
        }

        BigDecimal depositRate = request.getDepositRate() != null ? request.getDepositRate() : BigDecimal.valueOf(50.00);
        if (depositRate.compareTo(BigDecimal.valueOf(50.00)) < 0) {
            throw new AppException(ErrorCode.GROUP_CONTRACT_DEPOSIT_INSUFFICIENT);
        }

        Hotel hotel = hotelService.getEntityById(request.getHotelId());
        Company company = companyService.getEntityById(request.getCompanyId());
        Staff salesStaff = staffLookupService.getStaffEntityById(currentStaffId);
        if (salesStaff == null) {
            throw new AppException(ErrorCode.STAFF_NOT_FOUND);
        }

        long nights = ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());
        if (nights <= 0) {
            nights = 1;
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        if (request.getRoomBlocks() != null) {
            for (GroupContractCreateRequest.ContractRoomBlockItem block : request.getRoomBlocks()) {
                BigDecimal blockCost = block.getNegotiatedRatePerNight()
                        .multiply(BigDecimal.valueOf(block.getBlockedRooms()))
                        .multiply(BigDecimal.valueOf(nights));
                subtotal = subtotal.add(blockCost);
            }
        }

        BigDecimal serviceFeeRate = request.getServiceFeeRate() != null ? request.getServiceFeeRate() : BigDecimal.valueOf(5.00);
        BigDecimal serviceFeeAmount = subtotal.multiply(serviceFeeRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        BigDecimal vatBase = subtotal.add(serviceFeeAmount);
        BigDecimal vatRate = request.getVatRate() != null ? request.getVatRate() : BigDecimal.valueOf(10.00);
        BigDecimal vatAmount = vatBase.multiply(vatRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        BigDecimal totalContractValue = vatBase.add(vatAmount);
        BigDecimal depositAmount = totalContractValue.multiply(depositRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        // Giữ chỗ phòng tối đa 3 ngày để chờ chuyển khoản đặt cọc
        OffsetDateTime depositDueDate = OffsetDateTime.now().plusDays(3);
        // Hạn chốt danh sách phòng (Cutoff date: 7 ngày trước check-in)
        OffsetDateTime cutoffDate = request.getCheckInDate().minusDays(7).atStartOfDay().atOffset(ZoneOffset.ofHours(7));
        // Hạn thanh toán toàn bộ hợp đồng (3 ngày trước check-in)
        OffsetDateTime finalPaymentDueDate = request.getCheckInDate().minusDays(3).atStartOfDay().atOffset(ZoneOffset.ofHours(7));

        String contractNumber = generateContractNumber();

        GroupContract contract = GroupContract.builder()
                .contractNumber(contractNumber)
                .hotel(hotel)
                .company(company)
                .salesStaff(salesStaff)
                .groupName(request.getGroupName().trim())
                .companyTaxCode(request.getCompanyTaxCode().trim())
                .companyLegalRepresentative(request.getCompanyLegalRepresentative())
                .signerName(request.getSignerName())
                .signerTitle(request.getSignerTitle())
                .checkInDate(request.getCheckInDate())
                .checkOutDate(request.getCheckOutDate())
                .totalRooms(request.getTotalRooms())
                .totalGuests(request.getTotalGuests())
                .subtotalAmount(subtotal)
                .serviceFeeRate(serviceFeeRate)
                .serviceFeeAmount(serviceFeeAmount)
                .vatRate(vatRate)
                .vatAmount(vatAmount)
                .totalContractValue(totalContractValue)
                .depositRate(depositRate)
                .depositAmount(depositAmount)
                .depositPaidAmount(BigDecimal.ZERO)
                .depositDueDate(depositDueDate)
                .cutoffDate(cutoffDate)
                .finalPaymentDueDate(finalPaymentDueDate)
                .governingLaw(StringUtils.hasText(request.getGoverningLaw()) ? request.getGoverningLaw() : "VIETNAMESE_LAW")
                .status(GroupContractStatus.TENTATIVE)
                .specialRequests(request.getSpecialRequests())
                .build();

        List<GroupRoomBlock> roomBlocks = new ArrayList<>();
        if (request.getRoomBlocks() != null) {
            for (GroupContractCreateRequest.ContractRoomBlockItem item : request.getRoomBlocks()) {
                HotelRoomType roomType = hotelRoomTypeService.getEntityById(item.getHotelRoomTypeId());

                GroupRoomBlock block = GroupRoomBlock.builder()
                        .contract(contract)
                        .hotelRoomType(roomType)
                        .agreedRate(item.getNegotiatedRatePerNight())
                        .totalBlockedRooms(item.getBlockedRooms())
                        .allocatedRooms(0)
                        .releasedRooms(0)
                        .status(GroupRoomBlockStatus.TENTATIVE)
                        .build();

                List<GroupRoomBlockDaily> dailyList = new ArrayList<>();
                for (int i = 0; i < nights; i++) {
                    LocalDate stayDate = request.getCheckInDate().plusDays(i);
                    GroupRoomBlockDaily daily = GroupRoomBlockDaily.builder()
                            .roomBlock(block)
                            .hotelRoomType(roomType)
                            .stayDate(stayDate)
                            .blockedQuantity(item.getBlockedRooms())
                            .pickedUpQuantity(0)
                            .releasedQuantity(0)
                            .status(GroupRoomBlockDailyStatus.ACTIVE)
                            .build();
                    dailyList.add(daily);
                }
                block.setDailyBlocks(dailyList);
                roomBlocks.add(block);
            }
        }
        contract.setRoomBlocks(roomBlocks);

        GroupContract savedContract = groupContractRepository.save(contract);

        // Nếu hợp đồng được sinh từ Inquiry, cập nhật trạng thái Inquiry thành QUOTED và trỏ FK
        if (request.getInquiryId() != null) {
            groupInquiryService.markQuoted(request.getInquiryId(), savedContract);
        }

        return mapToResponse(savedContract, null);
    }

    @Override
    @Transactional
    public GroupContractResponse confirmDeposit(Long id, GroupContractDepositRequest request) {
        GroupContract contract = groupContractRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_CONTRACT_NOT_FOUND));

        if (contract.getStatus() == GroupContractStatus.DEPOSIT_PAID ||
                contract.getStatus() == GroupContractStatus.ROOMING_LIST_ATTACHED ||
                contract.getStatus() == GroupContractStatus.IN_HOUSE ||
                contract.getStatus() == GroupContractStatus.SETTLED ||
                contract.getStatus() == GroupContractStatus.COMPLETED) {
            throw new AppException(ErrorCode.GROUP_CONTRACT_ALREADY_DEPOSITED);
        }

        if (contract.getStatus() == GroupContractStatus.CANCELLED ||
                contract.getStatus() == GroupContractStatus.CANCELLED_NO_DEPOSIT) {
            throw new AppException(ErrorCode.GROUP_CONTRACT_INVALID_STATUS);
        }

        // Kiểm tra quá hạn đặt cọc 3 ngày giữ chỗ
        if (OffsetDateTime.now().isAfter(contract.getDepositDueDate())) {
            contract.setStatus(GroupContractStatus.CANCELLED_NO_DEPOSIT);
            for (GroupRoomBlock block : contract.getRoomBlocks()) {
                block.setStatus(GroupRoomBlockStatus.RELEASED);
            }
            groupContractRepository.save(contract);
            throw new AppException(ErrorCode.GROUP_CONTRACT_DEPOSIT_EXPIRED);
        }

        // Kiểm tra số tiền cọc tối thiểu >= 50% theo Điều 328 BLDS 2015
        if (request.getAmount().compareTo(contract.getDepositAmount()) < 0) {
            throw new AppException(ErrorCode.GROUP_CONTRACT_DEPOSIT_INSUFFICIENT);
        }

        contract.setDepositPaidAmount(request.getAmount());
        contract.setDepositPaidAt(OffsetDateTime.now());
        contract.setStatus(GroupContractStatus.DEPOSIT_PAID);

        // Kích hoạt Room Blocks từ TENTATIVE sang DEFINITE (chính thức trừ kho inventory của chuỗi)
        for (GroupRoomBlock block : contract.getRoomBlocks()) {
            block.setStatus(GroupRoomBlockStatus.DEFINITE);
        }

        // Cập nhật trạng thái Inquiry (nếu có) thành CONVERTED
        groupInquiryService.markConvertedByContractId(contract.getId());

        // Tự động sinh MasterFolio và khởi tạo 4 Billing Routing Rules mặc định qua finance service
        MasterFolio masterFolio = masterFolioService.createMasterFolioForContract(contract, request.getAmount());

        GroupContract updatedContract = groupContractRepository.save(contract);
        return mapToResponse(updatedContract, masterFolio);
    }

    @Override
    @Transactional
    public GroupContractResponse cancelContract(Long id, GroupContractCancelRequest request) {
        GroupContract contract = groupContractRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_CONTRACT_NOT_FOUND));

        if (contract.getStatus() == GroupContractStatus.COMPLETED ||
                contract.getStatus() == GroupContractStatus.CANCELLED ||
                contract.getStatus() == GroupContractStatus.CANCELLED_NO_DEPOSIT) {
            throw new AppException(ErrorCode.GROUP_CONTRACT_INVALID_STATUS);
        }

        contract.setStatus(contract.getDepositPaidAt() != null ? GroupContractStatus.CANCELLED : GroupContractStatus.CANCELLED_NO_DEPOSIT);
        contract.setCancellationReason(request.getCancellationReason());

        for (GroupRoomBlock block : contract.getRoomBlocks()) {
            block.setStatus(GroupRoomBlockStatus.CANCELLED);
        }

        GroupContract saved = groupContractRepository.save(contract);
        return mapToResponse(saved, null);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<GroupContractResponse> filterContracts(GroupContractSearchDto searchDto) {
        Sort sort = Sort.by(
                "ASC".equalsIgnoreCase(searchDto.getSortDirection()) ? Sort.Direction.ASC : Sort.Direction.DESC,
                StringUtils.hasText(searchDto.getSortBy()) ? searchDto.getSortBy() : "id"
        );
        Pageable pageable = PageRequest.of(searchDto.getPage(), searchDto.getPageSize(), sort);

        Specification<GroupContract> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("isDeleted")));

            if (searchDto.getHotelId() != null) {
                predicates.add(cb.equal(root.get("hotel").get("id"), searchDto.getHotelId()));
            }
            if (StringUtils.hasText(searchDto.getContractNumber())) {
                predicates.add(cb.like(cb.lower(root.get("contractNumber")), "%" + searchDto.getContractNumber().toLowerCase().trim() + "%"));
            }
            if (StringUtils.hasText(searchDto.getGroupName())) {
                predicates.add(cb.like(cb.lower(root.get("groupName")), "%" + searchDto.getGroupName().toLowerCase().trim() + "%"));
            }
            if (searchDto.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), searchDto.getStatus()));
            }
            if (searchDto.getCheckInDateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("checkInDate"), searchDto.getCheckInDateFrom()));
            }
            if (searchDto.getCheckInDateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("checkInDate"), searchDto.getCheckInDateTo()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<GroupContract> page = groupContractRepository.findAll(spec, pageable);
        return PageResponse.from(page.map(c -> mapToResponse(c, null)));
    }

    @Override
    @Transactional(readOnly = true)
    public GroupContractResponse getContractById(Long id) {
        GroupContract contract = groupContractRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_CONTRACT_NOT_FOUND));

        MasterFolio masterFolio = masterFolioService.getByContractId(id).orElse(null);
        return mapToResponse(contract, masterFolio);
    }

    private String generateContractNumber() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 4).toUpperCase();
        String code = "GC-" + datePart + "-" + randomSuffix;
        while (groupContractRepository.existsByContractNumber(code)) {
            randomSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 4).toUpperCase();
            code = "GC-" + datePart + "-" + randomSuffix;
        }
        return code;
    }

    private GroupContractResponse mapToResponse(GroupContract contract, MasterFolio masterFolio) {
        List<GroupRoomBlockResponse> blockResponses = new ArrayList<>();
        if (contract.getRoomBlocks() != null) {
            for (GroupRoomBlock b : contract.getRoomBlocks()) {
                blockResponses.add(GroupRoomBlockResponse.builder()
                        .id(b.getId())
                        .contractId(contract.getId())
                        .hotelRoomTypeId(b.getHotelRoomType() != null ? b.getHotelRoomType().getId() : null)
                        .roomTypeName(b.getHotelRoomType() != null && b.getHotelRoomType().getRoomType() != null
                                ? b.getHotelRoomType().getRoomType().getName() : null)
                        .blockedRooms(b.getTotalBlockedRooms())
                        .pickedUpRooms(b.getAllocatedRooms())
                        .negotiatedRatePerNight(b.getAgreedRate())
                        .status(b.getStatus())
                        .build());
            }
        }

        return GroupContractResponse.builder()
                .id(contract.getId())
                .contractNumber(contract.getContractNumber())
                .hotelId(contract.getHotel() != null ? contract.getHotel().getId() : null)
                .hotelName(contract.getHotel() != null ? contract.getHotel().getName() : null)
                .companyId(contract.getCompany() != null ? contract.getCompany().getId() : null)
                .companyName(contract.getCompany() != null ? contract.getCompany().getName() : null)
                .salesStaffId(contract.getSalesStaff() != null ? contract.getSalesStaff().getId() : null)
                .salesStaffName(contract.getSalesStaff() != null ? contract.getSalesStaff().getFullName() : null)
                .groupName(contract.getGroupName())
                .companyTaxCode(contract.getCompanyTaxCode())
                .companyLegalRepresentative(contract.getCompanyLegalRepresentative())
                .signerName(contract.getSignerName())
                .signerTitle(contract.getSignerTitle())
                .checkInDate(contract.getCheckInDate())
                .checkOutDate(contract.getCheckOutDate())
                .totalRooms(contract.getTotalRooms())
                .totalGuests(contract.getTotalGuests())
                .subtotalAmount(contract.getSubtotalAmount())
                .serviceFeeRate(contract.getServiceFeeRate())
                .serviceFeeAmount(contract.getServiceFeeAmount())
                .vatRate(contract.getVatRate())
                .vatAmount(contract.getVatAmount())
                .totalContractValue(contract.getTotalContractValue())
                .depositRate(contract.getDepositRate())
                .depositAmount(contract.getDepositAmount())
                .depositPaidAmount(contract.getDepositPaidAmount())
                .depositDueDate(contract.getDepositDueDate())
                .depositPaidAt(contract.getDepositPaidAt())
                .cutoffDate(contract.getCutoffDate())
                .finalPaymentDueDate(contract.getFinalPaymentDueDate())
                .lockCheckin(contract.getLockCheckin())
                .governingLaw(contract.getGoverningLaw())
                .status(contract.getStatus())
                .cancellationReason(contract.getCancellationReason())
                .specialRequests(contract.getSpecialRequests())
                .masterFolioId(masterFolio != null ? masterFolio.getId() : null)
                .masterFolioNumber(masterFolio != null ? masterFolio.getFolioNumber() : null)
                .createdAt(contract.getCreatedAt())
                .roomBlocks(blockResponses)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public GroupContract getEntityById(Long id) {
        return groupContractRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_CONTRACT_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return groupContractRepository.existsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupContract> findContractsPastCutoff(OffsetDateTime cutoff) {
        return groupContractRepository.findByStatusInAndCutoffDateBefore(
                List.of(GroupContractStatus.DEPOSIT_PAID, GroupContractStatus.ROOMING_LIST_ATTACHED),
                cutoff
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupContract> findContractsApproachingCutoff(OffsetDateTime now, OffsetDateTime threshold) {
        return groupContractRepository.findByStatusInAndCutoffDateBetween(
                List.of(GroupContractStatus.DEPOSIT_PAID),
                now,
                threshold
        );
    }

    @Override
    @Transactional
    public int cancelExpiredTentativeContracts(OffsetDateTime now) {
        List<GroupContract> expiredContracts = groupContractRepository.findByStatusAndDepositDueDateBefore(
                GroupContractStatus.TENTATIVE,
                now
        );

        if (expiredContracts.isEmpty()) {
            return 0;
        }

        for (GroupContract contract : expiredContracts) {
            contract.setStatus(GroupContractStatus.CANCELLED_NO_DEPOSIT);
            contract.setCancellationReason("Hệ thống tự động hủy do quá hạn đặt cọc 50% theo Điều 328 BLDS 2015");

            for (GroupRoomBlock block : contract.getRoomBlocks()) {
                block.setStatus(GroupRoomBlockStatus.CANCELLED);
                for (GroupRoomBlockDaily daily : block.getDailyBlocks()) {
                    daily.setStatus(GroupRoomBlockDailyStatus.EXPIRED);
                }
            }
            groupContractRepository.save(contract);
        }

        return expiredContracts.size();
    }

    @Override
    @Transactional
    public GroupContract saveEntity(GroupContract contract) {
        return groupContractRepository.save(contract);
    }
}
