package vn.edu.utc.hotel_booking.modules.ai.personalization.privacy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import vn.edu.utc.hotel_booking.modules.ai.personalization.dto.StayHistoryRecordDto;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Bộ lọc bảo vệ quyền riêng tư và chống phân loại theo thuộc tính nhạy cảm cho module U14.
 *
 * <p><strong>Nguyên tắc đạo đức & quy định bảo vệ dữ liệu:</strong>
 * Hệ thống tuyệt đối KHÔNG phân tích, lưu trữ hoặc tạo hồ sơ sở thích dựa trên:
 * <ul>
 *   <li>Tôn giáo, tín ngưỡng</li>
 *   <li>Tình trạng sức khỏe, bệnh lý, mang thai, khuyết tật</li>
 *   <li>Chủng tộc, sắc tộc</li>
 *   <li>Xu hướng tính dục</li>
 *   <li>Quan điểm chính trị</li>
 *   <li>Mã định danh cá nhân (CCCD, CMND, Hộ chiếu, Thẻ tín dụng, SĐT, Email)</li>
 *   <li>Thu nhập, mức lương, tài sản cá nhân</li>
 * </ul>
 * </p>
 */
@Component
public class PersonalizationPrivacyGuard {

    private static final Logger log = LoggerFactory.getLogger(PersonalizationPrivacyGuard.class);

    // 1. Tôn giáo / tín ngưỡng
    private static final Pattern RELIGION_PATTERN = Pattern.compile(
            "(?i)\\b(phật\\s*giáo|phat\\s*giao|thiên\\s*chúa|thien\\s*chua|hồi\\s*giáo|hoi\\s*giao|tin\\s*lành|tin\\s*lanh|ấn\\s*độ\\s*giáo|an\\s*do\\s*giao|đạo|tôn\\s*giáo|ton\\s*giao|halal|kosher|hindu|muslim|christian|buddhis\\w*|islam\\w*)\\b");

    // 2. Tình trạng sức khỏe / bệnh lý / mang thai / khuyết tật
    private static final Pattern HEALTH_PATTERN = Pattern.compile(
            "(?i)\\b(mang\\s*thai|có\\s*bầu|co\\s*bau|thai\\s*kỳ|thai\\s*ky|khuyết\\s*tật|khuyet\\s*tat|xe\\s*lăn|xe\\s*lan|dị\\s*ứng|di\\s*ung|bệnh|benh|tim\\s*mạch|tim\\s*mach|điều\\s*trị|dieu\\s*tri|ung\\s*thư|ung\\s*thu|hiv|tiểu\\s*đường|tieu\\s*duong|y\\s*tế|y\\s*te|thuốc|thuoc|medical|pregnancy|pregnant|wheelchair|disabilit\\w*|disease)\\b");

    // 3. Chủng tộc / sắc tộc
    private static final Pattern RACE_PATTERN = Pattern.compile(
            "(?i)\\b(chủng\\s*tộc|chung\\s*toc|sắc\\s*tộc|sac\\s*toc|da\\s*đen|da\\s*den|da\\s*trắng|da\\s*trang|da\\s*vàng|da\\s*vang|dân\\s*tộc|dan\\s*toc|race|ethnicit\\w*)\\b");

    // 4. Xu hướng tính dục
    private static final Pattern SEXUAL_ORIENTATION_PATTERN = Pattern.compile(
            "(?i)\\b(đồng\\s*tính|dong\\s*tinh|lgbt|gay|lesbian|bisexual|transgender|queer)\\b");

    // 5. Quan điểm chính trị
    private static final Pattern POLITICS_PATTERN = Pattern.compile(
            "(?i)\\b(chính\\s*trị|chinh\\s*tri|đảng\\s*phái|dang\\s*phai|biểu\\s*tình|bieu\\s*tinh|political|partisan)\\b");

    // 6. Định danh cá nhân / PII
    private static final Pattern PII_PATTERN = Pattern.compile(
            "(?i)(?:cccd|cmnd|passport|hộ\\s*chiếu|ho\\s*chieu|visa)\\s*[:#-]?\\s*\\b[A-Za-z0-9]+\\b|\\b\\d{9,12}\\b|\\b(?:0|\\+84)\\d{9,10}\\b|\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b");

    // 7. Thu nhập / lương
    private static final Pattern INCOME_PATTERN = Pattern.compile(
            "(?i)\\b(lương|luong|thu\\s*nhập|thu\\s*nhap|tài\\s*sản|tai\\s*san|salary|income|wage)\\b");

