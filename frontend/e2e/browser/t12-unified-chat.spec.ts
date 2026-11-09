import { test, expect, type Page } from '@playwright/test';

const sampleUnifiedResponse = {
  contractVersion: '2.1-proposed',
  requestId: '00000000-0000-4000-8000-000000000001',
  intent: 'SEARCH_AND_POLICY',
  advisor: {
    contractVersion: '2.0',
    requestId: '00000000-0000-4000-8000-000000000002',
    sessionId: '00000000-0000-4000-8000-000000000003',
    sessionVersion: 1,
    status: 'RESULTS',
    reply: 'Tôi tìm thấy phòng đôi phù hợp tại Đà Nẵng cho quý khách.',
    recommendations: [
      {
        recommendationId: 'rec-001',
        rank: 1,
        room: {
          hotelId: 2,
          hotelName: 'Khách sạn Đà Nẵng Heritage',
          hotelRoomTypeId: 101,
          roomTypeId: 1,
          roomTypeCode: 'DELUXE_DOUBLE',
          roomTypeName: 'Phòng Deluxe Đôi',
          maxAdults: 2,
          maxChildren: 1,
          maxTotalGuests: 3,
          extraBedsAllowed: 0,
        },
        price: {
          hotelRoomTypeId: 101,
          source: 'CORE_PRICING',
          totalNights: 2,
          currency: 'VND',
          finalTotalAmount: '3.000.000',
          serviceFeeStatus: 'INCLUDED',
          chargeCoverage: 'ALL_MANDATORY_CHARGES',
          budgetCheck: 'PASS',
        },
        reasons: [{ code: 'LOCATION_MATCH', text: 'Đúng địa điểm Đà Nẵng' }],
        action: { type: 'VIEW_ROOM_DETAILS', path: '/rooms/101' },
        stay: { checkInDate: '2026-11-14', checkOutDate: '2026-11-16', roomsCount: 1 },
      },
    ],
  },
  knowledge: {
    state: 'ANSWERED',
    hotelId: 2,
    hotelName: 'Khách sạn Đà Nẵng Heritage',
    policyQuestion: 'bé có được miễn phí buffet sáng không?',
    result: {
      status: 'GROUNDED_ANSWER',
      answer: 'Trẻ em dưới 6 tuổi đi cùng bố mẹ được miễn phí buffet sáng tiêu chuẩn.',
      citations: [
        {
          sourceId: 'POLICY-CHILD-BREAKFAST',
          version: 1,
          chunkId: '00000000-0000-4000-8000-000000000004',
          category: 'POLICY',
          hotelId: 2,
          snippet: 'Trẻ em dưới 6 tuổi đi cùng bố mẹ được miễn phí buffet sáng tiêu chuẩn.',
          similarityScore: 0.95,
        },
      ],
      warnings: [],
      disclaimers: ['Dữ liệu chính sách demo synthetic'],
    },
  },
};

const multiHotelResponse = {
  contractVersion: '2.1-proposed',
  requestId: '00000000-0000-4000-8000-000000000005',
  intent: 'SEARCH_AND_POLICY',
  advisor: {
    contractVersion: '2.0',
    requestId: '00000000-0000-4000-8000-000000000006',
    status: 'RESULTS',
    reply: 'Dưới đây là các lựa chọn phòng:',
    recommendations: [],
  },
  knowledge: {
    state: 'SCOPE_REQUIRED',
    policyQuestion: 'hồ bơi mở cửa lúc mấy giờ?',
    hotelChoices: [
      { hotelId: 1, hotelName: 'Khách sạn Hà Nội Central' },
      { hotelId: 2, hotelName: 'Khách sạn Đà Nẵng Heritage' },
    ],
    result: {
      status: 'SCOPE_REQUIRED',
      answer: 'Có nhiều khách sạn phù hợp. Vui lòng chọn khách sạn bên dưới để xem chính sách chi tiết:',
    },
  },
};

