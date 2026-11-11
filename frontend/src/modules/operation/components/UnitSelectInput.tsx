import React, { useState } from 'react';
import { RotateCcw } from 'lucide-react';
import Select from '../../../core/components/ui/Select';
import Input from '../../../core/components/ui/Input';
import type { MenuType } from '../types/operation.types';

interface UnitSelectInputProps {
  value: string;
  onChange: (val: string) => void;
  itemType: MenuType;
  error?: string;
  disabled?: boolean;
}

const PRODUCT_PRESETS = [
  { value: 'LON', label: 'LON — Lon nước, đồ uống' },
  { value: 'CHAI', label: 'CHAI — Chai thủy tinh / nhựa' },
  { value: 'LY', label: 'LY — Ly / Cốc đồ uống' },
  { value: 'DIA', label: 'DĨA — Dĩa / Đĩa thức ăn' },
  { value: 'PHAN', label: 'PHẦN — Phần / Suất ăn' },
  { value: 'GOI', label: 'GÓI — Gói / Bịch đồ ăn nhẹ' },
  { value: 'HOP', label: 'HỘP — Hộp / Khay đồ ăn' },
  { value: 'KG', label: 'KG — Kilogram' },
  { value: 'CUSTOM', label: '➕ KHÁC (Nhập đơn vị riêng in hoa...)' },
];

const SERVICE_PRESETS = [
  { value: 'LUOT', label: 'LƯỢT — Lượt phục vụ' },
  { value: 'LAN', label: 'LẦN — Lần gọi / thực hiện' },
  { value: 'GIO', label: 'GIỜ — Giờ phục vụ' },
  { value: 'BO', label: 'BỘ — Bộ đồ / Set giặt ủi' },
  { value: 'VE', label: 'VÉ — Vé dịch vụ / tiện ích' },
  { value: 'CHUYEN', label: 'CHUYẾN — Chuyến xe đưa đón' },
  { value: 'NGAY', label: 'NGÀY — Ngày thuê thiết bị' },
  { value: 'CUSTOM', label: '➕ KHÁC (Nhập đơn vị riêng in hoa...)' },
];

export const UnitSelectInput: React.FC<UnitSelectInputProps> = ({
  value,
  onChange,
  itemType,
  error,
  disabled = false,
}) => {
  const presets = itemType === 'PRODUCT' ? PRODUCT_PRESETS : SERVICE_PRESETS;
  const isPredefined = Boolean(value) && presets.some((p) => p.value !== 'CUSTOM' && p.value === value);

  // Track if user explicitly clicked CUSTOM in the dropdown
  const [userForcedCustom, setUserForcedCustom] = useState(false);

  const isCustomMode = !isPredefined && (Boolean(value) || userForcedCustom);
  const selectedMode = isPredefined ? value : (isCustomMode ? 'CUSTOM' : '');

  const handleSelectMode = (val: string) => {
    if (val === 'CUSTOM') {
      setUserForcedCustom(true);
      onChange('');
    } else {
      setUserForcedCustom(false);
      onChange(val);
    }
  };

  const handleCustomTextChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    // Luôn luôn in hoa theo yêu cầu người dùng: "phần nhập đơn vị riêng in hoa hết, không nhập thường"
    const upperText = e.target.value.toUpperCase();
    onChange(upperText);
  };

  const handleBackToPresets = () => {
    setUserForcedCustom(false);
    onChange('');
  };

  const options = [
    { value: '', label: '-- Chọn đơn vị chuẩn hóa --' },
    ...presets,
  ];

  // Khi ở chế độ nhập riêng (CUSTOM): Render duy nhất 1 ô input h-11 chuẩn để đảm bảo
  // chiều cao 2 cột luôn bằng nhau tuyệt đối, tích hợp nút "Gợi ý" để quay lại danh sách
  if (isCustomMode) {
    return (
      <div className="relative w-full">
        <Input
          value={value || ''}
          onChange={handleCustomTextChange}
          placeholder="NHẬP ĐƠN VỊ TÍNH (IN HOA: THÙNG, CÂY...)"
          disabled={disabled}
          className="h-11 uppercase font-semibold text-neutral-900 tracking-wide pr-20"
          maxLength={50}
          autoFocus
          error={error}
        />
        <button
          type="button"
          onClick={handleBackToPresets}
          className="absolute right-1.5 top-1.5 bottom-1.5 px-2.5 bg-neutral-100 hover:bg-neutral-200 text-neutral-700 rounded-lg text-xs font-semibold flex items-center gap-1 transition-colors cursor-pointer"
          title="Chọn lại từ danh sách gợi ý"
        >
          <RotateCcw className="w-3.5 h-3.5 text-neutral-500" />
          <span>Gợi ý</span>
        </button>
      </div>
    );
  }

  return (
    <Select<string>
      value={selectedMode}
      onChange={handleSelectMode}
      disabled={disabled}
      options={options}
      placeholder="-- Chọn đơn vị chuẩn hóa --"
      size="md"
      direction="up"
      error={error}
    />
  );
};

export default UnitSelectInput;
