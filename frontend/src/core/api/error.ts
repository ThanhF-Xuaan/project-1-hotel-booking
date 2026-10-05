import axios from 'axios';

export interface ApiErrorInfo {
  /** ErrorCode từ ApiResponse (vd: 8004, 8007, 8008) — undefined nếu lỗi mạng/không có body */
  code?: number;
  /** Message hiển thị trên popup */
  message: string;
}

/**
 * Trích (code, message) từ lỗi Axios theo chuẩn ApiResponse { code, message } của BE.
 * Dùng cho popup lỗi thanh toán (§6: popup → hủy → làm lại).
 */
export function getApiErrorMessage(err: unknown, fallback = 'Có lỗi xảy ra, vui lòng thử lại'): ApiErrorInfo {
  if (axios.isAxiosError(err)) {
    const data = err.response?.data as { code?: number; message?: string } | undefined;
    if (data?.message) {
      return { code: data.code, message: data.message };
    }
    if (err.response) {
      return { message: `${fallback} (HTTP ${err.response.status})` };
    }
    return { message: 'Không kết nối được máy chủ — vui lòng kiểm tra mạng' };
  }
  return { message: fallback };
}

export default getApiErrorMessage;
