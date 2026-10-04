package vn.edu.utc.hotel_booking.modules.finance.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
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

    @Override
    @Transactional
    public List<ErrorRowDto> importCsv(MultipartFile file) {
        List<ErrorRowDto> errors = new ArrayList<>();
        List<PayrollSummary> pendingRecords = new ArrayList<>();
        Set<Integer> pendingStaffIds = new HashSet<>();

        if (file.isEmpty()) {
            errors.add(new ErrorRowDto(0, "File is empty"));
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
                    Integer staffId = Integer.parseInt(record.get(0));
                    Integer periodMonth = Integer.parseInt(record.get(1));
                    Integer periodYear = Integer.parseInt(record.get(2));
                    
                    // Allow quotes or formatting by handling them, but since we used setTrim and CSVFormat, it handles basic quotes.
                    // Just replace commas or spaces if someone put formatted numbers
                    BigDecimal baseSalary = new BigDecimal(record.get(3).replace(",", ""));
                    BigDecimal bonus = new BigDecimal(record.get(4).replace(",", ""));
                    BigDecimal deductions = new BigDecimal(record.get(5).replace(",", ""));
                    BigDecimal netSalary = new BigDecimal(record.get(6).replace(",", ""));

                    PayrollSummary summary = PayrollSummary.builder()
                            .staffId(staffId)
                            .periodMonth(periodMonth)
                            .periodYear(periodYear)
                            .baseSalary(baseSalary)
                            .bonus(bonus)
                            .deductions(deductions)
                            .netSalary(netSalary)
                            .build();

                    pendingRecords.add(summary);
                    pendingStaffIds.add(staffId);
                    
                } catch (NumberFormatException e) {
                    errors.add(new ErrorRowDto(rowNum, "Invalid number format in row"));
                } catch (Exception e) {
                    errors.add(new ErrorRowDto(rowNum, "Error parsing row: " + e.getMessage()));
                }

                // Batch save to prevent memory issues for extremely large files
                if (pendingRecords.size() >= 1000) {
                    processBatch(pendingRecords, pendingStaffIds, errors, rowNum - pendingRecords.size() + 1);
                }

                rowNum++;
            }

            if (!pendingRecords.isEmpty()) {
                processBatch(pendingRecords, pendingStaffIds, errors, rowNum - pendingRecords.size());
            }

        } catch (Exception e) {
            log.error("Error reading CSV file", e);
            errors.add(new ErrorRowDto(0, "Error reading file: " + e.getMessage()));
        }

        return errors;
    }

    private void processBatch(List<PayrollSummary> pendingRecords, Set<Integer> pendingStaffIds, List<ErrorRowDto> errors, int startRowNum) {
        // Find existing staff ids in DB
        List<Staff> existingStaffs = staffRepository.findAllById(pendingStaffIds);
        Set<Integer> validStaffIds = existingStaffs.stream()
                .map(Staff::getId)
                .collect(Collectors.toSet());
        
        List<PayrollSummary> validRecords = new ArrayList<>();
        
        int currentRowNum = startRowNum;
        for (PayrollSummary record : pendingRecords) {
            if (validStaffIds.contains(record.getStaffId())) {
                validRecords.add(record);
            } else {
                errors.add(new ErrorRowDto(currentRowNum, "Staff ID does not exist: " + record.getStaffId()));
            }
            currentRowNum++;
        }
        
        if (!validRecords.isEmpty()) {
            payrollSummaryRepository.saveAll(validRecords);
        }
        
        pendingRecords.clear();
        pendingStaffIds.clear();
    }
}
