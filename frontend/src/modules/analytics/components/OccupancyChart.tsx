import { useId } from 'react'
import { displayDate, occupancyPercent, type OccupancyDemo } from '../services/occupancyDemoService'

export default function OccupancyChart({ data }: { data: OccupancyDemo }) {
  const id = useId()
  const history = data.history.samples.slice(-90)
  const points = data.forecast.points
  const width = 900, height = 280, left = 58, right = 16, top = 16, bottom = 34
  const count = history.length + points.length
  const x = (i: number) => left + i * (width - left - right) / Math.max(1, count - 1)
  const y = (n: number) => top + (100 - n) * (height - top - bottom) / 100
  const historicalPath = history.map((row, i) => {
    const value = occupancyPercent(row)
    if (value === null) return ''
    return `${i === 0 || occupancyPercent(history[i - 1]) === null ? 'M' : 'L'}${x(i)},${y(value)}`
  }).join(' ')
  const forecastPath = points.map((row, i) => `${i ? 'L' : 'M'}${x(history.length + i)},${y(row.occupancyPercent)}`).join(' ')
  const band = points.map((row, i) => `${x(history.length + i)},${y(row.upperPercent)}`)
    .concat(points.map((row, i) => `${x(history.length + i)},${y(row.lowerPercent)}`).reverse()).join(' ')

  return (
    <figure aria-labelledby={`${id}-title`} className="rounded-2xl border border-neutral-200 bg-white p-4 sm:p-6">
      <figcaption id={`${id}-title`} className="font-semibold text-neutral-900">Lịch sử và dự báo tỷ lệ lấp đầy</figcaption>
      <p id={`${id}-description`} className="mt-1 text-sm text-neutral-600">
        {history.length} ngày lịch sử gần nhất{points.length > 0 ? ` và ${points.length} ngày dự báo` : ''}.
        Ngày thiếu dữ liệu hoặc không có sức chứa tạo khoảng trống. Xem số liệu trong bảng bên dưới.
      </p>
      <p className="mt-2 text-xs text-neutral-600 sm:hidden">Cuộn ngang biểu đồ và bảng để xem đầy đủ số liệu.</p>
      <div role="region" aria-label="Biểu đồ có thể cuộn ngang" tabIndex={0} className="mt-4 overflow-x-auto rounded-lg focus-visible:outline-2 focus-visible:outline-red-700">
      <svg viewBox={`0 0 ${width} ${height}`} role="img" aria-labelledby={`${id}-title ${id}-description`} className="block w-full min-w-180">
        {[0, 25, 50, 75, 100].map(tick => <g key={tick}>
          <line x1={left} x2={width - right} y1={y(tick)} y2={y(tick)} stroke="currentColor" className="text-neutral-200" />
          <text x={left - 8} y={y(tick) + 4} textAnchor="end" fontSize="16" fill="currentColor" className="text-neutral-600">{tick}%</text>
        </g>)}
        {points.length > 0 && <polygon points={band} fill="currentColor" className="text-red-100" />}
        <path data-testid="occupancy-history-line" d={historicalPath} fill="none" stroke="currentColor" strokeWidth="2.5" className="text-neutral-700" />
        <path data-testid="occupancy-forecast-line" d={forecastPath} fill="none" stroke="currentColor" strokeWidth="2.5" strokeDasharray="6 4" className="text-red-700" />
        {[
          { index: 0, date: history[0].date, anchor: 'start' as const },
          { index: history.length - 1, date: history.at(-1)!.date, anchor: 'end' as const },
          ...(points.length ? [{ index: count - 1, date: points.at(-1)!.date, anchor: 'end' as const }] : []),
        ].map(tick => <text key={tick.index} x={x(tick.index)} y={height - 8} textAnchor={tick.anchor} fontSize="16" fill="currentColor" className="text-neutral-600">{displayDate(tick.date)}</text>)}
      </svg>
      </div>
      <div className="mt-3 flex flex-wrap gap-x-5 gap-y-2 text-sm text-neutral-700">
        <span className="flex items-center gap-2"><span aria-hidden="true" className="w-6 border-t-2 border-neutral-700" />Lịch sử mô phỏng</span>
        <span className="flex items-center gap-2"><span aria-hidden="true" className="w-6 border-t-2 border-dashed border-red-700" />Dự báo mô phỏng</span>
        <span className="flex items-center gap-2"><span aria-hidden="true" className="h-3 w-6 bg-red-100 border border-red-200" />Biên sai số tham chiếu P90</span>
      </div>
      <p className="mt-3 text-xs leading-relaxed text-neutral-600">Biên được lấy từ phân vị 90 của sai số tuyệt đối lịch sử. Đây không phải khoảng tin cậy 90% hoặc cam kết kết quả tương lai.</p>
    </figure>
  )
}
