import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { ArrowLeft, FlaskConical, RefreshCw } from 'lucide-react'
import Button from '../../../core/components/ui/Button'
import { getErrorStatus } from '../../../core/api/client'
import { GROUP_SCENARIOS, groupDemoService, groupDate, groupWarning, type GroupDemo, type GroupScenario } from '../services/groupDemoService'

function errorMessage(error: unknown) {
  switch (getErrorStatus(error)) {
    case 401: return 'Phiên đăng nhập đã hết hạn. Hãy đăng nhập lại.'
    case 403: return 'Quyền truy cập demo đã bị từ chối. Hãy kiểm tra tài khoản admin.'
    case 404: return 'Demo Group chưa được bật trên máy chủ. Hãy bật cấu hình demo rồi tải lại.'
    case 503: return 'Dịch vụ demo tạm thời chưa sẵn sàng. Hãy thử lại sau.'
    default: return 'Không tải được phương án demo hợp lệ. Hãy kiểm tra kết nối hoặc cấu hình rồi thử lại.'
  }
}
export default function GroupDemoPage() {
  const [selection, setSelection] = useState({ scenario: 'FIT_AND_TIE' as GroupScenario, revision: 0 })
  const [result, setResult] = useState<{ key: typeof selection; data?: GroupDemo; error?: string } | null>(null)
  const current = result?.key === selection ? result : null
  const loading = current === null, data = current?.data
  useEffect(() => {
    const controller = new AbortController()
    groupDemoService.get(selection.scenario, controller.signal).then(data => {
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
        <h1 className="mt-3 text-2xl font-bold tracking-tight sm:text-3xl">Phương án phân phòng Group</h1>
        <p className="mt-2 max-w-3xl text-sm leading-relaxed text-neutral-600">Chỉ xem phương án mô phỏng; không giữ phòng, phân phòng nghiệp vụ hoặc tạo đặt phòng. Các mã phòng, nhóm, block và loại phòng đều thuộc dữ liệu giả lập.</p>
      </header>
      <section aria-label="Chọn kịch bản Group" className="flex flex-wrap items-end gap-4 rounded-2xl border border-neutral-200 bg-white p-4 sm:p-6">
        <div className="w-full min-w-0 sm:w-auto"><label htmlFor="group-scenario" className="mb-2 block text-sm font-semibold">Kịch bản Group</label>
          <select id="group-scenario" value={selection.scenario} onChange={event => setSelection({ scenario: event.target.value as GroupScenario, revision: 0 })} className="min-h-12 w-full rounded-xl border border-neutral-300 bg-white px-3 text-sm focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-red-700 sm:w-96">
            {GROUP_SCENARIOS.map(([id, label]) => <option key={id} value={id}>{label}</option>)}
          </select>
        </div>
        <Button type="button" variant="outline" disabled={loading} className="min-h-11" onClick={reload} leftIcon={<RefreshCw aria-hidden="true" className="h-4 w-4" />}>Tải lại</Button>
        <p className="w-full text-xs text-neutral-600">Mốc mô phỏng: 31/10/2026 lúc 09:00 (giờ Việt Nam). Đây không phải thông tin phòng trống hiện tại.</p>
      </section>
      {loading && <div role="status" className="rounded-2xl border border-neutral-200 bg-white p-8 text-center text-neutral-600">Đang tính phương án mô phỏng…</div>}
      {current?.error && <div role="alert" className="rounded-2xl border border-red-200 bg-red-50 p-6 text-red-900"><p>{current.error}</p><Button type="button" variant="outline" className="mt-4 min-h-11 bg-white" onClick={reload}>Thử lại</Button></div>}
      {data && <>
        <p role="status" className={`rounded-xl border p-4 text-sm font-medium ${data.allDemandsAssigned ? 'border-emerald-200 bg-emerald-50 text-emerald-950' : 'border-amber-200 bg-amber-50 text-amber-950'}`}>
          Đã tính xong. {data.allDemandsAssigned ? 'Đủ phương án cho mọi yêu cầu mô phỏng.' : `Còn ${data.preview.unmet.length} yêu cầu chưa có phương án; tính xong không có nghĩa là đủ phòng.`}
        </p>
        <section aria-label="Tổng hợp phương án" className="grid gap-4 sm:grid-cols-3">
          {[['Yêu cầu mô phỏng', data.request.demands.length], ['Có phương án', data.preview.assignments.length], ['Chưa đáp ứng', data.preview.unmet.length]].map(([label, count]) => <div key={label} className="rounded-2xl border border-neutral-200 bg-white p-5"><h2 className="text-sm font-medium text-neutral-600">{label}</h2><p className="mt-2 text-2xl font-bold">{count}</p></div>)}
        </section>
        <section aria-label="Kết quả mô phỏng" className="rounded-2xl border border-neutral-200 bg-white p-4 sm:p-6">
          <h2 className="text-lg font-semibold">Yêu cầu và phương án đề xuất</h2>
          <p className="mt-2 text-sm text-neutral-600">Lưu trú {groupDate(data.request.checkInDate)}–{groupDate(data.request.checkOutDate)}; không tính đêm ngày trả phòng. Số giường tính theo đơn vị giường trong dữ liệu mô phỏng.</p>
          <ScrollTable label="Phương án theo yêu cầu"><table className="w-full min-w-190 text-left text-sm"><caption className="sr-only">Yêu cầu, phòng mô phỏng, giường, tầng và kết quả</caption>
            <thead><tr>{['Nhóm mô phỏng', 'Số người', 'Giường cần', 'Phòng mô phỏng', 'Giường phòng', 'Tầng', 'Kết quả'].map(label => <th key={label} scope="col" className="border-b border-neutral-200 px-3 py-3 font-semibold">{label}</th>)}</tr></thead>
            <tbody>{data.request.demands.map(demand => {
              const assignment = data.preview.assignments.find(row => row.demandKey === demand.demandKey)
              const room = assignment && data.snapshot.candidates.find(room => room.roomInstanceId === assignment.roomInstanceId)
              return <tr key={demand.demandKey} data-testid={`group-demand-${demand.demandKey}`} className="border-b border-neutral-100 tabular-nums last:border-0">
                <th scope="row" className="px-3 py-3">{demand.demandKey}</th><td className="px-3 py-3">{demand.persons}</td><td className="px-3 py-3">{demand.requiredBedUnits}</td>
                <td className="px-3 py-3">{room ? `#${room.roomInstanceId}` : '—'}</td><td className="px-3 py-3">{room?.bedUnits ?? '—'}</td><td className="px-3 py-3">{room?.floorNumber ?? '—'}</td>
                <td className={`min-w-52 px-3 py-3 ${room ? 'text-emerald-800' : 'text-amber-900'}`}>{room ? 'Có phương án mô phỏng' : 'Chưa có phương án: không đủ sức chứa hoặc quota phù hợp'}</td>
              </tr>
            })}</tbody>
          </table></ScrollTable>
          <p className="mt-4 text-sm text-neutral-600">Đơn vị giường dư: {data.preview.objective.bedWaste}. Độ trải tầng: {data.preview.objective.floorSpan ?? '—'}{data.preview.objective.floorObjectiveEnabled ? ' (chênh lệch tầng cao nhất và thấp nhất trong phương án).' : ' (không đánh giá vì thiếu thông tin tầng).'}</p>
          {data.preview.warnings.map(warning => <p key={warning} className="mt-2 text-sm text-amber-900">{groupWarning[warning]}</p>)}
        </section>
        <section className="rounded-2xl border border-neutral-200 bg-white p-4 sm:p-6">
          <h2 className="text-lg font-semibold">Phòng trong dữ liệu mô phỏng</h2>
          <ScrollTable label="Phòng và metadata mô phỏng"><table className="w-full min-w-170 text-left text-sm"><caption className="sr-only">Metadata phòng, block và loại phòng giả lập</caption>
            <thead><tr>{['Mã phòng', 'Block', 'Loại phòng', 'Sức chứa', 'Đơn vị giường', 'Tầng', 'Đủ điều kiện mọi đêm'].map(label => <th key={label} scope="col" className="border-b border-neutral-200 px-3 py-3 font-semibold">{label}</th>)}</tr></thead>
            <tbody>{data.snapshot.candidates.map(room => <tr key={room.roomInstanceId} className="border-b border-neutral-100 tabular-nums last:border-0"><th scope="row" className="px-3 py-3">#{room.roomInstanceId}</th>{[room.roomBlockId, room.hotelRoomTypeId, room.capacityPersons, room.bedUnits, room.floorNumber ?? '—', room.eligibleForEntireStay ? 'Có' : 'Không'].map((value, i) => <td key={i} className="px-3 py-3">{value}</td>)}</tr>)}</tbody>
          </table></ScrollTable>
        </section>
        <section className="rounded-2xl border border-neutral-200 bg-white p-4 sm:p-6">
          <h2 className="text-lg font-semibold">Quota theo từng đêm mô phỏng</h2>
          <p className="mt-2 text-sm text-neutral-600">Quota áp dụng riêng cho từng cặp block/loại phòng và mọi đêm lưu trú; không chuyển quota giữa các cặp.</p>
          <ScrollTable label="Quota từng đêm"><table className="w-full min-w-120 text-left text-sm"><caption className="sr-only">Ngày lưu trú, block, loại phòng và quota mô phỏng</caption>
            <thead><tr>{['Đêm lưu trú', 'Block', 'Loại phòng', 'Quota phòng'].map(label => <th key={label} scope="col" className="border-b border-neutral-200 px-3 py-3 font-semibold">{label}</th>)}</tr></thead>
            <tbody>{data.snapshot.quotaRows.map(row => <tr key={`${row.roomBlockId}/${row.hotelRoomTypeId}/${row.stayDate}`} className="border-b border-neutral-100 tabular-nums last:border-0"><th scope="row" className="px-3 py-3">{groupDate(row.stayDate)}</th><td className="px-3 py-3">{row.roomBlockId}</td><td className="px-3 py-3">{row.hotelRoomTypeId}</td><td className="px-3 py-3">{row.previewableRoomCount}</td></tr>)}</tbody>
          </table></ScrollTable>
        </section>
      </>}
    </div>
  </main>
}
function ScrollTable({ label, children }: { label: string; children: React.ReactNode }) {
  return <><p className="mt-3 text-xs text-neutral-600 sm:hidden">Cuộn ngang bảng để xem đầy đủ các cột.</p><div role="region" aria-label={label} tabIndex={0} className="mt-4 overflow-x-auto rounded-lg focus-visible:outline-2 focus-visible:outline-red-700">{children}</div></>
}
