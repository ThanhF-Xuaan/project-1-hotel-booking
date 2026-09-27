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
} from 'lucide-react';
import Button from '../../../core/components/ui/Button';
import Input from '../../../core/components/ui/Input';
import Modal, { ConfirmModal } from '../../../core/components/ui/Modal';
import Pagination from '../../../core/components/ui/Pagination';
import { operationService } from '../services/operation.service';
import type {
  MenuResponse,
  MenuType,
  ServicePricingType,
  MenuCreateRequest,
  MenuUpdateRequest,
} from '../types/operation.types';

export const MenuListPage: React.FC = () => {
  const [menus, setMenus] = useState<MenuResponse[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [page, setPage] = useState(0);
  const [pageSize] = useState(10);
  const [isLoading, setIsLoading] = useState(false);

  // Filters
  const [searchName, setSearchName] = useState('');
  const [searchType, setSearchType] = useState<MenuType | ''>('');
  const [searchStatus, setSearchStatus] = useState<string>('');

  // Selected items for batch delete
  const [selectedIds, setSelectedIds] = useState<number[]>([]);

  // Add/Edit Modal
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingItem, setEditingItem] = useState<MenuResponse | null>(null);
  const [formData, setFormData] = useState<{
    name: string;
    description: string;
    menuType: MenuType;
    basePrice: number;
    taxCategoryId: number;
    stockQuantity: number;
    pricingType: ServicePricingType;
    status: string;
  }>({
    name: '',
    description: '',
    menuType: 'PRODUCT',
    basePrice: 50000,
    taxCategoryId: 1,
    stockQuantity: 100,
    pricingType: 'PER_UNIT',
    status: 'ACTIVE',
  });
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Delete Confirm Modal
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [itemToDelete, setItemToDelete] = useState<number | null>(null);

  const fetchMenus = useCallback(async () => {
    setIsLoading(true);
    try {
      const res = await operationService.getMenus({
        name: searchName || undefined,
        menuType: searchType || undefined,
        status: searchStatus || undefined,
        page: page + 1,
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
  }, [searchName, searchType, searchStatus, page, pageSize]);

  useEffect(() => {
    fetchMenus();
  }, [fetchMenus]);

  const handleOpenAddModal = () => {
    setEditingItem(null);
    setFormData({
      name: '',
      description: '',
      menuType: 'PRODUCT',
      basePrice: 50000,
      taxCategoryId: 1,
      stockQuantity: 100,
      pricingType: 'PER_UNIT',
      status: 'ACTIVE',
    });
    setIsModalOpen(true);
  };

  const handleOpenEditModal = (item: MenuResponse) => {
    setEditingItem(item);
    setFormData({
      name: item.name,
      description: item.description || '',
      menuType: item.menuType,
      basePrice: item.basePrice,
      taxCategoryId: item.taxCategoryId,
      stockQuantity: item.stockQuantity || 0,
      pricingType: item.pricingType || 'PER_UNIT',
      status: item.status,
    });
    setIsModalOpen(true);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.name.trim()) return;

    setIsSubmitting(true);
    try {
      if (editingItem) {
        const updateReq: MenuUpdateRequest = {
          name: formData.name.trim(),
          description: formData.description,
          basePrice: formData.basePrice,
          taxCategoryId: formData.taxCategoryId,
          stockQuantity: formData.menuType === 'PRODUCT' ? formData.stockQuantity : undefined,
          pricingType: formData.menuType === 'SERVICE' ? formData.pricingType : undefined,
          status: formData.status,
        };
        await operationService.updateMenu(editingItem.id, updateReq);
      } else {
        const createReq: MenuCreateRequest = {
          name: formData.name.trim(),
          description: formData.description,
          menuType: formData.menuType,
          basePrice: formData.basePrice,
          taxCategoryId: formData.taxCategoryId,
          stockQuantity: formData.menuType === 'PRODUCT' ? formData.stockQuantity : undefined,
          pricingType: formData.menuType === 'SERVICE' ? formData.pricingType : undefined,
        };
        await operationService.createMenu(createReq);
      }
      setIsModalOpen(false);
      fetchMenus();
    } catch (err) {
      console.error('Save menu failed:', err);
    } finally {
      setIsSubmitting(false);
    }
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
      fetchMenus();
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

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold text-neutral-900 tracking-tight flex items-center gap-2">
            <UtensilsCrossed className="w-7 h-7 text-red-600" />
            Quản lý Thực đơn & Dịch vụ (F&B / POS)
          </h1>
          <p className="text-sm text-neutral-500 mt-1">
            Danh mục đồ ăn, thức uống minibar, dịch vụ giặt ủi và tiện ích phòng
          </p>
        </div>
        <Button onClick={handleOpenAddModal} className="flex items-center gap-2">
          <Plus className="w-4 h-4" />
          Thêm Món / Dịch vụ
        </Button>
      </div>

      {/* Row 1: Search & Filter Bar */}
      <div className="bg-white p-4 rounded-2xl border border-neutral-200 shadow-xs flex flex-wrap items-center gap-4">
        <div className="flex-1 min-w-[240px]">
          <Input
            placeholder="Tìm theo tên món ăn hoặc dịch vụ..."
            value={searchName}
            onChange={(e) => setSearchName(e.target.value)}
            leftIcon={<Search className="w-4 h-4 text-neutral-400" />}
          />
        </div>
        <div className="w-48">
          <select
            value={searchType}
            onChange={(e) => {
              setSearchType(e.target.value as MenuType | '');
              setPage(0);
            }}
            className="w-full h-12 px-3 border border-neutral-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-red-600 bg-white"
          >
            <option value="">Tất cả loại danh mục</option>
            <option value="PRODUCT">Sản phẩm vật lý (F&B, Minibar)</option>
            <option value="SERVICE">Dịch vụ (Giặt là, Spa, Xe...)</option>
          </select>
        </div>
        <div className="w-44">
          <select
            value={searchStatus}
            onChange={(e) => {
              setSearchStatus(e.target.value);
              setPage(0);
            }}
            className="w-full h-12 px-3 border border-neutral-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-red-600 bg-white"
          >
            <option value="">Tất cả trạng thái</option>
            <option value="ACTIVE">Đang phục vụ (Active)</option>
            <option value="INACTIVE">Tạm ngưng (Inactive)</option>
          </select>
        </div>
        <Button
          variant="secondary"
          onClick={() => {
            setSearchName('');
            setSearchType('');
            setSearchStatus('');
            setPage(0);
          }}
          className="flex items-center gap-1.5"
        >
          <RefreshCw className="w-4 h-4" />
          Làm mới
        </Button>
      </div>

      {/* Row 2: Action Toolbar */}
      <div className="flex justify-between items-center bg-neutral-50 px-4 py-3 rounded-xl border border-neutral-200">
        <div className="text-sm text-neutral-600">
          Tổng cộng: <span className="font-semibold text-neutral-900">{totalElements}</span> mục
          {selectedIds.length > 0 && (
            <span className="ml-3 font-medium text-red-600">
              (Đang chọn: {selectedIds.length})
            </span>
          )}
        </div>
        {selectedIds.length > 0 && (
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
                <th className="py-3.5 px-4 w-12 text-center">
                  <input
                    type="checkbox"
                    checked={menus.length > 0 && selectedIds.length === menus.length}
                    onChange={toggleSelectAll}
                    className="rounded border-neutral-300 text-red-600 focus:ring-red-600"
                  />
                </th>
                <th className="py-3.5 px-4">Tên Món / Dịch vụ</th>
                <th className="py-3.5 px-4">Loại danh mục</th>
                <th className="py-3.5 px-4">Đơn giá gốc</th>
                <th className="py-3.5 px-4">Tồn kho / Tính giá</th>
                <th className="py-3.5 px-4">Trạng thái</th>
                <th className="py-3.5 px-4 text-right">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-neutral-200">
              {isLoading ? (
                <tr>
                  <td colSpan={7} className="py-12 text-center text-neutral-400">
                    Đang tải dữ liệu thực đơn...
                  </td>
                </tr>
              ) : menus.length === 0 ? (
                <tr>
                  <td colSpan={7} className="py-12 text-center text-neutral-400">
                    Không tìm thấy món ăn hoặc dịch vụ nào phù hợp.
                  </td>
                </tr>
              ) : (
                menus.map((item) => (
                  <tr key={item.id} className="hover:bg-neutral-50 transition-colors">
                    <td className="py-3.5 px-4 text-center">
                      <input
                        type="checkbox"
                        checked={selectedIds.includes(item.id)}
                        onChange={() => toggleSelect(item.id)}
                        className="rounded border-neutral-300 text-red-600 focus:ring-red-600"
                      />
                    </td>
                    <td className="py-3.5 px-4">
                      <div className="font-semibold text-neutral-900">{item.name}</div>
                      {item.description && (
                        <div className="text-xs text-neutral-400 line-clamp-1">{item.description}</div>
                      )}
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
                        <span className={`text-xs font-semibold px-2 py-0.5 rounded-md ${
                          (item.stockQuantity ?? 0) > 10 ? 'bg-emerald-50 text-emerald-700' : 'bg-rose-50 text-rose-700'
                        }`}>
                          Còn {item.stockQuantity ?? 0} món
                        </span>
                      ) : (
                        <span className="text-xs text-neutral-600 font-medium">
                          {item.pricingType === 'PER_UNIT' && 'Theo lượt / lần'}
                          {item.pricingType === 'PER_STAY' && 'Theo chuyến lưu trú'}
                          {item.pricingType === 'PER_NIGHT' && 'Theo đêm nghỉ'}
                          {item.pricingType === 'PER_PERSON' && 'Theo số khách'}
                        </span>
                      )}
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
                    <td className="py-3.5 px-4 text-right">
                      <div className="flex justify-end gap-2">
                        <button
                          onClick={() => handleOpenEditModal(item)}
                          className="p-1.5 text-neutral-500 hover:text-blue-600 hover:bg-neutral-100 rounded-lg transition-colors"
                          title="Chỉnh sửa"
                        >
                          <Edit className="w-4 h-4" />
                        </button>
                        <button
                          onClick={() => {
                            setItemToDelete(item.id);
                            setIsDeleteModalOpen(true);
                          }}
                          className="p-1.5 text-neutral-500 hover:text-red-600 hover:bg-neutral-100 rounded-lg transition-colors"
                          title="Xóa"
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
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title={editingItem ? 'Cập nhật món / dịch vụ' : 'Thêm mới món / dịch vụ'}
      >
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-medium text-neutral-700 mb-1">
              Tên món / dịch vụ <span className="text-red-500">*</span>
            </label>
            <Input
              value={formData.name}
              onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              placeholder="VD: Cà phê sữa đá, Giặt ủi 1 bộ vest..."
              required
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-medium text-neutral-700 mb-1">
                Loại mục <span className="text-red-500">*</span>
              </label>
              <select
                value={formData.menuType}
                onChange={(e) => setFormData({ ...formData, menuType: e.target.value as MenuType })}
                disabled={!!editingItem}
                className="w-full h-12 px-3 border border-neutral-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-red-600 bg-white disabled:bg-neutral-100"
              >
                <option value="PRODUCT">Sản phẩm vật lý (F&B / Minibar)</option>
                <option value="SERVICE">Dịch vụ (Giặt là, Spa, Massage)</option>
              </select>
            </div>
            <div>
              <label className="block text-xs font-medium text-neutral-700 mb-1">
                Đơn giá (VNĐ) <span className="text-red-500">*</span>
              </label>
              <Input
                type="number"
                min="0"
                step="1000"
                value={formData.basePrice}
                onChange={(e) => setFormData({ ...formData, basePrice: parseFloat(e.target.value) || 0 })}
                required
              />
            </div>
          </div>

          {formData.menuType === 'PRODUCT' ? (
            <div>
              <label className="block text-xs font-medium text-neutral-700 mb-1">
                Số lượng tồn kho ban đầu <span className="text-red-500">*</span>
              </label>
              <Input
                type="number"
                min="0"
                value={formData.stockQuantity}
                onChange={(e) => setFormData({ ...formData, stockQuantity: parseInt(e.target.value) || 0 })}
                required
              />
            </div>
          ) : (
            <div>
              <label className="block text-xs font-medium text-neutral-700 mb-1">
                Hình thức tính giá <span className="text-red-500">*</span>
              </label>
              <select
                value={formData.pricingType}
                onChange={(e) => setFormData({ ...formData, pricingType: e.target.value as ServicePricingType })}
                className="w-full h-12 px-3 border border-neutral-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-red-600 bg-white"
              >
                <option value="PER_UNIT">Theo lượt / từng lần gọi</option>
                <option value="PER_STAY">Theo cả chuyến lưu trú</option>
                <option value="PER_NIGHT">Theo từng đêm nghỉ</option>
                <option value="PER_PERSON">Theo số lượng khách</option>
              </select>
            </div>
          )}

          <div>
            <label className="block text-xs font-medium text-neutral-700 mb-1">Mô tả chi tiết</label>
            <textarea
              value={formData.description}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              rows={3}
              className="w-full p-3 border border-neutral-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-red-600"
              placeholder="Thành phần, lưu ý bảo quản hoặc thời gian phục vụ..."
            />
          </div>

          {editingItem && (
            <div>
              <label className="block text-xs font-medium text-neutral-700 mb-1">Trạng thái phục vụ</label>
              <select
                value={formData.status}
                onChange={(e) => setFormData({ ...formData, status: e.target.value })}
                className="w-full h-12 px-3 border border-neutral-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-red-600 bg-white"
              >
                <option value="ACTIVE">Sẵn sàng phục vụ</option>
                <option value="INACTIVE">Tạm dừng phục vụ</option>
              </select>
            </div>
          )}

          <div className="flex justify-end gap-3 pt-4 border-t border-neutral-200">
            <Button variant="secondary" type="button" onClick={() => setIsModalOpen(false)}>
              Hủy
            </Button>
            <Button type="submit" disabled={isSubmitting}>
              {isSubmitting ? 'Đang lưu...' : editingItem ? 'Lưu thay đổi' : 'Thêm vào thực đơn'}
            </Button>
          </div>
        </form>
      </Modal>

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
