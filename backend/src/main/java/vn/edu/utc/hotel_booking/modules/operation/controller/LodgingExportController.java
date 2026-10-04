package vn.edu.utc.hotel_booking.modules.operation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.LodgingExportRequest;
import vn.edu.utc.hotel_booking.modules.operation.service.LodgingExportService;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/lodging-export")
@RequiredArgsConstructor
@Tag(name = "Lodging Export API", description = "Xuất file khai báo lưu trú gửi Bộ Công An (BCA)")
public class LodgingExportController {

    private final LodgingExportService lodgingExportService;

    @PostMapping("/download")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER', 'RECEPTIONIST')")
    @Operation(summary = "Xuất file Excel khai báo lưu trú BCA theo chuẩn Mẫu CT14 / Cổng BCA",
            description = "Hỗ trợ tách 2 luồng (Công dân Việt Nam vs Khách nước ngoài), tự động chốt các bản ghi đã xuất sang trạng thái EXPORTED")
    public ResponseEntity<ByteArrayResource> downloadBcaReport(@Valid @RequestBody LodgingExportRequest request) {
        byte[] excelBytes = lodgingExportService.exportBcaLodgingReport(request);
        ByteArrayResource resource = new ByteArrayResource(excelBytes);

        String categoryPrefix = (request.getCategory() != null) ? request.getCategory().name() : "ALL";
        String rawFileName = "BCA_Lodging_" + categoryPrefix + "_" + request.getDate() + ".xlsx";
        String encodedFileName = URLEncoder.encode(rawFileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + rawFileName + "\"; filename*=UTF-8''" + encodedFileName)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(excelBytes.length)
                .body(resource);
    }
}
