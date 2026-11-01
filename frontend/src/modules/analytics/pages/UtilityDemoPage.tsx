import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { ArrowLeft, FlaskConical, RefreshCw } from 'lucide-react'
import Button from '../../../core/components/ui/Button'
import { getErrorStatus } from '../../../core/api/client'
import UtilityAnomalyChart from '../components/UtilityAnomalyChart'
import { UTILITY_SCENARIOS, utilityDemoService, utilityDate, utilityNumber, utilityReason, utilityStatus, utilityUnit, type UtilityDemo, type UtilityScenario } from '../services/utilityDemoService'

function errorMessage(error: unknown) {
  switch (getErrorStatus(error)) {
    case 401: return 'Phiên đăng nhập đã hết hạn. Hãy đăng nhập lại.'
    case 403: return 'Quyền truy cập demo đã bị từ chối. Hãy kiểm tra tài khoản admin.'
    case 404: return 'Demo điện/nước chưa được bật trên máy chủ. Hãy bật cấu hình demo rồi tải lại.'
    case 503: return 'Dịch vụ demo tạm thời chưa sẵn sàng. Hãy thử lại sau.'
    default: return 'Không tải được dữ liệu demo hợp lệ. Hãy kiểm tra kết nối hoặc cấu hình rồi thử lại.'
  }
}
export default function UtilityDemoPage() {
  const [selection, setSelection] = useState({ scenario: 'ELECTRIC_SPIKES' as UtilityScenario, revision: 0 })
  const [result, setResult] = useState<{ key: typeof selection; data?: UtilityDemo; error?: string } | null>(null)
  const current = result?.key === selection ? result : null
  const loading = current === null, data = current?.data
  useEffect(() => {
    const controller = new AbortController()
    utilityDemoService.get(selection.scenario, controller.signal).then(data => {
      if (!controller.signal.aborted) setResult({ key: selection, data })
    }).catch((error: unknown) => {
      if (!controller.signal.aborted) setResult({ key: selection, error: errorMessage(error) })
    })
    return () => controller.abort()
  }, [selection])
  const reload = () => setSelection(previous => ({ ...previous, revision: previous.revision + 1 }))
  return <main className="min-h-screen bg-neutral-50 px-4 py-6 text-neutral-900 sm:px-8 sm:py-10">
    <div className="mx-auto max-w-6xl space-y-6">
      <Link to="/dashboard" className="inline-flex min-h-11 items-center gap-2 rounded-lg text-sm font-medium text-neutral-700 hover:text-red-700 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-red-700"><ArrowLeft aria-hidden="true" className="h-4 w-4" />Về bàn làm việc</Link>
      <header>
        <span className="inline-flex items-center gap-2 rounded-full border border-amber-300 bg-amber-50 px-3 py-1 text-xs font-semibold text-amber-900"><FlaskConical aria-hidden="true" className="h-4 w-4" />DEMO · DỮ LIỆU GIẢ LẬP</span>
        <h1 className="mt-3 text-2xl font-bold tracking-tight sm:text-3xl">Kiểm tra bất thường điện/nước</h1>
        <p className="mt-2 max-w-3xl text-sm leading-relaxed text-neutral-600">Mô phỏng lịch sử theo mốc cố định. Nhãn “Cần kiểm tra” không phải kết luận gian lận hoặc cảnh báo của đồng hồ thật.</p>
      </header>
      <section aria-label="Chọn kịch bản điện nước" className="flex flex-wrap items-end gap-4 rounded-2xl border border-neutral-200 bg-white p-4 sm:p-6">
        <div className="w-full min-w-0 sm:w-auto"><label htmlFor="utility-scenario" className="mb-2 block text-sm font-semibold">Kịch bản điện/nước</label>
          <select id="utility-scenario" value={selection.scenario} onChange={event => setSelection({ scenario: event.target.value as UtilityScenario, revision: 0 })} className="min-h-12 w-full rounded-xl border border-neutral-300 bg-white px-3 text-sm focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-red-700 sm:w-80">
            {UTILITY_SCENARIOS.map(([id, label]) => <option key={id} value={id}>{label}</option>)}
          </select>
        </div>
        <Button type="button" variant="outline" className="min-h-11" disabled={loading} onClick={reload} leftIcon={<RefreshCw aria-hidden="true" className="h-4 w-4" />}>Tải lại</Button>
        <p className="w-full text-xs text-neutral-600">Mốc mô phỏng: 31/10/2026 lúc 00:00 (giờ Việt Nam). Phân tích 19/09–30/10/2026; các kịch bản độc lập với đồng hồ thật.</p>
      </section>
      {loading && <div role="status" className="rounded-2xl border border-neutral-200 bg-white p-8 text-center text-neutral-600">Đang tải dữ liệu điện/nước mô phỏng…</div>}
      {current?.error && <div role="alert" className="rounded-2xl border border-red-200 bg-red-50 p-6 text-red-900"><p>{current.error}</p><Button type="button" variant="outline" className="mt-4 min-h-11 bg-white" onClick={reload}>Thử lại</Button></div>}
      {data && <>
        <p role="status" className="text-sm font-medium text-neutral-700">{data.analysis.status === 'ANALYZED' ? 'Có điểm đủ điều kiện đánh giá; các ngày còn lại vẫn được ghi rõ chất lượng dữ liệu.' : 'Chưa đủ dữ liệu để đánh giá. Không có kết luận bình thường hoặc bất thường thay thế.'} Đơn vị: {utilityUnit(data.analysis.unit)}.</p>
        {data.counts.needsReview > 0 && <p className="rounded-xl border border-red-200 bg-red-50 p-4 text-sm font-medium text-red-900">Ngày cần kiểm tra: {data.analysis.points.filter(p => p.status === 'NEEDS_REVIEW').map(p => utilityDate(p.date)).join(', ')}. Xem lý do và số liệu trong bảng phân tích.</p>}
        <section aria-label="Thống kê chất lượng" className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {[['Đã đánh giá', data.counts.evaluated], ['Cần kiểm tra', data.counts.needsReview], ['Chưa đủ tham chiếu', data.counts.insufficient], ['Bị loại', data.counts.excluded]].map(([label, count]) => <div key={label} className="rounded-2xl border border-neutral-200 bg-white p-5"><h2 className="text-sm font-medium text-neutral-600">{label}</h2><p className="mt-2 text-2xl font-bold">{count} <span className="text-sm font-normal text-neutral-600">ngày</span></p></div>)}
        </section>
        <UtilityAnomalyChart data={data} />
        <section className="rounded-2xl border border-neutral-200 bg-white p-4 sm:p-6">
          <h2 className="text-lg font-semibold">Bảng phân tích 42 ngày mô phỏng</h2>
          <p className="mt-1 text-sm text-neutral-600">Tiêu thụ, trung vị, MAD và ngưỡng cùng đơn vị {utilityUnit(data.analysis.unit)}. Dấu — nghĩa là không có số liệu hợp lệ cho trường đó.</p>
          <p className="mt-2 text-xs text-neutral-600">MAD là trung vị của độ lệch tuyệt đối so với trung vị tham chiếu.</p>
          <div role="region" aria-label="Số liệu phân tích điện nước" tabIndex={0} className="mt-4 overflow-x-auto rounded-lg focus-visible:outline-2 focus-visible:outline-red-700">
            <table className="w-full min-w-230 text-left text-sm"><caption className="sr-only">42 ngày cùng trạng thái, lý do và tham chiếu từ máy chủ</caption>
              <thead><tr>{['Ngày', 'Tiêu thụ', 'Trạng thái', 'Điểm trước', 'Trung vị', 'MAD', 'Ngưỡng', 'Lý do'].map(label => <th key={label} scope="col" className="border-b border-neutral-200 px-3 py-3 font-semibold">{label}</th>)}</tr></thead>
              <tbody>{data.analysis.points.map(point => <tr key={point.date} className={`border-b border-neutral-100 tabular-nums last:border-0 ${point.status === 'NEEDS_REVIEW' ? 'bg-red-50' : ''}`}>
                <th scope="row" className="whitespace-nowrap px-3 py-3 font-medium">{utilityDate(point.date)}</th><td className="px-3 py-3">{utilityNumber(point.usage)}</td>
                <td className={`px-3 py-3 font-medium ${point.status === 'NEEDS_REVIEW' ? 'text-red-800' : 'text-neutral-700'}`}>{utilityStatus[point.status]}</td><td className="px-3 py-3">{point.referenceCount}</td>
                <td className="px-3 py-3">{utilityNumber(point.median)}</td><td className="px-3 py-3">{utilityNumber(point.mad)}</td><td className="px-3 py-3">{utilityNumber(point.threshold)}</td><td className="min-w-44 px-3 py-3 text-neutral-600">{point.reasonCode === null ? '—' : utilityReason[point.reasonCode]}</td>
              </tr>)}</tbody>
            </table>
          </div>
        </section>
      </>}
    </div>
  </main>
}
