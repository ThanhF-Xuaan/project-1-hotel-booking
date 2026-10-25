import { test, expect } from '@playwright/test';
import { getKeycloakToken, BACKEND_URL } from '../helpers/auth.helper';

test.describe('U14 synthetic admin evaluation', () => {
  let adminToken: string;

  test.beforeAll(async () => {
    adminToken = await getKeycloakToken('admin');
  });

  test('should reject non-admin callers even when they submit consent and another guest id', async ({ request }) => {
    for (const username of ['manager_h1', 'reception_h1', 'test_customer']) {
      const token = await getKeycloakToken(username);
      for (const route of ['recommend', 'evaluate-baseline']) {
        const res = await request.post(`${BACKEND_URL}/api/v1/ai/personalization/${route}`, {
          headers: { Authorization: `Bearer ${token}` },
          data: { guestId: 999, consentGranted: true, stayHistory: [] },
        });
        expect(res.status(), `${username} must not access ${route}`).toBe(403);
      }
    }
  });

  test('should require explicit user consent before processing stay history', async ({ request }) => {
    const res = await request.post(`${BACKEND_URL}/api/v1/ai/personalization/recommend`, {
      headers: { Authorization: `Bearer ${adminToken}` },
      data: {
        guestId: 101,
        targetHotelId: 1,
        consentGranted: false,
        stayHistory: [
          {
            hotelId: 1,
            hotelRoomTypeId: 1,
            roomTypeCode: 'DELUXE',
            roomTypeName: 'Deluxe City View',
            checkInDate: '2026-05-01',
            checkOutDate: '2026-05-04',
            adults: 2,
            children: 0,
            usedServiceCodes: ['SPA_BODY', 'AIRPORT_TRANSFER'],
          },
        ],
      },
    });

    expect(res.status()).toBe(200);
    const body = await res.json();
    expect(body.code).toBe(200);
    expect(body.result.status).toBe('CONSENT_REQUIRED');
    expect(body.result.roomTypeSuggestions).toHaveLength(0);
    expect(body.result.warnings).toContain(
      'CONSENT_REQUIRED: Khách hàng chưa cấp quyền sử dụng lịch sử lưu trú để cá nhân hóa gợi ý.'
    );
  });

  test('should produce stable personalized recommendations when consent is granted', async ({ request }) => {
    const res = await request.post(`${BACKEND_URL}/api/v1/ai/personalization/recommend`, {
      headers: { Authorization: `Bearer ${adminToken}` },
      data: {
        guestId: 101,
        targetHotelId: 1,
        consentGranted: true,
        referenceDate: '2026-10-24',
        maxSuggestions: 3,
        stayHistory: [
          {
            hotelId: 1,
            hotelRoomTypeId: 1,
            roomTypeCode: 'DELUXE',
            roomTypeName: 'Deluxe City View',
            checkInDate: '2026-08-01',
            checkOutDate: '2026-08-04',
            adults: 2,
            children: 0,
            preferredFeatureCodes: ['SEA_VIEW', 'BALCONY'],
            usedServiceCodes: ['BUFFET_BREAKFAST', 'SPA_BODY'],
          },
          {
            hotelId: 1,
            hotelRoomTypeId: 2,
            roomTypeCode: 'SUITE',
            roomTypeName: 'Executive Suite',
            checkInDate: '2026-09-10',
            checkOutDate: '2026-09-13',
            adults: 2,
            children: 0,
            preferredFeatureCodes: ['HIGH_FLOOR', 'BATHTUB'],
            usedServiceCodes: ['AIRPORT_TRANSFER', 'BUFFET_BREAKFAST'],
          },
        ],
      },
    });

    expect(res.status()).toBe(200);
    const body = await res.json();
    expect(body.code).toBe(200);
    expect(body.result.status).toBe('PERSONALIZED_READY');
    expect(body.result.guestId).toBe(101);
    expect(body.result.detectedSegment).toBeDefined();
    expect(body.result.featureSuggestions.length).toBeGreaterThan(0);
  });
});
