import React, { useState, useEffect, useCallback } from 'react';
import {
  ShieldCheck,
  Search,
  RefreshCw,
  Plus,
  Trash2,
  Edit,
  Clock,
  Building,
  CheckCircle2,
  XCircle,
} from 'lucide-react';
import Button from '../../../core/components/ui/Button';
import Input from '../../../core/components/ui/Input';
import { ConfirmModal } from '../../../core/components/ui/Modal';
import Pagination from '../../../core/components/ui/Pagination';
import policyService from '../services/policy.service';
import CancellationPolicyModal from '../components/CancellationPolicyModal';
import NoShowPolicyModal from '../components/NoShowPolicyModal';
import PropertyOverrideModal from '../components/PropertyOverrideModal';
import type {
  CancellationPolicyResponse,
  NoShowPolicyResponse,
  PropertyPolicyOverrideResponse,
  BookingType,
  CancellationFeeType,
} from '../types/policy.types';

type ActiveTab = 'CANCELLATION' | 'NOSHOW' | 'OVERRIDES';

export const PolicyManagementPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState<ActiveTab>('CANCELLATION');

  // Search & Filter State
  const [keyword, setKeyword] = useState('');
  const [bookingTypeFilter, setBookingTypeFilter] = useState<BookingType | ''>('');
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'ACTIVE' | 'INACTIVE'>('ALL');

  // Pagination
  const [page, setPage] = useState(0);
  const [pageSize] = useState(10);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [isLoading, setIsLoading] = useState(false);

  // Data lists
  const [cancellationPolicies, setCancellationPolicies] = useState<CancellationPolicyResponse[]>([]);
  const [noShowPolicies, setNoShowPolicies] = useState<NoShowPolicyResponse[]>([]);
  const [overrides, setOverrides] = useState<PropertyPolicyOverrideResponse[]>([]);

  // Selection for Batch Actions
  const [selectedIds, setSelectedIds] = useState<number[]>([]);

  // Modals state
  const [isCancelModalOpen, setIsCancelModalOpen] = useState(false);
  const [selectedCancelPolicy, setSelectedCancelPolicy] = useState<CancellationPolicyResponse | null>(null);

  const [isNoShowModalOpen, setIsNoShowModalOpen] = useState(false);
  const [selectedNoShowPolicy, setSelectedNoShowPolicy] = useState<NoShowPolicyResponse | null>(null);

  const [isOverrideModalOpen, setIsOverrideModalOpen] = useState(false);
  const [selectedOverride, setSelectedOverride] = useState<PropertyPolicyOverrideResponse | null>(null);

  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [deleteTargetIds, setDeleteTargetIds] = useState<number[]>([]);
  const [isDeleting, setIsDeleting] = useState(false);

  // Load Data
  const loadData = useCallback(async () => {
    setIsLoading(true);
    setSelectedIds([]);

    const isActiveParam = statusFilter === 'ALL' ? undefined : statusFilter === 'ACTIVE';

    try {
      if (activeTab === 'CANCELLATION') {
        const res = await policyService.filterCancellationPolicies({
          keyword: keyword.trim() || undefined,
          bookingType: bookingTypeFilter || undefined,
          isActive: isActiveParam,
          page,
          pageSize,
        });
        if (res.result) {
          setCancellationPolicies(res.result.content || []);
          setTotalElements(res.result.totalElements);
          setTotalPages(res.result.totalPages);
        }
      } else if (activeTab === 'NOSHOW') {
        const res = await policyService.filterNoShowPolicies({
          keyword: keyword.trim() || undefined,
          bookingType: bookingTypeFilter || undefined,
          isActive: isActiveParam,
          page,
          pageSize,
        });
        if (res.result) {
          setNoShowPolicies(res.result.content || []);
          setTotalElements(res.result.totalElements);
          setTotalPages(res.result.totalPages);
        }
      } else {
        const res = await policyService.filterPropertyOverrides({
          isActive: isActiveParam,
          page,
          pageSize,
        });
        if (res.result) {
          setOverrides(res.result.content || []);
          setTotalElements(res.result.totalElements);
          setTotalPages(res.result.totalPages);
        }
      }
    } catch (err) {
      console.error('Lỗi tải dữ liệu chính sách', err);
    } finally {
      setIsLoading(false);
    }
  }, [activeTab, keyword, bookingTypeFilter, statusFilter, page, pageSize]);

  useEffect(() => {
    const timer = setTimeout(() => {
      void loadData();
    }, 0);
    return () => clearTimeout(timer);
  }, [loadData]);

  const handleTabChange = (tab: ActiveTab) => {
    setActiveTab(tab);
    setPage(0);
    setKeyword('');
    setBookingTypeFilter('');
    setStatusFilter('ALL');
  };

  // Checkbox Selection
  const toggleSelectAll = (checked: boolean) => {
    if (checked) {
      if (activeTab === 'CANCELLATION') {
        setSelectedIds(cancellationPolicies.map((p) => p.id));
      } else if (activeTab === 'NOSHOW') {
        setSelectedIds(noShowPolicies.map((p) => p.id));
      } else {
        setSelectedIds(overrides.map((p) => p.id));
      }
    } else {
      setSelectedIds([]);
    }
  };

  const toggleSelectOne = (id: number) => {
    if (selectedIds.includes(id)) {
      setSelectedIds(selectedIds.filter((item) => item !== id));
    } else {
      setSelectedIds([...selectedIds, id]);
    }
  };

  // Open Delete Confirm
  const handleOpenDelete = (ids: number[]) => {
    setDeleteTargetIds(ids);
    setIsDeleteModalOpen(true);
  };

  const handleConfirmDelete = async () => {
    if (deleteTargetIds.length === 0) return;
    try {
      setIsDeleting(true);
      if (activeTab === 'CANCELLATION') {
        await policyService.deleteCancellationPolicies(deleteTargetIds);
      } else if (activeTab === 'NOSHOW') {
        await policyService.deleteNoShowPolicies(deleteTargetIds);
      } else {
        await policyService.deletePropertyOverrides(deleteTargetIds);
      }
      setIsDeleteModalOpen(false);
      loadData();
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message || 'Không thể xóa bản ghi do đã có dữ liệu phụ thuộc';
      alert(msg);
    } finally {
      setIsDeleting(false);
    }
  };

  const renderFeeTypeBadge = (feeType: CancellationFeeType) => {
    switch (feeType) {
      case 'TIERED':
        return <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-blue-50 text-blue-700 border border-blue-200">Bậc Thang (TIERED)</span>;
      case 'FULL_DEPOSIT':
        return <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-purple-50 text-purple-700 border border-purple-200">Tiền Cọc (DEPOSIT)</span>;
      case 'PERCENTAGE':
        return <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-amber-50 text-amber-700 border border-amber-200">Tỷ Lệ % Cố Định</span>;
      case 'FIXED':
        return <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-neutral-100 text-neutral-800 border border-neutral-300">Cố Định Số Tiền</span>;
      default:
        return <span>{feeType}</span>;
    }
  };

  return (
    <div className="space-y-6">
      {/* Page Title */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 bg-white p-6 rounded-2xl border border-neutral-200 shadow-xs">
        <div>
          <div className="flex items-center gap-2 text-red-600 mb-1">
            <ShieldCheck className="w-6 h-6" />
            <h1 className="text-xl md:text-2xl font-bold text-neutral-900">
              Quản lý Chính sách Hủy & No-Show
            </h1>
          </div>
          <p className="text-sm text-neutral-500">
            Cơ chế phân cấp: Chuỗi mặc định (Chain Default) ➔ Khách sạn ghi đè (Property Override) ➔ Snapshot bất biến khi đặt phòng.
          </p>
        </div>

        {/* Tab Navigation */}
        <div className="flex bg-neutral-100 p-1.5 rounded-xl border border-neutral-200">
          <button
            onClick={() => handleTabChange('CANCELLATION')}
            className={`px-4 py-2 text-sm font-semibold rounded-lg transition-all cursor-pointer ${
              activeTab === 'CANCELLATION'
                ? 'bg-white text-red-600 shadow-xs'
                : 'text-neutral-600 hover:text-neutral-900'
            }`}
          >
            Chính sách Hủy phòng
          </button>
          <button
            onClick={() => handleTabChange('NOSHOW')}
            className={`px-4 py-2 text-sm font-semibold rounded-lg transition-all cursor-pointer ${
              activeTab === 'NOSHOW'
                ? 'bg-white text-red-600 shadow-xs'
                : 'text-neutral-600 hover:text-neutral-900'
            }`}
          >
            Chính sách Vắng mặt (No-Show)
          </button>
          <button
            onClick={() => handleTabChange('OVERRIDES')}
            className={`px-4 py-2 text-sm font-semibold rounded-lg transition-all cursor-pointer ${
              activeTab === 'OVERRIDES'
                ? 'bg-white text-red-600 shadow-xs'
                : 'text-neutral-600 hover:text-neutral-900'
            }`}
          >
            Ghi đè Cơ sở (Overrides)
          </button>
        </div>
      </div>

      {/* Row 1: Search & Filter Bar */}
      <div className="bg-white p-4 rounded-2xl border border-neutral-200 shadow-xs flex flex-wrap items-center justify-between gap-4">
        <div className="flex flex-wrap items-center gap-3 flex-1">
          {activeTab !== 'OVERRIDES' && (
            <div className="relative w-full sm:w-64">
              <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-neutral-400" />
              <Input
                placeholder="Tìm mã hoặc tên chính sách..."
                className="pl-9"
                value={keyword}
                onChange={(e) => setKeyword(e.target.value)}
              />
            </div>
          )}

          {activeTab !== 'OVERRIDES' && (
            <select
              value={bookingTypeFilter}
              onChange={(e) => setBookingTypeFilter(e.target.value as BookingType)}
              className="h-10 md:h-12 px-3 border border-neutral-300 rounded-xl bg-white text-sm focus:ring-2 focus:ring-red-600 focus:outline-none"
            >
              <option value="">Tất cả đối tượng</option>
              <option value="FIT">Khách lẻ (FIT)</option>
              <option value="GIT">Khách đoàn (GIT)</option>
              <option value="CORPORATE">Doanh nghiệp (CORPORATE)</option>
              <option value="ALL">Toàn bộ khách (ALL)</option>
            </select>
          )}

          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value as 'ALL' | 'ACTIVE' | 'INACTIVE')}
            className="h-10 md:h-12 px-3 border border-neutral-300 rounded-xl bg-white text-sm focus:ring-2 focus:ring-red-600 focus:outline-none"
          >
            <option value="ALL">Tất cả trạng thái</option>
            <option value="ACTIVE">Đang kích hoạt</option>
            <option value="INACTIVE">Ngưng áp dụng</option>
          </select>
        </div>

        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            leftIcon={<RefreshCw className={`w-4 h-4 ${isLoading ? 'animate-spin' : ''}`} />}
            onClick={() => loadData()}
          >
            Làm mới
          </Button>
        </div>
      </div>

      {/* Row 2: Action Toolbar */}
      <div className="flex items-center justify-between bg-neutral-50 p-3 rounded-xl border border-neutral-200">
        <div className="flex items-center gap-2">
          <Button
            variant="primary"
            leftIcon={<Plus className="w-4 h-4" />}
            onClick={() => {
              if (activeTab === 'CANCELLATION') {
                setSelectedCancelPolicy(null);
                setIsCancelModalOpen(true);
              } else if (activeTab === 'NOSHOW') {
                setSelectedNoShowPolicy(null);
                setIsNoShowModalOpen(true);
              } else {
                setSelectedOverride(null);
                setIsOverrideModalOpen(true);
              }
            }}
          >
            {activeTab === 'CANCELLATION' && 'Thêm Chính Sách Hủy'}
            {activeTab === 'NOSHOW' && 'Thêm Chính Sách No-Show'}
            {activeTab === 'OVERRIDES' && 'Thêm Ghi Đè Khách Sạn'}
          </Button>

          {selectedIds.length > 0 && (
            <Button
              variant="secondary"
              className="text-red-600 border-red-200 hover:bg-red-50"
              leftIcon={<Trash2 className="w-4 h-4" />}
              onClick={() => handleOpenDelete(selectedIds)}
            >
              Xóa ({selectedIds.length}) mục đã chọn
            </Button>
          )}
        </div>

        <div className="text-xs text-neutral-500">
          Tổng cộng: <strong className="text-neutral-800">{totalElements}</strong> bản ghi
        </div>
      </div>

      {/* Row 3: Data Table */}
      <div className="bg-white rounded-2xl border border-neutral-200 shadow-xs overflow-hidden">
        {activeTab === 'CANCELLATION' && (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="bg-neutral-100 text-neutral-700 font-semibold border-b border-neutral-200">
                <tr>
                  <th className="p-3 w-10 text-center">
                    <input
                      type="checkbox"
                      checked={cancellationPolicies.length > 0 && selectedIds.length === cancellationPolicies.length}
                      onChange={(e) => toggleSelectAll(e.target.checked)}
                      className="w-4 h-4 text-red-600 rounded border-neutral-300"
                    />
                  </th>
                  <th className="p-3">Mã Chính Sách</th>
                  <th className="p-3">Tên & Mô Tả</th>
                  <th className="p-3">Đối Tượng</th>
                  <th className="p-3">Cơ Chế Phạt</th>
                  <th className="p-3">Miễn Phí Hủy</th>
                  <th className="p-3">Bậc Phạt (Tiers)</th>
                  <th className="p-3">Trạng Thái</th>
                  <th className="p-3 text-right">Thao Tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-neutral-200">
                {cancellationPolicies.length === 0 ? (
                  <tr>
                    <td colSpan={9} className="p-8 text-center text-neutral-400">
                      Không có chính sách hủy phòng nào phù hợp
                    </td>
                  </tr>
                ) : (
                  cancellationPolicies.map((p) => (
                    <tr key={p.id} className="hover:bg-neutral-50/70 transition-colors">
                      <td className="p-3 text-center">
                        <input
                          type="checkbox"
                          checked={selectedIds.includes(p.id)}
                          onChange={() => toggleSelectOne(p.id)}
                          className="w-4 h-4 text-red-600 rounded border-neutral-300"
                        />
                      </td>
                      <td className="p-3 font-mono font-bold text-neutral-900">{p.policyCode}</td>
                      <td className="p-3">
                        <div className="font-semibold text-neutral-800">{p.name}</div>
                        {p.description && <div className="text-xs text-neutral-500 line-clamp-1">{p.description}</div>}
                      </td>
                      <td className="p-3">
                        <span className="px-2 py-0.5 rounded-full text-xs font-semibold bg-neutral-100 text-neutral-700">
                          {p.bookingType}
                        </span>
                      </td>
                      <td className="p-3">{renderFeeTypeBadge(p.cancellationFeeType)}</td>
                      <td className="p-3 font-semibold text-neutral-700">{p.freeCancellationBeforeDays} ngày</td>
                      <td className="p-3">
                        {p.tiers && p.tiers.length > 0 ? (
                          <div className="text-xs space-y-0.5">
                            {p.tiers.map((t, idx) => (
                              <div key={idx} className="text-neutral-600">
                                ≥{t.minDays} ngày: <strong className="text-red-600">{t.penaltyPercent}%</strong>
                              </div>
                            ))}
                          </div>
                        ) : (
                          <span className="text-xs text-neutral-400">Không dùng tiers</span>
                        )}
                      </td>
                      <td className="p-3">
                        {p.isActive ? (
                          <span className="inline-flex items-center gap-1 text-xs text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-full border border-emerald-200">
                            <CheckCircle2 className="w-3.5 h-3.5" /> Áp dụng
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1 text-xs text-neutral-500 bg-neutral-100 px-2 py-0.5 rounded-full">
                            <XCircle className="w-3.5 h-3.5" /> Ngưng
                          </span>
                        )}
                      </td>
                      <td className="p-3 text-right">
                        <div className="inline-flex items-center gap-1">
                          <button
                            onClick={() => {
                              setSelectedCancelPolicy(p);
                              setIsCancelModalOpen(true);
                            }}
                            className="p-1.5 text-neutral-600 hover:text-red-600 hover:bg-neutral-100 rounded-lg cursor-pointer"
                            title="Sửa chính sách"
                          >
                            <Edit className="w-4 h-4" />
                          </button>
                          <button
                            onClick={() => handleOpenDelete([p.id])}
                            className="p-1.5 text-neutral-400 hover:text-red-600 hover:bg-red-50 rounded-lg cursor-pointer"
                            title="Xóa chính sách"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        )}

        {activeTab === 'NOSHOW' && (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="bg-neutral-100 text-neutral-700 font-semibold border-b border-neutral-200">
                <tr>
                  <th className="p-3 w-10 text-center">
                    <input
                      type="checkbox"
                      checked={noShowPolicies.length > 0 && selectedIds.length === noShowPolicies.length}
                      onChange={(e) => toggleSelectAll(e.target.checked)}
                      className="w-4 h-4 text-red-600 rounded border-neutral-300"
                    />
                  </th>
                  <th className="p-3">Mã Chính Sách</th>
                  <th className="p-3">Tên & Mô Tả</th>
                  <th className="p-3">Đối Tượng</th>
                  <th className="p-3">Giờ Hủy Phòng</th>
                  <th className="p-3">Hình Thức Phạt</th>
                  <th className="p-3">Mức Phạt</th>
                  <th className="p-3">Trạng Thái</th>
                  <th className="p-3 text-right">Thao Tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-neutral-200">
                {noShowPolicies.length === 0 ? (
                  <tr>
                    <td colSpan={9} className="p-8 text-center text-neutral-400">
                      Không có chính sách No-Show nào phù hợp
                    </td>
                  </tr>
                ) : (
                  noShowPolicies.map((p) => (
                    <tr key={p.id} className="hover:bg-neutral-50/70 transition-colors">
                      <td className="p-3 text-center">
                        <input
                          type="checkbox"
                          checked={selectedIds.includes(p.id)}
                          onChange={() => toggleSelectOne(p.id)}
                          className="w-4 h-4 text-red-600 rounded border-neutral-300"
                        />
                      </td>
                      <td className="p-3 font-mono font-bold text-neutral-900">{p.policyCode}</td>
                      <td className="p-3">
                        <div className="font-semibold text-neutral-800">{p.name}</div>
                        {p.description && <div className="text-xs text-neutral-500 line-clamp-1">{p.description}</div>}
                      </td>
                      <td className="p-3">
                        <span className="px-2 py-0.5 rounded-full text-xs font-semibold bg-neutral-100 text-neutral-700">
                          {p.bookingType}
                        </span>
                      </td>
                      <td className="p-3 font-semibold text-neutral-700 flex items-center gap-1.5 pt-4">
                        <Clock className="w-4 h-4 text-neutral-400" />
                        {p.releaseRoomTime ? p.releaseRoomTime.substring(0, 5) : '23:59'}
                      </td>
                      <td className="p-3">
                        <span className="px-2 py-0.5 rounded-full text-xs font-semibold bg-blue-50 text-blue-700 border border-blue-200">
                          {p.penaltyType}
                        </span>
                      </td>
                      <td className="p-3 font-bold text-red-600">
                        {p.penaltyType === 'FIXED_AMOUNT'
                          ? `${Number(p.penaltyValue).toLocaleString()} đ`
                          : `${p.penaltyValue}%`}
                      </td>
                      <td className="p-3">
                        {p.isActive ? (
                          <span className="inline-flex items-center gap-1 text-xs text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-full border border-emerald-200">
                            <CheckCircle2 className="w-3.5 h-3.5" /> Áp dụng
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1 text-xs text-neutral-500 bg-neutral-100 px-2 py-0.5 rounded-full">
                            <XCircle className="w-3.5 h-3.5" /> Ngưng
                          </span>
                        )}
                      </td>
                      <td className="p-3 text-right">
                        <div className="inline-flex items-center gap-1">
                          <button
                            onClick={() => {
                              setSelectedNoShowPolicy(p);
                              setIsNoShowModalOpen(true);
                            }}
                            className="p-1.5 text-neutral-600 hover:text-red-600 hover:bg-neutral-100 rounded-lg cursor-pointer"
                            title="Sửa chính sách"
                          >
                            <Edit className="w-4 h-4" />
                          </button>
                          <button
                            onClick={() => handleOpenDelete([p.id])}
                            className="p-1.5 text-neutral-400 hover:text-red-600 hover:bg-red-50 rounded-lg cursor-pointer"
                            title="Xóa chính sách"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        )}

        {activeTab === 'OVERRIDES' && (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="bg-neutral-100 text-neutral-700 font-semibold border-b border-neutral-200">
                <tr>
                  <th className="p-3 w-10 text-center">
                    <input
                      type="checkbox"
                      checked={overrides.length > 0 && selectedIds.length === overrides.length}
                      onChange={(e) => toggleSelectAll(e.target.checked)}
                      className="w-4 h-4 text-red-600 rounded border-neutral-300"
                    />
                  </th>
                  <th className="p-3">Khách Sạn Cơ Sở</th>
                  <th className="p-3">Hạng Phòng Ghi Đè</th>
                  <th className="p-3">Chính Sách Hủy Gán</th>
                  <th className="p-3">Chính Sách No-Show Gán</th>
                  <th className="p-3">Thông Số Ghi Đè</th>
                  <th className="p-3">Lý Do Ghi Đè</th>
                  <th className="p-3">Trạng Thái</th>
                  <th className="p-3 text-right">Thao Tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-neutral-200">
                {overrides.length === 0 ? (
                  <tr>
                    <td colSpan={9} className="p-8 text-center text-neutral-400">
                      Chưa có cấu hình ghi đè nào tại cấp khách sạn cơ sở
                    </td>
                  </tr>
                ) : (
                  overrides.map((o) => (
                    <tr key={o.id} className="hover:bg-neutral-50/70 transition-colors">
                      <td className="p-3 text-center">
                        <input
                          type="checkbox"
                          checked={selectedIds.includes(o.id)}
                          onChange={() => toggleSelectOne(o.id)}
                          className="w-4 h-4 text-red-600 rounded border-neutral-300"
                        />
                      </td>
                      <td className="p-3 font-semibold text-neutral-900 flex items-center gap-1.5 pt-4">
                        <Building className="w-4 h-4 text-red-600" />
                        {o.hotelName}
                      </td>
                      <td className="p-3">
                        {o.roomTypeName ? (
                          <span className="font-medium text-neutral-800">{o.roomTypeName}</span>
                        ) : (
                          <span className="text-xs text-neutral-400 italic">Toàn bộ khách sạn</span>
                        )}
                      </td>
                      <td className="p-3">
                        {o.cancellationPolicyCode ? (
                          <span className="font-mono font-semibold text-neutral-800 bg-neutral-100 px-2 py-0.5 rounded-lg text-xs">
                            {o.cancellationPolicyCode}
                          </span>
                        ) : (
                          <span className="text-xs text-neutral-400">Dùng mặc định chuỗi</span>
                        )}
                      </td>
                      <td className="p-3">
                        {o.noShowPolicyCode ? (
                          <span className="font-mono font-semibold text-neutral-800 bg-neutral-100 px-2 py-0.5 rounded-lg text-xs">
                            {o.noShowPolicyCode}
                          </span>
                        ) : (
                          <span className="text-xs text-neutral-400">Dùng mặc định chuỗi</span>
                        )}
                      </td>
                      <td className="p-3 text-xs space-y-0.5">
                        {o.freeCancellationBeforeDaysOverride !== null && o.freeCancellationBeforeDaysOverride !== undefined && (
                          <div className="text-neutral-700">
                            Miễn phí hủy: <strong>{o.freeCancellationBeforeDaysOverride} ngày</strong>
                          </div>
                        )}
                        {o.noShowReleaseTimeOverride && (
                          <div className="text-neutral-700">
                            Giờ hủy No-Show: <strong>{o.noShowReleaseTimeOverride.substring(0, 5)}</strong>
                          </div>
                        )}
                        {!o.freeCancellationBeforeDaysOverride && !o.noShowReleaseTimeOverride && (
                          <span className="text-neutral-400 italic">Không ghi đè thông số</span>
                        )}
                      </td>
                      <td className="p-3 text-xs text-neutral-600 max-w-xs truncate" title={o.overrideReason}>
                        {o.overrideReason}
                      </td>
                      <td className="p-3">
                        {o.isActive ? (
                          <span className="inline-flex items-center gap-1 text-xs text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-full border border-emerald-200">
                            <CheckCircle2 className="w-3.5 h-3.5" /> Áp dụng
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1 text-xs text-neutral-500 bg-neutral-100 px-2 py-0.5 rounded-full">
                            <XCircle className="w-3.5 h-3.5" /> Ngưng
                          </span>
                        )}
                      </td>
                      <td className="p-3 text-right">
                        <div className="inline-flex items-center gap-1">
                          <button
                            onClick={() => {
                              setSelectedOverride(o);
                              setIsOverrideModalOpen(true);
                            }}
                            className="p-1.5 text-neutral-600 hover:text-red-600 hover:bg-neutral-100 rounded-lg cursor-pointer"
                            title="Sửa ghi đè"
                          >
                            <Edit className="w-4 h-4" />
                          </button>
                          <button
                            onClick={() => handleOpenDelete([o.id])}
                            className="p-1.5 text-neutral-400 hover:text-red-600 hover:bg-red-50 rounded-lg cursor-pointer"
                            title="Xóa ghi đè"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination */}
        <div className="p-4 border-t border-neutral-200">
          <Pagination
            pageNumber={page}
            totalPages={totalPages}
            totalElements={totalElements}
            pageSize={pageSize}
            onPageChange={(p) => setPage(p)}
          />
        </div>
      </div>

      {/* Modals */}
      <CancellationPolicyModal
        isOpen={isCancelModalOpen}
        onClose={() => setIsCancelModalOpen(false)}
        onSuccess={loadData}
        policy={selectedCancelPolicy}
      />

      <NoShowPolicyModal
        isOpen={isNoShowModalOpen}
        onClose={() => setIsNoShowModalOpen(false)}
        onSuccess={loadData}
        policy={selectedNoShowPolicy}
      />

      <PropertyOverrideModal
        isOpen={isOverrideModalOpen}
        onClose={() => setIsOverrideModalOpen(false)}
        onSuccess={loadData}
        override={selectedOverride}
      />

      <ConfirmModal
        isOpen={isDeleteModalOpen}
        onClose={() => setIsDeleteModalOpen(false)}
        onConfirm={handleConfirmDelete}
        title="Xác nhận xóa bản ghi chính sách"
        description={`Bạn có chắc chắn muốn xóa ${deleteTargetIds.length} bản ghi đã chọn? Các đơn đặt phòng đã hoàn tất vẫn giữ nguyên bản chụp snapshot độc lập.`}
        confirmText="Xóa vĩnh viễn"
        cancelText="Hủy"
        variant="danger"
        isLoading={isDeleting}
      />
    </div>
  );
};

export default PolicyManagementPage;