    /**
     * Kiểm tra xem một chuỗi văn bản hoặc mã có chứa bất kỳ thuộc tính nhạy cảm nào không.
     */
    public boolean containsSensitiveAttribute(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String normalized = text.replace('_', ' ').replace('-', ' ');
        String nfd = java.text.Normalizer.normalize(normalized, java.text.Normalizer.Form.NFD);
        String deAccented = nfd.replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace('đ', 'd').replace('Đ', 'd');

        return matchAnyPattern(normalized) || matchAnyPattern(deAccented);
    }

    private boolean matchAnyPattern(String text) {
        return RELIGION_PATTERN.matcher(text).find()
                || HEALTH_PATTERN.matcher(text).find()
                || RACE_PATTERN.matcher(text).find()
                || SEXUAL_ORIENTATION_PATTERN.matcher(text).find()
                || POLITICS_PATTERN.matcher(text).find()
                || PII_PATTERN.matcher(text).find()
                || INCOME_PATTERN.matcher(text).find();
    }

    /**
     * Làm sạch một bản ghi lịch sử lưu trú, loại bỏ triệt để các mã đặc tính và dịch vụ nhạy cảm,
     * làm sạch trường ghi chú nếu có.
     *
     * @param record   Bản ghi gốc
     * @param warnings Danh sách warnings để ghi nhận cảnh báo nếu phát hiện vi phạm
     * @return Bản ghi đã được làm sạch an toàn
     */
    public StayHistoryRecordDto sanitizeStayRecord(StayHistoryRecordDto record, List<String> warnings) {
        if (record == null) {
            return null;
        }

        boolean sensitiveDetected = false;

        // 1. Lọc preferredFeatureCodes
        List<String> safeFeatures = new ArrayList<>();
        if (record.getPreferredFeatureCodes() != null) {
            for (String f : record.getPreferredFeatureCodes()) {
                if (f == null || f.isBlank()) {
                    continue;
                }
                String trimmed = f.trim();
                if (containsSensitiveAttribute(trimmed)) {
                    sensitiveDetected = true;
                    log.warn("U14 Privacy: Phát hiện và loại bỏ mã đặc tính nhạy cảm trong lịch sử");
                } else {
                    safeFeatures.add(trimmed);
                }
            }
        }

        // 2. Lọc usedServiceCodes
        List<String> safeServices = new ArrayList<>();
        if (record.getUsedServiceCodes() != null) {
            for (String s : record.getUsedServiceCodes()) {
                if (s == null || s.isBlank()) {
                    continue;
                }
                String trimmed = s.trim();
                if (containsSensitiveAttribute(trimmed)) {
                    sensitiveDetected = true;
                    log.warn("U14 Privacy: Phát hiện và loại bỏ mã dịch vụ nhạy cảm trong lịch sử");
                } else {
                    safeServices.add(trimmed);
                }
            }
        }

        // 3. Làm sạch guestPreferenceNote
        String safeNote = record.getGuestPreferenceNote();
        if (safeNote != null) {
            String trimmed = safeNote.trim();
            if (trimmed.isBlank()) {
                safeNote = null;
            } else if (containsSensitiveAttribute(trimmed)) {
                sensitiveDetected = true;
                log.warn("U14 Privacy: Phát hiện thông tin nhạy cảm trong ghi chú sở thích, tiến hành che giấu");
                safeNote = "[REDACTED_SENSITIVE_PREFERENCE]";
            } else {
                safeNote = trimmed;
            }
        }

        if (sensitiveDetected && warnings != null) {
            String warningMsg = "SENSITIVE_ATTRIBUTE_BLOCKED: Phát hiện và loại bỏ thuộc tính nhạy cảm/PII khỏi hồ sơ cá nhân hóa (tuân thủ nguyên tắc công bằng và bảo vệ quyền riêng tư).";
            if (!warnings.contains(warningMsg)) {
                warnings.add(warningMsg);
            }
        }

        return StayHistoryRecordDto.builder()
                .hotelId(record.getHotelId())
                .hotelRoomTypeId(record.getHotelRoomTypeId())
                .roomTypeCode(record.getRoomTypeCode())
                .roomTypeName(record.getRoomTypeName())
                .checkInDate(record.getCheckInDate())
                .checkOutDate(record.getCheckOutDate())
                .adults(record.getAdults())
                .children(record.getChildren())
                .preferredFeatureCodes(safeFeatures)
                .usedServiceCodes(safeServices)
                .guestPreferenceNote(safeNote)
                .build();
    }
}
