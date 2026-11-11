import React, { useState, useEffect } from 'react';
import { Package, Sparkles } from 'lucide-react';
import Button from '../../../core/components/ui/Button';
import Input from '../../../core/components/ui/Input';
import CurrencyInput from '../../../core/components/ui/CurrencyInput';
import Select from '../../../core/components/ui/Select';
import Modal from '../../../core/components/ui/Modal';
import { operationService } from '../services/operation.service';
import { hotelService } from '../../organization/services/hotel.service';
import taxCategoryService from '../../pricing/services/taxCategory.service';
import type {
  MenuResponse,
  MenuCreateRequest,
  MenuUpdateRequest,
  MenuType,
  ServicePricingType,
  MasterItemResponse,
} from '../types/operation.types';
import type { HotelDto } from '../../organization/types/organization.types';
import type { TaxCategoryDto } from '../../pricing/types/pricing.types';

interface MenuModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
  item: MenuResponse | null;
  defaultHotelId?: number;
}

interface MenuFormState {
  hotelId: number;
  masterItemId: number;
  customName: string;
  name: string;
  description: string;
  menuType: MenuType;
  basePrice: number;
  taxCategoryId: number;
  stockQuantity: number;
  pricingType: ServicePricingType;
  status: string;
}

const DEFAULT_FORM: MenuFormState = {
  hotelId: 0,
  masterItemId: 0,
  customName: '',
  name: '',
  description: '',
  menuType: 'PRODUCT',
  basePrice: 0,
  taxCategoryId: 0,
  stockQuantity: 0,
  pricingType: 'PER_UNIT',
  status: 'ACTIVE',
};