async function openWidget(page: Page) {
  await page.addInitScript(() => {
    (window as unknown as { __VITE_ADVISOR_UNIFIED?: boolean }).__VITE_ADVISOR_UNIFIED = true;
  });
  await page.goto('/e2e/fixtures/t12-knowledge.html');
  await page.getByRole('button', { name: 'Mở Trợ lý Tìm phòng AI' }).click();
}

test('renders room cards and grounded knowledge in a single turn', async ({ page }) => {
  await page.route('**/room-advisor/converse', (route) =>
    route.fulfill({ json: { result: sampleUnifiedResponse } })
  );

  await openWidget(page);
  await page.getByPlaceholder(/Nhập nhu cầu/).fill(
    'Tìm phòng đôi ở Đà Nẵng cuối tuần này cho 2 người lớn và 1 bé 4 tuổi, bé có được miễn phí buffet sáng không?'
  );
  await page.getByLabel('Gửi tin nhắn').click();

  // Verify Room Advisor reply and room card
  await expect(page.getByText('Tôi tìm thấy phòng đôi phù hợp tại Đà Nẵng')).toBeVisible();
  await expect(page.getByText('Phòng Deluxe Đôi')).toBeVisible();
  await expect(page.getByText('3.000.000')).toBeVisible();

  // Verify Knowledge Answer Block
  await expect(page.getByText('Thông tin chính sách & tiện ích')).toBeVisible();
  await expect(page.getByText('Trẻ em dưới 6 tuổi đi cùng bố mẹ được miễn phí buffet sáng')).toBeVisible();

  // Verify citations toggle
  await page.getByRole('button', { name: /Nguồn trích dẫn/ }).click();
  await expect(page.getByText('POLICY-CHILD-BREAKFAST')).toBeVisible();
});

test('presents hotel choice chips when multiple hotels match policy question', async ({ page }) => {
  let requestedWithPolicyHotelId: number | undefined;

  await page.route('**/room-advisor/converse', async (route) => {
    const postData = route.request().postDataJSON();
    if (postData?.policyHotelId) {
      requestedWithPolicyHotelId = postData.policyHotelId;
      return route.fulfill({ json: { result: sampleUnifiedResponse } });
    }
    return route.fulfill({ json: { result: multiHotelResponse } });
  });

  await openWidget(page);
  await page.getByPlaceholder(/Nhập nhu cầu/).fill('Tìm phòng và hồ bơi mở cửa lúc mấy giờ?');
  await page.getByLabel('Gửi tin nhắn').click();

  // Verify hotel choices chips
  await expect(page.getByRole('button', { name: 'Khách sạn Đà Nẵng Heritage' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Khách sạn Hà Nội Central' })).toBeVisible();

  // Click on Da Nang chip
  await page.getByRole('button', { name: 'Khách sạn Đà Nẵng Heritage' }).click();

  // Verify follow-up sent policyHotelId = 2
  await expect(page.getByText('Trẻ em dưới 6 tuổi đi cùng bố mẹ được miễn phí buffet sáng')).toBeVisible();
  expect(requestedWithPolicyHotelId).toBe(2);
});

test('handles RAG temporary unavailability gracefully without breaking room search', async ({ page }) => {
  const degradedResponse = {
    ...sampleUnifiedResponse,
    knowledge: {
      state: 'UNAVAILABLE',
    },
  };

  await page.route('**/room-advisor/converse', (route) =>
    route.fulfill({ json: { result: degradedResponse } })
  );

  await openWidget(page);
  await page.getByPlaceholder(/Nhập nhu cầu/).fill('Tìm phòng và chính sách');
  await page.getByLabel('Gửi tin nhắn').click();

  // Room card still displays
  await expect(page.getByText('Phòng Deluxe Đôi')).toBeVisible();

  // Soft warning for knowledge
  await expect(page.getByText('Tra cứu chính sách tạm thời không khả dụng')).toBeVisible();
});
