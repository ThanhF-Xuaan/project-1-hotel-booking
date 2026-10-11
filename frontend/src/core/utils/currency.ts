/**
 * Tiện ích định dạng tiền tệ chuẩn Việt Nam (Dấu chấm ngăn cách hàng nghìn)
 * Ví dụ: 1000000 -> 1.000.000 đ
 */

/**
 * Định dạng số thành chuỗi tiền tệ có dấu chấm phân cách hàng nghìn.
 * @param amount Số tiền (number hoặc string)
 * @param includeSymbol Có hiển thị đơn vị 'đ' ở cuối hay không (mặc định: true)
 * @returns Chuỗi tiền tệ, ví dụ "1.000.000 đ" hoặc "1.000.000"
 */
export function formatCurrency(
  amount: number | string | null | undefined,
  includeSymbol: boolean = true
): string {
  if (amount === null || amount === undefined || amount === '') {
    return includeSymbol ? '0 đ' : '0';
  }

  const num = typeof amount === 'number' ? amount : Number(amount);
  if (isNaN(num)) {
    return includeSymbol ? '0 đ' : '0';
  }

  // Format dùng chuẩn vi-VN (ngăn cách hàng nghìn bằng dấu chấm '.')
  const formatted = new Intl.NumberFormat('vi-VN').format(Math.round(num));
  return includeSymbol ? `${formatted} đ` : formatted;
}

/**
 * Định dạng số thành chuỗi hiển thị trong ô nhập liệu (không có ký hiệu 'đ').
 * Ví dụ: 1000000 -> "1.000.000"
 */
export function formatCurrencyInput(amount: number | string | null | undefined): string {
  if (amount === null || amount === undefined || amount === '') {
    return '';
  }
  const num = typeof amount === 'number' ? amount : Number(String(amount).replace(/[^\d]/g, ''));
  if (isNaN(num) || num === 0) {
    return '0';
  }
  return new Intl.NumberFormat('vi-VN').format(Math.round(num));
}

/**
 * Chuyển chuỗi tiền tệ (có dấu chấm, phẩy hoặc chữ đ) thành số nguyên.
 * Ví dụ: "1.000.000 đ" -> 1000000
 */
export function parseCurrency(value: string | number | null | undefined): number {
  if (value === null || value === undefined) return 0;
  if (typeof value === 'number') return isNaN(value) ? 0 : value;

  const digits = String(value).replace(/[^\d]/g, '');
  if (!digits) return 0;
  const parsed = parseInt(digits, 10);
  return isNaN(parsed) ? 0 : parsed;
}

/**
 * Tìm vị trí con trỏ trong chuỗi đã định dạng dựa trên số lượng chữ số đứng trước con trỏ.
 */
export function getCursorPosFromDigitCount(formattedStr: string, digitCount: number): number {
  if (digitCount <= 0) return 0;
  let count = 0;
  for (let i = 0; i < formattedStr.length; i++) {
    if (/\d/.test(formattedStr[i])) {
      count++;
      if (count === digitCount) {
        return i + 1;
      }
    }
  }
  return formattedStr.length;
}
