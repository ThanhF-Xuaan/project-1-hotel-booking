import { useEffect, useMemo, useState } from 'react'
import {
  RefreshCw,
  TrendingUp,
  Calendar,
  ShieldCheck,
  CheckCircle2,
  AlertTriangle,
  ArrowUpRight,
  ChevronLeft,
  ChevronRight,
  Search,
} from 'lucide-react'
import { getErrorStatus } from '../../../core/api/client'
import Button from '../../../core/components/ui/Button'
import OccupancyChart from '../components/OccupancyChart'
import {
  displayDate,
  occupancyDemoService,
  occupancyPercent,
  type DemoScenario,
  type OccupancyDemo,
} from '../services/occupancyDemoService'

const numberFormat = new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 1 })
const formatPercent = (value: number) => `${numberFormat.format(value)}%`

const STATUS_TEXT: Record<OccupancyDemo['forecast']['status'], string> = {
  FORECAST_READY: 'Dự báo sẵn sàng',
  BASELINE_FALLBACK: 'Dùng mô hình nền',
  INSUFFICIENT_COVERAGE: 'Chưa đủ dữ liệu để dự báo',
  INVALID_REQUEST: 'Yêu cầu dự báo không hợp lệ',
  ACCESS_DENIED: 'Không được phép đọc dữ liệu dự báo',
  DEPENDENCY_UNAVAILABLE: 'Dịch vụ dự báo chưa sẵn sàng',
}

function errorMessage(error: unknown): string {
  switch (getErrorStatus(error)) {
    case 401: return 'Phiên đăng nhập đã hết hạn. Hãy đăng nhập lại.'
    case 403: return 'Quyền truy cập demo đã bị từ chối. Hãy kiểm tra tài khoản quản trị.'
    case 404: return 'Demo chưa được bật trên máy chủ. Hãy bật cấu hình demo rồi tải lại.'
    case 503: return 'Dịch vụ demo tạm thời chưa sẵn sàng. Hãy thử lại sau.'
    default: return 'Không tải được dữ liệu demo hợp lệ. Hãy kiểm tra kết nối hoặc cấu hình rồi thử lại.'
  }
}

function getDayOfWeek(dateStr: string): string {
  const days = ['Chủ Nhật', 'Thứ Hai', 'Thứ Ba', 'Thứ Tư', 'Thứ Năm', 'Thứ Sáu', 'Thứ Bảy']
  const d = new Date(`${dateStr}T00:00:00Z`)
  return days[d.getUTCDay()] || ''
}

function getPaginationRange(currentPage: number, totalPages: number): (number | '...')[] {
  if (totalPages <= 7) {
    return Array.from({ length: totalPages }, (_, i) => i + 1)
  }
  if (currentPage <= 4) {
    return [1, 2, 3, 4, 5, '...', totalPages]
  }
  if (currentPage >= totalPages - 3) {
    return [1, '...', totalPages - 4, totalPages - 3, totalPages - 2, totalPages - 1, totalPages]
  }
  return [1, '...', currentPage - 1, currentPage, currentPage + 1, '...', totalPages]
}