export const MenuModal: React.FC<MenuModalProps> = ({
  isOpen,
  onClose,
  onSuccess,
  item,
  defaultHotelId,
}) => {
  const [formData, setFormData] = useState<MenuFormState>(DEFAULT_FORM);
  const [hotels, setHotels] = useState<HotelDto[]>([]);
  const [masterItems, setMasterItems] = useState<MasterItemResponse[]>([]);
  const [taxCategories, setTaxCategories] = useState<TaxCategoryDto[]>([]);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    if (!isOpen) return;

    const timer = setTimeout(() => {
      setErrorMessage(null);
      void (async () => {
        try {
          const [hotelRes, masterList, taxList] = await Promise.all([
            hotelService.filter({ pageSize: 100 }),
            operationService.getAllActiveMasterItems(),
            taxCategoryService.getAllActive(),
          ]);

          const hotelContent = hotelRes.result?.content || [];
          setHotels(hotelContent);
          setMasterItems(masterList);
          setTaxCategories(taxList);

          if (item) {
            setFormData({
              hotelId: item.hotelId || 0,
              masterItemId: item.masterItemId || 0,
              customName: item.customName || '',
              name: item.name,
              description: item.description || '',
              menuType: item.menuType,
              basePrice: item.basePrice,
              taxCategoryId: item.taxCategoryId,
              stockQuantity: item.stockQuantity || 0,
              pricingType: item.pricingType || 'PER_UNIT',
              status: item.status,
            });
          } else {
            const initialHotelId = defaultHotelId || (hotelContent.length > 0 ? hotelContent[0].id : 0);
            const initialTaxId = taxList.length > 0 ? taxList[0].id : 0;
            setFormData({
              ...DEFAULT_FORM,
              hotelId: initialHotelId,
              taxCategoryId: initialTaxId,
            });
          }
        } catch (err) {
          console.error('Failed to load menu form dependencies:', err);
        }
      })();
    }, 0);

    return () => clearTimeout(timer);
  }, [isOpen, item, defaultHotelId]);

  const handleSelectMasterItem = (masterId: number) => {
    const selected = masterItems.find((m) => m.id === masterId);
    if (!selected) {
      setFormData((prev) => ({ ...prev, masterItemId: masterId }));
      return;
    }

    setFormData((prev) => ({
      ...prev,
      masterItemId: masterId,
      name: selected.name,
      description: selected.description || '',
      menuType: selected.itemType,
      basePrice: selected.defaultBasePrice,
      taxCategoryId: selected.taxCategoryId || prev.taxCategoryId,
      pricingType: selected.pricingType || prev.pricingType,
    }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.hotelId) {
      setErrorMessage('Vui lòng chọn khách sạn cơ sở');
      return;
    }
    if (!item && !formData.masterItemId) {
      setErrorMessage('Vui lòng chọn danh mục gốc toàn chuỗi (Master Item)');
      return;
    }
    if (!formData.name.trim()) {
      setErrorMessage('Vui lòng nhập tên món ăn / dịch vụ');
      return;
    }
    if (!formData.taxCategoryId) {
      setErrorMessage('Vui lòng chọn nhóm thuế áp dụng');
      return;
    }

    setIsSubmitting(true);
    setErrorMessage(null);

    try {
      if (item) {
        const updateReq: MenuUpdateRequest = {
          customName: formData.customName.trim() || undefined,
          name: formData.name.trim(),
          description: formData.description.trim() || undefined,
          basePrice: formData.basePrice,
          taxCategoryId: formData.taxCategoryId,
          stockQuantity: formData.menuType === 'PRODUCT' ? formData.stockQuantity : undefined,
          pricingType: formData.menuType === 'SERVICE' ? formData.pricingType : undefined,
          status: formData.status,
        };
        await operationService.updateMenu(item.id, updateReq);
      } else {
        const createReq: MenuCreateRequest = {
          hotelId: formData.hotelId,
          masterItemId: formData.masterItemId,
          customName: formData.customName.trim() || undefined,
          name: formData.name.trim(),
          description: formData.description.trim() || undefined,
          menuType: formData.menuType,
          basePrice: formData.basePrice,
          taxCategoryId: formData.taxCategoryId,
          stockQuantity: formData.menuType === 'PRODUCT' ? formData.stockQuantity : undefined,
          pricingType: formData.menuType === 'SERVICE' ? formData.pricingType : undefined,
        };
        await operationService.createMenu(createReq);
      }
      onSuccess();
      onClose();
    } catch (err: unknown) {
      const errorObj = err as { response?: { data?: { message?: string } }; message?: string };
      setErrorMessage(
        errorObj.response?.data?.message || errorObj.message || 'Lưu món ăn/dịch vụ thất bại'
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  const hotelOptions = hotels.map((h) => ({
    value: h.id,
    label: `${h.name} (${h.regionName})`,
  }));

  const masterItemOptions = masterItems.map((m) => ({
    value: m.id,
    label: `[${m.code}] ${m.name} (${m.itemType === 'PRODUCT' ? 'Sản phẩm' : 'Dịch vụ'} - ${new Intl.NumberFormat('vi-VN').format(m.defaultBasePrice)} đ)`,
  }));

  const taxOptions = taxCategories.map((t) => ({
    value: t.id,
    label: `${t.categoryName} (${t.categoryCode})`,
  }));

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={item ? 'Cập nhật món / dịch vụ cơ sở' : 'Thêm mới món / dịch vụ vào thực đơn'}
      maxWidth="lg"
    >
      <form onSubmit={handleSubmit} className="space-y-4">
        {errorMessage && (
          <div className="p-3 bg-red-50 border border-red-200 text-red-700 rounded-xl text-xs">
            {errorMessage}
          </div>
        )}

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label className="block text-xs font-semibold text-neutral-700 uppercase tracking-wider mb-1.5">
              Khách sạn cơ sở áp dụng <span className="text-red-500">*</span>
            </label>
            <Select<number>
              value={formData.hotelId}
              onChange={(val) => setFormData({ ...formData, hotelId: val })}
              disabled={!!item}
              options={hotelOptions}
              placeholder="-- Chọn khách sạn cơ sở --"
              size="lg"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-neutral-700 uppercase tracking-wider mb-1.5">
              Chọn từ Danh mục chuỗi (Master Item) <span className="text-red-500">*</span>
            </label>
            <Select<number>
              value={formData.masterItemId}
              onChange={handleSelectMasterItem}
              disabled={!!item}
              options={masterItemOptions}
              placeholder="-- Chọn mục gốc chuỗi --"
              size="lg"
            />
            {item && (
              <span className="text-[11px] text-neutral-400 mt-1 block">
                Mục gốc chuỗi: {item.masterItemCode || `ID ${item.masterItemId}`}
              </span>
            )}
          </div>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label className="block text-xs font-semibold text-neutral-700 uppercase tracking-wider mb-1.5">
              Tên chuẩn danh mục <span className="text-red-500">*</span>
            </label>
            <Input
              value={formData.name}
              onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              placeholder="Tên theo danh mục chuẩn"
              required
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-neutral-700 uppercase tracking-wider mb-1.5">
              Tên tùy biến tại cơ sở (Tùy chọn)
            </label>
            <Input
              value={formData.customName}
              onChange={(e) => setFormData({ ...formData, customName: e.target.value })}
              placeholder="VD: Bia Heineken ướp lạnh, Giặt ủi nhanh..."
            />
          </div>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <div>
            <label className="block text-xs font-semibold text-neutral-700 uppercase tracking-wider mb-1.5">
              Loại thực đơn <span className="text-red-500">*</span>
            </label>
            <Select<MenuType>
              value={formData.menuType}
              onChange={(val) => setFormData({ ...formData, menuType: val })}
              disabled={true}
              options={[
                { value: 'PRODUCT', label: 'Sản phẩm vật lý (F&B / Minibar)' },
                { value: 'SERVICE', label: 'Dịch vụ' },
              ]}
              size="lg"
            />
            <span className="text-[11px] text-neutral-400 mt-1 block">
              Kế thừa từ danh mục gốc
            </span>
          </div>

          <div>
            <label className="block text-xs font-semibold text-neutral-700 uppercase tracking-wider mb-1.5">
              Đơn giá cơ sở (VNĐ) <span className="text-red-500">*</span>
            </label>
            <CurrencyInput
              value={formData.basePrice}
              onChangeValue={(val) => setFormData({ ...formData, basePrice: val })}
              placeholder="0"
              suffix="VNĐ"
              required
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-neutral-700 uppercase tracking-wider mb-1.5">
              Nhóm thuế VAT <span className="text-red-500">*</span>
            </label>
            <Select<number>
              value={formData.taxCategoryId}
              onChange={(val) => setFormData({ ...formData, taxCategoryId: val })}
              options={taxOptions}
              placeholder="-- Chọn nhóm thuế --"
              size="lg"
            />
          </div>
        </div>

        {formData.menuType === 'PRODUCT' ? (
          <div>
            <label className="block text-xs font-semibold text-neutral-700 uppercase tracking-wider mb-1.5">
              Số lượng tồn kho tại cơ sở <span className="text-red-500">*</span>
            </label>
            <Input
              type="number"
              min="0"
              value={formData.stockQuantity}
              onChange={(e) =>
                setFormData({ ...formData, stockQuantity: parseInt(e.target.value) || 0 })
              }
              required
            />
          </div>
        ) : (
          <div>
            <label className="block text-xs font-semibold text-neutral-700 uppercase tracking-wider mb-1.5">
              Hình thức tính giá dịch vụ <span className="text-red-500">*</span>
            </label>
            <Select<ServicePricingType>
              value={formData.pricingType}
              onChange={(val) => setFormData({ ...formData, pricingType: val })}
              options={[
                { value: 'PER_UNIT', label: 'Theo lượt / lần phục vụ (PER_UNIT)' },
                { value: 'PER_STAY', label: 'Theo cả chuyến lưu trú (PER_STAY)' },
                { value: 'PER_NIGHT', label: 'Theo từng đêm nghỉ (PER_NIGHT)' },
                { value: 'PER_PERSON', label: 'Theo số lượng khách (PER_PERSON)' },
                { value: 'PER_PERSON_PER_NIGHT', label: 'Khách / Đêm (PER_PERSON_PER_NIGHT)' },
                { value: 'PER_USE', label: 'Theo lần sử dụng thực tế (PER_USE)' },
              ]}
              size="lg"
            />
          </div>
        )}

        <div>
          <label className="block text-xs font-semibold text-neutral-700 uppercase tracking-wider mb-1.5">
            Mô tả phục vụ tại cơ sở
          </label>
          <textarea
            value={formData.description}
            onChange={(e) => setFormData({ ...formData, description: e.target.value })}
            rows={3}
            className="w-full p-3 border border-neutral-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-red-600 transition-colors"
            placeholder="Lưu ý phục vụ, thời gian giao món, điều kiện áp dụng tại khách sạn..."
          />
        </div>

        {item && (
          <div>
            <label className="block text-xs font-semibold text-neutral-700 uppercase tracking-wider mb-1.5">
              Trạng thái phục vụ
            </label>
            <Select<string>
              value={formData.status}
              onChange={(val) => setFormData({ ...formData, status: val })}
              options={[
                { value: 'ACTIVE', label: 'Sẵn sàng phục vụ' },
                { value: 'INACTIVE', label: 'Tạm dừng phục vụ' },
              ]}
              size="lg"
            />
          </div>
        )}

        <div className="flex justify-end gap-3 pt-4 border-t border-neutral-200">
          <Button variant="secondary" type="button" onClick={onClose}>
            Hủy
          </Button>
          <Button type="submit" disabled={isSubmitting} className="flex items-center gap-2">
            {formData.menuType === 'PRODUCT' ? (
              <Package className="w-4 h-4" />
            ) : (
              <Sparkles className="w-4 h-4" />
            )}
            {isSubmitting ? 'Đang lưu...' : item ? 'Lưu thay đổi' : 'Thêm vào thực đơn'}
          </Button>
        </div>
      </form>
    </Modal>
  );
};

export default MenuModal;
