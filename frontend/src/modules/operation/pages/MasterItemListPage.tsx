import React, { useState, useEffect, useCallback } from 'react';
import {
  Search,
  Plus,
  Trash2,
  Edit,
  Package,
  Sparkles,
  RefreshCw,
  Layers,
} from 'lucide-react';
import Button from '../../../core/components/ui/Button';
import Input from '../../../core/components/ui/Input';
import Select from '../../../core/components/ui/Select';
import { ConfirmModal } from '../../../core/components/ui/Modal';
import Pagination from '../../../core/components/ui/Pagination';
import MasterItemModal from '../components/MasterItemModal';
import { operationService } from '../services/operation.service';
import taxCategoryService from '../../pricing/services/taxCategory.service';
import { hasAnyRole } from '../../auth/utils/token.utils';
import type {
  MasterItemResponse,
  MenuType,
} from '../types/operation.types';
import type { TaxCategoryDto } from '../../pricing/types/pricing.types';

export const MasterItemListPage: React.FC = () => {
  const isChainAdmin = hasAnyRole(['CHAIN_ADMIN']);

  const [items, setItems] = useState<MasterItemResponse[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [page, setPage] = useState(0);
  const [pageSize] = useState(10);
  const [isLoading, setIsLoading] = useState(false);

  // Filters
  const [searchKeyword, setSearchKeyword] = useState('');
  const [filterType, setFilterType] = useState<MenuType | ''>('');
  const [filterTaxId, setFilterTaxId] = useState<number | 0>(0);
  const [filterStatus, setFilterStatus] = useState<string>('');
  const [taxCategories, setTaxCategories] = useState<TaxCategoryDto[]>([]);

  // Selection
  const [selectedIds, setSelectedIds] = useState<number[]>([]);

  // Modals
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingItem, setEditingItem] = useState<MasterItemResponse | null>(null);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [itemToDelete, setItemToDelete] = useState<number | null>(null);

  // Load tax categories once for filter
  useEffect(() => {
    const timer = setTimeout(() => {
      void (async () => {
        try {
          const taxes = await taxCategoryService.getAllActive();
          setTaxCategories(taxes);
        } catch (err) {
          console.error('Failed to load tax categories for filter:', err);
        }
      })();
    }, 0);
    return () => clearTimeout(timer);
  }, []);

  const fetchItems = useCallback(async () => {
    setIsLoading(true);
    try {
      const res = await operationService.getMasterItems({
        name: searchKeyword.trim() || undefined,
        itemType: filterType || undefined,
        taxCategoryId: filterTaxId || undefined,
        status: filterStatus || undefined,
        page,
        pageSize,
      });
      setItems(res.content || []);
      setTotalElements(res.totalElements || 0);
      setTotalPages(res.totalPages || 0);
    } catch (err) {
      console.error('Failed to fetch master items:', err);
    } finally {
      setIsLoading(false);
    }
  }, [searchKeyword, filterType, filterTaxId, filterStatus, page, pageSize]);

  useEffect(() => {
    const timer = setTimeout(() => {
      void fetchItems();
    }, 0);
    return () => clearTimeout(timer);
  }, [fetchItems]);

  const handleOpenAddModal = () => {
    setEditingItem(null);
    setIsModalOpen(true);
  };

  const handleOpenEditModal = (item: MasterItemResponse) => {
    setEditingItem(item);
    setIsModalOpen(true);
  };

  const handleConfirmDelete = async () => {
    try {
      if (itemToDelete) {
        await operationService.deleteMasterItems([itemToDelete]);
      } else if (selectedIds.length > 0) {
        await operationService.deleteMasterItems(selectedIds);
        setSelectedIds([]);
      }
      setIsDeleteModalOpen(false);
      setItemToDelete(null);
      void fetchItems();
    } catch (err) {
      console.error('Delete master items failed:', err);
    }
  };

  const toggleSelectAll = () => {
    if (selectedIds.length === items.length) {
      setSelectedIds([]);
    } else {
      setSelectedIds(items.map((m) => m.id));
    }
  };

  const toggleSelect = (id: number) => {
    setSelectedIds((prev) =>
      prev.includes(id) ? prev.filter((item) => item !== id) : [...prev, id]
    );
  };

  const handleResetFilters = () => {
    setSearchKeyword('');
    setFilterType('');
    setFilterTaxId(0);
    setFilterStatus('');
    setPage(0);
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold text-neutral-900 tracking-tight flex items-center gap-2">
            <Layers className="w-7 h-7 text-red-600" />
            Danh mục Gốc Toàn Chuỗi (Master Items)
          </h1>
          <p className="text-sm text-neutral-500 mt-1">
            Quản lý danh mục mẫu sản phẩm và dịch vụ chuẩn hóa cho toàn bộ hệ thống khách sạn
          </p>
        </div>
        {isChainAdmin && (
          <Button onClick={handleOpenAddModal} className="flex items-center gap-2">
            <Plus className="w-4 h-4" />
            Thêm Master Item
          </Button>
        )}
      </div>

      {/* Row 1: Search & Filter Bar */}
      <div className="bg-white p-4 rounded-2xl border border-neutral-200 shadow-xs flex flex-wrap items-center gap-4">
        <div className="flex-1 min-w-[240px]">
          <Input
            placeholder="Tìm theo tên hoặc mã danh mục..."
            value={searchKeyword}
            onChange={(e) => {
              setSearchKeyword(e.target.value);
              setPage(0);
            }}
            leftIcon={<Search className="w-4 h-4 text-neutral-400" />}
          />
        </div>

        <div className="w-52">
          <Select<MenuType | ''>
            value={filterType}
            onChange={(val) => {
              setFilterType(val);
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

        <div className="w-56">
          <Select<number>
            value={filterTaxId}
            onChange={(val) => {
              setFilterTaxId(val);
              setPage(0);
            }}
            options={[
              { value: 0, label: 'Tất cả nhóm thuế' },
              ...taxCategories.map((t) => ({
                value: t.id,
                label: `${t.categoryName} (${t.categoryCode})`,
              })),
            ]}
            size="lg"
          />
        </div>

        <div className="w-48">
          <Select<string>
            value={filterStatus}
            onChange={(val) => {
              setFilterStatus(val);
              setPage(0);
            }}
            options={[
              { value: '', label: 'Tất cả trạng thái' },
              { value: 'ACTIVE', label: 'Đang hoạt động (ACTIVE)' },
              { value: 'INACTIVE', label: 'Tạm ngưng (INACTIVE)' },
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
          Tổng cộng: <span className="font-semibold text-neutral-900">{totalElements}</span> danh mục gốc
          {selectedIds.length > 0 && (
            <span className="ml-3 font-medium text-red-600">
              (Đang chọn: {selectedIds.length})
            </span>
          )}
        </div>
        {selectedIds.length > 0 && isChainAdmin && (
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
                {isChainAdmin && (
                  <th className="py-3.5 px-4 w-12 text-center">
                    <input
                      type="checkbox"
                      checked={items.length > 0 && selectedIds.length === items.length}
                      onChange={toggleSelectAll}
                      className="rounded border-neutral-300 text-red-600 focus:ring-red-600"
                    />
                  </th>
                )}
                <th className="py-3.5 px-4">Mã danh mục</th>
                <th className="py-3.5 px-4">Tên sản phẩm / dịch vụ</th>
                <th className="py-3.5 px-4">Loại danh mục</th>
                <th className="py-3.5 px-4">Đơn giá đề xuất</th>
                <th className="py-3.5 px-4">Đơn vị / Cách tính</th>
                <th className="py-3.5 px-4">Nhóm thuế</th>
                <th className="py-3.5 px-4">Trạng thái</th>
                {isChainAdmin && <th className="py-3.5 px-4 text-right">Thao tác</th>}
              </tr>
            </thead>
            <tbody className="divide-y divide-neutral-200">
              {isLoading ? (
                <tr>
                  <td colSpan={isChainAdmin ? 9 : 8} className="py-12 text-center text-neutral-400">
                    Đang tải danh mục gốc chuỗi...
                  </td>
                </tr>
              ) : items.length === 0 ? (
                <tr>
                  <td colSpan={isChainAdmin ? 9 : 8} className="py-12 text-center text-neutral-400">
                    Không tìm thấy danh mục gốc nào phù hợp.
                  </td>
                </tr>
              ) : (
                items.map((item) => (
                  <tr key={item.id} className="hover:bg-neutral-50 transition-colors">
                    {isChainAdmin && (
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
                      <span className="font-mono text-xs font-semibold px-2 py-0.5 rounded bg-neutral-100 text-neutral-800 border border-neutral-200">
                        {item.code}
                      </span>
                    </td>
                    <td className="py-3.5 px-4">
                      <div className="font-semibold text-neutral-900">{item.name}</div>
                      {item.description && (
                        <div className="text-xs text-neutral-400 line-clamp-1">{item.description}</div>
                      )}
                    </td>
                    <td className="py-3.5 px-4">
                      {item.itemType === 'PRODUCT' ? (
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
                      {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(item.defaultBasePrice)}
                    </td>
                    <td className="py-3.5 px-4 text-xs text-neutral-600">
                      {item.itemType === 'PRODUCT' ? (
                        item.unit ? `Đơn vị: ${item.unit}` : '—'
                      ) : (
                        <span>
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
                        {item.status === 'ACTIVE' ? 'Hoạt động' : 'Tạm ngưng'}
                      </span>
                    </td>
                    {isChainAdmin && (
                      <td className="py-3.5 px-4 text-right">
                        <div className="flex justify-end gap-2">
                          <button
                            onClick={() => handleOpenEditModal(item)}
                            className="p-1.5 text-neutral-500 hover:text-blue-600 hover:bg-neutral-100 rounded-lg transition-colors"
                            title="Chỉnh sửa danh mục gốc"
                          >
                            <Edit className="w-4 h-4" />
                          </button>
                          <button
                            onClick={() => {
                              setItemToDelete(item.id);
                              setIsDeleteModalOpen(true);
                            }}
                            className="p-1.5 text-neutral-500 hover:text-red-600 hover:bg-neutral-100 rounded-lg transition-colors"
                            title="Xóa danh mục gốc"
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
      <MasterItemModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onSuccess={fetchItems}
        item={editingItem}
      />

      {/* Delete Confirmation Modal */}
      <ConfirmModal
        isOpen={isDeleteModalOpen}
        onClose={() => setIsDeleteModalOpen(false)}
        onConfirm={handleConfirmDelete}
        title="Xác nhận xóa danh mục gốc chuỗi"
        description={
          itemToDelete
            ? 'Bạn có chắc chắn muốn xóa danh mục gốc này không? Các thực đơn cơ sở đã liên kết vẫn được giữ nguyên dữ liệu lịch sử.'
            : `Bạn có chắc chắn muốn xóa ${selectedIds.length} danh mục gốc đã chọn không?`
        }
      />
    </div>
  );
};

export default MasterItemListPage;
