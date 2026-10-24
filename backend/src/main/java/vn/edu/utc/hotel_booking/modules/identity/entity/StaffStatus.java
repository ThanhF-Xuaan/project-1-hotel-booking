package vn.edu.utc.hotel_booking.modules.identity.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;

/**
 * Trạng thái vòng đời tài khoản nhân viên khách sạn theo SRS US26 & FR-IAM-05.
 * Tích hợp statusId số nguyên theo Mục 1 quy chuẩn Backend (02-backend-standards) để tối ưu chỉ mục DB.
 */
@Getter
@RequiredArgsConstructor
public enum StaffStatus {
    RESIGNED("RESIGNED", 0, "Đã nghỉ việc / Chấm dứt hợp đồng lao động — Thu hồi vĩnh viễn quyền đăng nhập"),
    ACTIVE("ACTIVE", 1, "Đang làm việc / Hoạt động bình thường — Phân ca và sử dụng đầy đủ quyền hạn"),
    SUSPENDED("SUSPENDED", 2, "Tạm đình chỉ công tác / Khóa tài khoản khẩn cấp — Thu hồi tức thì tất cả phiên đăng nhập");

    private final String code;
    private final Integer statusId;
    private final String description;

    public static StaffStatus fromCode(String code) {
        if (code == null) return null;
        for (StaffStatus s : values()) {
            if (s.code.equalsIgnoreCase(code.trim())) return s;
        }
        throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Trạng thái nhân sự không hợp lệ: " + code);
    }
}
