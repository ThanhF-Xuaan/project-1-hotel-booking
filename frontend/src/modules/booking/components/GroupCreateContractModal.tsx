import React, { useState, useEffect, useCallback } from 'react';
import {
  X,
  Building2,
  Calendar,
  Users,
  BedDouble,
  DollarSign,
  AlertCircle,
  Plus,
  Trash2,
  Loader2,
  Search,
  PenTool,
} from 'lucide-react';
import { groupBookingService, type CompanyOption } from '../services/groupBooking.service';
import { hotelService } from '../../organization/services/hotel.service';
import { staffService, type StaffDto } from '../../organization/services/staff.service';
import { hotelRoomTypeService } from '../../inventory/services/hotelRoomType.service';
import type { HotelRoomTypeDto } from '../../inventory/types/inventory.types';
import type { GroupContractCreateRequest } from '../types/groupBooking.types';
import { Select } from '../../../core/components/ui/Select';

interface GroupCreateContractModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (dto: GroupContractCreateRequest, staffId?: number) => Promise<void>;
  hotelList?: { id: number; name: string }[];
}

interface RoomTypeItem {
  id: number;
  name: string;
  basePrice: number;
}

export const GroupCreateContractModal: React.FC<GroupCreateContractModalProps> = ({
  isOpen,
  onClose,
  onSubmit,
  hotelList,
}) => {
  // Mode selection: NEW (nhập mới trực tiếp - Cách 2) | EXISTING (chọn từ danh bạ có sẵn)
  const [companyMode, setCompanyMode] = useState<'NEW' | 'EXISTING'>('NEW');

  // New company form state (Cách 2)
  const [newCompanyName, setNewCompanyName] = useState('');
  const [newCompanyTaxCode, setNewCompanyTaxCode] = useState('');
  const [newCompanyAddress, setNewCompanyAddress] = useState('');
  const [newContactPhone, setNewContactPhone] = useState('');
  const [newContactEmail, setNewContactEmail] = useState('');

  // Existing companies state
  const [companies, setCompanies] = useState<CompanyOption[]>([]);
  const [selectedCompanyId, setSelectedCompanyId] = useState<number>(0);
  const [isLoadingCompanies, setIsLoadingCompanies] = useState(false);

  // Common contract fields
  const [fetchedHotels, setFetchedHotels] = useState<{ id: number; name: string }[]>([]);
  const [isLoadingHotels, setIsLoadingHotels] = useState(false);
  const displayHotels = fetchedHotels.length > 0 ? fetchedHotels : (hotelList && hotelList.length > 0 ? hotelList : []);
  const [hotelId, setHotelId] = useState<number>(0);
  const effectiveHotelId = displayHotels.some((h) => h.id === hotelId) ? hotelId : (displayHotels[0]?.id || 1);
  const [availableRoomTypes, setAvailableRoomTypes] = useState<RoomTypeItem[]>([]);
  const [isLoadingRoomTypes, setIsLoadingRoomTypes] = useState(false);

  // Staff / Employee handling
  const [staffs, setStaffs] = useState<StaffDto[]>([]);
  const [selectedStaffId, setSelectedStaffId] = useState<number | ''>('');
  const [isLoadingStaffs, setIsLoadingStaffs] = useState(false);

  // Lazy fetch staff list from DB on user click
  const loadStaffs = useCallback(async () => {
    if (staffs.length > 0 || isLoadingStaffs) return;
    setIsLoadingStaffs(true);
    try {
      const res = await staffService.filter({ page: 0, pageSize: 100 });
      const list = res.result?.content || [];
      setStaffs(list);
      if (list.length > 0 && !selectedStaffId) {
        setSelectedStaffId(list[0].id);
      }
    } catch (err: unknown) {
      console.warn('Lỗi khi tải danh sách nhân viên từ DB:', err);
    } finally {
      setIsLoadingStaffs(false);
    }
  }, [staffs.length, isLoadingStaffs, selectedStaffId]);

  // Lazy fetch hotel list when user clicks on "Khách sạn tiếp nhận" dropdown
  const loadHotels = useCallback(async () => {
    if (displayHotels.length > 0 || isLoadingHotels) return;
    setIsLoadingHotels(true);
    try {
      const res = await hotelService.filter({ page: 0, pageSize: 100 });
      const list = (res.result?.content || []).map((h) => ({
        id: h.id,
        name: h.name || `Khách sạn #${h.id}`,
      }));
      setFetchedHotels(list);
      if (list.length > 0 && !hotelId) {
        setHotelId(list[0].id);
      }
    } catch (err: unknown) {
      console.warn('Lỗi khi tải danh sách khách sạn tiếp nhận:', err);
    } finally {
      setIsLoadingHotels(false);
    }
  }, [displayHotels.length, isLoadingHotels, hotelId]);

  const [groupName, setGroupName] = useState('');
  const [companyLegalRep, setCompanyLegalRep] = useState('');
  const [signerName, setSignerName] = useState('');
  const [signerTitle, setSignerTitle] = useState('Đại diện ký kết');

  const today = new Date();
  const defCheckIn = new Date(today.getTime() + 7 * 86400000).toISOString().split('T')[0];
  const defCheckOut = new Date(today.getTime() + 10 * 86400000).toISOString().split('T')[0];

  const [checkInDate, setCheckInDate] = useState(defCheckIn);
  const [checkOutDate, setCheckOutDate] = useState(defCheckOut);
  const [totalGuests, setTotalGuests] = useState(12);

  const [blocks, setBlocks] = useState<
    { hotelRoomTypeId: number; roomTypeName: string; blockedRooms: number; negotiatedRatePerNight: number }[]
  >([]);

  const [specialRequests, setSpecialRequests] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadCompanies = useCallback(async () => {
    if (companies.length > 0 || isLoadingCompanies) return;
    setIsLoadingCompanies(true);
    try {
      const res = await groupBookingService.getCompanies();
      setCompanies(res);
      if (res.length > 0 && !selectedCompanyId) {
        setSelectedCompanyId(res[0].id);
      }
    } catch (err: unknown) {
      console.warn('Lỗi khi tải danh sách doanh nghiệp B2B:', err);
    } finally {
      setIsLoadingCompanies(false);
    }
  }, [companies.length, isLoadingCompanies, selectedCompanyId]);

  // Load companies when modal opens
  useEffect(() => {
    if (!isOpen) return;
    let ignore = false;
    Promise.resolve().then(() => {
      if (!ignore) setIsLoadingCompanies(true);
    });
    groupBookingService
      .getCompanies()
      .then((res) => {
        if (!ignore) {
          setCompanies(res);
          if (res.length > 0) {
            setSelectedCompanyId(res[0].id);
          }
        }
      })
      .finally(() => {
        if (!ignore) {
          setIsLoadingCompanies(false);
        }
      });

    return () => {
      ignore = true;
    };
  }, [isOpen]);

  // Load hotel room types from API when hotelId changes or modal opens
  useEffect(() => {
    if (!isOpen || !effectiveHotelId) return;
    let ignore = false;
    Promise.resolve().then(() => {
      if (!ignore) setIsLoadingRoomTypes(true);
    });
    hotelRoomTypeService
      .filter({ hotelId: Number(effectiveHotelId), page: 0, pageSize: 50 })
      .then((res) => {
        if (ignore) return;
        const content = res.result?.content || [];
        const items: RoomTypeItem[] = content.map((rt: HotelRoomTypeDto) => ({
          id: rt.id,
          name: rt.roomTypeName || rt.roomTypeCode || `Hạng phòng #${rt.id}`,
          basePrice: Number(rt.basePrice) || 1200000,
        }));

        setAvailableRoomTypes(items);
        if (items.length > 0) {
          setBlocks([
            {
              hotelRoomTypeId: items[0].id,
              roomTypeName: items[0].name,
              blockedRooms: 6,
              negotiatedRatePerNight: items[0].basePrice,
            },
          ]);
        } else {
          setBlocks([
            {
              hotelRoomTypeId: 1,
              roomTypeName: 'Standard Room',
              blockedRooms: 6,
              negotiatedRatePerNight: 1200000,
            },
          ]);
        }
      })
      .catch((err: unknown) => {
        if (!ignore) {
          console.warn('Lỗi khi tải danh sách hạng phòng từ API:', err);
          setBlocks([
            {
              hotelRoomTypeId: 1,
              roomTypeName: 'Standard Room',
              blockedRooms: 6,
              negotiatedRatePerNight: 1200000,
            },
          ]);
        }
      })
      .finally(() => {
        if (!ignore) {
          setIsLoadingRoomTypes(false);
        }
      });

    return () => {
      ignore = true;
    };
  }, [isOpen, effectiveHotelId]);

  if (!isOpen) return null;

  const handleSelectExistingCompany = (id: number) => {
    setSelectedCompanyId(id);
    const comp = companies.find((c) => c.id === id);
    if (comp) {
      setNewCompanyName(comp.companyName);
      setNewCompanyTaxCode(comp.taxCode || '');
      setNewCompanyAddress(comp.address || '');
      setCompanyLegalRep(comp.contactPerson || '');
      setSignerName(comp.contactPerson || '');
      setNewContactPhone(comp.contactPhone || '');
      setNewContactEmail(comp.contactEmail || '');
    }
  };

  const totalRooms = blocks.reduce((acc, b) => acc + (Number(b.blockedRooms) || 0), 0);
  const d1 = new Date(checkInDate);
  const d2 = new Date(checkOutDate);
  const nights = Math.max(1, Math.ceil((d2.getTime() - d1.getTime()) / (1000 * 60 * 60 * 24)));

  const subtotal = blocks.reduce(
    (acc, b) => acc + (Number(b.blockedRooms) || 0) * (Number(b.negotiatedRatePerNight) || 0) * nights,
    0
  );
  const serviceFee = Math.round(subtotal * 0.05);
  const vat = Math.round((subtotal + serviceFee) * 0.1);
  const totalContractValue = subtotal + serviceFee + vat;
  const depositRequired = Math.round(totalContractValue * 0.5);

  const handleAddBlock = () => {
    const unused = availableRoomTypes.find(
      (t) => !blocks.some((b) => b.hotelRoomTypeId === t.id)
    );
    if (unused) {
      setBlocks((prev) => [
        ...prev,
        {
          hotelRoomTypeId: unused.id,
          roomTypeName: unused.name,
          blockedRooms: 2,
          negotiatedRatePerNight: unused.basePrice,
        },
      ]);
    } else if (availableRoomTypes.length > 0) {
      const first = availableRoomTypes[0];
      setBlocks((prev) => [
        ...prev,
        {
          hotelRoomTypeId: first.id,
          roomTypeName: first.name,
          blockedRooms: 2,
          negotiatedRatePerNight: first.basePrice,
        },
      ]);
    }
  };

  const handleRemoveBlock = (index: number) => {
    if (blocks.length <= 1) return;
    setBlocks((prev) => prev.filter((_, i) => i !== index));
  };

  const handleBlockChange = (
    index: number,
    field: 'hotelRoomTypeId' | 'blockedRooms' | 'negotiatedRatePerNight',
    val: string | number
  ) => {
    setBlocks((prev) => {
      const copy = [...prev];
      if (field === 'hotelRoomTypeId') {
        const selected = availableRoomTypes.find((t) => t.id === Number(val));
        if (selected) {
          copy[index].hotelRoomTypeId = selected.id;
          copy[index].roomTypeName = selected.name;
          copy[index].negotiatedRatePerNight = selected.basePrice;
        }
      } else if (field === 'blockedRooms') {
        copy[index].blockedRooms = Math.max(1, Number(val) || 1);
      } else if (field === 'negotiatedRatePerNight') {
        copy[index].negotiatedRatePerNight = Math.max(0, Number(val) || 0);
      }
      return copy;
    });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    const compName = newCompanyName.trim();
    const compTax = newCompanyTaxCode.trim();

    if (!compName) {
      setError('Vui lòng nhập tên công ty / doanh nghiệp hoặc đoàn thể');
      return;
    }
    if (!compTax) {
      setError('Vui lòng nhập mã số thuế doanh nghiệp (hoặc mã định danh đoàn)');
      return;
    }
    if (!selectedStaffId) {
      setError('Vui lòng chọn nhân viên phụ trách hợp đồng từ danh sách nhân sự (bắt buộc)');
      return;
    }
    if (!groupName.trim()) {
      setError('Vui lòng nhập tên đoàn hoặc sự kiện');
      return;
    }
    if (totalRooms < 6) {
      setError('Quy định hợp đồng đoàn (GIT) yêu cầu tối thiểu từ 6 phòng trở lên');
      return;
    }
    if (checkOutDate <= checkInDate) {
      setError('Ngày check-out phải sau ngày check-in');
      return;
    }

    try {
      setIsSubmitting(true);
      let effectiveCompanyId = selectedCompanyId;

      // CÁCH 2: Nếu người dùng nhập thông tin công ty mới (chưa có ID hoặc ở tab Nhập mới)
      // Tự động gọi API POST /api/v1/companies để lưu vào DB và lấy ID thật
      if (companyMode === 'NEW' || !effectiveCompanyId) {
        try {
          const createCompanyRes = await groupBookingService.createCompany({
            name: compName,
            taxCode: compTax,
            address: newCompanyAddress.trim() || undefined,
            contactName: signerName.trim() || companyLegalRep.trim() || undefined,
            contactPhone: newContactPhone.trim() || undefined,
            contactEmail: newContactEmail.trim() || undefined,
          });

          if (createCompanyRes.result?.id) {
            effectiveCompanyId = Number(createCompanyRes.result.id);
          }
        } catch (companyErr: unknown) {
          console.warn('Lỗi khi tạo công ty mới, kiểm tra công ty sẵn có:', companyErr);
          // Nếu đã tồn tại hoặc lỗi, fallback tìm theo danh sách có sẵn
          if (companies.length > 0) {
            effectiveCompanyId = companies[0].id;
          }
        }
      }

      if (!effectiveCompanyId) {
        throw new Error('Không thể khởi tạo mã doanh nghiệp trong hệ thống. Vui lòng thử lại.');
      }

      const payload: GroupContractCreateRequest = {
        hotelId: Number(effectiveHotelId),
        companyId: Number(effectiveCompanyId),
        groupName: groupName.trim(),
        companyTaxCode: compTax,
        companyLegalRepresentative: companyLegalRep.trim() || undefined,
        signerName: signerName.trim() || undefined,
        signerTitle: signerTitle.trim() || undefined,
        checkInDate,
        checkOutDate,
        totalRooms,
        totalGuests: Math.max(1, Number(totalGuests) || totalRooms * 2),
        serviceFeeRate: 5.0,
        vatRate: 10.0,
        depositRate: 50.0,
        specialRequests: specialRequests.trim() || undefined,
        governingLaw: 'VIETNAMESE_LAW',
        roomBlocks: blocks.map((b) => ({
          hotelRoomTypeId: b.hotelRoomTypeId,
          blockedRooms: b.blockedRooms,
          negotiatedRatePerNight: b.negotiatedRatePerNight,
        })),
      };

      await onSubmit(payload, Number(selectedStaffId));
      onClose();
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } }; message?: string })?.response?.data?.message ||
        (err as { message?: string })?.message ||
        'Có lỗi xảy ra khi tạo hợp đồng đoàn';
      setError(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs p-4 overflow-y-auto">
      <div className="relative w-full max-w-3xl bg-white rounded-2xl shadow-2xl border border-neutral-200 overflow-hidden my-8 animate-in fade-in zoom-in-95 duration-200">
        {/* Header */}
        <div className="bg-neutral-900 text-white px-6 py-4 flex items-center justify-between border-b border-neutral-800">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-red-600 text-white flex items-center justify-center font-bold shadow-xs">
              <Users className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-lg font-black tracking-tight text-white uppercase">
                Tạo Hợp Đồng Khách Đoàn
              </h2>
              <p className="text-xs text-neutral-400">
                Lập hợp đồng B2B, khởi tạo Room Block và thiết lập cọc tối thiểu 50%
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="p-2 text-neutral-400 hover:text-white rounded-lg hover:bg-neutral-800 transition-colors cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Body */}
        <form onSubmit={handleSubmit} className="p-6 space-y-6 max-h-[80vh] overflow-y-auto">
          {error && (
            <div className="p-3.5 bg-red-50 border border-red-200 rounded-xl flex items-center gap-3 text-red-700 text-xs font-semibold">
              <AlertCircle className="w-4 h-4 shrink-0 text-red-600" />
              <span>{error}</span>
            </div>
          )}

          {/* Section 1: Doanh nghiệp B2B (Hỗ trợ Cách 2: Nhập trực tiếp linh hoạt) */}
          <div className="space-y-4">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-neutral-200 pb-2">
              <h3 className="text-xs font-black uppercase text-neutral-900 tracking-wider flex items-center gap-2">
                <Building2 className="w-4 h-4 text-red-600" />
                1. Thông Tin Doanh Nghiệp Đối Tác & Đoàn Khách
              </h3>

              {/* Mode Toggle Buttons: Nhập mới (Cách 2) vs Chọn từ danh bạ */}
              <div className="inline-flex p-1 bg-neutral-100 rounded-xl text-xs font-bold">
                <button
                  type="button"
                  onClick={() => setCompanyMode('NEW')}
                  className={`px-3 py-1 rounded-lg transition-all cursor-pointer flex items-center gap-1.5 ${companyMode === 'NEW'
                    ? 'bg-white text-neutral-900 shadow-2xs font-black'
                    : 'text-neutral-500 hover:text-neutral-800'
                    }`}
                >
                  <PenTool className="w-3.5 h-3.5 text-red-600" />
                  <span>Nhập thông tin mới</span>
                </button>
                <button
                  type="button"
                  onClick={() => setCompanyMode('EXISTING')}
                  className={`px-3 py-1 rounded-lg transition-all cursor-pointer flex items-center gap-1.5 ${companyMode === 'EXISTING'
                    ? 'bg-white text-neutral-900 shadow-2xs font-black'
                    : 'text-neutral-500 hover:text-neutral-800'
                    }`}
                >
                  <Search className="w-3.5 h-3.5 text-neutral-600" />
                  <span>Chọn từ danh bạ ({companies.length})</span>
                </button>
              </div>
            </div>

            {/* Nếu ở chế độ chọn từ danh bạ */}
            {companyMode === 'EXISTING' && (
              <div className="p-3 bg-neutral-50 border border-neutral-200 rounded-xl">
                <label className="block text-xs font-bold text-neutral-700 uppercase mb-1 flex items-center justify-between">
                  <span>Chọn Doanh Nghiệp B2B Có Sẵn</span>
                  {isLoadingCompanies && (
                    <span className="text-[11px] text-neutral-500 font-normal flex items-center gap-1">
                      <Loader2 className="w-3 h-3 animate-spin text-red-600" />
                      Đang tải từ API...
                    </span>
                  )}
                </label>
                <Select<number>
                  value={selectedCompanyId}
                  onChange={(val) => handleSelectExistingCompany(val)}
                  onOpen={loadCompanies}
                  isLoading={isLoadingCompanies}
                  options={companies.map((c) => ({
                    value: c.id,
                    label: c.companyName,
                    badge: c.taxCode ? `MST: ${c.taxCode}` : undefined,
                    description: c.address,
                  }))}
                  placeholder="Chọn doanh nghiệp..."
                  size="sm"
                />
              </div>
            )}

            {/* Các trường nhập liệu doanh nghiệp trực tiếp (Cách 2) */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-bold text-neutral-700 uppercase mb-1">
                  Tên Doanh Nghiệp / Cơ Quan Đối Tác *
                </label>
                <input
                  type="text"
                  value={newCompanyName}
                  onChange={(e) => setNewCompanyName(e.target.value)}
                  placeholder="VD: Tổng Công ty Cổ phần May 10"
                  required
                  className="w-full h-11 px-3.5 border border-neutral-300 rounded-xl text-sm font-semibold focus:ring-2 focus:ring-red-500 focus:outline-none"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-neutral-700 uppercase mb-1">
                  Mã Số Thuế Doanh Nghiệp *
                </label>
                <input
                  type="text"
                  value={newCompanyTaxCode}
                  onChange={(e) => setNewCompanyTaxCode(e.target.value)}
                  placeholder="0100109106"
                  required
                  className="w-full h-11 px-3.5 border border-neutral-300 rounded-xl text-sm font-semibold focus:ring-2 focus:ring-red-500 focus:outline-none font-mono"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-neutral-700 uppercase mb-1">
                  Tên đoàn / Mục đích sự kiện *
                </label>
                <input
                  type="text"
                  value={groupName}
                  onChange={(e) => setGroupName(e.target.value)}
                  placeholder="VD: Đoàn Hội Thảo Tổng Kết Q4/2026"
                  required
                  className="w-full h-11 px-3.5 border border-neutral-300 rounded-xl text-sm font-semibold focus:ring-2 focus:ring-red-500 focus:outline-none"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-neutral-700 uppercase mb-1">
                  Địa chỉ trụ sở đối tác
                </label>
                <input
                  type="text"
                  value={newCompanyAddress}
                  onChange={(e) => setNewCompanyAddress(e.target.value)}
                  placeholder="VD: 765 Nguyễn Văn Linh, Long Biên, Hà Nội"
                  className="w-full h-11 px-3.5 border border-neutral-300 rounded-xl text-sm font-medium focus:ring-2 focus:ring-red-500 focus:outline-none"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-neutral-700 uppercase mb-1">
                  Người đại diện pháp luật
                </label>
                <input
                  type="text"
                  value={companyLegalRep}
                  onChange={(e) => setCompanyLegalRep(e.target.value)}
                  placeholder="VD: Thân Đức Việt"
                  className="w-full h-11 px-3.5 border border-neutral-300 rounded-xl text-sm font-medium focus:ring-2 focus:ring-red-500 focus:outline-none"
                />
              </div>

              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="block text-xs font-bold text-neutral-700 uppercase mb-1">
                    Chức danh
                  </label>
                  <input
                    type="text"
                    value={signerTitle}
                    onChange={(e) => setSignerTitle(e.target.value)}
                    placeholder="Giám đốc điều hành"
                    className="w-full h-11 px-3.5 border border-neutral-300 rounded-xl text-sm font-medium focus:ring-2 focus:ring-red-500 focus:outline-none"
                  />
                </div>
              </div>
            </div>
          </div>

          {/* Section 2: Khách sạn & Lịch lưu trú */}
          <div className="space-y-4">
            <h3 className="text-xs font-black uppercase text-neutral-900 tracking-wider flex items-center gap-2 border-b border-neutral-200 pb-2">
              <Calendar className="w-4 h-4 text-red-600" />
              2. Khách Sạn & Lịch Trình Lưu Trú
            </h3>

            {/* Hàng 1: Khách sạn cơ sở & Nhân viên phụ trách */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-bold text-neutral-700 uppercase mb-1 flex items-center justify-between">
                  <span>Khách sạn tiếp nhận *</span>
                  {isLoadingHotels && (
                    <span className="text-[11px] text-neutral-500 font-normal flex items-center gap-1">
                      <Loader2 className="w-3 h-3 animate-spin text-red-600" />
                      Đang tải...
                    </span>
                  )}
                </label>
                <Select<number>
                  value={effectiveHotelId}
                  onChange={(val) => setHotelId(val)}
                  onOpen={loadHotels}
                  isLoading={isLoadingHotels}
                  loadingText="Đang tải danh sách cơ sở..."
                  placeholder="Chọn khách sạn tiếp nhận..."
                  options={displayHotels.map((h) => ({
                    value: h.id,
                    label: h.name,
                  }))}
                  prefixIcon={<Building2 className="w-4 h-4 text-neutral-400" />}
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-neutral-700 uppercase mb-1 flex items-center justify-between">
                  <span className="flex items-center gap-1">
                    <span>Nhân viên phụ trách*</span>
                  </span>
                </label>
                <Select<number>
                  value={selectedStaffId === '' ? 0 : selectedStaffId}
                  onChange={(val) => setSelectedStaffId(val)}
                  onOpen={loadStaffs}
                  isLoading={isLoadingStaffs}
                  placeholder="Nhân viên phụ trách"
                  options={staffs.map((s) => ({
                    value: s.id,
                    label: `${s.fullName || s.username} (#${s.id})`,
                    badge: s.roleName || s.departmentName || undefined,
                    description: s.email ? `${s.email}${s.phone ? ` • ${s.phone}` : ''}` : s.phone,
                  }))}
                />
              </div>
            </div>

            {/* Hàng 2: Lịch trình & Số lượng khách */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div>
                <label className="block text-xs font-bold text-neutral-700 uppercase mb-1">
                  Ngày nhận phòng *
                </label>
                <input
                  type="date"
                  value={checkInDate}
                  onChange={(e) => setCheckInDate(e.target.value)}
                  required
                  className="w-full h-11 px-3 border border-neutral-300 rounded-xl text-sm font-semibold focus:ring-2 focus:ring-red-500 focus:outline-none"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-neutral-700 uppercase mb-1">
                  Ngày trả phòng *
                </label>
                <input
                  type="date"
                  value={checkOutDate}
                  onChange={(e) => setCheckOutDate(e.target.value)}
                  required
                  className="w-full h-11 px-3 border border-neutral-300 rounded-xl text-sm font-semibold focus:ring-2 focus:ring-red-500 focus:outline-none"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-neutral-700 uppercase mb-1">
                  Dự kiến số khách *
                </label>
                <input
                  type="number"
                  min="1"
                  value={totalGuests}
                  onChange={(e) => setTotalGuests(Number(e.target.value))}
                  required
                  className="w-full h-11 px-3 border border-neutral-300 rounded-xl text-sm font-semibold text-center focus:ring-2 focus:ring-red-500 focus:outline-none"
                />
              </div>
            </div>
          </div>

          {/* Section 3: Cấu hình Room Block */}
          <div className="space-y-4">
            <div className="flex items-center justify-between border-b border-neutral-200 pb-2">
              <div className="flex items-center gap-2">
                <BedDouble className="w-4 h-4 text-red-600" />
                <h3 className="text-xs font-black uppercase text-neutral-900 tracking-wider">
                  3. Thiết Lập Khối Phòng Đoàn (Room Block)
                </h3>
                {isLoadingRoomTypes && (
                  <span className="text-[11px] text-neutral-500 flex items-center gap-1 font-normal">
                    <Loader2 className="w-3 h-3 animate-spin text-red-600" />
                    Đang tải hạng phòng từ server...
                  </span>
                )}
              </div>
              <button
                type="button"
                onClick={handleAddBlock}
                className="inline-flex items-center gap-1 px-3 py-1.5 bg-neutral-100 hover:bg-neutral-200 text-neutral-700 rounded-lg text-xs font-bold transition-colors cursor-pointer"
              >
                <Plus className="w-3.5 h-3.5 text-neutral-600" />
                Thêm loại phòng
              </button>
            </div>

            <div className="space-y-3">
              {blocks.map((block, idx) => (
                <div
                  key={idx}
                  className="p-3.5 bg-neutral-50 rounded-xl border border-neutral-200 grid grid-cols-1 md:grid-cols-12 gap-3 items-center"
                >
                  <div className="md:col-span-6">
                    <label className="block text-[11px] font-bold text-neutral-600 uppercase mb-1">
                      Loại phòng
                    </label>
                    <Select<number>
                      value={block.hotelRoomTypeId}
                      onChange={(val) => handleBlockChange(idx, 'hotelRoomTypeId', val)}
                      options={availableRoomTypes.map((t) => ({
                        value: t.id,
                        label: t.name,
                        badge: `${t.basePrice.toLocaleString('vi-VN')} đ/đêm`,
                      }))}
                      size="sm"
                    />
                  </div>

                  <div className="md:col-span-2">
                    <label className="block text-[11px] font-bold text-neutral-600 uppercase mb-1">
                      Số phòng
                    </label>
                    <input
                      type="number"
                      min="1"
                      value={block.blockedRooms}
                      onChange={(e) => handleBlockChange(idx, 'blockedRooms', e.target.value)}
                      className="w-full h-10 px-3 bg-white border border-neutral-300 rounded-lg text-xs font-bold text-center focus:ring-2 focus:ring-red-500 focus:outline-none"
                    />
                  </div>

                  <div className="md:col-span-3">
                    <label className="block text-[11px] font-bold text-neutral-600 uppercase mb-1">
                      Đơn giá thỏa thuận (đ/đêm)
                    </label>
                    <input
                      type="number"
                      min="0"
                      step="50000"
                      value={block.negotiatedRatePerNight}
                      onChange={(e) => handleBlockChange(idx, 'negotiatedRatePerNight', e.target.value)}
                      className="w-full h-10 px-3 bg-white border border-neutral-300 rounded-lg text-xs font-bold text-right focus:ring-2 focus:ring-red-500 focus:outline-none"
                    />
                  </div>

                  <div className="md:col-span-1 flex justify-center pt-5 md:pt-4">
                    {blocks.length > 1 && (
                      <button
                        type="button"
                        onClick={() => handleRemoveBlock(idx)}
                        className="p-2 text-neutral-400 hover:text-red-600 rounded-lg hover:bg-red-50 transition-colors cursor-pointer"
                        title="Xóa loại phòng"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Section 4: Ghi chú & Tóm tắt tài chính */}
          <div className="space-y-4">
            <div>
              <label className="block text-xs font-bold text-neutral-700 uppercase mb-1">
                Yêu cầu đặc biệt / Hội trường / Tiệc
              </label>
              <textarea
                rows={2}
                value={specialRequests}
                onChange={(e) => setSpecialRequests(e.target.value)}
                placeholder="VD: Hội trường 80 người set up chữ U ngày 16/11, xuất hóa đơn VAT công ty..."
                className="w-full p-3 border border-neutral-300 rounded-xl text-xs font-medium focus:ring-2 focus:ring-red-500 focus:outline-none"
              />
            </div>

            {/* Financial Summary Card */}
            <div className="bg-neutral-900 text-white rounded-xl p-4 space-y-2">
              <div className="flex items-center justify-between text-xs">
                <span className="text-neutral-400">Tổng số phòng block:</span>
                <span
                  className={`font-black text-sm ${totalRooms >= 6 ? 'text-green-400' : 'text-red-400'
                    }`}
                >
                  {totalRooms} phòng {totalRooms < 6 && '(Yêu cầu tối thiểu 6 phòng)'}
                </span>
              </div>
              <div className="flex items-center justify-between text-xs">
                <span className="text-neutral-400">Tiền phòng trước thuế phí ({nights} đêm):</span>
                <span className="font-semibold text-neutral-200">
                  {subtotal.toLocaleString('vi-VN')} đ
                </span>
              </div>
              <div className="flex items-center justify-between text-xs">
                <span className="text-neutral-400">Phí phục vụ (5%):</span>
                <span className="text-neutral-300">{serviceFee.toLocaleString('vi-VN')} đ</span>
              </div>
              <div className="flex items-center justify-between text-xs">
                <span className="text-neutral-400">Thuế GTGT / VAT (10%):</span>
                <span className="text-neutral-300">{vat.toLocaleString('vi-VN')} đ</span>
              </div>
              <div className="border-t border-neutral-800 pt-2 flex items-center justify-between text-xs">
                <span className="text-neutral-200 font-bold">Tổng giá trị hợp đồng (Total):</span>
                <span className="font-black text-white text-sm">
                  {totalContractValue.toLocaleString('vi-VN')} đ
                </span>
              </div>
              <div className="border-t border-neutral-800 pt-2 flex items-center justify-between text-sm">
                <span className="text-red-400 font-bold flex items-center gap-1.5">
                  <DollarSign className="w-4 h-4 text-red-500" />
                  Tiền cọc tối thiểu 50% (Điều 328 BLDS):
                </span>
                <span className="font-black text-red-400 text-base">
                  {depositRequired.toLocaleString('vi-VN')} đ
                </span>
              </div>
            </div>
          </div>

          {/* Footer */}
          <div className="border-t border-neutral-200 pt-4 flex items-center justify-end gap-3">
            <button
              type="button"
              onClick={onClose}
              disabled={isSubmitting}
              className="h-11 px-5 rounded-xl border border-neutral-300 font-bold text-xs text-neutral-700 hover:bg-neutral-100 transition-colors cursor-pointer"
            >
              Hủy bỏ
            </button>
            <button
              type="submit"
              disabled={isSubmitting || totalRooms < 6}
              className="h-11 px-6 rounded-xl bg-red-600 hover:bg-red-700 text-white font-bold text-xs uppercase tracking-wider transition-colors shadow-sm disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer flex items-center gap-2"
            >
              {isSubmitting ? 'Đang khởi tạo...' : 'Tạo Hợp Đồng & Giữ Phòng 3 Ngày'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
