package vn.edu.utc.hotel_booking.modules.finance.service;

import org.springframework.web.multipart.MultipartFile;
import vn.edu.utc.hotel_booking.modules.finance.dto.response.ErrorRowDto;

import java.util.List;

public interface PayrollSummaryService {
    List<ErrorRowDto> importCsv(MultipartFile file);
}
