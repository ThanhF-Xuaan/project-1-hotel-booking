/**
 * Tiện ích định dạng ngày giờ chuẩn Việt Nam (Múi giờ Asia/Ho_Chi_Minh - GMT+7)
 */

export const VN_TIMEZONE = 'Asia/Ho_Chi_Minh';

/**
 * Format ngày giờ đầy đủ theo chuẩn Việt Nam (DD/MM/YYYY HH:mm).
 * Đảm bảo hiển thị đúng múi giờ khách sạn kể cả khi người dùng truy cập từ nước ngoài.
 *
 * @param dateInput Chuỗi ISO date, timestamp hoặc Date object
 * @returns Chuỗi định dạng "DD/MM/YYYY HH:mm" hoặc "—" nếu rỗng/không hợp lệ
 */
export function formatVnDateTime(dateInput: string | number | Date | null | undefined): string {
  if (!dateInput) return '—';
  const date = typeof dateInput === 'string' || typeof dateInput === 'number' ? new Date(dateInput) : dateInput;
  if (isNaN(date.getTime())) return '—';

  return new Intl.DateTimeFormat('vi-VN', {
    timeZone: VN_TIMEZONE,
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false
  }).format(date);
}

/**
 * Format ngày giờ kèm giây theo chuẩn Việt Nam (DD/MM/YYYY HH:mm:ss).
 *
 * @param dateInput Chuỗi ISO date, timestamp hoặc Date object
 * @returns Chuỗi định dạng "DD/MM/YYYY HH:mm:ss" hoặc "—" nếu rỗng/không hợp lệ
 */
export function formatVnDateTimeWithSeconds(dateInput: string | number | Date | null | undefined): string {
  if (!dateInput) return '—';
  const date = typeof dateInput === 'string' || typeof dateInput === 'number' ? new Date(dateInput) : dateInput;
  if (isNaN(date.getTime())) return '—';

  return new Intl.DateTimeFormat('vi-VN', {
    timeZone: VN_TIMEZONE,
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false
  }).format(date);
}

/**
 * Format chỉ ngày (DD/MM/YYYY) theo múi giờ Việt Nam.
 *
 * @param dateInput Chuỗi ISO date, timestamp hoặc Date object
 * @returns Chuỗi định dạng "DD/MM/YYYY" hoặc "—" nếu rỗng/không hợp lệ
 */
export function formatVnDate(dateInput: string | number | Date | null | undefined): string {
  if (!dateInput) return '—';
  const date = typeof dateInput === 'string' || typeof dateInput === 'number' ? new Date(dateInput) : dateInput;
  if (isNaN(date.getTime())) return '—';

  return new Intl.DateTimeFormat('vi-VN', {
    timeZone: VN_TIMEZONE,
    day: '2-digit',
    month: '2-digit',
    year: 'numeric'
  }).format(date);
}

/**
 * Format chỉ giờ:phút (HH:mm) theo múi giờ Việt Nam.
 *
 * @param dateInput Chuỗi ISO date, timestamp hoặc Date object
 * @returns Chuỗi định dạng "HH:mm" hoặc "—" nếu rỗng/không hợp lệ
 */
export function formatVnTime(dateInput: string | number | Date | null | undefined): string {
  if (!dateInput) return '—';
  const date = typeof dateInput === 'string' || typeof dateInput === 'number' ? new Date(dateInput) : dateInput;
  if (isNaN(date.getTime())) return '—';

  return new Intl.DateTimeFormat('vi-VN', {
    timeZone: VN_TIMEZONE,
    hour: '2-digit',
    minute: '2-digit',
    hour12: false
  }).format(date);
}

