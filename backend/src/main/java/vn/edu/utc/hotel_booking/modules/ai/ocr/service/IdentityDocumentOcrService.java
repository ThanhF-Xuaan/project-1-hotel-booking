package vn.edu.utc.hotel_booking.modules.ai.ocr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vn.edu.utc.hotel_booking.modules.ai.ocr.dto.OcrDocumentExtractRequestDto;
import vn.edu.utc.hotel_booking.modules.ai.ocr.dto.OcrStayGuestDraftDto;
import vn.edu.utc.hotel_booking.modules.ai.ocr.enums.OcrExtractionStatus;
import vn.edu.utc.hotel_booking.modules.ai.ocr.model.ExtractedFieldDto;
import vn.edu.utc.hotel_booking.modules.ai.ocr.port.OcrVisionPort;
import vn.edu.utc.hotel_booking.modules.ai.ocr.util.PiiRedactionUtil;
import vn.edu.utc.hotel_booking.modules.operation.entity.enums.DocumentType;
import vn.edu.utc.hotel_booking.modules.operation.entity.enums.Gender;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dịch vụ bóc tách thông tin giấy tờ tuỳ thân (CCCD Việt Nam và Hộ chiếu Quốc tế).
 *
 * <p>Triển khai tất định (deterministic) dựa trên phân tích regex + pattern matching
 * trên văn bản OCR thô. Không gọi API Vision bên ngoài khi chưa được phê duyệt.</p>
 *
 * <p><strong>Bảo mật PII:</strong> Không ghi log số CCCD, họ tên, địa chỉ gốc.
 * Mọi dữ liệu nhạy cảm đưa vào log đều đi qua {@link PiiRedactionUtil}.</p>
 *
 * <p><strong>Bất biến U13:</strong> {@code requiresHumanVerification} luôn {@code true}.</p>
 */
@Service
public class IdentityDocumentOcrService implements OcrVisionPort {

    private static final Logger log = LoggerFactory.getLogger(IdentityDocumentOcrService.class);

    /** Ngưỡng tin cậy tối thiểu để đánh dấu trường là hợp lệ. */
    static final double CONFIDENCE_THRESHOLD = 0.85;

    // ---- Patterns CCCD / CMND Việt Nam ----

    /** Số CCCD: 12 chữ số, bắt đầu bằng '0' (mã tỉnh 001-096). */
    private static final Pattern CCCD_PATTERN = Pattern.compile(
            "(?:^|\\b)(0\\d{11})(?:\\b|$)", Pattern.MULTILINE);

    /** Số CMND: 9 chữ số cũ. */
    private static final Pattern CMND_PATTERN = Pattern.compile(
            "(?:^|\\b)(\\d{9})(?:\\b|$)", Pattern.MULTILINE);

    /** Ngày sinh dạng dd/MM/yyyy hoặc dd-MM-yyyy. */
    private static final Pattern DATE_VN_PATTERN = Pattern.compile(
            "\\b(\\d{2})[/\\-](\\d{2})[/\\-](\\d{4})\\b");

