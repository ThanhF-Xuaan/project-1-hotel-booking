import { useId, useState } from 'react'
import { displayDate, occupancyPercent, type OccupancyDemo, type OccupancySample, type ForecastPoint } from '../services/occupancyDemoService'

interface OccupancyChartProps {
  data: OccupancyDemo
  rangeDays?: number
  onRangeDaysChange?: (range: number) => void
}

const numberFormat = new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 1 })

export default function OccupancyChart({ data, rangeDays = 90, onRangeDaysChange }: OccupancyChartProps) {
  const id = useId()
  const [selectedRange, setSelectedRange] = useState<number>(rangeDays)
  const [hoveredPoint, setHoveredPoint] = useState<{
    date: string
    type: 'history' | 'forecast'
    percent: number
    lower?: number
    upper?: number
    occupied?: number | null
    capacity?: number | null
    x: number
    y: number
  } | null>(null)

  const handleRangeChange = (range: number) => {
    setSelectedRange(range)
    if (onRangeDaysChange) onRangeDaysChange(range)
  }

  // Filter history based on selectedRange
  const fullHistory = data.history.samples
  const history = selectedRange >= fullHistory.length ? fullHistory : fullHistory.slice(-selectedRange)
  const points = data.forecast.points

  const width = 960, height = 320, left = 55, right = 20, top = 20, bottom = 40
  const count = history.length + points.length
  const x = (i: number) => left + i * (width - left - right) / Math.max(1, count - 1)
  const y = (n: number) => top + (100 - n) * (height - top - bottom) / 100

  // Build SVG Paths
  const historicalPath = history.map((row, i) => {
    const value = occupancyPercent(row)
    if (value === null) return ''
    return `${i === 0 || occupancyPercent(history[i - 1]) === null ? 'M' : 'L'}${x(i).toFixed(1)},${y(value).toFixed(1)}`
  }).filter(Boolean).join(' ')

  const forecastPath = points.map((row, i) => `${i ? 'L' : 'M'}${x(history.length + i).toFixed(1)},${y(row.occupancyPercent).toFixed(1)}`).join(' ')
  
  const band = points.length > 0 
    ? points.map((row, i) => `${x(history.length + i).toFixed(1)},${y(row.upperPercent).toFixed(1)}`)
      .concat(points.map((row, i) => `${x(history.length + i).toFixed(1)},${y(row.lowerPercent).toFixed(1)}`).reverse()).join(' ')
    : ''

  // X-axis ticks (4 or 5 evenly spaced ticks)
  const totalTicks = 5
  const tickIndices = Array.from({ length: totalTicks }, (_, idx) => 
    Math.round(idx * (count - 1) / (totalTicks - 1))
  )

  const getDateByIndex = (idx: number): string => {
    if (idx < history.length) return history[idx].date
    const fIdx = idx - history.length
    return points[fIdx]?.date || ''
  }

  return (
    <div className="rounded-2xl border border-neutral-200 bg-white p-5 sm:p-6 shadow-xs">
      {/* Chart Header Toolbar */}
      <div className="flex flex-wrap items-center justify-between gap-4 mb-4">
        <div>
          <h2 id={`${id}-title`} className="text-base font-bold text-neutral-900 sm:text-lg">
            Xu hướng lấp đầy thực tế & Dự báo 30 ngày tới
          </h2>
          <p id={`${id}-description`} className="text-xs sm:text-sm text-neutral-500 mt-0.5">
            Đang hiển thị {history.length} ngày lịch sử thực tế và {points.length} ngày dự báo mô hình AI.
          </p>
        </div>

        {/* Range Selection Pills */}
        <div className="flex items-center gap-1.5 p-1 bg-neutral-100 rounded-xl text-xs font-semibold">
          {[
            { label: '60 ngày', value: 60 },
            { label: '90 ngày', value: 90 },
            { label: '180 ngày', value: 180 },
            { label: 'Toàn bộ 240 ngày', value: 240 },
          ].map(btn => (
            <button
              key={btn.value}
              type="button"
              onClick={() => handleRangeChange(btn.value)}
              className={`px-3 py-1.5 rounded-lg transition-all ${
                selectedRange === btn.value
                  ? 'bg-white text-neutral-900 shadow-xs font-bold'
                  : 'text-neutral-600 hover:text-neutral-900'
              }`}
            >
              {btn.label}
            </button>
          ))}
        </div>
      </div>

      {/* SVG Chart Container */}
      <div className="relative overflow-x-auto rounded-xl bg-neutral-50/50 p-2 border border-neutral-100">
        <svg 
          viewBox={`0 0 ${width} ${height}`} 
          role="img" 
          aria-labelledby={`${id}-title ${id}-description`} 
          className="block w-full min-w-180"
          onMouseLeave={() => setHoveredPoint(null)}
        >
          {/* Horizontal Grid lines */}
          {[0, 25, 50, 75, 100].map(tick => (
            <g key={tick}>
              <line 
                x1={left} 
                x2={width - right} 
                y1={y(tick)} 
                y2={y(tick)} 
                stroke="currentColor" 
                className="text-neutral-200" 
                strokeDasharray={tick === 0 || tick === 100 ? undefined : '3 3'}
              />
              <text 
                x={left - 10} 
                y={y(tick) + 4} 
                textAnchor="end" 
                fontSize="12" 
                fill="currentColor" 
                className="text-neutral-400 font-mono"
              >
                {tick}%
              </text>
            </g>
          ))}

          {/* Dividing vertical line between history and forecast */}
          {points.length > 0 && (
            <g>
              <line
                x1={x(history.length - 1)}
                x2={x(history.length - 1)}
                y1={top}
                y2={height - bottom}
                stroke="#EE0000"
                strokeWidth="1.5"
                strokeDasharray="4 2"
                opacity="0.4"
              />
              <text
                x={x(history.length - 1)}
                y={top - 4}
                textAnchor="middle"
                fontSize="11"
                fill="#EE0000"
                fontWeight="600"
              >
                MỐC HIỆN TẠI
              </text>
            </g>
          )}

          {/* Confidence Error Band */}
          {points.length > 0 && (
            <polygon 
              points={band} 
              fill="currentColor" 
              className="text-red-500/15" 
            />
          )}

          {/* Historical Solid Line */}
          <path 
            data-testid="occupancy-history-line" 
            d={historicalPath} 
            fill="none" 
            stroke="currentColor" 
            strokeWidth="2.5" 
            className="text-neutral-900" 
          />

          {/* Forecast Dashed Line */}
          {points.length > 0 && (
            <path 
              data-testid="occupancy-forecast-line" 
              d={forecastPath} 
              fill="none" 
              stroke="#EE0000" 
              strokeWidth="2.5" 
              strokeDasharray="6 4" 
            />
          )}

          {/* Interactive invisible hit areas for history points */}
          {history.map((row: OccupancySample, i: number) => {
            const val = occupancyPercent(row)
            if (val === null) return null
            const cx = x(i)
            const cy = y(val)
            return (
              <circle
                key={`h-${row.date}`}
                cx={cx}
                cy={cy}
                r="6"
                fill="transparent"
                className="cursor-pointer hover:stroke-neutral-900 hover:stroke-2 hover:fill-white"
                onMouseEnter={() => setHoveredPoint({
                  date: row.date,
                  type: 'history',
                  percent: val,
                  occupied: row.occupiedRooms,
                  capacity: row.capacityRooms,
                  x: cx,
                  y: cy
                })}
              />
            )
          })}

          {/* Interactive invisible hit areas for forecast points */}
          {points.map((row: ForecastPoint, i: number) => {
            const cx = x(history.length + i)
            const cy = y(row.occupancyPercent)
            return (
              <circle
                key={`f-${row.date}`}
                cx={cx}
                cy={cy}
                r="6"
                fill="transparent"
                className="cursor-pointer hover:stroke-red-600 hover:stroke-2 hover:fill-white"
                onMouseEnter={() => setHoveredPoint({
                  date: row.date,
                  type: 'forecast',
                  percent: row.occupancyPercent,
                  lower: row.lowerPercent,
                  upper: row.upperPercent,
                  x: cx,
                  y: cy
                })}
              />
            )
          })}

          {/* X Axis Dates */}
          {tickIndices.map((tIdx, i) => {
            const d = getDateByIndex(tIdx)
            if (!d) return null
            const anchor = i === 0 ? 'start' : i === totalTicks - 1 ? 'end' : 'middle'
            return (
              <text 
                key={tIdx} 
                x={x(tIdx)} 
                y={height - 12} 
                textAnchor={anchor} 
                fontSize="12" 
                fill="currentColor" 
                className="text-neutral-500 font-medium"
              >
                {displayDate(d)}
              </text>
            )
          })}
        </svg>

        {/* Hover Tooltip Overlay */}
        {hoveredPoint && (
          <div 
            className="absolute z-20 pointer-events-none rounded-xl bg-neutral-900/95 text-white px-3 py-2 text-xs shadow-lg backdrop-blur-sm transition-all transform -translate-x-1/2 -translate-y-full mb-2"
            style={{ 
              left: `${(hoveredPoint.x / width) * 100}%`, 
              top: `${(hoveredPoint.y / height) * 100}%` 
            }}
          >
            <div className="font-bold border-b border-neutral-700 pb-1 mb-1 flex items-center justify-between gap-3">
              <span>{displayDate(hoveredPoint.date)}</span>
              <span className={`text-[10px] px-1.5 py-0.5 rounded font-semibold ${
                hoveredPoint.type === 'history' ? 'bg-neutral-800 text-neutral-300' : 'bg-red-900/60 text-red-300'
              }`}>
                {hoveredPoint.type === 'history' ? 'Thực tế' : 'Dự báo AI'}
              </span>
            </div>
            <div className="space-y-0.5 tabular-nums">
              <div className="flex justify-between gap-4">
                <span className="text-neutral-400">Tỷ lệ lấp đầy:</span>
                <span className="font-bold text-white">{numberFormat.format(hoveredPoint.percent)}%</span>
              </div>
              {hoveredPoint.type === 'history' && hoveredPoint.occupied !== null && hoveredPoint.capacity !== null && (
                <div className="flex justify-between gap-4 text-neutral-300 text-[11px]">
                  <span className="text-neutral-400">Phòng có khách:</span>
                  <span>{hoveredPoint.occupied} / {hoveredPoint.capacity} phòng</span>
                </div>
              )}
              {hoveredPoint.type === 'forecast' && hoveredPoint.lower !== undefined && hoveredPoint.upper !== undefined && (
                <div className="flex justify-between gap-4 text-neutral-300 text-[11px]">
                  <span className="text-neutral-400">Biên P90:</span>
                  <span>{numberFormat.format(hoveredPoint.lower)}% – {numberFormat.format(hoveredPoint.upper)}%</span>
                </div>
              )}
            </div>
          </div>
        )}
      </div>

      {/* Legend & Details */}
      <div className="mt-4 pt-3 border-t border-neutral-100 flex flex-wrap items-center justify-between gap-4 text-xs text-neutral-600">
        <div className="flex flex-wrap items-center gap-x-6 gap-y-2">
          <span className="flex items-center gap-2">
            <span aria-hidden="true" className="w-5 h-0.5 bg-neutral-900 rounded" />
            <span className="font-medium text-neutral-800">Lịch sử thực tế (PostgreSQL)</span>
          </span>
          <span className="flex items-center gap-2">
            <span aria-hidden="true" className="w-5 border-t-2 border-dashed border-red-600" />
            <span className="font-medium text-red-700">Dự báo mô hình Ridge (30 ngày tới)</span>
          </span>
          <span className="flex items-center gap-2">
            <span aria-hidden="true" className="h-3 w-5 bg-red-500/20 rounded border border-red-200" />
            <span>Dải biên sai số P90 (Backtest)</span>
          </span>
        </div>
        <p className="text-[11px] text-neutral-400 italic">
          * Rê chuột vào các điểm trên biểu đồ để xem chi tiết từng ngày.
        </p>
      </div>
    </div>
  )
}
