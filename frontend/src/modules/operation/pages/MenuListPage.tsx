import React, { useState, useEffect, useCallback } from 'react';
import {
  Search,
  Plus,
  Trash2,
  Edit,
  UtensilsCrossed,
  Package,
  Sparkles,
  RefreshCw,
  Building2,
} from 'lucide-react';
import Button from '../../../core/components/ui/Button';
import Input from '../../../core/components/ui/Input';
import Select from '../../../core/components/ui/Select';
import { ConfirmModal } from '../../../core/components/ui/Modal';
import Pagination from '../../../core/components/ui/Pagination';
import MenuModal from '../components/MenuModal';
import { operationService } from '../services/operation.service';
import { hotelService } from '../../organization/services/hotel.service';
import { hasAnyRole } from '../../auth/utils/token.utils';
import type {
  MenuResponse,
  MenuType,
} from '../types/operation.types';
import type { HotelDto } from '../../organization/types/organization.types';

export const MenuListPage: React.FC = () => {
  const canManage = hasAnyRole(['CHAIN_ADMIN', 'PROPERTY_MANAGER', 'F_AND_B']);

  const [menus, setMenus] = useState<MenuResponse[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [page, setPage] = useState(0);
  const [pageSize] = useState(10);
  const [isLoading, setIsLoading] = useState(false);

  // Filters
  const [searchName, setSearchName] = useState('');
  const [filterHotelId, setFilterHotelId] = useState<number | 0>(0);
  const [searchType, setSearchType] = useState<MenuType | ''>('');
  const [searchStatus, setSearchStatus] = useState<string>('');
  const [hotels, setHotels] = useState<HotelDto[]>([]);

  // Selection
  const [selectedIds, setSelectedIds] = useState<number[]>([]);

  // Add/Edit Modal
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingItem, setEditingItem] = useState<MenuResponse | null>(null);

  // Delete Confirm Modal
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [itemToDelete, setItemToDelete] = useState<number | null>(null);

  // Load hotel list for filter
  useEffect(() => {
    const timer = setTimeout(() => {
      void (async () => {
        try {
          const res = await hotelService.filter({ pageSize: 100 });
          setHotels(res.result?.content || []);
        } catch (err) {
          console.error('Failed to load hotels for filter:', err);
        }
      })();
    }, 0);
    return () => clearTimeout(timer);
  }, []);

  const fetchMenus = useCallback(async () => {
    setIsLoading(true);
    try {
      const res = await operationService.getMenus({
        hotelId: filterHotelId || undefined,
        name: searchName.trim() || undefined,
        menuType: searchType || undefined,
        status: searchStatus || undefined,
        page,
        pageSize,
      });
      setMenus(res.content || []);
      setTotalElements(res.totalElements || 0);
      setTotalPages(res.totalPages || 0);
    } catch (err) {
      console.error('Failed to fetch menus:', err);
    } finally {
      setIsLoading(false);
    }
  }, [filterHotelId, searchName, searchType, searchStatus, page, pageSize]);

  useEffect(() => {
    const timer = setTimeout(() => {
      void fetchMenus();
    }, 0);
    return () => clearTimeout(timer);
  }, [fetchMenus]);

  const handleOpenAddModal = () => {
    setEditingItem(null);
    setIsModalOpen(true);
  };

  const handleOpenEditModal = (item: MenuResponse) => {
    setEditingItem(item);
    setIsModalOpen(true);
  };

  const handleConfirmDelete = async () => {
    try {
      if (itemToDelete) {
        await operationService.deleteMenus([itemToDelete]);
      } else if (selectedIds.length > 0) {
        await operationService.deleteMenus(selectedIds);
        setSelectedIds([]);
      }
      setIsDeleteModalOpen(false);
      setItemToDelete(null);
      void fetchMenus();
    } catch (err) {
      console.error('Delete menu failed:', err);
    }
  };

  const toggleSelectAll = () => {
    if (selectedIds.length === menus.length) {
      setSelectedIds([]);
    } else {
      setSelectedIds(menus.map((m) => m.id));
    }
  };

  const toggleSelect = (id: number) => {
    setSelectedIds((prev) =>
      prev.includes(id) ? prev.filter((item) => item !== id) : [...prev, id]
    );
  };

  const handleResetFilters = () => {
    setSearchName('');
    setFilterHotelId(0);
    setSearchType('');
    setSearchStatus('');
    setPage(0);
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold text-neutral-900 tracking-tight flex items-center gap-2">
            <UtensilsCrossed className="w-7 h-7 text-red-600" />
            Quản lý Thực đơn & Dịch vụ Cơ sở (F&B / POS)
          </h1>
          <p className="text-sm text-neutral-500 mt-1">
            Danh mục đồ ăn, thức uống minibar, dịch vụ phòng theo từng khách sạn cơ sở
          </p>
        </div>
        {canManage && (
          <Button onClick={handleOpenAddModal} className="flex items-center gap-2">
            <Plus className="w-4 h-4" />
            Thêm Món / Dịch vụ
          </Button>
        )}
      </div>

      {/* Row 1: Search & Filter Bar */}
      <div className="bg-white p-4 rounded-2xl border border-neutral-200 shadow-xs flex flex-wrap items-center gap-4">
        <div className="flex-1 min-w-[220px]">
          <Input
            placeholder="Tìm theo tên món ăn hoặc dịch vụ..."
            value={searchName}
            onChange={(e) => {
              setSearchName(e.target.value);
              setPage(0);
            }}
            leftIcon={<Search className="w-4 h-4 text-neutral-400" />}
          />
        </div>

        <div className="w-56">
          <Select<number>
            value={filterHotelId}
            onChange={(val) => {
              setFilterHotelId(val);
              setPage(0);
            }}
            options={[
              { value: 0, label: 'Tất cả khách sạn cơ sở' },
              ...hotels.map((h) => ({
                value: h.id,
                label: h.name,
              })),
            ]}
            size="lg"
          />
        </div>

        <div className="w-52">
          <Select<MenuType | ''>
            value={searchType}
            onChange={(val) => {
              setSearchType(val);
              setPage(0);
            }}
            options={[
              { value: '', label: 'Tất cả loại danh mục' },
              { value: 'PRODUCT', label: 'Sản phẩm vật lý (F&B / Minibar)' },
              { value: 'SERVICE', label: 'Dịch vụ (Giặt là, Spa...)' },
            ]}
            size="lg"
          />
        </div>

        <div className="w-48">
          <Select<string>
            value={searchStatus}
            onChange={(val) => {
              setSearchStatus(val);
              setPage(0);
            }}
            options={[
              { value: '', label: 'Tất cả trạng thái' },
              { value: 'ACTIVE', label: 'Đang phục vụ (Active)' },
              { value: 'INACTIVE', label: 'Tạm ngưng (Inactive)' },
            ]}
            size="lg"
          />
        </div>

        <Button
          variant="secondary"
          onClick={handleResetFilters}
          className="flex items-center gap-1.5"
        >
          <RefreshCw className="w-4 h-4" />
          Làm mới
        </Button>
      </div>

      {/* Row 2: Action Toolbar */}
      <div className="flex justify-between items-center bg-neutral-50 px-4 py-3 rounded-xl border border-neutral-200">
        <div className="text-sm text-neutral-600">
          Tổng cộng: <span className="font-semibold text-neutral-900">{totalElements}</span> món / dịch vụ
          {selectedIds.length > 0 && (
            <span className="ml-3 font-medium text-red-600">
              (Đang chọn: {selectedIds.length})
            </span>
          )}
        </div>
        {selectedIds.length > 0 && canManage && (
          <Button
            variant="danger"
            onClick={() => {
              setItemToDelete(null);
              setIsDeleteModalOpen(true);
            }}
            className="flex items-center gap-2 py-1.5 text-sm"
          >
            <Trash2 className="w-4 h-4" />
            Xóa {selectedIds.length} mục đã chọn
          </Button>
        )}
      </div>

      {/* Row 3: Data Table */}
      <div className="bg-white rounded-2xl border border-neutral-200 overflow-hidden shadow-xs">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse text-sm">
            <thead>
              <tr className="bg-neutral-50 border-b border-neutral-200 text-neutral-600 font-semibold text-xs uppercase tracking-wider">
                {canManage && (
                  <th className="py-3.5 px-4 w-12 text-center">
                    <input
                      type="checkbox"
                      checked={menus.length > 0 && selectedIds.length === menus.length}
                      onChange={toggleSelectAll}
                      className="rounded border-neutral-300 text-red-600 focus:ring-red-600"
                    />
                  </th>
                )}
                <th className="py-3.5 px-4">Tên Món / Dịch vụ</th>
                <th className="py-3.5 px-4">Cơ sở khách sạn</th>
                <th className="py-3.5 px-4">Loại danh mục</th>
                <th className="py-3.5 px-4">Đơn giá cơ sở</th>
                <th className="py-3.5 px-4">Tồn kho / Tính giá</th>
                <th className="py-3.5 px-4">Thuế VAT</th>
                <th className="py-3.5 px-4">Trạng thái</th>
                {canManage && <th className="py-3.5 px-4 text-right">Thao tác</th>}
              </tr>
            </thead>
            <tbody className="divide-y divide-neutral-200">
              {isLoading ? (
                <tr>
                  <td colSpan={canManage ? 9 : 8} className="py-12 text-center text-neutral-400">
                    Đang tải dữ liệu thực đơn...
                  </td>
                </tr>
              ) : menus.length === 0 ? (
                <tr>
                  <td colSpan={canManage ? 9 : 8} className="py-12 text-center text-neutral-400">
                    Không tìm thấy món ăn hoặc dịch vụ nào phù hợp.
                  </td>
                </tr>
              ) : (
                menus.map((item) => (
                  <tr key={item.id} className="hover:bg-neutral-50 transition-colors">
                    {canManage && (
                      <td className="py-3.5 px-4 text-center">
                        <input
                          type="checkbox"
                          checked={selectedIds.includes(item.id)}
                          onChange={() => toggleSelect(item.id)}
                          className="rounded border-neutral-300 text-red-600 focus:ring-red-600"
                        />
                      </td>
                    )}
                    <td className="py-3.5 px-4">
                      <div className="font-semibold text-neutral-900">
                        {item.customName ? (
                          <>
                            {item.customName}
                            <span className="text-xs text-neutral-400 font-normal ml-2 block sm:inline">
                              (Gốc: {item.name})
                            </span>
                          </>
                        ) : (
                          item.name
                        )}
                      </div>
                      {item.description && (
                        <div className="text-xs text-neutral-400 line-clamp-1">{item.description}</div>
                      )}
                    </td>
                    <td className="py-3.5 px-4">
                      <span className="inline-flex items-center gap-1 text-xs text-neutral-700">
                        <Building2 className="w-3.5 h-3.5 text-neutral-400" />
                        {item.hotelName || `Khách sạn #${item.hotelId}`}
                      </span>
                    </td>
                    <td className="py-3.5 px-4">
                      {item.menuType === 'PRODUCT' ? (
                        <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-medium bg-amber-50 text-amber-700 border border-amber-200">
                          <Package className="w-3.5 h-3.5" />
                          Sản phẩm (F&B)
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-medium bg-blue-50 text-blue-700 border border-blue-200">
                          <Sparkles className="w-3.5 h-3.5" />
                          Dịch vụ
                        </span>
                      )}
                    </td>
                    <td className="py-3.5 px-4 font-semibold text-neutral-900">
                      {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(item.basePrice)}
                    </td>
                    <td className="py-3.5 px-4">
                      {item.menuType === 'PRODUCT' ? (
                        <span
                          className={`text-xs font-semibold px-2 py-0.5 rounded-md ${
                            (item.stockQuantity ?? 0) > 10
                              ? 'bg-emerald-50 text-emerald-700'
                              : 'bg-rose-50 text-rose-700'
                          }`}
                        >
                          Còn {item.stockQuantity ?? 0} {item.unit || 'món'}
                        </span>
                      ) : (
                        <span className="text-xs text-neutral-600 font-medium">
                          {item.pricingType === 'PER_UNIT' && 'Theo lượt / lần'}
                          {item.pricingType === 'PER_STAY' && 'Theo chuyến lưu trú'}
                          {item.pricingType === 'PER_NIGHT' && 'Theo đêm nghỉ'}
                          {item.pricingType === 'PER_PERSON' && 'Theo số khách'}
                          {item.pricingType === 'PER_PERSON_PER_NIGHT' && 'Khách / Đêm'}
                          {item.pricingType === 'PER_USE' && 'Theo lần sử dụng'}
                        </span>
                      )}
                    </td>
                    <td className="py-3.5 px-4 text-xs text-neutral-700">
                      {item.taxCategoryName || '—'}
                    </td>
                    <td className="py-3.5 px-4">
                      <span
                        className={`inline-block px-2.5 py-0.5 rounded-full text-xs font-medium ${
                          item.status === 'ACTIVE'
                            ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                            : 'bg-neutral-100 text-neutral-600 border border-neutral-300'
                        }`}
                      >
                        {item.status === 'ACTIVE' ? 'Sẵn sàng' : 'Tạm ngưng'}
                      </span>
                    </td>
                    {canManage && (
                      <td className="py-3.5 px-4 text-right">
                        <div className="flex justify-end gap-2">
                          <button
                            onClick={() => handleOpenEditModal(item)}
                            className="p-1.5 text-neutral-500 hover:text-blue-600 hover:bg-neutral-100 rounded-lg transition-colors"
                            title="Chỉnh sửa món"
                          >
                            <Edit className="w-4 h-4" />
                          </button>
                          <button
                            onClick={() => {
                              setItemToDelete(item.id);
                              setIsDeleteModalOpen(true);
                            }}
                            className="p-1.5 text-neutral-500 hover:text-red-600 hover:bg-neutral-100 rounded-lg transition-colors"
                            title="Xóa món"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        </div>
                      </td>
                    )}
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {totalPages > 1 && (
          <div className="p-4 border-t border-neutral-200">
            <Pagination
              pageNumber={page}
              pageSize={pageSize}
              totalElements={totalElements}
              totalPages={totalPages}
              onPageChange={(p) => setPage(p)}
            />
          </div>
        )}
      </div>

      {/* Add / Edit Modal */}
      <MenuModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onSuccess={fetchMenus}
        item={editingItem}
        defaultHotelId={filterHotelId || undefined}
      />

      {/* Delete Confirmation Modal */}
      <ConfirmModal
        isOpen={isDeleteModalOpen}
        onClose={() => setIsDeleteModalOpen(false)}
        onConfirm={handleConfirmDelete}
        title="Xác nhận xóa món / dịch vụ"
        description={
          itemToDelete
            ? 'Bạn có chắc chắn muốn xóa mục này khỏi thực đơn không?'
            : `Bạn có chắc chắn muốn xóa ${selectedIds.length} mục đã chọn không?`
        }
      />
    </div>
  );
};

export default MenuListPage;