    /** Họ và tên tiếng Việt (in hoa / có dấu). */
    private static final Pattern FULLNAME_VN_PATTERN = Pattern.compile(
            "(?:Họ và tên|Họ tên|Họ, tên|Full name|Name)[:\\s]+([A-ZÀÁÂÃÈÉÊÌÍÒÓÔÕÙÚÝĂĐƠƯẠẢẤẦẨẪẬẮẰẲẴẶẸẺẼẾỀỂỄỆỈỊỌỎỐỒỔỖỘỚỜỞỠỢỤỦỨỪỬỮỰỲỴỶỸ][A-Za-zÀ-ỹ\\s]+)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    /** Giới tính tiếng Việt. */
    private static final Pattern GENDER_VN_PATTERN = Pattern.compile(
            "(?:Giới tính|Gioi tinh|Sex|Gender)[:\\s]*(Nam|Nữ|Nu|Male|Female|M|F)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    /** Nơi thường trú / nơi ĐKTT. */
    private static final Pattern ADDRESS_VN_PATTERN = Pattern.compile(
            "(?:Nơi thường trú|Noi thuong tru|Nơi ĐKTT|Địa chỉ|Permanent address|Place of residence)[:\\s]+(.+)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    /** Quốc tịch tiếng Việt. */
    private static final Pattern NATIONALITY_VN_PATTERN = Pattern.compile(
            "(?:Quốc tịch|Quoc tich|Nationality)[:\\s]+([A-Za-zÀ-ỹ\\s]+)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    // ---- Patterns MRZ Hộ chiếu ICAO 9303 TD3 ----

    /** Dòng MRZ 1: P<{mã quốc gia}{HỌ}<<{TÊN} – đúng 44 ký tự. */
    private static final Pattern MRZ_LINE1 = Pattern.compile(
            "P<([A-Z]{3})([A-Z<]{39})");

    /**
     * Dòng MRZ 2 (ICAO 9303 TD3):
     * Group 1: 8-9 ký tự số hộ chiếu (gồm cả đệm <)
     * Group 2: check digit số hộ chiếu (0-9 hoặc <)
     * Group 3: mã quốc tịch (3 ký tự)
     * Group 4: ngày sinh YYMMDD (6 chữ số)
     * Group 5: check digit ngày sinh (0-9 hoặc <)
     * Group 6: giới tính (M/F/<)
     * Group 7: ngày hết hạn YYMMDD (6 chữ số)
     */
    private static final Pattern MRZ_LINE2 = Pattern.compile(
            "([A-Z0-9<]{8,9})<*([0-9<])([A-Z]{3})(\\d{6})([0-9<])([MF<])(\\d{6})");

    /** Bảng ánh xạ mã quốc tịch 3 ký tự ISO 3166-1 alpha-3 → tên quốc gia. */
    private static final Map<String, String> NATIONALITY_CODES = Map.ofEntries(
            Map.entry("VNM", "Việt Nam"),
            Map.entry("USA", "Hoa Kỳ"),
            Map.entry("JPN", "Nhật Bản"),
            Map.entry("KOR", "Hàn Quốc"),
            Map.entry("GBR", "Anh"),
            Map.entry("FRA", "Pháp"),
            Map.entry("AUS", "Úc"),
            Map.entry("SGP", "Singapore"),
            Map.entry("CHN", "Trung Quốc"),
            Map.entry("DEU", "Đức"),
            Map.entry("CAN", "Canada"),
            Map.entry("IND", "Ấn Độ"),
            Map.entry("THA", "Thái Lan"),
            Map.entry("MYS", "Malaysia"),
            Map.entry("IDN", "Indonesia"),
            Map.entry("PHL", "Philippines"),
            Map.entry("RUS", "Nga"),
            Map.entry("ITA", "Ý"),
            Map.entry("ESP", "Tây Ban Nha"),
            Map.entry("NLD", "Hà Lan")
    );

    @Override
    public OcrStayGuestDraftDto extract(OcrDocumentExtractRequestDto request) {
        String text = request.getRawOcrText();
        DocumentType hint = request.getDocumentTypeHint();

        log.debug("U13 OCR: bắt đầu bóc tách, fileSize={}, hint={}",
                request.getFileSizeBytes(), hint);

        // Ưu tiên: nếu phát hiện MRZ → hộ chiếu
        if (containsMrz(text)) {
            log.debug("U13 OCR: phát hiện MRZ → xử lý hộ chiếu");
            return extractPassportMrz(text);
        }

        // Nếu gợi ý là passport nhưng không có MRZ → thử text-based
        if (hint == DocumentType.PASSPORT) {
            return extractPassportTextBased(text);
        }

        // Mặc định: CCCD / CMND Việt Nam
        return extractVietnameseCccd(text);
    }

    // =========================================================
    // Bóc tách CCCD / CMND Việt Nam
    // =========================================================

    private OcrStayGuestDraftDto extractVietnameseCccd(String text) {
        OcrStayGuestDraftDto draft = new OcrStayGuestDraftDto();
        List<String> lowFields = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // --- Số giấy tờ ---
        ExtractedFieldDto<String> docNum = extractCccdOrCmndNumber(text, lowFields, warnings);

        // --- Họ và tên ---
        ExtractedFieldDto<String> fullName = extractFullNameVn(text, lowFields);

        // Nếu không có cả số giấy tờ lẫn họ tên → UNREADABLE
        if (docNum.getValue() == null && fullName.getValue() == null) {
            draft.setStatus(OcrExtractionStatus.UNREADABLE_DOCUMENT);
            draft.setDocumentNumber(docNum);
            draft.setFullName(fullName);
            draft.setOverallConfidence(0.0);
            draft.setMissingOrLowConfidenceFields(List.of("documentNumber", "fullName"));
            draft.setWarnings(List.of("Không đọc được thông tin tối thiểu từ văn bản OCR"));
            return draft;
        }

        // --- Ngày sinh ---
        ExtractedFieldDto<LocalDate> dob = extractDateOfBirth(text, lowFields);

        // --- Giới tính ---
        ExtractedFieldDto<Gender> gender = extractGenderVn(text, lowFields);

        // --- Quốc tịch ---
        ExtractedFieldDto<String> nationality = extractNationalityVn(text);

        // --- Địa chỉ thường trú ---
        ExtractedFieldDto<String> address = extractAddressVn(text);

        // Theo chuẩn BCA: khách Việt Nam bắt buộc permanentAddress
        boolean isVietnamese = isVietnameseNationality(nationality.getValue());
        if (isVietnamese && (address.getValue() == null || !address.isValid())) {
            lowFields.add("permanentAddress");
        }

        draft.setDocumentType(ExtractedFieldDto.of(
                docNum.getValue() != null
                        ? (docNum.getValue().length() == 12 ? DocumentType.CCCD : DocumentType.CMND)
                        : DocumentType.CCCD,
                docNum.getValue() != null ? 0.92 : 0.0));
        draft.setDocumentNumber(docNum);
        draft.setFullName(fullName);
        draft.setDateOfBirth(dob);
        draft.setGender(gender);
        draft.setNationality(nationality);
        draft.setPermanentAddress(address);

        // Tính confidence tổng
        double overall = computeOverall(List.of(
                docNum.getConfidence(), fullName.getConfidence(),
                dob.getConfidence(), gender.getConfidence(),
                nationality.getConfidence()));
        draft.setOverallConfidence(overall);

        draft.setMissingOrLowConfidenceFields(lowFields);
        draft.setWarnings(warnings);
        draft.setStatus(lowFields.isEmpty()
                ? OcrExtractionStatus.SUCCESS
                : OcrExtractionStatus.LOW_CONFIDENCE_REVIEW_REQUIRED);

        log.debug("U13 OCR CCCD: docNum={}, status={}, overallConf={}, lowFields={}",
                PiiRedactionUtil.redact(docNum.getValue()), draft.getStatus(), overall, lowFields.size());
        return draft;
    }

    // =========================================================
    // Bóc tách Hộ chiếu Quốc tế (MRZ ICAO 9303 TD3)
    // =========================================================

    private OcrStayGuestDraftDto extractPassportMrz(String text) {
        OcrStayGuestDraftDto draft = new OcrStayGuestDraftDto();
        List<String> lowFields = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        Matcher m1 = MRZ_LINE1.matcher(text);
        Matcher m2 = MRZ_LINE2.matcher(text);

        boolean hasLine1 = m1.find();
        boolean hasLine2 = m2.find();

        if (!hasLine1 || !hasLine2) {
            draft.setStatus(OcrExtractionStatus.UNREADABLE_DOCUMENT);
            draft.setOverallConfidence(0.0);
            draft.setMissingOrLowConfidenceFields(List.of("documentNumber", "fullName"));
            draft.setWarnings(List.of("Không tìm thấy cả 2 dòng MRZ hợp lệ trong văn bản"));
            return draft;
        }

        // Dòng 1: mã quốc gia phát hành, họ và tên
        String issuingCountry = m1.group(1);
        String nameSection = m1.group(2);
        String[] nameParts = nameSection.split("<<", 2);
        String lastName = nameParts[0].replace("<", " ").trim();
        String firstName = nameParts.length > 1 ? nameParts[1].replace("<", " ").trim() : "";
        String fullNameRaw = (lastName + " " + firstName).trim();

        // Dòng 2: số hộ chiếu, quốc tịch, ngày sinh, giới tính
        String passportNumber = m2.group(1).replace("<", "").trim();
        String nationalityCode = m2.group(3);
        String dobRaw = m2.group(4); // YYMMDD
        String genderCode = m2.group(6);

        // Kiểm tra check digit ICAO 9303 cho số hộ chiếu (group 2)
        String docCheckChar = m2.group(2);
        int expectedDocCheck = computeIcaoCheckDigit(m2.group(1));
        boolean docCheckValid = expectedDocCheck >= 0 && !docCheckChar.isEmpty()
                && (docCheckChar.charAt(0) == (char) ('0' + expectedDocCheck)
                || (docCheckChar.charAt(0) == '<' && expectedDocCheck == 0));

        double docConf = 0.95;
        if (passportNumber.length() < 6) {
            docConf = 0.40;
            lowFields.add("documentNumber");
            warnings.add("Số hộ chiếu quá ngắn (< 6 ký tự)");
        } else if (!docCheckValid) {
            docConf = 0.55;
            lowFields.add("documentNumber");
            warnings.add("Check digit số hộ chiếu không khớp (MRZ có thể bị mờ hoặc sai lệch)");
        }

        // Bóc tách ngày sinh từ YYMMDD
        LocalDate dob = parseMrzDate(dobRaw, warnings);
        String dobCheckChar = m2.group(5);
        int expectedDobCheck = computeIcaoCheckDigit(dobRaw);
        boolean dobCheckValid = expectedDobCheck >= 0 && !dobCheckChar.isEmpty()
                && (dobCheckChar.charAt(0) == (char) ('0' + expectedDobCheck)
                || (dobCheckChar.charAt(0) == '<' && expectedDobCheck == 0));

        double dobConf = 0.95;
        if (dob == null) {
            dobConf = 0.0;
            lowFields.add("dateOfBirth");
        } else if (!dobCheckValid) {
            dobConf = 0.55;
            lowFields.add("dateOfBirth");
            warnings.add("Check digit ngày sinh không khớp (MRZ có thể bị mờ hoặc sai lệch)");
        }

        // Giới tính
        Gender gender = switch (genderCode) {
            case "M" -> Gender.MALE;
            case "F" -> Gender.FEMALE;
            default -> null;
        };
        double genderConf = gender != null ? 0.97 : 0.50;
        if (gender == null) {
            lowFields.add("gender");
        }

        // Quốc tịch
        String nationalityName = NATIONALITY_CODES.getOrDefault(nationalityCode, nationalityCode);

        // Hộ chiếu quốc tế không bắt buộc permanentAddress (theo logic BCA VNM)
        boolean requiresAddress = "VNM".equals(nationalityCode);

        if (fullNameRaw.isBlank()) {
            lowFields.add("fullName");
        }

        draft.setDocumentType(ExtractedFieldDto.of(DocumentType.PASSPORT, 0.99));
        draft.setDocumentNumber(passportNumber.isBlank()
                ? ExtractedFieldDto.missing()
                : (docCheckValid
                        ? ExtractedFieldDto.of(passportNumber, docConf)
                        : ExtractedFieldDto.invalid(passportNumber, docConf, "Check digit số hộ chiếu không khớp")));
        draft.setFullName(fullNameRaw.isBlank()
                ? ExtractedFieldDto.missing()
                : ExtractedFieldDto.of(fullNameRaw, 0.90));
        draft.setDateOfBirth(dob != null
                ? (dobCheckValid
                        ? ExtractedFieldDto.of(dob, dobConf)
                        : ExtractedFieldDto.invalid(dob, dobConf, "Check digit ngày sinh không khớp"))
                : ExtractedFieldDto.missing());
        draft.setGender(gender != null
                ? ExtractedFieldDto.of(gender, genderConf)
                : ExtractedFieldDto.invalid(null, genderConf, "Mã giới tính không nhận diện được: " + genderCode));
        draft.setNationality(ExtractedFieldDto.of(nationalityName, 0.97));
        draft.setPermanentAddress(requiresAddress
                ? extractAddressVn(text)
                : ExtractedFieldDto.of(null, 1.0)); // Không bắt buộc với khách quốc tế

        if (requiresAddress && (draft.getPermanentAddress().getValue() == null)) {
            lowFields.add("permanentAddress");
        }

        double overall = computeOverall(List.of(docConf, 0.90, dobConf, genderConf, 0.97));
        draft.setOverallConfidence(overall);
        draft.setMissingOrLowConfidenceFields(lowFields);
        draft.setWarnings(warnings);
        draft.setStatus(lowFields.isEmpty()
                ? OcrExtractionStatus.SUCCESS
                : OcrExtractionStatus.LOW_CONFIDENCE_REVIEW_REQUIRED);

        log.debug("U13 OCR Passport MRZ: status={}, country={}, docNum={}, conf={}",
                draft.getStatus(), issuingCountry, PiiRedactionUtil.redact(passportNumber), overall);
        return draft;
    }

    private OcrStayGuestDraftDto extractPassportTextBased(String text) {
        // Fallback: xử lý hộ chiếu qua nhãn text thông thường (không có MRZ)
        OcrStayGuestDraftDto draft = new OcrStayGuestDraftDto();
        List<String> lowFields = new ArrayList<>();

        ExtractedFieldDto<String> docNum = extractDocumentNumberGeneric(text, lowFields);
        ExtractedFieldDto<String> fullName = extractFullNameVn(text, lowFields);

        if (docNum.getValue() == null && fullName.getValue() == null) {
            draft.setStatus(OcrExtractionStatus.UNREADABLE_DOCUMENT);
            draft.setOverallConfidence(0.0);
            draft.setMissingOrLowConfidenceFields(List.of("documentNumber", "fullName"));
            draft.setWarnings(List.of("Không đọc được thông tin tối thiểu từ hộ chiếu"));
            return draft;
        }

        draft.setDocumentType(ExtractedFieldDto.of(DocumentType.PASSPORT, 0.80));
        draft.setDocumentNumber(docNum);
        draft.setFullName(fullName);
        draft.setDateOfBirth(extractDateOfBirth(text, lowFields));
        draft.setGender(extractGenderVn(text, lowFields));
        draft.setNationality(extractNationalityVn(text));
        draft.setPermanentAddress(ExtractedFieldDto.of(null, 1.0));

        double overall = computeOverall(List.of(
                docNum.getConfidence(), fullName.getConfidence(), 0.5));
        draft.setOverallConfidence(overall);
        draft.setMissingOrLowConfidenceFields(lowFields);
        draft.setWarnings(List.of("Bóc tách hộ chiếu qua text-label (không có MRZ) – độ tin cậy thấp hơn"));
        draft.setStatus(lowFields.isEmpty()
                ? OcrExtractionStatus.SUCCESS
                : OcrExtractionStatus.LOW_CONFIDENCE_REVIEW_REQUIRED);
        return draft;
    }

    // =========================================================
    // Các hàm trích xuất từng trường
    // =========================================================

    private ExtractedFieldDto<String> extractCccdOrCmndNumber(String text, List<String> lowFields,
                                                               List<String> warnings) {
        Matcher mCccd = CCCD_PATTERN.matcher(text);
        if (mCccd.find()) {
            String num = mCccd.group(1);
            log.debug("U13: tìm thấy số CCCD {}", PiiRedactionUtil.redact(num));
            return ExtractedFieldDto.of(num, 0.95);
        }
        Matcher mCmnd = CMND_PATTERN.matcher(text);
        if (mCmnd.find()) {
            String num = mCmnd.group(1);
            log.debug("U13: tìm thấy số CMND {}", PiiRedactionUtil.redact(num));
            warnings.add("Phát hiện số CMND 9 chữ số (có thể cần cập nhật lên CCCD 12 số)");
            return ExtractedFieldDto.of(num, 0.80);
        }
        lowFields.add("documentNumber");
        return ExtractedFieldDto.missing();
    }

    private ExtractedFieldDto<String> extractDocumentNumberGeneric(String text, List<String> lowFields) {
        // Số hộ chiếu: thường là 1 chữ cái + 7-8 chữ số
        Pattern passportNumPattern = Pattern.compile("\\b([A-Z]\\d{7,8})\\b");
        Matcher m = passportNumPattern.matcher(text);
        if (m.find()) {
            return ExtractedFieldDto.of(m.group(1), 0.88);
        }
        lowFields.add("documentNumber");
        return ExtractedFieldDto.missing();
    }

    private ExtractedFieldDto<String> extractFullNameVn(String text, List<String> lowFields) {
        Matcher m = FULLNAME_VN_PATTERN.matcher(text);
        if (m.find()) {
            String name = m.group(1).trim();
            if (name.length() >= 2) {
                log.debug("U13: tìm thấy họ tên {}", PiiRedactionUtil.redact(name));
                return ExtractedFieldDto.of(name.toUpperCase(), 0.90);
            }
        }
        lowFields.add("fullName");
        return ExtractedFieldDto.missing();
    }

    private ExtractedFieldDto<LocalDate> extractDateOfBirth(String text, List<String> lowFields) {
        Matcher m = DATE_VN_PATTERN.matcher(text);
        if (m.find()) {
            String day = m.group(1);
            String month = m.group(2);
            String year = m.group(3);
            try {
                LocalDate dob = LocalDate.of(
                        Integer.parseInt(year),
                        Integer.parseInt(month),
                        Integer.parseInt(day));
                if (dob.isAfter(LocalDate.now())) {
                    lowFields.add("dateOfBirth");
                    return ExtractedFieldDto.invalid(dob, 0.30, "Ngày sinh nằm trong tương lai");
                }
                if (dob.isBefore(LocalDate.of(1900, 1, 1))) {
                    lowFields.add("dateOfBirth");
                    return ExtractedFieldDto.invalid(dob, 0.40, "Ngày sinh không hợp lý (trước 1900)");
                }
                return ExtractedFieldDto.of(dob, 0.92);
            } catch (DateTimeException | NumberFormatException e) {
                log.debug("U13: ngày sinh không hợp lệ trên lịch: {}", e.getMessage());
                lowFields.add("dateOfBirth");
                return ExtractedFieldDto.invalid(null, 0.20,
                        "Ngày sinh không hợp lệ trên lịch: " + day + "/" + month + "/" + year);
            }
        }
        lowFields.add("dateOfBirth");
        return ExtractedFieldDto.missing();
    }

    private ExtractedFieldDto<Gender> extractGenderVn(String text, List<String> lowFields) {
        Matcher m = GENDER_VN_PATTERN.matcher(text);
        if (m.find()) {
            String raw = m.group(1).trim().toLowerCase();
            Gender g = switch (raw) {
                case "nam", "male", "m" -> Gender.MALE;
                case "nữ", "nu", "female", "f" -> Gender.FEMALE;
                default -> null;
            };
            if (g != null) return ExtractedFieldDto.of(g, 0.95);
        }
        lowFields.add("gender");
        return ExtractedFieldDto.missing();
    }

    private ExtractedFieldDto<String> extractNationalityVn(String text) {
        Matcher m = NATIONALITY_VN_PATTERN.matcher(text);
        if (m.find()) {
            return ExtractedFieldDto.of(m.group(1).trim(), 0.90);
        }
        // Mặc định Việt Nam nếu không tìm thấy nhãn nhưng văn bản có tiếng Việt
        return ExtractedFieldDto.of("Việt Nam", 0.70);
    }

    private ExtractedFieldDto<String> extractAddressVn(String text) {
        Matcher m = ADDRESS_VN_PATTERN.matcher(text);
        if (m.find()) {
            String addr = m.group(1).trim();
            if (addr.length() >= 5) {
                return ExtractedFieldDto.of(addr, 0.85);
            }
        }
        return ExtractedFieldDto.invalid(null, 0.0, "Không tìm thấy nơi thường trú");
    }

    // =========================================================
    // Tiện ích
    // =========================================================

    private boolean containsMrz(String text) {
        return MRZ_LINE1.matcher(text).find() && MRZ_LINE2.matcher(text).find();
    }

    private LocalDate parseMrzDate(String yymmdd, List<String> warnings) {
        if (yymmdd == null || yymmdd.length() != 6) return null;
        try {
            int yy = Integer.parseInt(yymmdd.substring(0, 2));
            int mm = Integer.parseInt(yymmdd.substring(2, 4));
            int dd = Integer.parseInt(yymmdd.substring(4, 6));

            int currentYear = LocalDate.now().getYear();
            int currentYear2Digits = currentYear % 100;
            int year = (yy > currentYear2Digits) ? 1900 + yy : 2000 + yy;

            LocalDate dob = LocalDate.of(year, mm, dd);
            if (dob.isAfter(LocalDate.now())) {
                dob = LocalDate.of(1900 + yy, mm, dd);
            }
            if (dob.isAfter(LocalDate.now())) {
                warnings.add("Ngày sinh MRZ nằm trong tương lai – cần kiểm tra");
                return null;
            }
            return dob;
        } catch (DateTimeException | NumberFormatException e) {
            warnings.add("Ngày sinh MRZ không hợp lệ trên lịch: " + yymmdd);
            return null;
        }
    }

    /**
     * Tính check digit theo ICAO Doc 9303 (thuật toán 7-3-1 mod 10).
     *
     * @param input Chuỗi ký tự (chữ hoa A-Z, chữ số 0-9, hoặc ký tự đệm '&lt;')
     * @return Giá trị check digit từ 0 đến 9, hoặc -1 nếu đầu vào không hợp lệ.
     */
    static int computeIcaoCheckDigit(String input) {
        if (input == null || input.isEmpty()) return -1;
        int[] weights = {7, 3, 1};
        int sum = 0;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            int val;
            if (c >= '0' && c <= '9') {
                val = c - '0';
            } else if (c >= 'A' && c <= 'Z') {
                val = c - 'A' + 10;
            } else if (c == '<') {
                val = 0;
            } else {
                return -1;
            }
            sum += val * weights[i % 3];
        }
        return sum % 10;
    }

    private boolean isVietnameseNationality(String nationality) {
        if (nationality == null) return true; // Giả định Việt Nam nếu không rõ
        String lower = nationality.toLowerCase();
        return lower.contains("việt") || lower.contains("viet") || lower.contains("vietnam");
    }

    private double computeOverall(List<Double> confidences) {
        if (confidences.isEmpty()) return 0.0;
        return confidences.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }
}
