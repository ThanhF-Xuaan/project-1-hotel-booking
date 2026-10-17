package vn.edu.utc.hotel_booking.modules.ai.ocr.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import vn.edu.utc.hotel_booking.modules.operation.entity.enums.DocumentType;

/**
 * Request DTO để bóc tách thông tin giấy tờ tuỳ thân qua văn bản OCR thô.
 *
 * <p>Không lưu trữ ảnh – chỉ nhận văn bản OCR đã được đọc bởi scanner/camera phía client
 * (hoặc chuỗi MRZ tổng hợp). Kiểm tra file thực tế thông qua endpoint {@code /extract-file}.</p>
 */
public class OcrDocumentExtractRequestDto {

    /** Tên file gốc. Không được chứa path traversal (../, ..\). */
    @NotBlank(message = "Tên file không được để trống")
    @Size(max = 255, message = "Tên file không vượt quá 255 ký tự")
    private String fileName;

    /** MIME type được khai báo (image/jpeg, image/png, image/webp). */
    @NotBlank(message = "Content-type không được để trống")
    private String contentType;

    /** Kích thước file theo byte. Tối đa 5 MB (5,242,880 bytes). */
    @Min(value = 1, message = "Kích thước file phải lớn hơn 0")
    @Max(value = 5_242_880, message = "Kích thước file không vượt quá 5 MB")
    private long fileSizeBytes;

    /**
     * ID cơ sở lưu trú (tùy chọn – dành cho multi-property scope).
     * Nếu cung cấp, phải >= 1.
     */
    @Min(value = 1, message = "hotelId phải >= 1 nếu được cung cấp")
    private Integer hotelId;

    /**
     * Gợi ý loại giấy tờ (tùy chọn).
     * Nếu null, hệ thống tự suy luận qua cấu trúc văn bản.
     */
    private DocumentType documentTypeHint;

    /**
     * Văn bản OCR thô từ scanner/camera hoặc dòng MRZ tổng hợp.
     * Tối đa 10,000 ký tự để giới hạn payload.
     */
    @NotBlank(message = "Văn bản OCR không được để trống")
    @Size(max = 10_000, message = "Văn bản OCR không vượt quá 10,000 ký tự")
    private String rawOcrText;

    /**
     * Byte header của file (tùy chọn – để xác minh magic bytes).
     * Tối đa 16 bytes đầu là đủ để phát hiện định dạng thực tế.
     */
    private byte[] fileHeaderBytes;

    public OcrDocumentExtractRequestDto() {}

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }

    public Integer getHotelId() { return hotelId; }
    public void setHotelId(Integer hotelId) { this.hotelId = hotelId; }

    public DocumentType getDocumentTypeHint() { return documentTypeHint; }
    public void setDocumentTypeHint(DocumentType documentTypeHint) { this.documentTypeHint = documentTypeHint; }

    public String getRawOcrText() { return rawOcrText; }
    public void setRawOcrText(String rawOcrText) { this.rawOcrText = rawOcrText; }

    public byte[] getFileHeaderBytes() { return fileHeaderBytes; }
    public void setFileHeaderBytes(byte[] fileHeaderBytes) { this.fileHeaderBytes = fileHeaderBytes; }
}
