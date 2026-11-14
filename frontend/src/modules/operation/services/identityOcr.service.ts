import { z } from 'zod';
import { apiClient, getErrorStatus } from '../../../core/api/client';

const field = <T extends z.ZodType>(value: T) => z.object({
  value: value.nullable(), confidence: z.number().min(0).max(1), valid: z.boolean(),
}).nullish().transform(value => value ?? { value: null, confidence: 0, valid: false });
const draftSchema = z.object({
  fullName: field(z.string()), dateOfBirth: field(z.string()),
  gender: field(z.enum(['MALE', 'FEMALE', 'OTHER'])), nationality: field(z.string()),
  documentType: field(z.enum(['CCCD', 'PASSPORT', 'CMND', 'DRIVER_LICENSE', 'OTHER'])),
  documentNumber: field(z.string()), permanentAddress: field(z.string()),
  status: z.enum(['SUCCESS', 'LOW_CONFIDENCE_REVIEW_REQUIRED', 'UNREADABLE_DOCUMENT', 'INVALID_FILE']),
  requiresHumanVerification: z.literal(true), warnings: z.array(z.string()),
  missingOrLowConfidenceFields: z.array(z.string()),
});
export type IdentityOcrDraft = z.infer<typeof draftSchema>;
export const ocrFields = {
  fullName: 'Họ và tên', dateOfBirth: 'Ngày sinh', gender: 'Giới tính',
  nationality: 'Quốc tịch', documentType: 'Loại giấy tờ',
  documentNumber: 'Số giấy tờ', permanentAddress: 'Địa chỉ thường trú',
} as const;

export function identityOcrErrorMessage(error: unknown): string {
  const status = getErrorStatus(error);
  if (status === 503) return 'Không thể đọc ảnh: OCR quá thời gian xử lý hoặc đang bận. Vui lòng thử lại hoặc nhập thủ công.';
  if (status === 413) return 'Không thể đọc ảnh: dung lượng vượt 5 MiB. Vui lòng chọn ảnh nhỏ hơn.';
  if (status === 401) return 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại để quét giấy tờ.';
  return 'Không thể đọc ảnh. Vui lòng kiểm tra định dạng, giới hạn 5 MiB / 12 megapixel hoặc nhập thủ công.';
}

export async function extractIdentityImage(file: File, signal: AbortSignal): Promise<IdentityOcrDraft> {
  if (!['image/png', 'image/jpeg', 'image/webp'].includes(file.type) || file.size > 5 * 1024 * 1024 || !file.size) {
    throw new Error('INVALID_IMAGE');
  }
  const body = new FormData();
  body.append('file', file);
  const response = await apiClient.post<unknown>('/api/v1/ai/ocr/extract-file', body, {
    signal, timeout: 90000, headers: { 'Content-Type': undefined },
  });
  return draftSchema.parse(response.result);
}