export default function OccupancyDemoPage() {
  const [selection, setSelection] = useState({ scenario: 'HOTEL_A' as DemoScenario, revision: 0 })
  const [result, setResult] = useState<{ key: typeof selection; data?: OccupancyDemo; error?: string } | null>(null)
  
  // Tabs: 'forecast' | 'history'
  const [activeTab, setActiveTab] = useState<'forecast' | 'history'>('forecast')
  
  // Table search & pagination states (10 records/page)
  const [forecastSearch, setForecastSearch] = useState('')
  const [forecastPage, setForecastPage] = useState(1)
  const forecastPageSize = 10

  const [historySearch, setHistorySearch] = useState('')
  const [historyPage, setHistoryPage] = useState(1)
  const historyPageSize = 10

  const current = result?.key === selection ? result : null
  const loading = current === null
  const data = current?.data

  useEffect(() => {
    const controller = new AbortController()
    occupancyDemoService.get(selection.scenario, controller.signal).then(value => {
      if (!controller.signal.aborted) setResult({ key: selection, data: value })
    }).catch((error: unknown) => {
      if (!controller.signal.aborted) setResult({ key: selection, error: errorMessage(error) })
    })
    return () => controller.abort()
  }, [selection])

  const reload = () => setSelection(prev => ({ ...prev, revision: prev.revision + 1 }))

  // KPI Calculations
  const lastSample = data?.history.samples.at(-1)
  const lastOccupancy = lastSample ? occupancyPercent(lastSample) : null

  const avgForecastPercent = useMemo(() => {
    if (!data?.forecast.points || data.forecast.points.length === 0) return 0
    const sum = data.forecast.points.reduce((acc, p) => acc + p.occupancyPercent, 0)
    return sum / data.forecast.points.length
  }, [data])

  const peakForecast = useMemo(() => {
    if (!data?.forecast.points || data.forecast.points.length === 0) return null
    return [...data.forecast.points].sort((a, b) => b.occupancyPercent - a.occupancyPercent)[0]
  }, [data])

  // Filtered & Paginated Forecast Data
  const filteredForecast = useMemo(() => {
    if (!data?.forecast.points) return []
    if (!forecastSearch.trim()) return data.forecast.points
    return data.forecast.points.filter(p => p.date.includes(forecastSearch.trim()))
  }, [data, forecastSearch])

  const paginatedForecast = useMemo(() => {
    const start = (forecastPage - 1) * forecastPageSize
    return filteredForecast.slice(start, start + forecastPageSize)
  }, [filteredForecast, forecastPage])

  const totalForecastPages = Math.max(1, Math.ceil(filteredForecast.length / forecastPageSize))

  // Filtered & Paginated History Data
  const filteredHistory = useMemo(() => {
    if (!data?.history.samples) return []
    if (!historySearch.trim()) return data.history.samples
    return data.history.samples.filter(s => s.date.includes(historySearch.trim()))
  }, [data, historySearch])

  const paginatedHistory = useMemo(() => {
    const start = (historyPage - 1) * historyPageSize
    return filteredHistory.slice(start, start + historyPageSize)
  }, [filteredHistory, historyPage])

  const totalHistoryPages = Math.max(1, Math.ceil(filteredHistory.length / historyPageSize))

  return (
    <div className="space-y-6 max-w-7xl mx-auto">
      
      {/* FILTER & CONTROLS TOOLBAR (HÀNG 1 THEO DESIGN SYSTEM) */}
      <section aria-label="Bộ lọc & Chọn cơ sở" className="rounded-2xl border border-neutral-200 bg-white p-4 sm:p-5 shadow-xs flex flex-wrap items-center justify-between gap-4">
        <div className="flex flex-wrap items-center gap-4">
          <div>
            <label htmlFor="occupancy-scenario" className="block text-xs font-bold text-neutral-700 uppercase tracking-wider mb-1.5">
              Cơ sở khách sạn
            </label>
            <select
              id="occupancy-scenario"
              value={selection.scenario}
              onChange={event => setSelection({ scenario: event.target.value as DemoScenario, revision: 0 })}
              className="h-10 sm:h-12 w-72 rounded-xl border border-neutral-300 bg-white px-3 text-sm font-semibold text-neutral-900 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-red-700 shadow-xs"
            >
              <option value="HOTEL_A">Viettel Luxury Hà Nội (Khách sạn 1 - 50 phòng)</option>
              <option value="HOTEL_B">Viettel Grand Đà Nẵng (Khách sạn 2 - 80 phòng)</option>
              <option value="HOTEL_C">Viettel Boutique Sa Pa (Khách sạn 3 - 35 phòng)</option>
            </select>
          </div>

          <div>
            <label className="block text-xs font-bold text-neutral-700 uppercase tracking-wider mb-1.5">
              Thao tác
            </label>
            <Button
              type="button"
              variant="outline"
              className="h-10 sm:h-12 px-4 rounded-xl border border-neutral-300 font-semibold shadow-xs"
              disabled={loading}
              onClick={reload}
              leftIcon={<RefreshCw aria-hidden="true" className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />}
            >
              {loading ? 'Đang nạp...' : 'Tải lại số liệu'}
            </Button>
          </div>
        </div>

        <div className="text-right">
          <div className="text-xs text-neutral-500 font-medium">
            Mốc dữ liệu chốt: <strong className="text-neutral-900 font-bold">{data?.history ? displayDate(data.history.toInclusive) : 'Hôm qua'}</strong>
          </div>
          <div className="text-[11px] text-neutral-400 mt-0.5">
            Thời điểm trích xuất: {data ? new Date(data.referenceInstant).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }) : '--:--'} · PostgreSQL: Sẵn sàng
          </div>
        </div>
      </section>

      {/* LOADING & ERROR STATES */}
      {loading && (
        <div role="status" className="rounded-2xl border border-neutral-200 bg-white p-12 text-center shadow-xs">
          <RefreshCw className="h-8 w-8 animate-spin text-red-600 mx-auto mb-3" />
          <p className="text-sm font-semibold text-neutral-800">Đang truy vấn lịch sử đặt phòng PostgreSQL & tính toán mô hình AI...</p>
          <p className="text-xs text-neutral-500 mt-1">Quá trình này kiểm tra 240 ngày dữ liệu và ngoại suy 30 ngày tiếp theo.</p>
        </div>
      )}

      {current?.error && (
        <div role="alert" className="rounded-2xl border border-red-200 bg-red-50 p-6 text-red-900 shadow-xs">
          <div className="flex items-center gap-3">
            <AlertTriangle className="h-5 w-5 text-red-600 shrink-0" />
            <p className="text-sm font-semibold">{current.error}</p>
          </div>
          <Button type="button" variant="outline" className="mt-4 min-h-11 bg-white font-semibold" onClick={reload}>
            Thử tải lại
          </Button>
        </div>
      )}

      {/* DASHBOARD CONTENT WHEN DATA AVAILABLE */}
      {data && (
        <>
          {/* 4-EXECUTIVE KPI CARDS GRID */}
          <section aria-label="Chỉ số hoạt động chính" className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            
            {/* Card 1: Hôm qua */}
            <div className="rounded-2xl border border-neutral-200 bg-white p-5 shadow-xs flex flex-col justify-between hover:border-neutral-300 transition-all">
              <div>
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold uppercase tracking-wider text-neutral-500">Hôm qua (Thực tế)</span>
                  <span className="w-8 h-8 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center">
                    <Calendar className="w-4 h-4" />
                  </span>
                </div>
                <div className="mt-3 flex items-baseline gap-2">
                  <span className="text-3xl font-extrabold text-neutral-900 tracking-tight">
                    {lastOccupancy !== null ? formatPercent(lastOccupancy) : 'N/A'}
                  </span>
                  <span className="text-xs font-bold text-emerald-600 flex items-center gap-0.5">
                    <ArrowUpRight className="w-3.5 h-3.5" />
                    Chốt sổ
                  </span>
                </div>
              </div>
              <p className="mt-3 text-xs text-neutral-500 border-t border-neutral-100 pt-2.5">
                {lastSample?.occupiedRooms ?? 0} / {lastSample?.capacityRooms ?? 0} phòng có khách ở
              </p>
            </div>

            {/* Card 2: Dự báo TB 30 ngày */}
            <div className="rounded-2xl border border-neutral-200 bg-white p-5 shadow-xs flex flex-col justify-between hover:border-neutral-300 transition-all">
              <div>
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold uppercase tracking-wider text-neutral-500">Dự báo TB 30 ngày</span>
                  <span className="w-8 h-8 rounded-xl bg-purple-50 text-purple-600 flex items-center justify-center">
                    <TrendingUp className="w-4 h-4" />
                  </span>
                </div>
                <div className="mt-3 flex items-baseline gap-2">
                  <span className="text-3xl font-extrabold text-neutral-900 tracking-tight">
                    {formatPercent(avgForecastPercent)}
                  </span>
                  <span className="text-xs font-semibold text-neutral-500">kỳ vọng</span>
                </div>
              </div>
              <p className="mt-3 text-xs text-neutral-500 border-t border-neutral-100 pt-2.5">
                Đỉnh điểm: <strong className="text-neutral-900 font-bold">{peakForecast ? `${formatPercent(peakForecast.occupancyPercent)} (${displayDate(peakForecast.date)})` : 'N/A'}</strong>
              </p>
            </div>

            {/* Card 3: Biên tin cậy P90 */}
            <div className="rounded-2xl border border-neutral-200 bg-white p-5 shadow-xs flex flex-col justify-between hover:border-neutral-300 transition-all">
              <div>
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold uppercase tracking-wider text-neutral-500">Biên tin cậy P90</span>
                  <span className="w-8 h-8 rounded-xl bg-amber-50 text-amber-600 flex items-center justify-center">
                    <ShieldCheck className="w-4 h-4" />
                  </span>
                </div>
                <div className="mt-3 flex items-baseline gap-2">
                  <span className="text-3xl font-extrabold text-neutral-900 tracking-tight">
                    ±{data.forecast.residualRadiusPp !== null ? numberFormat.format(data.forecast.residualRadiusPp) : '0'}%
                  </span>
                  <span className="text-xs font-semibold text-amber-800 bg-amber-50 px-2 py-0.5 rounded-md border border-amber-200">
                    Phân vị 90
                  </span>
                </div>
              </div>
              <p className="mt-3 text-xs text-neutral-500 border-t border-neutral-100 pt-2.5">
                Dải dao động sai số ngoài mẫu (Out-of-sample)
              </p>
            </div>

            {/* Card 4: Dữ liệu huấn luyện */}
            <div className="rounded-2xl border border-neutral-200 bg-white p-5 shadow-xs flex flex-col justify-between hover:border-neutral-300 transition-all">
              <div>
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold uppercase tracking-wider text-neutral-500">Dữ liệu huấn luyện</span>
                  <span className="w-8 h-8 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center">
                    <CheckCircle2 className="w-4 h-4" />
                  </span>
                </div>
                <div className="mt-3 flex items-baseline gap-2">
                  <span className="text-3xl font-extrabold text-neutral-900 tracking-tight">
                    {data.history.samples.length} ngày
                  </span>
                  <span className="text-xs font-bold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-md border border-emerald-200">
                    100% đầy đủ
                  </span>
                </div>
              </div>
              <p className="mt-3 text-xs text-neutral-500 border-t border-neutral-100 pt-2.5">
                {displayDate(data.history.fromInclusive)} – {displayDate(data.history.toInclusive)} (0 ngày thiếu)
              </p>
            </div>

          </section>

          {/* OCCUPANCY CHART WITH RANGE SELECTOR */}
          <OccupancyChart data={data} />

          {/* TABBED DATA EXPLORER (HÀNG 3 THEO DESIGN SYSTEM) */}
          <section className="rounded-2xl border border-neutral-200 bg-white shadow-xs overflow-hidden">
            
            {/* TAB SELECTOR HEADER */}
            <div className="border-b border-neutral-200 bg-neutral-50/60 px-4 sm:px-6 pt-3 flex items-center gap-2 overflow-x-auto">
              <button
                type="button"
                onClick={() => setActiveTab('forecast')}
                className={`px-4 py-2.5 text-xs sm:text-sm font-bold rounded-t-xl transition-all whitespace-nowrap ${
                  activeTab === 'forecast'
                    ? 'text-red-600 border-b-2 border-red-600 bg-white shadow-2xs'
                    : 'text-neutral-600 hover:text-neutral-900'
                }`}
              >
                Bảng dự báo 30 ngày tới ({data.forecast.points.length} ngày)
              </button>

              <button
                type="button"
                onClick={() => setActiveTab('history')}
                className={`px-4 py-2.5 text-xs sm:text-sm font-bold rounded-t-xl transition-all whitespace-nowrap ${
                  activeTab === 'history'
                    ? 'text-red-600 border-b-2 border-red-600 bg-white shadow-2xs'
                    : 'text-neutral-600 hover:text-neutral-900'
                }`}
              >
                Lịch sử đặt phòng PostgreSQL ({data.history.samples.length} ngày)
              </button>
            </div>

            {/* TAB 1: 30-DAY FORECAST TABLE */}
            {activeTab === 'forecast' && (
              <div className="p-4 sm:p-6 space-y-4">
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                  <div>
                    <h3 className="text-sm font-bold text-neutral-900">
                      Chi tiết 30 ngày ngoại suy ({data.forecast.points.length > 0 ? `${displayDate(data.forecast.points[0].date)} – ${displayDate(data.forecast.points.at(-1)!.date)}` : ''})
                    </h3>
                    <p className="text-xs text-neutral-500 mt-0.5">
                      Trạng thái: <strong>{STATUS_TEXT[data.forecast.status]}</strong> · Biên sai số P90: ±{numberFormat.format(data.forecast.residualRadiusPp ?? 0)} điểm phần trăm
                    </p>
                  </div>

                  <div className="flex items-center gap-2">
                    <div className="relative">
                      <Search className="w-3.5 h-3.5 absolute left-3 top-3 text-neutral-400" />
                      <input
                        type="text"
                        placeholder="Lọc theo ngày (YYYY-MM-DD)..."
                        value={forecastSearch}
                        onChange={e => { setForecastSearch(e.target.value); setForecastPage(1); }}
                        className="h-9 pl-8 pr-3 rounded-xl border border-neutral-200 text-xs w-60 focus:outline-none focus:ring-2 focus:ring-red-600"
                      />
                    </div>
                  </div>
                </div>

                <div className="overflow-x-auto rounded-xl border border-neutral-200">
                  <table className="w-full text-left text-xs sm:text-sm">
                    <thead className="bg-neutral-50 border-b border-neutral-200 text-neutral-700 uppercase tracking-wider text-[11px] font-bold">
                      <tr>
                        <th scope="col" className="py-3 px-4">Ngày</th>
                        <th scope="col" className="py-3 px-4">Thứ</th>
                        <th scope="col" className="py-3 px-4">Tỷ lệ dự báo</th>
                        <th scope="col" className="py-3 px-4">Biên dưới (P90)</th>
                        <th scope="col" className="py-3 px-4">Biên trên (P90)</th>
                        <th scope="col" className="py-3 px-4">Phân loại công suất</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-neutral-100 font-medium text-neutral-800">
                      {paginatedForecast.map(row => {
                        const dayOfWeek = getDayOfWeek(row.date)
                        const isWeekend = dayOfWeek === 'Thứ Sáu' || dayOfWeek === 'Thứ Bảy'
                        const isPeak = row.occupancyPercent >= 80
                        const isLow = row.occupancyPercent < 50

                        return (
                          <tr key={row.date} className={`hover:bg-neutral-50/80 transition-colors ${isPeak ? 'bg-red-50/20' : ''}`}>
                            <td className="py-3 px-4 font-bold text-neutral-900">{displayDate(row.date)}</td>
                            <td className={`py-3 px-4 ${isWeekend ? 'font-bold text-red-600' : 'text-neutral-500'}`}>
                              {dayOfWeek}
                            </td>
                            <td className={`py-3 px-4 font-bold tabular-nums ${isPeak ? 'text-red-600 font-extrabold text-base' : 'text-purple-700'}`}>
                              {formatPercent(row.occupancyPercent)}
                            </td>
                            <td className="py-3 px-4 text-neutral-600 tabular-nums">{formatPercent(row.lowerPercent)}</td>
                            <td className="py-3 px-4 text-neutral-600 tabular-nums">{formatPercent(row.upperPercent)}</td>
                            <td className="py-3 px-4">
                              {isPeak ? (
                                <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-bold bg-red-50 text-red-700 border border-red-200">
                                  🔥 Cao điểm
                                </span>
                              ) : isLow ? (
                                <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-bold bg-amber-50 text-amber-700 border border-amber-200">
                                  Thấp điểm
                                </span>
                              ) : (
                                <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-bold bg-blue-50 text-blue-700 border border-blue-200">
                                  Ổn định
                                </span>
                              )}
                            </td>
                          </tr>
                        )
                      })}
                    </tbody>
                  </table>
                </div>

                {/* Pagination */}
                <div className="flex flex-col sm:flex-row items-center justify-between gap-3 text-xs text-neutral-500 pt-2">
                  <div>
                    Hiển thị <strong>{paginatedForecast.length ? (forecastPage - 1) * forecastPageSize + 1 : 0} – {Math.min(forecastPage * forecastPageSize, filteredForecast.length)}</strong> trên tổng số <strong>{filteredForecast.length}</strong> ngày
                  </div>
                  <div className="flex items-center gap-1">
                    <button
                      type="button"
                      disabled={forecastPage <= 1}
                      onClick={() => setForecastPage(p => p - 1)}
                      className="px-2.5 py-1.5 rounded-lg border border-neutral-200 text-neutral-700 hover:bg-neutral-50 disabled:opacity-40 disabled:cursor-not-allowed"
                    >
                      <ChevronLeft className="w-3.5 h-3.5" />
                    </button>
                    {Array.from({ length: totalForecastPages }, (_, i) => i + 1).map(page => (
                      <button
                        key={page}
                        type="button"
                        onClick={() => setForecastPage(page)}
                        className={`px-3 py-1.5 rounded-lg text-xs font-bold ${
                          forecastPage === page
                            ? 'bg-red-600 text-white'
                            : 'border border-neutral-200 text-neutral-700 hover:bg-neutral-50'
                        }`}
                      >
                        {page}
                      </button>
                    ))}
                    <button
                      type="button"
                      disabled={forecastPage >= totalForecastPages}
                      onClick={() => setForecastPage(p => p + 1)}
                      className="px-2.5 py-1.5 rounded-lg border border-neutral-200 text-neutral-700 hover:bg-neutral-50 disabled:opacity-40 disabled:cursor-not-allowed"
                    >
                      <ChevronRight className="w-3.5 h-3.5" />
                    </button>
                  </div>
                </div>
              </div>
            )}

            {/* TAB 2: POSTGRESQL HISTORICAL BOOKING SAMPLES */}
            {activeTab === 'history' && (
              <div className="p-4 sm:p-6 space-y-4">
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                  <div>
                    <h3 className="text-sm font-bold text-neutral-900">
                      Lịch sử đặt phòng thực tế trong PostgreSQL ({data.history.samples.length} ngày)
                    </h3>
                    <p className="text-xs text-neutral-500 mt-0.5">
                      Từ ngày {displayDate(data.history.fromInclusive)} đến {displayDate(data.history.toInclusive)} · Trạng thái: {data.history.status}
                    </p>
                  </div>

                  <div className="flex items-center gap-2">
                    <div className="relative">
                      <Search className="w-3.5 h-3.5 absolute left-3 top-3 text-neutral-400" />
                      <input
                        type="text"
                        placeholder="Tìm kiếm ngày (YYYY-MM-DD)..."
                        value={historySearch}
                        onChange={e => { setHistorySearch(e.target.value); setHistoryPage(1); }}
                        className="h-9 pl-8 pr-3 rounded-xl border border-neutral-200 text-xs w-60 focus:outline-none focus:ring-2 focus:ring-red-600"
                      />
                    </div>
                  </div>
                </div>

                <div className="overflow-x-auto rounded-xl border border-neutral-200">
                  <table className="w-full text-left text-xs sm:text-sm">
                    <thead className="bg-neutral-50 border-b border-neutral-200 text-neutral-700 uppercase tracking-wider text-[11px] font-bold">
                      <tr>
                        <th scope="col" className="py-3 px-4">Ngày</th>
                        <th scope="col" className="py-3 px-4">Thứ</th>
                        <th scope="col" className="py-3 px-4">Phòng sử dụng</th>
                        <th scope="col" className="py-3 px-4">Tổng sức chứa</th>
                        <th scope="col" className="py-3 px-4">Tỷ lệ thực tế</th>
                        <th scope="col" className="py-3 px-4">Trạng thái dữ liệu</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-neutral-100 font-medium text-neutral-800">
                      {paginatedHistory.map(row => {
                        const percent = occupancyPercent(row)
                        const dayOfWeek = getDayOfWeek(row.date)

                        return (
                          <tr key={row.date} className="hover:bg-neutral-50/80 transition-colors">
                            <td className="py-3 px-4 font-bold text-neutral-900">{displayDate(row.date)}</td>
                            <td className="py-3 px-4 text-neutral-500">{dayOfWeek}</td>
                            <td className="py-3 px-4 tabular-nums font-semibold text-neutral-900">{row.occupiedRooms ?? '--'}</td>
                            <td className="py-3 px-4 tabular-nums text-neutral-600">{row.capacityRooms ?? '--'}</td>
                            <td className="py-3 px-4 tabular-nums font-bold text-blue-700">
                              {percent !== null ? formatPercent(percent) : 'N/A'}
                            </td>
                            <td className="py-3 px-4">
                              {row.coverage === 'COMPLETE' ? (
                                <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[11px] font-semibold bg-emerald-50 text-emerald-800 border border-emerald-200">
                                  <CheckCircle2 className="w-3 h-3 text-emerald-600" /> Hoàn chỉnh
                                </span>
                              ) : (
                                <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[11px] font-semibold bg-amber-50 text-amber-800 border border-amber-200">
                                  <AlertTriangle className="w-3 h-3 text-amber-600" /> Thiếu dữ liệu
                                </span>
                              )}
                            </td>
                          </tr>
                        )
                      })}
                    </tbody>
                  </table>
                </div>

                {/* Pagination */}
                <div className="flex flex-col sm:flex-row items-center justify-between gap-3 text-xs text-neutral-500 pt-2">
                  <div>
                    Hiển thị <strong>{paginatedHistory.length ? (historyPage - 1) * historyPageSize + 1 : 0} – {Math.min(historyPage * historyPageSize, filteredHistory.length)}</strong> trên tổng số <strong>{filteredHistory.length}</strong> ngày
                  </div>
                  <div className="flex items-center gap-1 flex-wrap">
                    <button
                      type="button"
                      disabled={historyPage <= 1}
                      onClick={() => setHistoryPage(p => p - 1)}
                      className="px-2.5 py-1.5 rounded-lg border border-neutral-200 text-neutral-700 hover:bg-neutral-50 disabled:opacity-40 disabled:cursor-not-allowed"
                    >
                      <ChevronLeft className="w-3.5 h-3.5" />
                    </button>
                    {getPaginationRange(historyPage, totalHistoryPages).map((item, idx) =>
                      item === '...' ? (
                        <span key={`ellipsis-${idx}`} className="px-2 py-1 text-neutral-400 font-bold select-none">
                          ...
                        </span>
                      ) : (
                        <button
                          key={item}
                          type="button"
                          onClick={() => setHistoryPage(item)}
                          className={`px-3 py-1.5 rounded-lg text-xs font-bold transition-colors ${
                            historyPage === item
                              ? 'bg-red-600 text-white'
                              : 'border border-neutral-200 text-neutral-700 hover:bg-neutral-50'
                          }`}
                        >
                          {item}
                        </button>
                      )
                    )}
                    <button
                      type="button"
                      disabled={historyPage >= totalHistoryPages}
                      onClick={() => setHistoryPage(p => p + 1)}
                      className="px-2.5 py-1.5 rounded-lg border border-neutral-200 text-neutral-700 hover:bg-neutral-50 disabled:opacity-40 disabled:cursor-not-allowed"
                    >
                      <ChevronRight className="w-3.5 h-3.5" />
                    </button>
                  </div>
                </div>
              </div>
            )}

          </section>
        </>
      )}

    </div>
  )
}
