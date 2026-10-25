package vn.edu.utc.hotel_booking.modules.finance.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.common.util.SecurityUtils;
import vn.edu.utc.hotel_booking.modules.finance.dto.response.ErrorRowDto;
import vn.edu.utc.hotel_booking.modules.finance.entity.PayrollSummary;
import vn.edu.utc.hotel_booking.modules.finance.repository.PayrollSummaryRepository;
import vn.edu.utc.hotel_booking.modules.finance.service.PayrollSummaryService;
import vn.edu.utc.hotel_booking.modules.identity.entity.Staff;
import vn.edu.utc.hotel_booking.modules.identity.repository.StaffRepository;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayrollSummaryServiceImpl implements PayrollSummaryService {

    private final PayrollSummaryRepository payrollSummaryRepository;
    private final StaffRepository staffRepository;

    private record PayrollRowHolder(int physicalRowNum, PayrollSummary summary) {}

    @Override
    @Transactional
    public List<ErrorRowDto> importCsv(MultipartFile file) {
        List<ErrorRowDto> errors = new ArrayList<>();
        List<PayrollRowHolder> pendingRecords = new ArrayList<>();
        Set<Integer> pendingStaffIds = new HashSet<>();
        Set<String> seenStaffPeriodInCsv = new HashSet<>();

        if (file == null || file.isEmpty()) {
            errors.add(new ErrorRowDto(0, "File is empty"));
            return errors;
        }

        Staff currentStaff = resolveCurrentStaff();
        boolean isChainAdmin = SecurityUtils.hasRole("ROLE_CHAIN_ADMIN");
        Integer allowedHotelId = (currentStaff != null && "PROPERTY".equalsIgnoreCase(currentStaff.getScopeType()))
                ? currentStaff.getScopeEntityId()
                : null;

        if (!isChainAdmin && allowedHotelId == null) {
            errors.add(new ErrorRowDto(0, "Không xác định được phạm vi khách sạn được phép import bảng lương"));
            return errors;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).setTrim(true).build())) {

            int rowNum = 2; // Row 1 is header

            for (CSVRecord record : csvParser) {
                if (record.size() < 7) {
                    errors.add(new ErrorRowDto(rowNum, "Missing columns. Expected at least 7"));
                    rowNum++;
                    continue;
                }

                try {
                    Integer staffId = Integer.parseInt(record.get(0).trim());
                    Integer periodMonth = Integer.parseInt(record.get(1).trim());
                    Integer periodYear = Integer.parseInt(record.get(2).trim());

                    if (periodMonth < 1 || periodMonth > 12) {
                        errors.add(new ErrorRowDto(rowNum, "Invalid period month: " + periodMonth + ". Must be between 1 and 12"));
                        rowNum++;
                        continue;
                    }

                    if (periodYear < 2000 || periodYear > 2100) {
                        errors.add(new ErrorRowDto(rowNum, "Invalid period year: " + periodYear + ". Must be between 2000 and 2100"));
                        rowNum++;
                        continue;
                    }

                    BigDecimal baseSalary = new BigDecimal(record.get(3).replace(",", "").trim());
                    BigDecimal bonus = new BigDecimal(record.get(4).replace(",", "").trim());
                    BigDecimal deductions = new BigDecimal(record.get(5).replace(",", "").trim());
                    BigDecimal netSalary = new BigDecimal(record.get(6).replace(",", "").trim());

                    if (baseSalary.compareTo(BigDecimal.ZERO) < 0 || bonus.compareTo(BigDecimal.ZERO) < 0 || deductions.compareTo(BigDecimal.ZERO) < 0) {
                        errors.add(new ErrorRowDto(rowNum, "Salary components (base, bonus, deductions) must be non-negative"));
                        rowNum++;
                        continue;
                    }

                    BigDecimal expectedNet = baseSalary.add(bonus).subtract(deductions);
                    if (netSalary.compareTo(expectedNet) != 0) {
                        errors.add(new ErrorRowDto(rowNum, "Net salary mismatch: provided " + netSalary + " but expected " + expectedNet + " (baseSalary + bonus - deductions)"));
                        rowNum++;
                        continue;
                    }

                    String staffPeriodKey = staffId + "-" + periodMonth + "-" + periodYear;
                    if (!seenStaffPeriodInCsv.add(staffPeriodKey)) {
                        errors.add(new ErrorRowDto(rowNum, "Duplicate staff ID " + staffId + " for period " + periodMonth + "/" + periodYear + " within import file"));
                        rowNum++;
                        continue;
                    }

                    PayrollSummary summary = PayrollSummary.builder()
                            .staffId(staffId)
                            .periodMonth(periodMonth)
                            .periodYear(periodYear)
                            .baseSalary(baseSalary)
                            .bonus(bonus)
                            .deductions(deductions)
                            .netSalary(netSalary)
                            .build();

                    pendingRecords.add(new PayrollRowHolder(rowNum, summary));
                    pendingStaffIds.add(staffId);

                } catch (NumberFormatException e) {
                    errors.add(new ErrorRowDto(rowNum, "Invalid number format in row"));
                } catch (Exception e) {
                    errors.add(new ErrorRowDto(rowNum, "Error parsing row: " + e.getMessage()));
                }

                // Batch save to prevent memory issues for extremely large files
                if (pendingRecords.size() >= 1000) {
                    processBatch(pendingRecords, pendingStaffIds, errors, isChainAdmin, allowedHotelId);
                }

                rowNum++;
            }

            if (!pendingRecords.isEmpty()) {
                processBatch(pendingRecords, pendingStaffIds, errors, isChainAdmin, allowedHotelId);
            }

        } catch (Exception e) {
            log.error("Error reading CSV file", e);
            errors.add(new ErrorRowDto(0, "Error reading file: " + e.getMessage()));
        }

        return errors;
    }

    private void processBatch(List<PayrollRowHolder> pendingRecords,
                              Set<Integer> pendingStaffIds,
                              List<ErrorRowDto> errors,
                              boolean isChainAdmin,
                              Integer allowedHotelId) {
        // Find existing staff in DB
        Map<Integer, Staff> staffMap = staffRepository.findAllById(pendingStaffIds).stream()
                .collect(Collectors.toMap(Staff::getId, s -> s));

        List<PayrollSummary> validRecordsToSave = new ArrayList<>();

        for (PayrollRowHolder holder : pendingRecords) {
            int rowNum = holder.physicalRowNum();
            PayrollSummary record = holder.summary();
            Integer staffId = record.getStaffId();

            if (!staffMap.containsKey(staffId)) {
                errors.add(new ErrorRowDto(rowNum, "Staff ID does not exist: " + staffId));
                continue;
            }

            Staff targetStaff = staffMap.get(staffId);

            // Scope check: Property manager can only import payroll for staff belonging to their assigned property
            if (!isChainAdmin && allowedHotelId != null) {
                if (!"PROPERTY".equalsIgnoreCase(targetStaff.getScopeType())
                        || targetStaff.getScopeEntityId() == null
                        || !targetStaff.getScopeEntityId().equals(allowedHotelId)) {
                    errors.add(new ErrorRowDto(rowNum, "Staff ID " + staffId + " does not belong to your authorized hotel (hotel ID: " + allowedHotelId + ")"));
                    continue;
                }
            }

            // Database idempotency: if record for staff and period already exists, update in-place rather than duplicating
            Optional<PayrollSummary> existingOpt = payrollSummaryRepository
                    .findFirstByStaffIdAndPeriodMonthAndPeriodYearAndIsDeletedFalseOrderByIdDesc(
                            staffId, record.getPeriodMonth(), record.getPeriodYear());

            if (existingOpt.isPresent()) {
                PayrollSummary existing = existingOpt.get();
                existing.setBaseSalary(record.getBaseSalary());
                existing.setBonus(record.getBonus());
                existing.setDeductions(record.getDeductions());
                existing.setNetSalary(record.getNetSalary());
                validRecordsToSave.add(existing);
            } else {
                validRecordsToSave.add(record);
            }
        }

        if (!validRecordsToSave.isEmpty()) {
            payrollSummaryRepository.saveAll(validRecordsToSave);
        }

        pendingRecords.clear();
        pendingStaffIds.clear();
    }

    private Staff resolveCurrentStaff() {
        return SecurityUtils.getCurrentUserKeycloakId()
                .flatMap(staffRepository::findByKeycloakIdAndIsDeletedFalse)
                .or(() -> SecurityUtils.getCurrentUsername()
                        .flatMap(staffRepository::findByUsernameAndIsDeletedFalse))
                .orElse(null);
    }
}
