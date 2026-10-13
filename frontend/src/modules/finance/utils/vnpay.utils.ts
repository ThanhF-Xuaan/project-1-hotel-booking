/**
 * Tiện ích và bảng tra cứu dữ liệu cổng thanh toán VNPAY theo tài liệu kỹ thuật VNPAY v2.1.0:
 * - Thông tin Thẻ ATM kiểm thử Sandbox (Ngân hàng NCB)
 * - Bảng đối chiếu mã phản hồi vnp_ResponseCode
 * - Định dạng ngày giờ vnp_PayDate
 */

export interface VnPayResponseInfo {
  code: string;
  title: string;
  message: string;
  isSuccess: boolean;
}

/** Thông tin thẻ ATM nội địa test chuẩn Docs VNPay Sandbox (Ngân hàng NCB) */
export const VNPAY_ATM_TEST_CARD = {
  bankName: 'Ngân hàng TMCP Quốc Dân (NCB)',
  bankCode: 'NCB',
  cardNumber: '9704198526191432198',
  formattedCardNumber: '9704 1985 2619 1432 198',
  cardHolder: 'NGUYEN VAN A',
  issueDate: '07/15',
  otp: '123456',
};

/** Bảng tra cứu mã phản hồi giao dịch vnp_ResponseCode theo tài liệu kỹ thuật VNPAY */
export const VNPAY_RESPONSE_CODE_MAP: Record<string, { title: string; message: string; isSuccess: boolean }> = {
  '00': {
    title: 'Giao dịch thành công',
    message: 'Giao dịch thanh toán đã được VNPay và Ngân hàng phát hành xác thực thành công.',
    isSuccess: true,
  },
  '07': {
    title: 'Trừ tiền thành công / Nghi ngờ gian lận',
    message: 'Trừ tiền thành công. Giao dịch bị nghi ngờ (liên quan tới gian lận, giao dịch bất thường).',
    isSuccess: false,
  },
  '09': {
    title: 'Chưa đăng ký InternetBanking',
    message: 'Thẻ / Tài khoản của khách hàng chưa đăng ký dịch vụ InternetBanking tại ngân hàng.',
    isSuccess: false,
  },
  '10': {
    title: 'Xác thực thông tin sai quá 3 lần',
    message: 'Khách hàng xác thực thông tin thẻ/tài khoản không đúng quá 3 lần quy định.',
    isSuccess: false,
  },
  '11': {
    title: 'Hết hạn chờ thanh toán',
    message: 'Đã hết thời gian chờ thanh toán. Quý khách vui lòng tạo phiên thanh toán mới.',
    isSuccess: false,
  },
  '12': {
    title: 'Thẻ / Tài khoản bị khóa',
    message: 'Thẻ hoặc tài khoản ngân hàng của khách hàng hiện đang bị khóa.',
    isSuccess: false,
  },
  '13': {
    title: 'Sai mật khẩu xác thực OTP',
    message: 'Quý khách đã nhập sai mật khẩu xác thực giao dịch (OTP). Xin vui lòng thử lại.',
    isSuccess: false,
  },
  '24': {
    title: 'Khách hàng hủy giao dịch',
    message: 'Khách hàng đã bấm Hủy bỏ giao dịch trên cổng thanh toán VNPay.',
    isSuccess: false,
  },
  '51': {
    title: 'Tài khoản không đủ số dư',
    message: 'Tài khoản của quý khách không đủ số dư để thực hiện giao dịch.',
    isSuccess: false,
  },
  '65': {
    title: 'Vượt hạn mức giao dịch',
    message: 'Tài khoản của Quý khách đã vượt quá hạn mức thanh toán trong ngày.',
    isSuccess: false,
  },
  '75': {
    title: 'Ngân hàng đang bảo trì',
    message: 'Ngân hàng thanh toán hiện đang bảo trì hệ thống. Vui lòng thử lại sau.',
    isSuccess: false,
  },
  '79': {
    title: 'Nhập sai mật khẩu quá số lần',
    message: 'Khách hàng nhập sai mật khẩu thanh toán quá số lần quy định của ngân hàng.',
    isSuccess: false,
  },
  '99': {
    title: 'Lỗi phát sinh từ ngân hàng',
    message: 'Lỗi phát sinh không xác định từ hệ thống ngân hàng hoặc cổng VNPay.',
    isSuccess: false,
  },
};

/** Tra cứu ý nghĩa mã phản hồi VNPay */
export function getVnPayResponseInfo(code?: string | null): VnPayResponseInfo {
  if (!code) {
    return {
      code: 'UNKNOWN',
      title: 'Chưa có mã kết quả',
      message: 'Giao dịch chưa nhận được phản hồi từ cổng thanh toán.',
      isSuccess: false,
    };
  }

  const found = VNPAY_RESPONSE_CODE_MAP[code];
  if (found) {
    return {
      code,
      title: found.title,
      message: found.message,
      isSuccess: found.isSuccess,
    };
  }

  return {
    code,
    title: `Mã phản hồi (${code})`,
    message: `Phản hồi từ cổng VNPay với mã kết quả ${code}`,
    isSuccess: false,
  };
}

/** Chuyển đổi định dạng ngày vnp_PayDate yyyyMMddHHmmss thành dd/MM/yyyy HH:mm:ss */
export function formatVnPayDate(raw?: string | null): string {
  if (!raw) return '—';
  const clean = raw.trim();
  if (clean.length < 14) return clean;

  const yyyy = clean.slice(0, 4);
  const MM = clean.slice(4, 6);
  const dd = clean.slice(6, 8);
  const HH = clean.slice(8, 10);
  const mm = clean.slice(10, 12);
  const ss = clean.slice(12, 14);

  return `${dd}/${MM}/${yyyy} ${HH}:${mm}:${ss}`;
}

/** Parse JSON an toàn cho rawCallbackPayload */
export function parseVnPayRawPayload(raw?: string | null): Record<string, string> | null {
  if (!raw) return null;
  try {
    const parsed = JSON.parse(raw);
    if (typeof parsed === 'object' && parsed !== null) {
      return parsed as Record<string, string>;
    }
    return null;
  } catch {
    return null;
  }
}
