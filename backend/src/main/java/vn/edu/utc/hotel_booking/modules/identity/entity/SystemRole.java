package vn.edu.utc.hotel_booking.modules.identity.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Danh mục Vai trò Hệ thống Chuẩn hóa theo SRS Mục 1.2.1.1.
 * Đóng vai trò là Single Source of Truth (SSOT) cho toàn bộ hệ thống phân quyền RBAC.
 * Tuyệt đối cấm xóa mềm hoặc sửa mã code của các vai trò này qua API.
 */
@Getter
@RequiredArgsConstructor
public enum SystemRole {
    CHAIN_EXECUTIVE("CHAIN_EXECUTIVE", "Ban Điều hành Chuỗi — Báo cáo tổng thể toàn chuỗi, phê duyệt chính sách"),
    CHAIN_ADMIN("CHAIN_ADMIN", "Quản trị viên Hệ thống Chuỗi — Quản lý tổ chức, danh mục và phân quyền toàn hệ thống"),
    REGION_MANAGER("REGION_MANAGER", "Giám đốc Quản lý Vùng — Giám sát cụm khách sạn cơ sở, phê duyệt ngoại lệ vùng"),
    PROPERTY_MANAGER("PROPERTY_MANAGER", "Tổng Quản lý Khách sạn Cơ sở — Điều hành toàn diện hoạt động khách sạn"),
    RECEPTIONIST("RECEPTIONIST", "Bộ phận Lễ tân Tiền sảnh (Front Office) — Check-in/out, đổi phòng, Folio, khai báo tạm trú"),
    SALES_GROUP("SALES_GROUP", "Bộ phận Kinh doanh Đoàn & Doanh nghiệp — Báo giá, hợp đồng, Room Block, Rooming List"),
    HOUSEKEEPING("HOUSEKEEPING", "Bộ phận Buồng phòng — Trạng thái dọn dẹp buồng phòng, Minibar, giặt là"),
    F_AND_B("F_AND_B", "Bộ phận Ẩm thực & Dịch vụ — Tiếp nhận order nhà hàng/quầy bar và ghi nợ tự động vào Folio"),
    ENGINEERING("ENGINEERING", "Bộ phận Kỹ thuật & Bảo trì — Xử lý sự cố, khóa phòng ngừng khai thác (OOO), chỉ số điện nước"),
    FINANCE("FINANCE", "Bộ phận Tài chính & Kế toán — Đối soát cọc, thanh toán, lập hóa đơn VAT, kiểm toán đêm (Night Audit)");

    private final String code;
    private final String description;

    public String getAuthority() {
        return "ROLE_" + code;
    }

    public static boolean isSystemRole(String roleCode) {
        if (roleCode == null) return false;
        String cleanCode = roleCode.trim();
        for (SystemRole r : values()) {
            if (r.code.equalsIgnoreCase(cleanCode)) return true;
        }
        // Hỗ trợ alias tương thích với SRS (REGIONAL_MANAGER và FRONT_OFFICE)
        if ("REGIONAL_MANAGER".equalsIgnoreCase(cleanCode) || "FRONT_OFFICE".equalsIgnoreCase(cleanCode)) {
            return true;
        }
        return false;
    }

    public static Set<String> getSystemRoleCodes() {
        return Arrays.stream(values()).map(SystemRole::getCode).collect(Collectors.toSet());
    }
}
