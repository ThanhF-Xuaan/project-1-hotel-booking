import { createRoot } from 'react-dom/client';
import '../../index.css';
import { RoomAdvisorWidget } from './components/RoomAdvisor/RoomAdvisorWidget';
import type { AdvisorRecommendation, AdvisorSearchCriteria } from './types/roomAdvisor.types';

const dateIn = (days: number) => new Date(Date.now() + days * 86_400_000).toISOString().slice(0, 10);

function openRoomDetails(recommendation: AdvisorRecommendation, criteria: AdvisorSearchCriteria) {
  const query = new URLSearchParams({
    checkInDate: recommendation.stay.checkInDate,
    checkOutDate: recommendation.stay.checkOutDate,
    adults: String(criteria.adults ?? 2),
    children: String(criteria.children ?? 0),
    infants: String(criteria.infants ?? 0),
    extraBeds: String(criteria.extraBeds ?? 0),
    roomsCount: String(recommendation.stay.roomsCount ?? 1),
  });
  window.location.assign(`${recommendation.action.path}?${query.toString()}`);
}

createRoot(document.getElementById('root')!).render(
  <main className="mx-auto min-h-screen max-w-2xl px-6 py-12">
    <h1 className="text-2xl font-semibold">Kiểm thử chatbot tìm phòng</h1>
    <p className="mt-4">
      Mở trợ lý ở góc phải rồi chọn <strong>Tìm phòng</strong>. Dữ liệu trên thẻ được đọc từ
      PostgreSQL qua Core Availability/Pricing và API chatbot.
    </p>
    <ol className="mt-3 list-decimal space-y-2 pl-5">
      <li>Tìm phòng ở Hạ Long từ {dateIn(2)} đến {dateIn(4)} cho 2 người lớn.</li>
      <li>Đổi thành 1 người lớn.</li>
      <li>Nhấn <strong>Cập nhật giá</strong> hoặc <strong>Xem chi tiết</strong> trên một thẻ phòng.</li>
    </ol>
    <p className="mt-3">
      Kỳ vọng thấy 4 hạng phòng pilot. Giá, số lượng và lịch tồn là dữ liệu thao tác trong DB pilot,
      không phải dữ liệu vận hành production của Vinpearl.
    </p>
    <RoomAdvisorWidget onViewRoomDetails={openRoomDetails} />
  </main>,
);
