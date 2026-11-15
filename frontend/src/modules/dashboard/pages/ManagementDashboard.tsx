import React, { useState, useEffect } from 'react';
import {
  LayoutDashboard,
  TrendingUp,
  Percent,
  DollarSign,
  Sparkles,
  RefreshCw,
  UserCheck,
  LogOut,
  Users,
} from 'lucide-react';
import Button from '../../../core/components/ui/Button';
import Input from '../../../core/components/ui/Input';
import Select from '../../../core/components/ui/Select';
import { dashboardService } from '../services/dashboard.service';
import type {
  DashboardKpiResponse,
  OccupancyTrendDto,
  RevenueTrendDto,
} from '../types/dashboard.types';

export const ManagementDashboard: React.FC = () => {
  const [hotelId, setHotelId] = useState<number>(0);
  const [hotels, setHotels] = useState<{ id: number; name: string }[]>([]);
  const [hotelsReady, setHotelsReady] = useState(false);
  const [hotelError, setHotelError] = useState('');
  const [hotelRevision, setHotelRevision] = useState(0);
  const [targetDate, setTargetDate] = useState<string>(
    new Intl.DateTimeFormat('sv-SE', { timeZone: 'Asia/Ho_Chi_Minh' }).format(new Date())
  );
  const [revision, setRevision] = useState(0);
  const [loaded, setLoaded] = useState<{
    key: string; kpis?: DashboardKpiResponse; revenue?: RevenueTrendDto[];
    occupancy?: OccupancyTrendDto[]; errors: string[];
  }>();
  const key = JSON.stringify([hotelId, targetDate, revision]);
  const current = loaded?.key === key ? loaded : undefined;
  const kpis = current?.kpis;
  const revenueTrends = current?.revenue ?? [];
  const occupancyTrends = current?.occupancy ?? [];
  const validDate = /^\d{4}-\d{2}-\d{2}$/.test(targetDate);
  const isLoading = !!hotelId && validDate && !current;
  const fetchDashboardData = () => setRevision(value => value + 1);

  useEffect(() => {
    const controller = new AbortController();
    void dashboardService.getHotels(controller.signal).then(options => {
      if (controller.signal.aborted) return;
      setHotels(options);
      setHotelId(options[0]?.id ?? 0);
      setHotelsReady(true);
      setHotelError('');
    }).catch(() => {
      if (!controller.signal.aborted) {
        setHotelsReady(true);
        setHotelError('Không tải được danh sách khách sạn. Vui lòng thử lại.');
      }
    });
    return () => controller.abort();
  }, [hotelRevision]);

  useEffect(() => {
    if (!hotelId || !validDate) return;
    let active = true;
    const end = new Date(targetDate + 'T00:00:00Z');
    end.setUTCDate(end.getUTCDate() - 6);
    const startDate = end.toISOString().slice(0, 10);
    void Promise.allSettled([
      dashboardService.getExecutiveKpis(hotelId, targetDate),
      dashboardService.getRevenueTrends(hotelId, startDate, targetDate),
      dashboardService.getOccupancyTrends(hotelId, startDate, targetDate),
    ]).then(([kpi, revenue, occupancy]) => {
      if (!active) return;
      setLoaded({ key,
        kpis: kpi.status === 'fulfilled' ? kpi.value : undefined,
        revenue: revenue.status === 'fulfilled' ? revenue.value : undefined,
        occupancy: occupancy.status === 'fulfilled' ? occupancy.value : undefined,
        errors: [kpi.status === 'rejected' ? 'chỉ số KPI' : '',
          revenue.status === 'rejected' ? 'biểu đồ doanh thu' : '',
          occupancy.status === 'rejected' ? 'biểu đồ lấp đầy' : ''].filter(Boolean),
      });
    });
    return () => { active = false; };
  }, [hotelId, targetDate, validDate, key]);

  const formatVND = (amount: number) =>
    new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount || 0);

  const maxRevenue = Math.max(...revenueTrends.map((r) => r.totalRevenue), 1);

  return (
    <div className="space-y-6" aria-busy={!hotelsReady || isLoading}>
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold text-neutral-900 tracking-tight flex items-center gap-2">
            <LayoutDashboard className="w-7 h-7 text-red-600" />
            Bảng Điều Hành Quản Trị Khách Sạn (Executive Dashboard)
          </h1>
          <p className="text-sm text-neutral-500 mt-1">
            Tổng quan hiệu suất kinh doanh, chỉ số KPI ngành khách sạn (OCC, ADR, RevPAR) thời gian thực
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-3">
          <div className="w-56">
            <Select<number>
              value={hotelId}
              onChange={(val) => setHotelId(Number(val))}
              options={hotels.map(hotel => ({ value: hotel.id, label: hotel.name }))}
              disabled={!hotelsReady || !!hotelError || hotels.length === 0}
              placeholder="Chọn khách sạn"
              size="md"
            />
          </div>

          <div className="w-44">
            <Input
              type="date"
              aria-label="Ngày báo cáo"
              value={targetDate}
              onChange={(e) => setTargetDate(e.target.value)}
              className="h-11"
            />
          </div>

          <Button
            variant="secondary"
            onClick={fetchDashboardData}
            disabled={isLoading || !hotelId || !validDate}
            className="h-11 flex items-center gap-1.5 text-xs"
          >
            <RefreshCw className={`w-4 h-4 ${isLoading ? 'animate-spin text-red-600' : ''}`} />
            {isLoading ? 'Đang tải...' : 'Làm mới'}
          </Button>
        </div>
      </div>

      {!hotelsReady && <p role="status">Đang tải danh sách khách sạn…</p>}
      {hotelError && <div role="alert">{hotelError} <Button onClick={() => setHotelRevision(v => v + 1)}>Thử lại</Button></div>}
      {hotelsReady && !hotelError && hotels.length === 0 && <p role="status">Chưa có khách sạn trong phạm vi truy cập.</p>}
      {isLoading && <p role="status">Đang tải dữ liệu dashboard…</p>}
      {!validDate && <p role="alert">Vui lòng chọn ngày báo cáo hợp lệ.</p>}
      {!!current?.errors.length && <p role="alert">Không tải được {current.errors.join(', ')}. Nhấn Làm mới để thử lại.</p>}

      {/* 4 Core KPI Metric Cards */}
      {kpis && <>
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* OCC */}
        <div className="bg-white p-5 rounded-2xl border border-neutral-200 shadow-xs hover:border-red-300 transition-all">
          <div className="flex justify-between items-start">
            <div className="text-xs font-bold text-neutral-500 uppercase tracking-wider flex items-center gap-1">
              <Percent className="w-4 h-4 text-red-600" />
              Tỷ Lệ Lấp Đầy (OCC)
            </div>
          </div>
          <div className="text-3xl font-extrabold text-neutral-900 mt-3">
            {kpis?.occupancyRate ?? 0}%
          </div>
          <div className="mt-3">
            <div className="w-full bg-neutral-100 rounded-full h-2 overflow-hidden">
              <div
                className="bg-red-600 h-2 rounded-full transition-all duration-500"
                style={{ width: `${Math.min(100, kpis?.occupancyRate ?? 0)}%` }}
              />
            </div>
            <div className="flex justify-between text-xs text-neutral-500 mt-1.5">
              <span>Đang ở: {kpis?.occupiedRooms ?? 0} phòng</span>
              <span>Tổng: {kpis?.totalActiveRooms ?? 0} phòng</span>
            </div>
          </div>
        </div>

        {/* ADR */}
        <div className="bg-white p-5 rounded-2xl border border-neutral-200 shadow-xs hover:border-red-300 transition-all">
          <div className="flex justify-between items-start">
            <div className="text-xs font-bold text-neutral-500 uppercase tracking-wider flex items-center gap-1">
              <TrendingUp className="w-4 h-4 text-blue-600" />
              Giá BQ / Phòng (ADR)
            </div>
            <span className="text-xs font-semibold px-2 py-0.5 bg-blue-50 text-blue-700 rounded-full">
              Average Rate
            </span>
          </div>
          <div className="text-2xl font-extrabold text-neutral-900 mt-3">
            {formatVND(kpis?.averageDailyRate ?? 0)}
          </div>
          <p className="text-xs text-neutral-500 mt-2">
            Doanh thu phòng trung bình trên mỗi phòng có khách ở
          </p>
        </div>

        {/* RevPAR */}
        <div className="bg-white p-5 rounded-2xl border border-neutral-200 shadow-xs hover:border-red-300 transition-all">
          <div className="flex justify-between items-start">
            <div className="text-xs font-bold text-neutral-500 uppercase tracking-wider flex items-center gap-1">
              <DollarSign className="w-4 h-4 text-purple-600" />
              Doanh Thu / Tồn Phòng (RevPAR)
            </div>
            <span className="text-xs font-semibold px-2 py-0.5 bg-purple-50 text-purple-700 rounded-full">
              OCC × ADR
            </span>
          </div>
          <div className="text-2xl font-extrabold text-neutral-900 mt-3">
            {formatVND(kpis?.revPar ?? 0)}
          </div>
          <p className="text-xs text-neutral-500 mt-2">
            Hiệu suất doanh thu tính trên toàn bộ số phòng khả dụng
          </p>
        </div>

        {/* Total Revenue Today */}
        <div className="bg-white p-5 rounded-2xl border-2 border-red-500 shadow-xs bg-red-50/10 hover:shadow-md transition-all">
          <div className="flex justify-between items-start">
            <div className="text-xs font-bold text-red-600 uppercase tracking-wider flex items-center gap-1">
              <Sparkles className="w-4 h-4 text-red-600" />
              Tổng Doanh Thu Ngày
            </div>
            <span className="text-xs font-bold px-2 py-0.5 bg-red-600 text-white rounded-full">
              {targetDate.split('-').reverse().join('/')}
            </span>
          </div>
          <div className="text-2xl font-extrabold text-red-600 mt-3">
            {formatVND(kpis?.totalRevenueToday ?? 0)}
          </div>
          <div className="mt-2 text-xs text-neutral-600 space-y-0.5">
            <div className="flex justify-between">
              <span>Tiền phòng:</span>
              <span className="font-semibold">{formatVND(kpis?.totalRoomRevenue ?? 0)}</span>
            </div>
            <div className="flex justify-between">
              <span>Dịch vụ & F&B:</span>
              <span className="font-semibold">{formatVND(kpis?.totalServiceRevenue ?? 0)}</span>
            </div>
          </div>
        </div>
      </div>

      {/* Operations Quick Feed Bar */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div className="bg-white p-4.5 rounded-2xl border border-neutral-200 shadow-xs flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center shrink-0">
            <UserCheck className="w-6 h-6" />
          </div>
          <div>
            <div className="text-xs font-semibold text-neutral-500 uppercase">Khách Đến Hôm Nay (Arrivals)</div>
            <div className="text-2xl font-bold text-neutral-900 mt-0.5">
              {kpis?.arrivalsTodayCount ?? 0} <span className="text-xs font-normal text-neutral-500">lượt check-in</span>
            </div>
          </div>
        </div>

        <div className="bg-white p-4.5 rounded-2xl border border-neutral-200 shadow-xs flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-amber-50 text-amber-600 flex items-center justify-center shrink-0">
            <LogOut className="w-6 h-6" />
          </div>
          <div>
            <div className="text-xs font-semibold text-neutral-500 uppercase">Khách Rời Hôm Nay (Departures)</div>
            <div className="text-2xl font-bold text-neutral-900 mt-0.5">
              {kpis?.departuresTodayCount ?? 0} <span className="text-xs font-normal text-neutral-500">lượt trả phòng</span>
            </div>
          </div>
        </div>

        <div className="bg-white p-4.5 rounded-2xl border border-neutral-200 shadow-xs flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center shrink-0">
            <Users className="w-6 h-6" />
          </div>
          <div>
            <div className="text-xs font-semibold text-neutral-500 uppercase">Khách Đang Lưu Trú (In-House)</div>
            <div className="text-2xl font-bold text-neutral-900 mt-0.5">
              {kpis?.inHouseGuestsCount ?? 0} <span className="text-xs font-normal text-neutral-500">khách đang ở</span>
            </div>
          </div>
        </div>
      </div>

      </>}

      {/* Visual Analytics / Trend Charts */}
      {current && <>
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* 7-Day Revenue Trend Chart */}
        <div className="bg-white p-5 rounded-2xl border border-neutral-200 shadow-xs space-y-4">
          <div className="flex justify-between items-center">
            <div>
              <h2 className="text-base font-bold text-neutral-900">Xu hướng Doanh thu 7 ngày qua</h2>
              <p className="text-xs text-neutral-500">Phân rã tiền phòng và dịch vụ minibar/F&B</p>
            </div>
            <div className="flex items-center gap-3 text-xs">
              <span className="flex items-center gap-1 text-red-600 font-medium">
                <span className="w-2.5 h-2.5 bg-red-600 rounded-sm" /> Tiền phòng
              </span>
              <span className="flex items-center gap-1 text-purple-600 font-medium">
                <span className="w-2.5 h-2.5 bg-purple-500 rounded-sm" /> Dịch vụ
              </span>
            </div>
          </div>

          <div className="pt-6 h-64 flex items-end gap-3 justify-between border-b border-neutral-200 pb-2">
            {revenueTrends.map((t) => {
              const heightPct = Math.max(0, Math.round((t.totalRevenue / maxRevenue) * 100));
              const roomPct = t.totalRevenue > 0 ? (t.roomRevenue / t.totalRevenue) * 100 : 100;

              return (
                <div key={t.date} className="flex-1 flex flex-col items-center gap-1 h-full justify-end group">
                  <div className="text-[10px] font-bold text-neutral-600 opacity-0 group-hover:opacity-100 transition-opacity">
                    {formatVND(t.totalRevenue)}
                  </div>
                  <div
                    className="w-full max-w-[40px] rounded-t-lg overflow-hidden flex flex-col justify-end transition-all duration-300 group-hover:opacity-90 shadow-2xs"
                    style={{ height: `${heightPct}%` }}
                  >
                    <div className="bg-purple-500 w-full" style={{ height: `${100 - roomPct}%` }} />
                    <div className="bg-red-600 w-full" style={{ height: `${roomPct}%` }} />
                  </div>
                  <div className="text-[11px] text-neutral-500 font-medium mt-1 truncate max-w-[50px]">
                    {t.date.split('-').slice(1).join('/')}
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* 7-Day Occupancy Trend Chart */}
        <div className="bg-white p-5 rounded-2xl border border-neutral-200 shadow-xs space-y-4">
          <div className="flex justify-between items-center">
            <div>
              <h2 className="text-base font-bold text-neutral-900">Biểu đồ Tỷ lệ Lấp đầy (OCC %) 7 ngày</h2>
              <p className="text-xs text-neutral-500">Mức độ sử dụng buồng phòng trong tuần</p>
            </div>
          </div>

          <div className="pt-6 h-64 flex items-end gap-3 justify-between border-b border-neutral-200 pb-2">
            {occupancyTrends.map((t) => {
              const occ = Math.min(100, Math.max(0, t.occupancyRate));

              return (
                <div key={t.date} className="flex-1 flex flex-col items-center gap-1 h-full justify-end group">
                  <div className="text-[10px] font-bold text-neutral-700 opacity-0 group-hover:opacity-100 transition-opacity">
                    {t.occupancyRate}%
                  </div>
                  <div
                    className="w-full max-w-[36px] bg-neutral-900 rounded-t-lg transition-all duration-300 group-hover:bg-red-600 shadow-2xs"
                    style={{ height: `${occ}%` }}
                  />
                  <div className="text-[11px] text-neutral-500 font-medium mt-1 truncate max-w-[50px]">
                    {t.date.split('-').slice(1).join('/')}
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </div>
      {current.revenue?.length === 0 && <p>Chưa có dữ liệu xu hướng doanh thu trong khoảng ngày đã chọn.</p>}
      {current.occupancy?.length === 0 && <p>Chưa có dữ liệu xu hướng lấp đầy trong khoảng ngày đã chọn.</p>}
      </>}
    </div>
  );
};

export default ManagementDashboard;
