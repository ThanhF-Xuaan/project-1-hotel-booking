package vn.edu.utc.hotel_booking.modules.finance.mapper;

public final class VnPayResponseHelper {

    private VnPayResponseHelper() {}

    public static String getVnPayResponseDescription(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return switch (code) {
            case "00" -> "Giao dịch thành công";
            case "07" -> "Trừ tiền thành công. Giao dịch bị nghi ngờ (bất thường)";
            case "09" -> "Thẻ/Tài khoản chưa đăng ký dịch vụ InternetBanking";
            case "10" -> "Khách hàng xác thực thông tin thẻ/tài khoản sai quá 3 lần";
            case "11" -> "Đã hết hạn chờ thanh toán";
            case "12" -> "Thẻ/Tài khoản của khách hàng bị khóa";
            case "13" -> "Quý khách nhập sai mật khẩu xác thực OTP";
            case "24" -> "Khách hàng hủy giao dịch";
            case "51" -> "Tài khoản không đủ số dư";
            case "65" -> "Tài khoản vượt quá hạn mức giao dịch trong ngày";
            case "75" -> "Ngân hàng thanh toán đang bảo trì";
            case "79" -> "Nhập sai mật khẩu thanh toán quá số lần quy định";
            default -> "Mã phản hồi (" + code + ")";
        };
    }
}
