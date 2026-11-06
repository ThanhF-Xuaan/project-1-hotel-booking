import React, { useState, useEffect, useCallback } from 'react';
import {
  Users,
  Search,
  Plus,
  FileSpreadsheet,
  Calendar,
  DollarSign,
  BedDouble,
  RotateCcw,
  Eye,
  ChevronLeft,
  ChevronRight,
  Filter,
  Building2,
  Receipt,
} from 'lucide-react';
import { groupBookingService } from '../services/groupBooking.service';
import { hotelService } from '../../organization/services/hotel.service';
import type {
  GroupContractResponse,
  GroupContractStatus,
  GroupContractCreateRequest,
} from '../types/groupBooking.types';
import { GroupCreateContractModal } from '../components/GroupCreateContractModal';
import { GroupDetailDrawer } from '../components/GroupDetailDrawer';
import { Select } from '../../../core/components/ui/Select';
import { MasterFolioModal } from '../../finance/components/MasterFolioModal';

export const GroupManagementPage: React.FC = () => {
  const [contracts, setContracts] = useState<GroupContractResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);

  // Filters
  const [keyword, setKeyword] = useState('');
  const [statusFilter, setStatusFilter] = useState<GroupContractStatus | 'ALL'>('ALL');
  const [hotelFilter, setHotelFilter] = useState<number>(0);
  const [hotelList, setHotelList] = useState<{ id: number; name: string }[]>([]);
  const [isHotelsLoading, setIsHotelsLoading] = useState(false);
  const [hasLoadedHotels, setHasLoadedHotels] = useState(false);

  // Modals & Drawers
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [selectedContract, setSelectedContract] = useState<GroupContractResponse | null>(null);
  const [isDrawerOpen, setIsDrawerOpen] = useState(false);
  const [selectedMasterFolioContractId, setSelectedMasterFolioContractId] = useState<number | null>(null);

  // Lazy fetch hotel list when user opens the hotel Select dropdown
  const handleOpenHotelSelect = useCallback(async () => {
    if (hasLoadedHotels || isHotelsLoading) return;
    setIsHotelsLoading(true);
    try {
      const res = await hotelService.filter({ page: 0, pageSize: 50 });
      const list = (res.result?.content || []).map((h) => ({
        id: h.id,
        name: h.name || `Khách sạn #${h.id}`,
      }));
      setHotelList(list);
      setHasLoadedHotels(true);
    } catch (err: unknown) {
      console.warn('Lỗi khi tải danh sách khách sạn:', err);
    } finally {
      setIsHotelsLoading(false);
    }
  }, [hasLoadedHotels, isHotelsLoading]);

  const loadData = useCallback(async (targetPage = 0) => {
    setIsLoading(true);
    try {
      const res = await groupBookingService.filter({
        contractNumber: keyword.trim() || undefined,
        groupName: keyword.trim() || undefined,
        status: statusFilter !== 'ALL' ? statusFilter : undefined,
        hotelId: hotelFilter ? Number(hotelFilter) : undefined,
        page: targetPage,
        pageSize: 10,
        sortBy: 'id',
        sortDirection: 'DESC',
      });

      const pageData = res.result;
      setContracts(pageData?.content || []);
      setTotalPages(pageData?.totalPages || 1);
      setTotalElements(pageData?.totalElements || 0);
      setPage(pageData?.pageNumber || 0);
    } catch (err: unknown) {
      console.error('Lỗi khi tải danh sách hợp đồng đoàn từ API:', err);
      setContracts([]);
    } finally {
      setIsLoading(false);
    }
  }, [keyword, statusFilter, hotelFilter]);

  useEffect(() => {
    let ignore = false;
    Promise.resolve().then(() => {
      if (!ignore) {
        loadData(0);
      }
    });
    return () => {
      ignore = true;
    };
  }, [loadData]);

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    loadData(0);
  };

  const handleCreateSubmit = async (dto: GroupContractCreateRequest, staffId?: number) => {
    await groupBookingService.create(dto, staffId);
    alert('Tạo hợp đồng khách đoàn thành công! Khối phòng đã được giữ 3 ngày.');
    await loadData(0);
  };

  const handleRefreshDetail = async () => {
    if (selectedContract) {
      const updated = await groupBookingService.getById(selectedContract.id);
      if (updated.result) {
        setSelectedContract(updated.result);
      }
    }
    await loadData(page);
  };

  // Stats computed from current page/total
  const totalRoomsBlocked = contracts.reduce((acc, c) => acc + (c.totalRooms || 0), 0);
  const totalDepositReceived = contracts.reduce(
    (acc, c) => acc + (Number(c.depositPaidAmount) || 0),
    0
  );

  return (
    <div className="space-y-6">
      {/* 1. Header & Title Block */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold text-neutral-900 tracking-tight flex items-center gap-2.5">
            <span>Quản Lý Khách Đoàn</span>
          </h1>
        </div>

        <div className="flex items-center gap-3">
          <button
            type="button"
            className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-white border border-neutral-300 hover:bg-neutral-100 rounded-lg text-xs font-bold text-neutral-700 shadow-2xs transition-colors cursor-pointer"
            onClick={() => alert('Xuất báo cáo hợp đồng đoàn Excel thành công!')}
          >
            <FileSpreadsheet className="w-3.5 h-3.5 text-green-600" />
            <span>Xuất Excel</span>
          </button>
          <button
            type="button"
            onClick={() => loadData(page)}
            disabled={isLoading}
            className="p-2.5 rounded-xl border border-neutral-300 hover:bg-neutral-100 text-neutral-700 transition-colors shadow-2xs cursor-pointer disabled:opacity-50"
            title="Tải lại dữ liệu"
          >
            <RotateCcw className={`w-5 h-5 ${isLoading ? 'animate-spin text-red-600' : ''}`} />
          </button>
          <button
            type="button"
            onClick={() => setIsCreateOpen(true)}
            className="h-11 px-5 bg-red-600 hover:bg-red-700 text-white rounded-xl font-bold text-xs uppercase tracking-wider transition-colors shadow-xs flex items-center gap-2 cursor-pointer"
          >
            <Plus className="w-4 h-4" />
            Tạo Hợp Đồng
          </button>
        </div>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="bg-white border border-neutral-200 rounded-2xl p-4 shadow-xs flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-red-50 text-red-600 flex items-center justify-center font-bold">
            <Users className="w-6 h-6" />
          </div>
          <div>
            <span className="text-xs font-bold text-neutral-500 uppercase">Tổng số đoàn</span>
            <div className="text-xl font-black text-neutral-900 mt-0.5">
              {totalElements}
            </div>
          </div>
        </div>

        <div className="bg-white border border-neutral-200 rounded-2xl p-4 shadow-xs flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center font-bold">
            <BedDouble className="w-6 h-6" />
          </div>
          <div>
            <span className="text-xs font-bold text-neutral-500 uppercase">Khối phòng đang block</span>
            <div className="text-xl font-black text-neutral-900 mt-0.5">
              {totalRoomsBlocked}
            </div>
          </div>
        </div>

        <div className="bg-white border border-neutral-200 rounded-2xl p-4 shadow-xs flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-green-50 text-green-600 flex items-center justify-center font-bold">
            <DollarSign className="w-6 h-6" />
          </div>
          <div>
            <span className="text-xs font-bold text-neutral-500 uppercase">Cọc thực nhận</span>
            <div className="text-xl font-black text-green-600 mt-0.5">
              {totalDepositReceived.toLocaleString('vi-VN')} đ
            </div>
          </div>
        </div>
      </div>

      {/* 2. Standard 3-Row Layout: Row 1 Filter Bar */}
      <div className="bg-white border border-neutral-200 rounded-2xl p-4 shadow-xs">
        <form onSubmit={handleSearchSubmit} className="grid grid-cols-1 md:grid-cols-12 gap-3 items-center">
          <div className="md:col-span-5 relative">
            <Search className="w-4 h-4 absolute left-3.5 top-1/2 -translate-y-1/2 text-neutral-400" />
            <input
              type="text"
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              placeholder="Tìm theo mã hợp đồng, tên đoàn, công ty..."
              className="w-full h-11 pl-10 pr-3 bg-neutral-50 border border-neutral-300 rounded-xl text-xs font-medium focus:ring-2 focus:ring-red-500 focus:bg-white focus:outline-none"
            />
          </div>

          <div className="md:col-span-3">
            <Select<GroupContractStatus | 'ALL'>
              value={statusFilter}
              onChange={(val) => setStatusFilter(val)}
              options={[
                { value: 'ALL', label: 'Tất cả trạng thái' },
                { value: 'TENTATIVE', label: 'Giữ chỗ', badge: 'Chờ cọc' },
                { value: 'DEPOSIT_PAID', label: 'Đã cọc', badge: 'Đã cọc' },
                { value: 'ROOMING_LIST_ATTACHED', label: 'Đã nộp danh sách' },
                { value: 'IN_HOUSE', label: 'Đang lưu trú' },
                { value: 'SETTLED', label: 'Đã quyết toán' },
                { value: 'CANCELLED', label: 'Đã hủy' },
              ]}
              prefixIcon={<Filter className="w-4 h-4 text-neutral-400" />}
            />
          </div>

          <div className="md:col-span-3">
            <Select<number>
              value={hotelFilter}
              onChange={(val) => setHotelFilter(val)}
              onOpen={handleOpenHotelSelect}
              isLoading={isHotelsLoading}
              options={[
                { value: 0, label: 'Tất cả cơ sở' },
                ...hotelList.map((h) => ({
                  value: h.id,
                  label: h.name,
                })),
              ]}
              prefixIcon={<Building2 className="w-4 h-4 text-neutral-400" />}
            />
          </div>

          <div className="md:col-span-1">
            <button
              type="submit"
              className="w-full h-11 bg-neutral-900 hover:bg-black text-white rounded-xl text-xs font-bold uppercase transition-colors cursor-pointer"
            >
              Lọc
            </button>
          </div>
        </form>
      </div>

      {/* 3. Row 2 & 3: Table and Action Toolbar */}
      <div className="bg-white border border-neutral-200 rounded-2xl shadow-xs overflow-hidden">

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs border-collapse">
            <thead className="bg-neutral-50 text-neutral-600 font-black border-b border-neutral-200 uppercase tracking-wider text-[11px]">
              <tr>
                <th className="p-3.5">Mã Hợp Đồng & Đoàn</th>
                <th className="p-3.5">Lịch Lưu Trú</th>
                <th className="p-3.5 text-center">Hạn Cọc / Cut-off</th>
                <th className="p-3.5 text-center">Phòng Block</th>
                <th className="p-3.5 text-right">Tổng Tiền & Cọc (50%)</th>
                <th className="p-3.5 text-center">Trạng Thái</th>
                <th className="p-3.5 text-right">Thao Tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-neutral-200">
              {isLoading ? (
                <tr>
                  <td colSpan={7} className="p-12 text-center text-neutral-400 font-medium">
                    Đang gọi API Backend tải danh sách hợp đồng đoàn...
                  </td>
                </tr>
              ) : contracts.length === 0 ? (
                <tr>
                  <td colSpan={7} className="p-12 text-center text-neutral-400 font-medium">
                    Chưa có hợp đồng đoàn nào được tạo trên hệ thống.
                  </td>
                </tr>
              ) : (
                contracts.map((c) => {
                  const depositPercent =
                    c.depositAmount > 0
                      ? Math.min(100, Math.round((c.depositPaidAmount / c.depositAmount) * 100))
                      : 0;
                  const isCutoffPassed = c.cutoffDate ? new Date(c.cutoffDate) < new Date() : false;

                  return (
                    <tr key={c.id} className="hover:bg-neutral-50/70 transition-colors">
                      {/* Mã HĐ & Tên đoàn */}
                      <td className="p-3.5">
                        <span className="font-mono font-bold text-red-600 text-[11px] block">
                          {c.contractNumber}
                        </span>
                        <span className="font-bold text-neutral-900 text-sm block mt-0.5">
                          {c.groupName}
                        </span>
                        <span className="text-[11px] text-neutral-500 block">
                          {c.companyName || 'Khách đoàn B2B'} • MST: {c.companyTaxCode}
                        </span>
                      </td>

                      {/* Lịch lưu trú */}
                      <td className="p-3.5 text-neutral-700">
                        <div className="font-semibold flex items-center gap-1.5">
                          <Calendar className="w-3.5 h-3.5 text-neutral-400 shrink-0" />
                          <span>{c.checkInDate}</span>
                          <span className="text-neutral-400">➔</span>
                          <span>{c.checkOutDate}</span>
                        </div>
                        <span className="text-[11px] text-neutral-400 block mt-0.5">
                          {c.hotelName || `Khách sạn #${c.hotelId}`}
                        </span>
                      </td>

                      {/* Hạn cọc / Cut-off */}
                      <td className="p-3.5 text-center">
                        <span
                          className={`inline-block font-mono font-bold text-xs px-2.5 py-1 rounded-lg ${isCutoffPassed && c.status === 'TENTATIVE'
                            ? 'bg-red-100 text-red-700 border border-red-300'
                            : 'bg-neutral-100 text-neutral-800'
                            }`}
                        >
                          {c.depositDueDate
                            ? new Date(c.depositDueDate).toLocaleDateString('vi-VN')
                            : c.cutoffDate || '3 ngày'}
                        </span>
                        {isCutoffPassed && c.status === 'TENTATIVE' && (
                          <span className="text-[10px] font-bold text-red-600 block mt-1">
                            Quá hạn giữ chỗ
                          </span>
                        )}
                      </td>

                      {/* Room Block */}
                      <td className="p-3.5 text-center">
                        <span className="font-black text-neutral-900 text-sm">
                          {c.totalRooms} phòng
                        </span>
                        <span className="block text-[11px] text-neutral-500">
                          {c.totalGuests} khách
                        </span>
                      </td>

                      {/* Cọc 50% & Tổng tiền */}
                      <td className="p-3.5 text-right">
                        <div className="font-bold text-neutral-900">
                          {Number(c.totalContractValue).toLocaleString('vi-VN')} đ
                        </div>
                        <div className="text-[11px] text-neutral-500 flex items-center justify-end gap-1 mt-0.5">
                          <span>Cọc:</span>
                          <span
                            className={`font-bold ${c.depositPaidAmount >= c.depositAmount
                              ? 'text-green-600'
                              : 'text-red-600'
                              }`}
                          >
                            {Number(c.depositPaidAmount).toLocaleString('vi-VN')} đ ({depositPercent}%)
                          </span>
                        </div>
                      </td>

                      {/* Trạng thái */}
                      <td className="p-3.5 text-center">
                        <span
                          className={`inline-block px-2.5 py-1 rounded-lg text-[11px] font-bold uppercase ${c.status === 'DEPOSIT_PAID' || c.status === 'ROOMING_LIST_ATTACHED'
                            ? 'bg-green-100 text-green-700'
                            : c.status === 'TENTATIVE'
                              ? 'bg-amber-100 text-amber-800'
                              : c.status === 'IN_HOUSE'
                                ? 'bg-blue-100 text-blue-700'
                                : 'bg-neutral-200 text-neutral-700'
                            }`}
                        >
                          {c.status === 'DEPOSIT_PAID'
                            ? 'Đã cọc 50%'
                            : c.status === 'TENTATIVE'
                              ? 'Giữ chỗ 3 ngày'
                              : c.status === 'ROOMING_LIST_ATTACHED'
                                ? 'Đã nộp danh sách'
                                : c.status === 'IN_HOUSE'
                                  ? 'Đang ở'
                                  : c.status}
                        </span>
                      </td>

                      {/* Thao tác */}
                      <td className="p-3.5 text-right">
                        <div className="flex items-center justify-end gap-2">
                          <button
                            type="button"
                            onClick={() => setSelectedMasterFolioContractId(c.id)}
                            title="Mở Sổ Cái Master Folio & Quyết toán"
                            className="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-lg bg-red-50 hover:bg-red-100 text-red-700 font-bold text-xs transition-colors cursor-pointer"
                          >
                            <Receipt className="w-3.5 h-3.5 text-red-600" />
                            <span>Master Folio</span>
                          </button>
                          <button
                            type="button"
                            onClick={() => {
                              setSelectedContract(c);
                              setIsDrawerOpen(true);
                            }}
                            className="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg bg-neutral-100 hover:bg-neutral-200 text-neutral-800 font-bold text-xs transition-colors cursor-pointer"
                          >
                            <Eye className="w-3.5 h-3.5 text-neutral-600" />
                            <span>Chi tiết</span>
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination Bar */}
        {totalPages > 1 && (
          <div className="px-5 py-3 border-t border-neutral-200 flex items-center justify-between text-xs">
            <span className="text-neutral-500 font-medium">
              Trang {page + 1} / {totalPages} (Tổng cộng {totalElements} hợp đồng)
            </span>
            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={() => loadData(Math.max(0, page - 1))}
                disabled={page === 0}
                className="p-1.5 rounded-lg border border-neutral-300 hover:bg-neutral-100 disabled:opacity-40 cursor-pointer"
              >
                <ChevronLeft className="w-4 h-4" />
              </button>
              <button
                type="button"
                onClick={() => loadData(Math.min(totalPages - 1, page + 1))}
                disabled={page >= totalPages - 1}
                className="p-1.5 rounded-lg border border-neutral-300 hover:bg-neutral-100 disabled:opacity-40 cursor-pointer"
              >
                <ChevronRight className="w-4 h-4" />
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Modals & Drawers */}
      <GroupCreateContractModal
        isOpen={isCreateOpen}
        onClose={() => setIsCreateOpen(false)}
        onSubmit={handleCreateSubmit}
        hotelList={hotelList}
      />

      <GroupDetailDrawer
        contract={selectedContract}
        isOpen={isDrawerOpen}
        onClose={() => setIsDrawerOpen(false)}
        onRefresh={handleRefreshDetail}
      />

      {/* Sổ Cái Master Folio Modal */}
      {selectedMasterFolioContractId !== null && (
        <MasterFolioModal
          isOpen={true}
          onClose={() => setSelectedMasterFolioContractId(null)}
          contractId={selectedMasterFolioContractId}
        />
      )}
    </div>
  );
};
