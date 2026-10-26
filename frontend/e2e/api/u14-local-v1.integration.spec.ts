import { test, expect } from '@playwright/test';
import { getAcceptanceDatabaseDigest } from '../helpers/acceptance-db.helper';
import { getKeycloakToken, BACKEND_URL } from '../helpers/auth.helper';

type ConsentView = {
  hotelId: number;
  status: string;
  granted: boolean;
  revision: number;
  bindingVersion: number;
  currentPolicyVersion: string;
  dataMode: string;
};

const CONSENT_URL = `${BACKEND_URL}/api/v1/me/personalization-consent`;
const RECOMMENDATIONS_URL = `${BACKEND_URL}/api/v1/me/personalization/recommendations`;
const HOTEL_ID = 1;

async function readConsent(
  request: import('@playwright/test').APIRequestContext,
  token: string,
  hotelId = HOTEL_ID
): Promise<ConsentView> {
  const response = await request.get(`${CONSENT_URL}?hotelId=${hotelId}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  expect(response.status()).toBe(200);
  const body = await response.json();
  expect(body.code).toBe(200);
  return body.result as ConsentView;
}

async function updateConsent(
  request: import('@playwright/test').APIRequestContext,
  token: string,
  current: ConsentView,
  granted: boolean
) {
  return request.put(CONSENT_URL, {
    headers: { Authorization: `Bearer ${token}` },
    data: {
      hotelId: current.hotelId,
      granted,
      expectedRevision: current.revision,
      expectedBindingVersion: current.bindingVersion,
      ...(granted ? { policyVersion: current.currentPolicyVersion } : {}),
    },
  });
}

async function ensureRevoked(
  request: import('@playwright/test').APIRequestContext,
  token: string
) {
  const current = await readConsent(request, token);
  if (current.granted) {
    const response = await updateConsent(request, token, current, false);
    expect(response.status()).toBe(200);
  }
}

test.describe('U14 local v1 — real API, Keycloak and PostgreSQL', () => {
  test.describe.configure({ mode: 'serial' });

  let customerToken: string;
  let secondCustomerToken: string;
  let unlinkedCustomerToken: string;
  let managerToken: string;

  test.beforeAll(async () => {
    customerToken = await getKeycloakToken('test_customer');
    secondCustomerToken = await getKeycloakToken('test_customer_b');
    unlinkedCustomerToken = await getKeycloakToken('test_customer_unlinked');
    managerToken = await getKeycloakToken('manager_h2');
  });

  test('requires CUSTOMER, one allowlisted hotelId and private no-store responses', async ({ request }) => {
    await ensureRevoked(request, customerToken);
    await ensureRevoked(request, secondCustomerToken);

    const anonymous = await request.get(`${CONSENT_URL}?hotelId=${HOTEL_ID}`);
    expect(anonymous.status()).toBe(401);

    const staffConsent = await request.get(`${CONSENT_URL}?hotelId=${HOTEL_ID}`, {
      headers: { Authorization: `Bearer ${managerToken}` },
    });
    expect(staffConsent.status()).toBe(403);

    const staffRecommendation = await request.get(`${RECOMMENDATIONS_URL}?hotelId=${HOTEL_ID}`, {
      headers: { Authorization: `Bearer ${managerToken}` },
    });
    expect(staffRecommendation.status()).toBe(403);

    const duplicateHotel = await request.get(`${CONSENT_URL}?hotelId=1&hotelId=2`, {
      headers: { Authorization: `Bearer ${customerToken}` },
    });
    expect(duplicateHotel.status()).toBe(400);

    const consent = await request.get(`${CONSENT_URL}?hotelId=${HOTEL_ID}`, {
      headers: { Authorization: `Bearer ${customerToken}` },
    });
    expect(consent.status()).toBe(200);
    expect(consent.headers()['cache-control']).toContain('private');
    expect(consent.headers()['cache-control']).toContain('no-store');
  });

  test('fails closed for an unlinked CUSTOMER and rejects an unknown hotel without writing DB data', async ({ request }) => {
    const databaseBefore = getAcceptanceDatabaseDigest();
    const unlinkedConsent = await request.get(`${CONSENT_URL}?hotelId=${HOTEL_ID}`, {
      headers: { Authorization: `Bearer ${unlinkedCustomerToken}` },
    });
    expect(unlinkedConsent.status()).toBe(409);
    expect((await unlinkedConsent.json()).result.reasonCode).toBe('IDENTITY_LINK_REQUIRED');
    expect(unlinkedConsent.headers()['cache-control']).toContain('no-store');

    const unlinkedRecommendation = await request.get(`${RECOMMENDATIONS_URL}?hotelId=${HOTEL_ID}`, {
      headers: { Authorization: `Bearer ${unlinkedCustomerToken}` },
    });
    expect(unlinkedRecommendation.status()).toBe(409);
    expect((await unlinkedRecommendation.json()).result.reasonCode).toBe('IDENTITY_LINK_REQUIRED');
    expect(unlinkedRecommendation.headers()['cache-control']).toContain('no-store');

    const unknownHotelConsent = await request.get(`${CONSENT_URL}?hotelId=32767`, {
      headers: { Authorization: `Bearer ${secondCustomerToken}` },
    });
    expect(unknownHotelConsent.status()).toBe(404);
    expect((await unknownHotelConsent.json()).result.reasonCode).toBe('HOTEL_NOT_FOUND');
    expect(unknownHotelConsent.headers()['cache-control']).toContain('no-store');

    const unknownHotelRecommendation = await request.get(`${RECOMMENDATIONS_URL}?hotelId=32767`, {
      headers: { Authorization: `Bearer ${secondCustomerToken}` },
    });
    expect(unknownHotelRecommendation.status()).toBe(404);
    expect((await unknownHotelRecommendation.json()).result.reasonCode).toBe('HOTEL_NOT_FOUND');
    expect(unknownHotelRecommendation.headers()['cache-control']).toContain('no-store');
    expect(getAcceptanceDatabaseDigest()).toBe(databaseBefore);
  });

  test('persists customer-only consent, keeps account/hotel scope, and recommendation does not write DB data', async ({ request }) => {
    const before = await readConsent(request, secondCustomerToken);
    expect(before.granted).toBe(false);
    expect(before.dataMode).toBe('LOCAL_SYNTHETIC');

    const grantResponse = await updateConsent(request, secondCustomerToken, before, true);
    expect(grantResponse.status()).toBe(200);
    expect(grantResponse.headers()['cache-control']).toContain('no-store');
    const grant = (await grantResponse.json()).result as ConsentView;
    expect(grant.granted).toBe(true);
    expect(grant.revision).toBe(before.revision + 1);
    expect(grant.bindingVersion).toBe(before.bindingVersion);
    expect(grant.status).toBe('GRANTED');

    expect((await readConsent(request, customerToken)).granted).toBe(false);
    expect((await readConsent(request, secondCustomerToken, 2)).granted).toBe(false);

    const databaseBeforeRecommendation = getAcceptanceDatabaseDigest();
    const recommendationResponse = await request.get(`${RECOMMENDATIONS_URL}?hotelId=${HOTEL_ID}&maxSuggestions=3`, {
      headers: { Authorization: `Bearer ${secondCustomerToken}` },
    });
    expect(recommendationResponse.status()).toBe(200);
    expect(recommendationResponse.headers()['cache-control']).toContain('no-store');
    const recommendation = (await recommendationResponse.json()).result;
    expect(recommendation.status).toBe('PERSONALIZED_READY');
    expect(recommendation.hotelId).toBe(HOTEL_ID);
    expect(recommendation.dataMode).toBe('LOCAL_SYNTHETIC');
    expect(recommendation.roomTypeSuggestions).toHaveLength(1);
    expect(recommendation.roomTypeSuggestions[0]).toMatchObject({
      hotelRoomTypeId: 2,
      roomTypeCode: 'DLX',
      affinityScore: 1,
      reasonCode: 'HISTORICAL_BOOKING_AFFINITY',
    });
    expect(recommendation.roomTypeSuggestions[0].roomTypeName).toContain('Deluxe Room');
    expect(recommendation.nonBindingSuggestion).toBe(true);
    expect(recommendation.pricingOrPromotionMutated).toBe(false);
    expect(JSON.stringify(recommendation)).not.toMatch(/bookingRoomId|bookingId|guestId|actualCheck|checkIn|checkOut|identityNumber/i);
    expect(Object.keys(recommendation).sort()).toEqual([
      'dataMode',
      'hotelId',
      'nonBindingSuggestion',
      'pricingOrPromotionMutated',
      'roomTypeSuggestions',
      'status',
    ]);
    expect(recommendation).not.toHaveProperty('reasonCode');
    expect(getAcceptanceDatabaseDigest()).toBe(databaseBeforeRecommendation);
  });

  test('keeps an in-progress stay out of personalized history and returns cold start', async ({ request }) => {
    const before = await readConsent(request, customerToken);
    const grantResponse = await updateConsent(request, customerToken, before, true);
    expect(grantResponse.status()).toBe(200);

    const recommendationResponse = await request.get(`${RECOMMENDATIONS_URL}?hotelId=${HOTEL_ID}`, {
      headers: { Authorization: `Bearer ${customerToken}` },
    });
    expect(recommendationResponse.status()).toBe(200);
    const recommendation = (await recommendationResponse.json()).result;
    expect(recommendation.status).toBe('COLD_START_FALLBACK');
    expect(recommendation.roomTypeSuggestions).toEqual([]);

    const current = await readConsent(request, customerToken);
    const revokeResponse = await updateConsent(request, customerToken, current, false);
    expect(revokeResponse.status()).toBe(200);
  });

  test('rejects stale consent revision, policy and binding version without writing events', async ({ request }) => {
    const current = await readConsent(request, secondCustomerToken);
    expect(current.granted).toBe(true);
    const databaseBefore = getAcceptanceDatabaseDigest();
    const headers = { Authorization: `Bearer ${secondCustomerToken}` };

    const staleRevision = await request.put(CONSENT_URL, {
      headers,
      data: {
        hotelId: HOTEL_ID,
        granted: true,
        expectedRevision: current.revision - 1,
        expectedBindingVersion: current.bindingVersion,
        policyVersion: current.currentPolicyVersion,
      },
    });
    expect(staleRevision.status()).toBe(409);
    expect((await staleRevision.json()).result.reasonCode).toBe('CONSENT_REVISION_CONFLICT');
    expect(staleRevision.headers()['cache-control']).toContain('no-store');

    const stalePolicy = await request.put(CONSENT_URL, {
      headers,
      data: {
        hotelId: HOTEL_ID,
        granted: true,
        expectedRevision: current.revision,
        expectedBindingVersion: current.bindingVersion,
        policyVersion: 'u14-obsolete-policy',
      },
    });
    expect(stalePolicy.status()).toBe(409);
    expect((await stalePolicy.json()).result.reasonCode).toBe('CONSENT_POLICY_CONFLICT');

    const staleBinding = await request.put(CONSENT_URL, {
      headers,
      data: {
        hotelId: HOTEL_ID,
        granted: true,
        expectedRevision: current.revision,
        expectedBindingVersion: current.bindingVersion + 1,
        policyVersion: current.currentPolicyVersion,
      },
    });
    expect(staleBinding.status()).toBe(409);
    expect((await staleBinding.json()).result.reasonCode).toBe('IDENTITY_BINDING_CONFLICT');

    const missingPolicy = await request.put(CONSENT_URL, {
      headers,
      data: {
        hotelId: HOTEL_ID,
        granted: true,
        expectedRevision: current.revision,
        expectedBindingVersion: current.bindingVersion,
      },
    });
    expect(missingPolicy.status()).toBe(400);
    expect((await missingPolicy.json()).result.reasonCode).toBe('INVALID_REQUEST');
    expect(missingPolicy.headers()['cache-control']).toContain('no-store');

    const after = await readConsent(request, secondCustomerToken);
    expect(after.granted).toBe(current.granted);
    expect(after.revision).toBe(current.revision);
    expect(getAcceptanceDatabaseDigest()).toBe(databaseBefore);
  });

  test('allows only one concurrent update for the same consent revision and stops recommendations after revoke', async ({ request }) => {
    const granted = await readConsent(request, secondCustomerToken);
    expect(granted.granted).toBe(true);

    const [first, second] = await Promise.all([
      updateConsent(request, secondCustomerToken, granted, false),
      updateConsent(request, secondCustomerToken, granted, false),
    ]);
    expect([first.status(), second.status()].sort()).toEqual([200, 409]);

    const current = await readConsent(request, secondCustomerToken);
    expect(current.granted).toBe(false);
    expect(current.revision).toBe(granted.revision + 1);

    const recommendation = await request.get(`${RECOMMENDATIONS_URL}?hotelId=${HOTEL_ID}`, {
      headers: { Authorization: `Bearer ${secondCustomerToken}` },
    });
    expect(recommendation.status()).toBe(200);
    expect((await recommendation.json()).result.status).toBe('CONSENT_REQUIRED');
  });
});
