import { useEffect, useRef, useState } from 'react';
import { askHotelKnowledge, loadKnowledgeHotels, type HotelKnowledgeResult, type KnowledgeHotel } from '../../services/hotelKnowledgeService';

export function HotelKnowledgePanel() {
  const [hotels, setHotels] = useState<KnowledgeHotel[]>([]);
  const [hotelId, setHotelId] = useState('');
  const [question, setQuestion] = useState('');
  const [result, setResult] = useState<HotelKnowledgeResult | null>(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const request = useRef<AbortController | null>(null);
  const generation = useRef(0);
  useEffect(() => {
    const controller = new AbortController();
    void loadKnowledgeHotels(controller.signal).then(setHotels).catch(() => {
      if (!controller.signal.aborted) setError('Chưa tải được danh sách khách sạn. Có thể hỏi thông tin chung hoặc mở lại để thử.');
    });
    return () => { controller.abort(); request.current?.abort(); };
  }, []);
  const changeHotel = (id: string) => {
    generation.current++;
    request.current?.abort();
    setHotelId(id); setResult(null); setError(''); setQuestion(''); setLoading(false);
  };
  const ask = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!question.trim() || loading) return;
    request.current?.abort();
    const controller = new AbortController(); request.current = controller;
    const current = ++generation.current;
    setLoading(true); setResult(null); setError('');
    try {
      const answer = await askHotelKnowledge(question.trim(), hotelId ? Number(hotelId) : undefined, controller.signal);
      if (!controller.signal.aborted && generation.current === current) setResult(answer);
    } catch {
      if (!controller.signal.aborted && generation.current === current) setError('Tra cứu chính sách tạm thời không khả dụng. Vẫn có thể dùng mục Tìm phòng.');
    } finally {
      if (!controller.signal.aborted && generation.current === current) setLoading(false);
    }
  };
  return <section aria-label="Tra cứu chính sách khách sạn" className="flex-1 min-h-0 overflow-y-auto p-4 space-y-4 text-sm">
    <p>Tra cứu chính sách và tiện ích từ nguồn đã duyệt. Giá và phòng trống được kiểm tra ở mục Tìm phòng.</p>
    <label className="block">Khách sạn
      <select aria-label="Khách sạn tra cứu" value={hotelId} onChange={event => changeHotel(event.target.value)} className="block w-full border rounded p-2">
        <option value="">Thông tin chung của chuỗi</option>
        {hotels.map(hotel => <option key={hotel.id} value={hotel.id}>{hotel.name}</option>)}
      </select>
    </label>
    <form onSubmit={event => { void ask(event); }} className="space-y-2">
      <label className="block">Câu hỏi chính sách
        <textarea aria-label="Câu hỏi chính sách" value={question} onChange={event => setQuestion(event.target.value)} maxLength={2000} required rows={3} className="block w-full border rounded p-2" />
      </label>
      <button type="submit" disabled={loading || !question.trim()} className="bg-red-600 text-white rounded px-3 py-2 disabled:opacity-50">Tra cứu chính sách</button>
    </form>
    {loading && <p role="status">Đang tra cứu...</p>}
    {error && <p role="alert">{error}</p>}
    {result && <div role="status" className="space-y-3">
      <p className="whitespace-pre-line">{result.answer}</p>
      {result.status === 'INSUFFICIENT_EVIDENCE' && <p>Chưa có đủ nguồn để xác nhận thông tin.</p>}
      {result.citations.length > 0 && <div aria-label="Nguồn tham khảo"><strong>Nguồn tham khảo</strong>
        {result.citations.map(source => <div key={source.chunkId} className="border rounded p-2 mt-2">
          <p>{source.sourceId} — phiên bản {source.version}</p><p>{source.snippet}</p>
        </div>)}
      </div>}
      {result.warnings.map((warning, index) => <p key={`w-${index}`}>{warning}</p>)}
      {result.disclaimers.map((disclaimer, index) => <p key={`d-${index}`}>{disclaimer}</p>)}
    </div>}
  </section>;
}
