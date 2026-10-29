import { z } from 'zod';
import { apiClient } from '../../../core/api/client';

const field = <T extends z.ZodType>(value: T) => z.object({
  value: value.nullable(), confidence: z.number().min(0).max(1), valid: z.boolean(),
});
const draftSchema = z.object({
  fullName: field(z.string()), dateOfBirth: field(z.iso.date()),
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

export async function extractIdentityImage(file: File, signal: AbortSignal): Promise<IdentityOcrDraft> {
  if (!['image/png', 'image/jpeg', 'image/webp'].includes(file.type) || file.size > 5 * 1024 * 1024 || !file.size) {
    throw new Error('INVALID_IMAGE');
  }
  const body = new FormData();
  body.append('file', file);
  const response = await apiClient.post<unknown>('/api/v1/ai/ocr/extract-file', body, {
    signal, headers: { 'Content-Type': undefined },
  });
  return draftSchema.parse(response.result);
}
