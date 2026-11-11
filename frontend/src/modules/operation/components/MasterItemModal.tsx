import React, { useState, useEffect } from 'react';
import { Package, Sparkles } from 'lucide-react';
import Button from '../../../core/components/ui/Button';
import Input from '../../../core/components/ui/Input';
import CurrencyInput from '../../../core/components/ui/CurrencyInput';
import Select from '../../../core/components/ui/Select';
import Modal from '../../../core/components/ui/Modal';
import UnitSelectInput from './UnitSelectInput';
import { operationService } from '../services/operation.service';
import taxCategoryService from '../../pricing/services/taxCategory.service';
import type {
  MasterItemResponse,
  MasterItemCreateRequest,
  MasterItemUpdateRequest,
  MenuType,
  ServicePricingType,
} from '../types/operation.types';
import type { TaxCategoryDto } from '../../pricing/types/pricing.types';

interface MasterItemModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
  item: MasterItemResponse | null;
}

interface FormState {
  code: string;
  name: string;
  description: string;
  itemType: MenuType;
  defaultBasePrice: number;
  taxCategoryId: number;
  pricingType: ServicePricingType;
  unit: string;
  status: string;
}

const DEFAULT_FORM: FormState = {
  code: '',
  name: '',
  description: '',
  itemType: 'PRODUCT',
  defaultBasePrice: 0,
  taxCategoryId: 0,
  pricingType: 'PER_UNIT',
  unit: '',
  status: 'ACTIVE',
};

