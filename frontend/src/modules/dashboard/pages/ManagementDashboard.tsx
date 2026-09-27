import React, { useState, useEffect, useCallback } from 'react';
import {
  LayoutDashboard,
  TrendingUp,
  Percent,
  DollarSign,
  Sparkles,
  RefreshCw,
  ArrowUpRight,
  UserCheck,
  LogOut,
  Users,
} from 'lucide-react';
import Button from '../../../core/components/ui/Button';
import Input from '../../../core/components/ui/Input';
import { dashboardService } from '../services/dashboard.service';
import type {
  DashboardKpiResponse,
  OccupancyTrendDto,
  RevenueTrendDto,
} from '../types/dashboard.types';

export const ManagementDashboard: React.FC = () => {
  const [hotelId, setHotelId] = useState<number>(1);
  const [targetDate, setTargetDate] = useState<string>(
    new Date().toISOString().split('T')[0]
  );
  const [kpis, setKpis] = useState<DashboardKpiResponse | null>(null);
  const [revenueTrends, setRevenueTrends] = useState<RevenueTrendDto[]>([]);
  const [occupancyTrends, setOccupancyTrends] = useState<OccupancyTrendDto[]>([]);
  const [isLoading, setIsLoading] = useState(false);

  const fetchDashboardData = useCallback(async () => {
    setIsLoading(true);
    try {
      const [kpiRes, revRes, occRes] = await Promise.all([
        dashboardService.getExecutiveKpis(hotelId, targetDate),
        dashboardService.getRevenueTrends(hotelId),
        dashboardService.getOccupancyTrends(hotelId),
      ]);
      setKpis(kpiRes);
      setRevenueTrends(revRes || []);
      setOccupancyTrends(occRes || []);
    } catch (err) {
      console.error('Failed to load dashboard data:', err);
    } finally {
      setIsLoading(false);
    }
  }, [hotelId, targetDate]);

  useEffect(() => {
    fetchDashboardData();
  }, [fetchDashboardData]);

  const formatVND = (amount: number) =>
    new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount || 0);

  const maxRevenue = Math.max(...revenueTrends.map((r) => r.totalRevenue), 1);

  return (
    <div className="space-y-6">
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
            <select
              value={hotelId}
              onChange={(e) => setHotelId(Number(e.target.value))}
              className="w-full h-11 px-3 border border-neutral-300 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-red-600 bg-white"
            >
              <option value={1}>Khách sạn Grand Hà Nội</option>
              <option value={2}>Khách sạn Premier Đà Nẵng</option>
              <option value={3}>Khách sạn Boutique Sài Gòn</option>
            </select>
          </div>

          <div className="w-44">
            <Input
              type="date"
              value={targetDate}
              onChange={(e) => setTargetDate(e.target.value)}
              className="h-11"
            />
          </div>

          <Button
            variant="secondary"
            onClick={fetchDashboardData}
            disabled={isLoading}
            className="h-11 flex items-center gap-1.5 text-xs"
          >
            <RefreshCw className={`w-4 h-4 ${isLoading ? 'animate-spin text-red-600' : ''}`} />
            {isLoading ? 'Đang tải...' : 'Làm mới'}
          </Button>
        </div>
      </div>

      {/* 4 Core KPI Metric Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* OCC */}
        <div className="bg-white p-5 rounded-2xl border border-neutral-200 shadow-xs hover:border-red-300 transition-all">
          <div className="flex justify-between items-start">
            <div className="text-xs font-bold text-neutral-500 uppercase tracking-wider flex items-center gap-1">
              <Percent className="w-4 h-4 text-red-600" />
              Tỷ Lệ Lấp Đầy (OCC)
            </div>
            <span className="text-xs font-semibold px-2 py-0.5 bg-emerald-50 text-emerald-700 rounded-full flex items-center gap-0.5">
              <ArrowUpRight className="w-3 h-3" />
              Tốt
            </span>
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
              Hôm nay
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

      {/* Visual Analytics / Trend Charts */}
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
              const heightPct = Math.max(10, Math.round((t.totalRevenue / maxRevenue) * 100));
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
              const occ = Math.min(100, Math.max(8, t.occupancyRate));

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
    </div>
  );
};

export default ManagementDashboard;
