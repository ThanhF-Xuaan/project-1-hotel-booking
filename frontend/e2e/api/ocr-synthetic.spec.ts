import { test, expect } from '@playwright/test';
import { getKeycloakToken, BACKEND_URL } from '../helpers/auth.helper';

test.describe('U13 Synthetic Document OCR & Receptionist Extraction', () => {
  let receptionToken: string;

  test.beforeAll(async () => {
    receptionToken = await getKeycloakToken('reception_h1');
  });

  test('should extract CCCD fields from synthetic OCR text with draft status', async ({ request }) => {
    const rawText = `
Căn cước công dân
Họ và tên: NGUYEN VAN TEST
Ngày sinh: 15/06/1990
Giới tính: Nam
Quốc tịch: Việt Nam
Số CCCD: 079190012345
Nơi thường trú: 123 Đường Test, Quận 1, TP. Hồ Chí Minh
    `.trim();

    const res = await request.post(`${BACKEND_URL}/api/v1/ai/ocr/extract`, {
      headers: { Authorization: `Bearer ${receptionToken}` },
      data: {
        fileName: 'synthetic_cccd_sample.jpg',
        contentType: 'image/jpeg',
        fileSizeBytes: 102400,
        hotelId: 1,
        rawOcrText: rawText,
      },
    });

    expect(res.status()).toBe(200);
    const body = await res.json();
    expect(body.code).toBe(200);
    const draft = body.result;
    expect(draft.status).toBe('SUCCESS');
    expect(draft.requiresHumanVerification).toBe(true);
    expect(draft.fullName.value).toContain('NGUYEN VAN TEST');
    expect(draft.documentNumber.value).toBe('079190012345');
    expect(draft.gender.value).toBe('MALE');
  });

  test('should return UNREADABLE_DOCUMENT for empty or garbled OCR text', async ({ request }) => {
    const res = await request.post(`${BACKEND_URL}/api/v1/ai/ocr/extract`, {
      headers: { Authorization: `Bearer ${receptionToken}` },
      data: {
        fileName: 'unreadable_noise.jpg',
        contentType: 'image/jpeg',
        fileSizeBytes: 10240,
        hotelId: 1,
        rawOcrText: '??? --- @@@ !!! ###',
      },
    });

    expect(res.status()).toBe(200);
    const body = await res.json();
    expect(body.result.status).toBe('UNREADABLE_DOCUMENT');
  });

  test('should reject path traversal in fileName with 400 Bad Request', async ({ request }) => {
    const res = await request.post(`${BACKEND_URL}/api/v1/ai/ocr/extract`, {
      headers: { Authorization: `Bearer ${receptionToken}` },
      data: {
        fileName: '../../../etc/passwd',
        contentType: 'image/jpeg',
        fileSizeBytes: 10240,
        hotelId: 1,
        rawOcrText: 'Sample text',
      },
    });

    expect(res.status()).toBe(400);
  });
});
