import { useId } from 'react'
import { utilityDate, utilityUnit, type UtilityDemo } from '../services/utilityDemoService'

export default function UtilityAnomalyChart({ data }: { data: UtilityDemo }) {
  const id = useId(), points = data.analysis.points
  const width = 900, height = 280, left = 78, right = 18, top = 18, bottom = 36
  const ceiling = Math.max(0.001, ...points.map(p => Math.max(p.usage ?? 0, (p.median ?? 0) + (p.threshold ?? 0)))) * 1.1
  const x = (i: number) => left + i * (width - left - right) / 41
  const y = (value: number) => top + (1 - value / ceiling) * (height - top - bottom)
  const path = (values: (number | null)[]) => values.map((value, i) => value === null ? '' : `${i === 0 || values[i - 1] === null ? 'M' : 'L'}${x(i)},${y(value)}`).join(' ')
  const axis = new Intl.NumberFormat('vi-VN', { maximumSignificantDigits: 3, notation: ceiling >= 10000 ? 'compact' : 'standard' })
  return <figure aria-labelledby={`${id}-title`} className="rounded-2xl border border-neutral-200 bg-white p-4 sm:p-6">
    <figcaption id={`${id}-title`} className="font-semibold">Mức tiêu thụ và tham chiếu theo ngày</figcaption>
    <p id={`${id}-description`} className="mt-1 text-sm text-neutral-600">42 ngày mô phỏng · {utilityUnit(data.analysis.unit)}. Điểm cần kiểm tra có hình thoi. Điểm bị loại tạo khoảng trống; ngày chưa đủ tham chiếu vẫn có thể có mức tiêu thụ.</p>
    <p className="mt-2 text-xs text-neutral-600 sm:hidden">Cuộn ngang biểu đồ và bảng để xem đầy đủ số liệu.</p>
    <div role="region" aria-label="Biểu đồ điện nước có thể cuộn ngang" tabIndex={0} className="mt-4 overflow-x-auto rounded-lg focus-visible:outline-2 focus-visible:outline-red-700">
      <svg viewBox={`0 0 ${width} ${height}`} role="img" aria-labelledby={`${id}-title ${id}-description`} className="block w-full min-w-180">
        {[0, 0.25, 0.5, 0.75, 1].map(ratio => <g key={ratio}>
          <line x1={left} x2={width - right} y1={y(ceiling * ratio)} y2={y(ceiling * ratio)} stroke="currentColor" className="text-neutral-200" />
          <text x={left - 10} y={y(ceiling * ratio) + 5} textAnchor="end" fontSize="15" fill="currentColor" className="text-neutral-600">{axis.format(ceiling * ratio)}</text>
        </g>)}
        <path data-testid="utility-usage-line" d={path(points.map(p => p.usage))} fill="none" stroke="currentColor" strokeWidth="2.5" className="text-neutral-700" />
        <path d={path(points.map(p => p.median))} fill="none" stroke="currentColor" strokeWidth="2" strokeDasharray="6 4" className="text-blue-700" />
        <path d={path(points.map(p => p.median === null || p.threshold === null ? null : p.median + p.threshold))} fill="none" stroke="currentColor" strokeDasharray="2 4" className="text-amber-700" />
        <path d={path(points.map(p => p.median === null || p.threshold === null ? null : Math.max(0, p.median - p.threshold)))} fill="none" stroke="currentColor" strokeDasharray="2 4" className="text-amber-700" />
        {points.map((p, i) => p.status === 'NEEDS_REVIEW' && p.usage !== null ? <path data-testid="utility-review-marker" key={p.date} d={`M${x(i)},${y(p.usage) - 6}l6,6l-6,6l-6,-6Z`} className="fill-red-700 stroke-white" strokeWidth="1.5"><title>{utilityDate(p.date)}: Cần kiểm tra</title></path> : null)}
        {[0, 20, 41].map(i => <text key={i} x={x(i)} y={height - 8} textAnchor={i === 0 ? 'start' : i === 41 ? 'end' : 'middle'} fontSize="15" fill="currentColor" className="text-neutral-600">{utilityDate(points[i].date)}</text>)}
      </svg>
    </div>
    <div className="mt-4 flex flex-wrap gap-x-5 gap-y-2 text-sm text-neutral-700">
      <span className="flex items-center gap-2"><span aria-hidden="true" className="w-6 border-t-2 border-neutral-700" />Tiêu thụ</span>
      <span className="flex items-center gap-2"><span aria-hidden="true" className="w-6 border-t-2 border-dashed border-blue-700" />Trung vị tham chiếu</span>
      <span className="flex items-center gap-2"><span aria-hidden="true" className="w-6 border-t-2 border-dotted border-amber-700" />Trung vị ± ngưỡng</span>
      <span className="flex items-center gap-2 font-medium text-red-800"><svg aria-hidden="true" viewBox="0 0 12 12" className="h-3 w-3 fill-red-700"><path d="M6 0L12 6L6 12L0 6Z" /></svg>Cần kiểm tra — xem bảng bên dưới</span>
    </div>
    <p className="mt-3 text-xs leading-relaxed text-neutral-600">Ngưỡng tham chiếu chỉ dùng các điểm hợp lệ trước ngày đang xét. Đường dưới giới hạn ở 0 vì mức tiêu thụ không âm; đây không phải khoảng tin cậy hoặc kết luận gian lận.</p>
  </figure>
}