export const MasterItemModal: React.FC<MasterItemModalProps> = ({
  isOpen,
  onClose,
  onSuccess,
  item,
}) => {
  const [formData, setFormData] = useState<FormState>(DEFAULT_FORM);
  const [taxCategories, setTaxCategories] = useState<TaxCategoryDto[]>([]);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    if (!isOpen) return;

    const timer = setTimeout(() => {
      setErrorMessage(null);
      void (async () => {
        try {
          const taxes = await taxCategoryService.getAllActive();
          setTaxCategories(taxes);

          if (item) {
            setFormData({
              code: item.code,
              name: item.name,
              description: item.description || '',
              itemType: item.itemType,
              defaultBasePrice: item.defaultBasePrice,
              taxCategoryId: item.taxCategoryId,
              pricingType: item.pricingType || 'PER_UNIT',
              unit: item.unit || '',
              status: item.status,
            });
          } else {
            setFormData({
              ...DEFAULT_FORM,
              taxCategoryId: taxes.length > 0 ? taxes[0].id : 0,
            });
          }
        } catch (err) {
          console.error('Failed to load tax categories:', err);
        }
      })();
    }, 0);

    return () => clearTimeout(timer);
  }, [isOpen, item]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.name.trim()) {
      setErrorMessage('Vui lòng nhập tên danh mục');
      return;
    }
    if (!item && !formData.code.trim()) {
      setErrorMessage('Vui lòng nhập mã danh mục duy nhất');
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
        const updateReq: MasterItemUpdateRequest = {
          name: formData.name.trim(),
          description: formData.description.trim() || undefined,
          defaultBasePrice: formData.defaultBasePrice,
          taxCategoryId: formData.taxCategoryId,
          pricingType: formData.itemType === 'SERVICE' ? formData.pricingType : undefined,
          unit: formData.unit.trim().toUpperCase() || undefined,
          status: formData.status,
        };
        await operationService.updateMasterItem(item.id, updateReq);
      } else {
        const createReq: MasterItemCreateRequest = {
          code: formData.code.trim().toUpperCase(),
          name: formData.name.trim(),
          description: formData.description.trim() || undefined,
          itemType: formData.itemType,
          defaultBasePrice: formData.defaultBasePrice,
          taxCategoryId: formData.taxCategoryId,
          pricingType: formData.itemType === 'SERVICE' ? formData.pricingType : undefined,
          unit: formData.unit.trim().toUpperCase() || undefined,
          status: formData.status,
        };
        await operationService.createMasterItem(createReq);
      }
      onSuccess();
      onClose();
    } catch (err: unknown) {
      const errorObj = err as { response?: { data?: { message?: string } }; message?: string };
      setErrorMessage(
        errorObj.response?.data?.message || errorObj.message || 'Lưu danh mục gốc chuỗi thất bại'
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  const taxOptions = taxCategories.map((t) => ({
    value: t.id,
    label: `${t.categoryName} (${t.categoryCode})`,
  }));

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={item ? 'Cập nhật danh mục gốc chuỗi (Master Item)' : 'Thêm mới danh mục gốc chuỗi (Master Item)'}
      maxWidth="2xl"
    >
      <form onSubmit={handleSubmit} className="space-y-4">
        {errorMessage && (
          <div className="p-3 bg-red-50 border border-red-200 text-red-700 rounded-xl text-xs">
            {errorMessage}
          </div>
        )}

        {/* Hàng 1: Mã danh mục + Loại danh mục */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label className="block text-xs font-bold text-neutral-700 uppercase tracking-wider h-5 flex items-center mb-1.5">
              Mã danh mục gốc <span className="text-red-500 ml-1">*</span>
            </label>
            <Input
              value={formData.code}
              onChange={(e) => setFormData({ ...formData, code: e.target.value.toUpperCase() })}
              placeholder="VD: COFFEE_DEN, LAUNDRY_SHIRT"
              disabled={!!item}
              className="h-11 font-mono uppercase text-sm"
              required
            />
          </div>

          <div>
            <label className="block text-xs font-bold text-neutral-700 uppercase tracking-wider h-5 flex items-center mb-1.5">
              Loại danh mục <span className="text-red-500 ml-1">*</span>
            </label>
            <Select<MenuType>
              value={formData.itemType}
              onChange={(val) => setFormData({ ...formData, itemType: val })}
              disabled={!!item}
              options={[
                { value: 'PRODUCT', label: 'Sản phẩm vật lý (F&B / Minibar)' },
                { value: 'SERVICE', label: 'Dịch vụ (Giặt là, Spa, Xe...)' },
              ]}
              size="md"
            />
          </div>
        </div>

        {/* Hàng 2: Tên sản phẩm / dịch vụ */}
        <div>
          <label className="block text-xs font-bold text-neutral-700 uppercase tracking-wider h-5 flex items-center mb-1.5">
            Tên sản phẩm / dịch vụ <span className="text-red-500 ml-1">*</span>
          </label>
          <Input
            value={formData.name}
            onChange={(e) => setFormData({ ...formData, name: e.target.value })}
            placeholder="VD: Cà phê đen Sài Gòn, Giặt ủi áo sơ mi cao cấp..."
            className="h-11 text-sm"
            required
          />
        </div>

        {/* Hàng 3: Đơn giá đề xuất + Nhóm thuế VAT */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label className="block text-xs font-bold text-neutral-700 uppercase tracking-wider h-5 flex items-center mb-1.5">
              Giá đề xuất chuỗi (VNĐ) <span className="text-red-500 ml-1">*</span>
            </label>
            <CurrencyInput
              value={formData.defaultBasePrice}
              onChangeValue={(val) => setFormData({ ...formData, defaultBasePrice: val })}
              placeholder="0"
              suffix="VNĐ"
              className="h-11 text-sm font-semibold"
              required
            />
          </div>

          <div>
            <label className="block text-xs font-bold text-neutral-700 uppercase tracking-wider h-5 flex items-center mb-1.5">
              Nhóm thuế VAT <span className="text-red-500 ml-1">*</span>
            </label>
            <Select<number>
              value={formData.taxCategoryId}
              onChange={(val) => setFormData({ ...formData, taxCategoryId: val })}
              options={taxOptions}
              placeholder="-- Chọn nhóm thuế --"
              size="md"
            />
          </div>
        </div>

        {/* Hàng 4: Đơn vị tính + Trạng thái hoặc Hình thức tính giá */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label className="block text-xs font-bold text-neutral-700 uppercase tracking-wider h-5 flex items-center mb-1.5">
              Đơn vị tính chuẩn hóa
            </label>
            <UnitSelectInput
              value={formData.unit}
              onChange={(val) => setFormData({ ...formData, unit: val })}
              itemType={formData.itemType}
            />
          </div>

          {formData.itemType === 'SERVICE' ? (
            <div>
              <label className="block text-xs font-bold text-neutral-700 uppercase tracking-wider h-5 flex items-center mb-1.5">
                Cách tính giá dịch vụ <span className="text-red-500 ml-1">*</span>
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
                size="md"
              />
            </div>
          ) : (
            <div>
              <label className="block text-xs font-bold text-neutral-700 uppercase tracking-wider h-5 flex items-center mb-1.5">
                Trạng thái danh mục
              </label>
              <Select<string>
                value={formData.status}
                onChange={(val) => setFormData({ ...formData, status: val })}
                options={[
                  { value: 'ACTIVE', label: 'Đang hoạt động (ACTIVE)' },
                  { value: 'INACTIVE', label: 'Tạm ngưng (INACTIVE)' },
                ]}
                size="md"
              />
            </div>
          )}
        </div>

        {/* Trạng thái nếu là SERVICE */}
        {formData.itemType === 'SERVICE' && (
          <div>
            <label className="block text-xs font-bold text-neutral-700 uppercase tracking-wider h-5 flex items-center mb-1.5">
              Trạng thái danh mục
            </label>
            <Select<string>
              value={formData.status}
              onChange={(val) => setFormData({ ...formData, status: val })}
              options={[
                { value: 'ACTIVE', label: 'Đang hoạt động (ACTIVE)' },
                { value: 'INACTIVE', label: 'Tạm ngưng (INACTIVE)' },
              ]}
              size="md"
            />
          </div>
        )}

        {/* Mô tả */}
        <div>
          <label className="block text-xs font-bold text-neutral-700 uppercase tracking-wider h-5 flex items-center mb-1.5">
            Mô tả tiêu chuẩn danh mục
          </label>
          <textarea
            value={formData.description}
            onChange={(e) => setFormData({ ...formData, description: e.target.value })}
            rows={3}
            className="w-full p-3 border border-neutral-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-red-600 transition-colors placeholder:text-neutral-400"
            placeholder="Mô tả công thức, quy chuẩn phục vụ, thành phần áp dụng chung toàn chuỗi..."
          />
        </div>

        {/* Footer Buttons */}
        <div className="flex justify-end gap-3 pt-4 border-t border-neutral-200">
          <Button variant="secondary" type="button" onClick={onClose} className="h-11 px-5">
            Hủy
          </Button>
          <Button type="submit" disabled={isSubmitting} className="h-11 px-6 flex items-center gap-2">
            {formData.itemType === 'PRODUCT' ? (
              <Package className="w-4 h-4" />
            ) : (
              <Sparkles className="w-4 h-4" />
            )}
            {isSubmitting ? 'Đang lưu...' : item ? 'Lưu thay đổi' : 'Tạo danh mục gốc'}
          </Button>
        </div>
      </form>
    </Modal>
  );
};

export default MasterItemModal;
