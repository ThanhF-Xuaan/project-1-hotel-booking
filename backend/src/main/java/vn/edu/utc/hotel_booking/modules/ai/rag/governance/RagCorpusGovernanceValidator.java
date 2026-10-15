package vn.edu.utc.hotel_booking.modules.ai.rag.governance;

import org.springframework.stereotype.Component;
import vn.edu.utc.hotel_booking.modules.ai.rag.dto.RagIngestRequestDto;
import vn.edu.utc.hotel_booking.modules.ai.rag.enums.RagScopeType;
import vn.edu.utc.hotel_booking.modules.ai.rag.model.CorpusValidationResult;
import vn.edu.utc.hotel_booking.modules.ai.rag.model.SanitizedContentResult;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RagCorpusGovernanceValidator {

    // PII patterns
    private static final Pattern CCCD_PATTERN = Pattern.compile("(?i)(?:cccd|cmnd|căn\\s*cước|định\\s*danh)?\\s*[:#-]?\\s*\\b(\\d{9}|\\d{12})\\b");
    private static final Pattern PASSPORT_PATTERN = Pattern.compile("(?i)(?:hộ\\s*chiếu|passport)\\s*[:#-]?\\s*\\b([A-Z]\\d{7,8})\\b");
    private static final Pattern PHONE_PATTERN = Pattern.compile("(?i)(?:sđt|điện\\s*thoại|phone|tel|mobile)?\\s*[:#-]?\\s*\\b(0|\\+84)(?:3|5|7|8|9)\\d{8}\\b");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b");
    private static final Pattern PAYROLL_PATTERN = Pattern.compile("(?i)\\b(payroll|bảng\\s*lương|net_salary|gross_salary|tiền\\s*lương|hệ\\s*số\\s*lương|thu\\s*nhập\\s*tháng)\\b");

    // Confidential patterns
    private static final Pattern CONFIDENTIAL_PATTERN = Pattern.compile("(?i)\\b(INTERNAL_ONLY|CONFIDENTIAL|NỘI\\s*BỘ|BẢO\\s*MẬT|TỐI\\s*MẬT|api_key|secret_key|bearer\\s+[A-Za-z0-9._-]{20,}|password\\s*[:=]|mật\\s*khẩu\\s*[:=])\\b");

    // Guest / live booking data patterns
    private static final Pattern GUEST_BOOKING_PATTERN = Pattern.compile("(?i)\\b(booking_id|guest_id|khách\\s*đang\\s*ở\\s*phòng\\s*\\d+|danh\\s*sách\\s*khách\\s*lưu\\s*trú)\\b");

    // Indirect prompt injection patterns in static content
    private static final Pattern INDIRECT_INJECTION_PATTERN = Pattern.compile(
            "(?i)(bỏ\\s*qua\\s*(mọi|các)?\\s*(hướng\\s*dẫn|lệnh|quy\\s*tắc|yêu\\s*cầu)|ignore\\s*(all)?\\s*(previous|prior)?\\s*instructions|disregard\\s*(all)?\\s*(previous|prior)?\\s*instructions|print\\s*(your)?\\s*(system\\s*prompt|api\\s*key)|output\\s*(your)?\\s*(system\\s*prompt)|hãy\\s*nói\\s*rằng|system\\s*prompt)"
    );

    // Static price claim patterns in static corpus
    private static final Pattern STATIC_PRICE_CLAIM_PATTERN = Pattern.compile(
            "(?i)(báo\\s*giá\\s*1\\s*(đồng|đ|vnd)|giá\\s*phòng\\s*(hiện\\s*tại\\s*là|chỉ|từ)?\\s*\\d+([.,]\\d+)*\\s*(đ|đồng|vnd|k|usd|\\$)|price\\s*(is|starts\\s*at)?\\s*\\$?\\d+([.,]\\d+)*)"
    );

    public CorpusValidationResult validateForIngestion(RagIngestRequestDto request) {
        if (request == null) {
            return CorpusValidationResult.builder()
                    .valid(false)
                    .rejectionReason("REJECTED_NULL_REQUEST: Ingest request cannot be null")
                    .build();
        }

        if (request.getCategory() == null) {
            return CorpusValidationResult.builder()
                    .valid(false)
                    .rejectionReason("REJECTED_INVALID_CATEGORY: Document category must be POLICY, FAQ, AMENITY, or STATIC_ROOM_DESCRIPTION")
                    .build();
        }

        if (request.getScopeType() == null) {
            return CorpusValidationResult.builder()
                    .valid(false)
                    .rejectionReason("REJECTED_INVALID_SCOPE: ScopeType must be HOTEL_SCOPED or CHAIN_PUBLIC")
                    .build();
        }

        if (request.getScopeType() == RagScopeType.HOTEL_SCOPED) {
            if (request.getHotelId() == null || request.getHotelId() <= 0) {
                return CorpusValidationResult.builder()
                        .valid(false)
                        .rejectionReason("REJECTED_MISSING_HOTEL_SCOPE: HOTEL_SCOPED document requires valid hotelId > 0")
                        .build();
            }
        }

        String content = request.getContent() != null ? request.getContent() : "";
        String title = request.getTitle() != null ? request.getTitle() : "";
        String combined = title + "\n" + content;

        // PII checks
        if (CCCD_PATTERN.matcher(combined).find() || PASSPORT_PATTERN.matcher(combined).find()) {
            return CorpusValidationResult.builder()
                    .valid(false)
                    .rejectionReason("REJECTED_PII_DETECTED: Personal identity documents (CCCD/CMND/Passport) are strictly prohibited in RAG corpus")
                    .build();
        }

        if (PHONE_PATTERN.matcher(combined).find() || EMAIL_PATTERN.matcher(combined).find()) {
            return CorpusValidationResult.builder()
                    .valid(false)
                    .rejectionReason("REJECTED_PII_DETECTED: Personal phone numbers or email addresses are prohibited in RAG corpus")
                    .build();
        }

        if (PAYROLL_PATTERN.matcher(combined).find()) {
            return CorpusValidationResult.builder()
                    .valid(false)
                    .rejectionReason("REJECTED_PAYROLL_DATA: Payroll and employee salary data are strictly prohibited in RAG corpus")
                    .build();
        }

        // Confidentiality checks
        if (CONFIDENTIAL_PATTERN.matcher(combined).find()) {
            return CorpusValidationResult.builder()
                    .valid(false)
                    .rejectionReason("REJECTED_CONFIDENTIAL_CONTENT: Internal-only, confidential documents or credentials cannot be ingested")
                    .build();
        }

        // Live guest/booking checks
        if (GUEST_BOOKING_PATTERN.matcher(combined).find()) {
            return CorpusValidationResult.builder()
                    .valid(false)
                    .rejectionReason("REJECTED_GUEST_BOOKING_DATA: Live guest occupancy and booking records are prohibited in static RAG corpus")
                    .build();
        }

        return CorpusValidationResult.builder()
                .valid(true)
                .build();
    }

    public SanitizedContentResult sanitizeChunkContent(String text) {
        if (text == null || text.isBlank()) {
            return SanitizedContentResult.builder()
                    .sanitizedText("")
                    .injectionDetected(false)
                    .priceClaimsStripped(false)
                    .warnings(new ArrayList<>())
                    .build();
        }

        boolean injectionFound = false;
        boolean priceClaimFound = false;
        List<String> warnings = new ArrayList<>();

        // 1. Sanitize indirect prompt injection
        Matcher injMatcher = INDIRECT_INJECTION_PATTERN.matcher(text);
        String cleaned = text;
        if (injMatcher.find()) {
            injectionFound = true;
            cleaned = injMatcher.replaceAll("[LỆNH ĐÃ BỊ LOẠI BỎ]");
            warnings.add("INDIRECT_INJECTION_STRIPPED");
        }

        // 2. Sanitize static price claims that could conflict with live core prices
        Matcher priceMatcher = STATIC_PRICE_CLAIM_PATTERN.matcher(cleaned);
        if (priceMatcher.find()) {
            priceClaimFound = true;
            cleaned = priceMatcher.replaceAll("[THÔNG TIN GIÁ PHÒNG: Vui lòng tra cứu trực tiếp từ hệ thống đặt phòng Core Booking]");
            warnings.add("PRICE_CLAIMS_STRIPPED_DEFER_TO_CORE");
        }

        return SanitizedContentResult.builder()
                .sanitizedText(cleaned.trim())
                .injectionDetected(injectionFound)
                .priceClaimsStripped(priceClaimFound)
                .warnings(warnings)
                .build();
    }
}
