/**
 * Biểu thức chính quy cho Số điện thoại Việt Nam:
 * - Số cố định / máy bàn (11 số): 02x + 8 số (hoặc +842x + 8 số) (VD: 02466668888, 02838220000)
 * - Số di động (10 số): 03x, 05x, 07x, 08x, 09x (hoặc +843x, ...) (VD: 0912345678)
 */
export const VIETNAMESE_PHONE_REGEX = /^(0|\+84)(2[0-9]{9}|[35789][0-9]{8})$/;

/**
 * Loại bỏ khoảng trắng, dấu gạch nối, dấu chấm để chuẩn hóa trước khi kiểm tra
 */
export const cleanPhoneNumber = (phone: string): string => {
  return phone.replace(/[\s.-]/g, '');
};

/**
 * Kiểm tra tính hợp lệ của số điện thoại Việt Nam
 * @param phone Chuỗi số điện thoại cần kiểm tra
 * @param isRequired Nếu true, chuỗi rỗng sẽ coi là không hợp lệ; nếu false (optional), chuỗi rỗng coi là hợp lệ
 */
export const isValidVietnamesePhone = (phone?: string | null, isRequired = false): boolean => {
  if (!phone || !phone.trim()) {
    return !isRequired;
  }
  const cleaned = cleanPhoneNumber(phone.trim());
  return VIETNAMESE_PHONE_REGEX.test(cleaned);
};

export const PHONE_INVALID_MESSAGE =
  'Số điện thoại không đúng định dạng (Ví dụ: 02466668888 đối với máy bàn hoặc 0912345678 đối với di động).';
