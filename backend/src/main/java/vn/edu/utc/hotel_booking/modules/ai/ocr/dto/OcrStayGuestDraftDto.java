package vn.edu.utc.hotel_booking.modules.ai.ocr.dto;

import java.time.LocalDate;
import java.util.List;
import vn.edu.utc.hotel_booking.modules.ai.ocr.enums.OcrExtractionStatus;
import vn.edu.utc.hotel_booking.modules.ai.ocr.model.ExtractedFieldDto;
import vn.edu.utc.hotel_booking.modules.operation.entity.enums.DocumentType;
import vn.edu.utc.hotel_booking.modules.operation.entity.enums.Gender;

/**
 * Kết quả bóc tách OCR – bản nháp để điền vào form đăng ký khách lưu trú (StayGuestCreateRequest).
 *
 * <p><strong>Bất biến kiến trúc U13:</strong>
 * {@code requiresHumanVerification} luôn luôn bằng {@code true}.
 * Kết quả này KHÔNG được tự động lưu vào database mà chỉ trả về cho nhân viên lễ tân
 * xác nhận trước khi nhập vào hệ thống.</p>
 */
public class OcrStayGuestDraftDto {

    // ---- 7 trường nghiệp vụ tương ứng StayGuestCreateRequest ----

    private ExtractedFieldDto<String> fullName;
    private ExtractedFieldDto<LocalDate> dateOfBirth;
    private ExtractedFieldDto<Gender> gender;
    private ExtractedFieldDto<String> nationality;
    private ExtractedFieldDto<DocumentType> documentType;
    private ExtractedFieldDto<String> documentNumber;
    /** Nơi ĐKTT / nơi thường trú – bắt buộc theo chuẩn BCA đối với khách Việt Nam. */
    private ExtractedFieldDto<String> permanentAddress;

    // ---- Metadata kiểm soát an toàn ----

    private OcrExtractionStatus status;
    private double overallConfidence;

    /**
     * Luôn luôn {@code true} – bất biến kiến trúc U13.
     * Nhân viên phải xác minh thủ công trước khi lưu.
     */
    private final boolean requiresHumanVerification = true;

    private List<String> missingOrLowConfidenceFields;
    private List<String> warnings;

    public OcrStayGuestDraftDto() {}

    public ExtractedFieldDto<String> getFullName() { return fullName; }
    public void setFullName(ExtractedFieldDto<String> fullName) { this.fullName = fullName; }

    public ExtractedFieldDto<LocalDate> getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(ExtractedFieldDto<LocalDate> dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public ExtractedFieldDto<Gender> getGender() { return gender; }
    public void setGender(ExtractedFieldDto<Gender> gender) { this.gender = gender; }

    public ExtractedFieldDto<String> getNationality() { return nationality; }
    public void setNationality(ExtractedFieldDto<String> nationality) { this.nationality = nationality; }

    public ExtractedFieldDto<DocumentType> getDocumentType() { return documentType; }
    public void setDocumentType(ExtractedFieldDto<DocumentType> documentType) { this.documentType = documentType; }

    public ExtractedFieldDto<String> getDocumentNumber() { return documentNumber; }
    public void setDocumentNumber(ExtractedFieldDto<String> documentNumber) { this.documentNumber = documentNumber; }

    public ExtractedFieldDto<String> getPermanentAddress() { return permanentAddress; }
    public void setPermanentAddress(ExtractedFieldDto<String> permanentAddress) { this.permanentAddress = permanentAddress; }

    public OcrExtractionStatus getStatus() { return status; }
    public void setStatus(OcrExtractionStatus status) { this.status = status; }

    public double getOverallConfidence() { return overallConfidence; }
    public void setOverallConfidence(double overallConfidence) { this.overallConfidence = overallConfidence; }

    /** Bất biến – luôn trả về {@code true}. */
    public boolean isRequiresHumanVerification() { return requiresHumanVerification; }

    public List<String> getMissingOrLowConfidenceFields() { return missingOrLowConfidenceFields; }
    public void setMissingOrLowConfidenceFields(List<String> missingOrLowConfidenceFields) {
        this.missingOrLowConfidenceFields = missingOrLowConfidenceFields;
    }

    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }
}
