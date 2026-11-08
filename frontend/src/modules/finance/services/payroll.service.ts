import { apiClient } from '../../../core/api/client'

export const PAYROLL_IMPORT_ROLES = ['CHAIN_ADMIN', 'PROPERTY_MANAGER']

export const PAYROLL_CSV_HEADER =
  'staff_id,period_month,period_year,base_salary,bonus,deductions,net_salary'

export interface PayrollImportIssue {
  row: number
  error: string
}

export async function importPayroll(file: File): Promise<PayrollImportIssue[]> {
  const data = new FormData()
  data.append('file', file)
  const { result } = await apiClient.post<PayrollImportIssue[]>('/api/v1/payroll/import', data, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  if (!Array.isArray(result)) throw new Error('Không nhận được kết quả nhập.')
  return result
}

// Backend error details can contain salary values and staff identifiers.
export function payrollIssueLabel(issue: PayrollImportIssue) {
  const message = issue.error || ''
  if (message === 'INVALID_CSV_HEADER') return 'Tiêu đề CSV không khớp mẫu.'
  if (message.startsWith('File is empty')) return 'File CSV rỗng.'
  if (message.includes('phạm vi khách sạn')) return 'Tài khoản chưa được gán khách sạn hợp lệ.'
  if (message.startsWith('Missing columns') || message.startsWith('Extra columns')) return 'Dòng phải có đúng 7 cột.'
  if (message.startsWith('Invalid period month')) return 'Tháng phải từ 1 đến 12.'
  if (message.startsWith('Invalid period year')) return 'Năm phải từ 2000 đến 2100.'
  if (message.startsWith('Salary components')) return 'Lương cơ bản, thưởng và khấu trừ phải không âm.'
  if (message.startsWith('Net salary mismatch')) return 'Lương thực nhận phải bằng lương cơ bản cộng thưởng trừ khấu trừ.'
  if (message.startsWith('Duplicate staff ID')) return 'Trùng nhân viên và kỳ lương trong file.'
  if (message.startsWith('Staff ID does not exist')) return 'Không tìm thấy nhân viên.'
  if (message.startsWith('Staff ID')) return 'Nhân viên nằm ngoài phạm vi khách sạn được phép.'
  if (message.includes('SALARY_AMOUNT_NOT_REPRESENTABLE')) return 'Số tiền vượt giới hạn hoặc độ chính xác được hỗ trợ.'
  if (message.startsWith('Invalid number format')) return 'Kiểm tra định dạng số trong dòng.'
  return issue.row === 0 ? 'Không đọc được file CSV. Kiểm tra định dạng và đối chiếu dữ liệu trước khi gửi lại.'
    : 'Dữ liệu dòng này không hợp lệ.'
}
