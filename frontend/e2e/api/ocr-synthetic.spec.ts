import { test, expect } from '@playwright/test';
import { readFile } from 'node:fs/promises';
import { getKeycloakToken, BACKEND_URL } from '../helpers/auth.helper';

test.describe('U13 Synthetic Document OCR & Receptionist Extraction', () => {
  let receptionToken: string;

  test.beforeAll(async () => {
    receptionToken = process.env.OCR_RECEPTION_TOKEN || await getKeycloakToken('reception_h1');
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
    expect(draft.fullName.value).toBe('NGUYEN VAN TEST');
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

  test('multipart image returns a human-reviewed draft according to the OCR flag', async ({ request }) => {
    const image = await readFile(new URL('../../../backend/src/test/resources/ai/ocr/cccd-synthetic.png', import.meta.url));
    const res = await request.post(`${BACKEND_URL}/api/v1/ai/ocr/extract-file`, {
      headers: { Authorization: `Bearer ${receptionToken}` },
      multipart: { file: { name: 'cccd-synthetic.png', mimeType: 'image/png', buffer: image } },
    });
    expect(res.status()).toBe(200);
    const draft = (await res.json()).result;
    expect(draft.requiresHumanVerification).toBe(true);
    if (process.env.OCR_IMAGE_ENABLED === 'true') {
      expect(draft.documentNumber.value).toBe('079190012345');
      expect(draft.fullName.value).toContain('NGUYEN VAN TEST');
      expect(draft.overallConfidence).toBeGreaterThan(0);
    } else {
      expect(draft.status).toBe('UNREADABLE_DOCUMENT');
      expect(draft.overallConfidence).toBe(0);
      expect(draft.missingOrLowConfidenceFields).toContain('rawOcrText');
    }
  });

  test('multipart invalid file returns a safe error and never a saved guest', async ({ request }) => {
    const res = await request.post(`${BACKEND_URL}/api/v1/ai/ocr/extract-file`, {
      headers: { Authorization: `Bearer ${receptionToken}` },
      multipart: { file: { name: 'synthetic.png', mimeType: 'image/png', buffer: Buffer.from('invalid synthetic image') } },
    });
    expect(res.status()).toBe(400);
    const body = await res.json();
    expect(body.result.status).toBe('INVALID_FILE');
    expect(body.result.requiresHumanVerification).toBe(true);
  });

});
