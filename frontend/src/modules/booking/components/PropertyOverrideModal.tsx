import React, { useState, useEffect, useCallback } from 'react';
import { AlertCircle, Building2, BedDouble } from 'lucide-react';
import Modal from '../../../core/components/ui/Modal';
import Button from '../../../core/components/ui/Button';
import Input from '../../../core/components/ui/Input';
import { Select } from '../../../core/components/ui/Select';
import policyService from '../services/policy.service';
import hotelService from '../../organization/services/hotel.service';
import hotelRoomTypeService from '../../inventory/services/hotelRoomType.service';
import type {
  PropertyPolicyOverrideResponse,
  CancellationPolicyResponse,
  NoShowPolicyResponse,
} from '../types/policy.types';
import type { HotelDto } from '../../organization/types/organization.types';
import type { HotelRoomTypeDto } from '../../inventory/types/inventory.types';

interface PropertyOverrideModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
  override: PropertyPolicyOverrideResponse | null;
}

export const PropertyOverrideModal: React.FC<PropertyOverrideModalProps> = ({
  isOpen,
  onClose,
  onSuccess,
  override,
}) => {
  const isEdit = !!override;
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  // Reference data
  const [hotels, setHotels] = useState<HotelDto[]>([]);
  const [isHotelsLoading, setIsHotelsLoading] = useState(false);

  const [roomTypes, setRoomTypes] = useState<HotelRoomTypeDto[]>([]);
  const [isRoomTypesLoading, setIsRoomTypesLoading] = useState(false);

  const [cancellationPolicies, setCancellationPolicies] = useState<CancellationPolicyResponse[]>([]);
  const [isCancelLoading, setIsCancelLoading] = useState(false);

  const [noShowPolicies, setNoShowPolicies] = useState<NoShowPolicyResponse[]>([]);
  const [isNoShowLoading, setIsNoShowLoading] = useState(false);

  // Form State
  const [selectedHotelId, setSelectedHotelId] = useState<number | ''>('');
  const [selectedRoomTypeId, setSelectedRoomTypeId] = useState<number | ''>('');
  const [selectedCancelPolicyId, setSelectedCancelPolicyId] = useState<number | ''>('');
  const [selectedNoShowPolicyId, setSelectedNoShowPolicyId] = useState<number | ''>('');
  const [freeDaysOverride, setFreeDaysOverride] = useState<string>('');
  const [releaseTimeOverride, setReleaseTimeOverride] = useState<string>('');
  const [overrideReason, setOverrideReason] = useState('');
  const [isActive, setIsActive] = useState(true);

  // Lazy loaders for onOpen
  const loadHotels = useCallback(async () => {
    if (hotels.length > 0 || isHotelsLoading) return;
    setIsHotelsLoading(true);
    try {
      const res = await hotelService.filter({ page: 0, pageSize: 100 });
      if (res.result?.content) setHotels(res.result.content);
    } catch (err) {
      console.error('Lỗi tải danh mục khách sạn', err);
    } finally {
      setIsHotelsLoading(false);
    }
  }, [hotels.length, isHotelsLoading]);

  const loadRoomTypes = useCallback(async () => {
    if (!selectedHotelId || isRoomTypesLoading) return;
    setIsRoomTypesLoading(true);
    try {
      const res = await hotelRoomTypeService.filter({
        hotelId: Number(selectedHotelId),
        page: 0,
        pageSize: 100,
      });
      if (res.result?.content) setRoomTypes(res.result.content);
    } catch (err) {
      console.error('Lỗi tải danh sách hạng phòng', err);
    } finally {
      setIsRoomTypesLoading(false);
    }
  }, [selectedHotelId, isRoomTypesLoading]);

  const loadCancelPolicies = useCallback(async () => {
    if (cancellationPolicies.length > 0 || isCancelLoading) return;
    setIsCancelLoading(true);
    try {
      const res = await policyService.filterCancellationPolicies({ page: 0, pageSize: 100, isActive: true });
      if (res.result?.content) setCancellationPolicies(res.result.content);
    } catch (err) {
      console.error('Lỗi tải chính sách hủy', err);
    } finally {
      setIsCancelLoading(false);
    }
  }, [cancellationPolicies.length, isCancelLoading]);

  const loadNoShowPolicies = useCallback(async () => {
    if (noShowPolicies.length > 0 || isNoShowLoading) return;
    setIsNoShowLoading(true);
    try {
      const res = await policyService.filterNoShowPolicies({ page: 0, pageSize: 100, isActive: true });
      if (res.result?.content) setNoShowPolicies(res.result.content);
    } catch (err) {
      console.error('Lỗi tải chính sách no-show', err);
    } finally {
      setIsNoShowLoading(false);
    }
  }, [noShowPolicies.length, isNoShowLoading]);

  // When editing, load relevant options to display labels properly
  useEffect(() => {
    if (!isOpen || !isEdit) return;
    let ignore = false;
    Promise.resolve().then(() => {
      if (ignore) return;
      void loadHotels();
      void loadCancelPolicies();
      void loadNoShowPolicies();
      if (override?.hotelId) {
        hotelRoomTypeService.filter({
          hotelId: Number(override.hotelId),
          page: 0,
          pageSize: 100,
        }).then((res) => {
          if (!ignore && res.result?.content) setRoomTypes(res.result.content);
        }).catch(console.error);
      }
    });
    return () => {
      ignore = true;
    };
  }, [isOpen, isEdit, override?.hotelId, loadHotels, loadCancelPolicies, loadNoShowPolicies]);

  // Sync form with edit target
  useEffect(() => {
    const timer = setTimeout(() => {
      if (override) {
        setSelectedHotelId(override.hotelId);
        setSelectedRoomTypeId(override.hotelRoomTypeId || '');
        setSelectedCancelPolicyId(override.cancellationPolicyId || '');
        setSelectedNoShowPolicyId(override.noShowPolicyId || '');
        setFreeDaysOverride(
          override.freeCancellationBeforeDaysOverride !== null && override.freeCancellationBeforeDaysOverride !== undefined
            ? String(override.freeCancellationBeforeDaysOverride)
            : ''
        );
        setReleaseTimeOverride(override.noShowReleaseTimeOverride ? override.noShowReleaseTimeOverride.substring(0, 5) : '');
        setOverrideReason(override.overrideReason || '');
        setIsActive(override.isActive);
      } else {
        setSelectedHotelId('');
        setSelectedRoomTypeId('');
        setSelectedCancelPolicyId('');
        setSelectedNoShowPolicyId('');
        setFreeDaysOverride('');
        setReleaseTimeOverride('');
        setOverrideReason('');
        setIsActive(true);
      }
      setErrorMsg(null);
    }, 0);
    return () => clearTimeout(timer);
  }, [override, isOpen]);

  // Selected cancellation policy inspection
  const selectedCancelPolicy = cancellationPolicies.find((p) => p.id === Number(selectedCancelPolicyId));
  const isSelectedTiered = selectedCancelPolicy?.cancellationFeeType === 'TIERED';

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedHotelId) {
      setErrorMsg('Vui lòng chọn khách sạn cơ sở');
      return;
    }
    if (!overrideReason.trim()) {
      setErrorMsg('Vui lòng nhập lý do ghi đè chính sách');
      return;
    }
    if (!selectedCancelPolicyId && !selectedNoShowPolicyId && !freeDaysOverride && !releaseTimeOverride) {
      setErrorMsg('Vui lòng chọn ít nhất một chính sách hoặc thông số ghi đè');
      return;
    }
    if (isSelectedTiered && freeDaysOverride) {
      setErrorMsg('Chính sách loại TIERED không cho phép ghi đè số ngày miễn phí hủy phòng (tránh mâu thuẫn tiers)');
      return;
    }

    try {
      setIsSubmitting(true);
      setErrorMsg(null);

      const formattedReleaseTime = releaseTimeOverride
        ? releaseTimeOverride.length === 5
          ? `${releaseTimeOverride}:00`
          : releaseTimeOverride
        : null;

      if (isEdit && override) {
        await policyService.updatePropertyOverride(override.id, {
          cancellationPolicyId: selectedCancelPolicyId ? Number(selectedCancelPolicyId) : null,
          noShowPolicyId: selectedNoShowPolicyId ? Number(selectedNoShowPolicyId) : null,
          freeCancellationBeforeDaysOverride: freeDaysOverride ? Number(freeDaysOverride) : null,
          noShowReleaseTimeOverride: formattedReleaseTime,
          overrideReason: overrideReason.trim(),
          isActive,
        });
      } else {
        await policyService.createPropertyOverride({
          hotelId: Number(selectedHotelId),
          hotelRoomTypeId: selectedRoomTypeId ? Number(selectedRoomTypeId) : null,
          cancellationPolicyId: selectedCancelPolicyId ? Number(selectedCancelPolicyId) : null,
          noShowPolicyId: selectedNoShowPolicyId ? Number(selectedNoShowPolicyId) : null,
          freeCancellationBeforeDaysOverride: freeDaysOverride ? Number(freeDaysOverride) : null,
          noShowReleaseTimeOverride: formattedReleaseTime,
          overrideReason: overrideReason.trim(),
          isActive,
        });
      }

      onSuccess();
      onClose();
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message || 'Có lỗi xảy ra khi lưu ghi đè chính sách';
      setErrorMsg(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={isEdit ? `Cập nhật Ghi đè: #${override?.id} - ${override?.hotelName}` : 'Tạo mới Cấu hình Ghi đè Chính sách'}
      description="Cho phép khách sạn cơ sở hoặc từng hạng phòng đặc thù tùy biến chính sách so với mặc định chuỗi"
      maxWidth="2xl"
    >
      <form onSubmit={handleSubmit} className="space-y-4">
        {errorMsg && (
          <div className="p-3 bg-red-50 border border-red-200 rounded-xl text-red-700 text-sm flex items-center gap-2">
            <AlertCircle className="w-5 h-5 shrink-0" />
            <span>{errorMsg}</span>
          </div>
        )}

        {/* Khách sạn & Hạng phòng */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-semibold text-neutral-800 mb-1 flex items-center gap-1.5">
              <Building2 className="w-4 h-4 text-red-600" />
              Khách Sạn Cơ Sở <span className="text-red-500">*</span>
            </label>
            <Select<number | ''>
              value={selectedHotelId}
              onChange={(val) => {
                setSelectedHotelId(val);
                setSelectedRoomTypeId('');
                setRoomTypes([]);
              }}
              onOpen={loadHotels}
              isLoading={isHotelsLoading}
              disabled={isEdit}
              placeholder="Chọn khách sạn cơ sở"
              options={[
                { value: '', label: 'Chọn khách sạn cơ sở' },
                ...hotels.map((h) => ({
                  value: h.id,
                  label: `${h.name} (#${h.id})`,
                })),
              ]}
              prefixIcon={<Building2 className="w-4 h-4 text-red-600" />}
            />
          </div>

          <div>
            <label className="block text-sm font-semibold text-neutral-800 mb-1 flex items-center gap-1.5">
              <BedDouble className="w-4 h-4 text-neutral-600" />
              Hạng Phòng (Tùy chọn)
            </label>
            <Select<number | ''>
              value={selectedRoomTypeId}
              onChange={(val) => setSelectedRoomTypeId(val)}
              onOpen={loadRoomTypes}
              isLoading={isRoomTypesLoading}
              disabled={isEdit || !selectedHotelId}
              placeholder="Áp dụng cho toàn bộ khách sạn"
              options={[
                { value: '', label: 'Áp dụng cho toàn bộ khách sạn' },
                ...roomTypes.map((rt) => ({
                  value: rt.id,
                  label: rt.roomTypeName || `Hạng phòng #${rt.id}`,
                })),
              ]}
              prefixIcon={<BedDouble className="w-4 h-4 text-neutral-600" />}
            />
            <p className="text-xs text-neutral-500 mt-1">
              Để trống nếu muốn áp dụng chung cho mọi phòng của khách sạn này.
            </p>
          </div>
        </div>

        {/* Chính sách gán */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-semibold text-neutral-800 mb-1">
              Chính Sách Hủy Phòng Gán Mới
            </label>
            <Select<number | ''>
              value={selectedCancelPolicyId}
              onChange={(val) => {
                setSelectedCancelPolicyId(val);
                const policyObj = cancellationPolicies.find((p) => p.id === val);
                if (policyObj?.cancellationFeeType === 'TIERED') {
                  setFreeDaysOverride('');
                }
              }}
              onOpen={loadCancelPolicies}
              isLoading={isCancelLoading}
              placeholder="Mặc định chuỗi"
              options={[
                { value: '', label: 'Mặc định chuỗi' },
                ...cancellationPolicies.map((p) => ({
                  value: p.id,
                  label: `${p.policyCode} - ${p.name} (${p.cancellationFeeType})`,
                })),
              ]}
            />
          </div>

          <div>
            <label className="block text-sm font-semibold text-neutral-800 mb-1">
              Chính Sách No-Show Gán Mới
            </label>
            <Select<number | ''>
              value={selectedNoShowPolicyId}
              onChange={(val) => setSelectedNoShowPolicyId(val)}
              onOpen={loadNoShowPolicies}
              isLoading={isNoShowLoading}
              placeholder="Mặc định chuỗi"
              options={[
                { value: '', label: 'Mặc định chuỗi' },
                ...noShowPolicies.map((p) => ({
                  value: p.id,
                  label: `${p.policyCode} - ${p.name}`,
                })),
              ]}
            />
          </div>
        </div>

        {/* Ghi đè thông số đơn lẻ */}
        <div className="border border-neutral-200 rounded-xl p-4 bg-neutral-50/50 space-y-3">
          <h4 className="text-sm font-bold text-neutral-900">Tùy biến thông số chi tiết</h4>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-neutral-700 mb-1">
                Ghi đè số ngày miễn phí hủy (ngày)
              </label>
              <Input
                type="number"
                min={0}
                placeholder="VD: 3"
                value={freeDaysOverride}
                onChange={(e) => setFreeDaysOverride(e.target.value)}
                disabled={isSelectedTiered}
              />
              {isSelectedTiered && (
                <p className="text-xs text-amber-600 mt-1">
                  Chính sách TIERED không cho phép ghi đè số ngày miễn phí (dùng tiers trực tiếp).
                </p>
              )}
            </div>

            <div>
              <label className="block text-xs font-semibold text-neutral-700 mb-1">
                Ghi đè giờ giải phóng phòng No-Show
              </label>
              <Input
                type="time"
                value={releaseTimeOverride}
                onChange={(e) => setReleaseTimeOverride(e.target.value)}
              />
              <p className="text-xs text-neutral-500 mt-1">
                Để trống nếu dùng giờ mặc định của chính sách No-Show.
              </p>
            </div>
          </div>
        </div>

        <div>
          <label className="block text-sm font-semibold text-neutral-800 mb-1">
            Lý Do Ghi Đè <span className="text-red-500">*</span>
          </label>
          <Input
            value={overrideReason}
            onChange={(e) => setOverrideReason(e.target.value)}
            placeholder="VD: Khách sạn mùa cao điểm du lịch biển cần thu cọc nghiêm ngặt hơn"
            required
          />
        </div>

        <div className="flex items-center gap-2 pt-2">
          <input
            type="checkbox"
            id="override-active"
            checked={isActive}
            onChange={(e) => setIsActive(e.target.checked)}
            className="w-4 h-4 text-red-600 rounded border-neutral-300 focus:ring-red-500"
          />
          <label htmlFor="override-active" className="text-sm font-medium text-neutral-700 cursor-pointer">
            Kích hoạt cấu hình ghi đè này
          </label>
        </div>

        <div className="flex justify-end gap-3 pt-4 border-t border-neutral-100">
          <Button type="button" variant="secondary" onClick={onClose} disabled={isSubmitting}>
            Hủy
          </Button>
          <Button type="submit" variant="primary" isLoading={isSubmitting}>
            {isEdit ? 'Lưu thay đổi' : 'Tạo ghi đè'}
          </Button>
        </div>
      </form>
    </Modal>
  );
};

export default PropertyOverrideModal;
