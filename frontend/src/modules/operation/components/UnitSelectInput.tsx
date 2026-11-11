import React, { useState } from 'react';
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

  const options = [
    { value: '', label: '-- Chọn đơn vị chuẩn hóa --' },
    ...presets,
  ];

  return (
    <div className="space-y-2">
      <Select<string>
        value={selectedMode}
        onChange={handleSelectMode}
        disabled={disabled}
        options={options}
        placeholder="-- Chọn đơn vị chuẩn hóa --"
        size="md"
        error={error}
      />

      {isCustomMode && (
        <div className="animate-in fade-in duration-200">
          <Input
            value={value || ''}
            onChange={handleCustomTextChange}
            placeholder="NHẬP ĐƠN VỊ TÍNH (IN HOA: THÙNG, THỐ, CÂY...)"
            disabled={disabled}
            className="h-11 uppercase font-semibold text-neutral-900 tracking-wide"
            maxLength={50}
          />
          <span className="text-[11px] text-neutral-400 mt-1 block">
            Hệ thống tự động chuyển in hoa toàn bộ ký tự đơn vị tính riêng
          </span>
        </div>
      )}
    </div>
  );
};

export default UnitSelectInput;
