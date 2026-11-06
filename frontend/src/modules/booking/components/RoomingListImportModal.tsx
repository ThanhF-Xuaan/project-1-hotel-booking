import React, { useState, useEffect, useRef, useMemo, useCallback } from 'react';
import {
  Upload,
  FileSpreadsheet,
  AlertCircle,
  CheckCircle2,
  XCircle,
  RefreshCw,
  Check,
  X,
  FileCheck2,
  HelpCircle,
  Search,
  Building,
  User,
  BedDouble,
  Copy,
  Info,
} from 'lucide-react';
import { Modal } from '../../../core/components/ui/Modal';
import Button from '../../../core/components/ui/Button';
import bookingService from '../services/booking.service';
import roomInstanceService from '../../inventory/services/roomInstance.service';
import type { RoomInstanceDto } from '../../inventory/types/inventory.types';
import type {
  BookingResponse,
  RoomingListPreviewResponse,
  RoomingListImportResultResponse,
  RoomingListRowDto,
} from '../types/booking.types';
import type { GroupContractResponse } from '../types/groupBooking.types';

export interface RoomingListImportModalProps {
  isOpen: boolean;
  onClose: () => void;
  initialBookingId?: number;
  initialBookingNumber?: string;
  contract?: GroupContractResponse | null;
  onSuccess?: (result: RoomingListImportResultResponse) => void;
}

type TabType = 'ALL' | 'VALID' | 'ERROR';

const ROOM_TYPE_HIERARCHY: Record<string, number> = {
  STD: 1,
  SUP: 2,
  DLX: 3,
  STE: 4,
  EXE: 5,
  FAM: 6,
};

const ROOM_TYPE_TABS = [
  { code: 'ALL', label: 'Tất cả' },
  { code: 'STD', label: 'STD - Tiêu chuẩn' },
  { code: 'SUP', label: 'SUP - Cao cấp' },
  { code: 'DLX', label: 'DLX - Sang trọng' },
  { code: 'STE', label: 'STE - Suite' },
  { code: 'EXE', label: 'EXE - Tổng thống' },
  { code: 'FAM', label: 'FAM - Gia đình' },
];

const ROOM_TYPE_BADGE_STYLE: Record<string, { bg: string; text: string; border: string }> = {
  STD: { bg: 'bg-blue-50', text: 'text-blue-700', border: 'border-blue-200' },
  SUP: { bg: 'bg-indigo-50', text: 'text-indigo-700', border: 'border-indigo-200' },
  DLX: { bg: 'bg-amber-50', text: 'text-amber-800', border: 'border-amber-200' },
  STE: { bg: 'bg-purple-50', text: 'text-purple-700', border: 'border-purple-200' },
  EXE: { bg: 'bg-rose-50', text: 'text-rose-700', border: 'border-rose-200' },
  FAM: { bg: 'bg-emerald-50', text: 'text-emerald-800', border: 'border-emerald-200' },
};

export const RoomingListImportModal: React.FC<RoomingListImportModalProps> = ({
  isOpen,
  onClose,
  initialBookingId,
  initialBookingNumber,
  contract,
  onSuccess,
}) => {
  // Booking Lookup State
  const defaultInitialBookingNumber = initialBookingNumber || (contract ? contract.contractNumber : initialBookingId ? String(initialBookingId) : '');
  const [bookingNumberInput, setBookingNumberInput] = useState(defaultInitialBookingNumber);
  const [isLoadingBooking, setIsLoadingBooking] = useState(false);
  const [lookupError, setLookupError] = useState<string | null>(null);
  const [booking, setBooking] = useState<BookingResponse | null>(null);
  const [searchResults, setSearchResults] = useState<BookingResponse[]>([]);

  // Available Rooms Modal State
  const [isAvailableRoomsModalOpen, setIsAvailableRoomsModalOpen] = useState(false);
  const [availableRooms, setAvailableRooms] = useState<RoomInstanceDto[]>([]);
  const [isLoadingAvailableRooms, setIsLoadingAvailableRooms] = useState(false);
  const [availableRoomsSearch, setAvailableRoomsSearch] = useState('');
  const [availableRoomsTypeTab, setAvailableRoomsTypeTab] = useState<string>('ALL');
  const [copiedRoomNumber, setCopiedRoomNumber] = useState<string | null>(null);

  // File & Import State
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [fileError, setFileError] = useState<string | null>(null);
  const [isLoadingPreview, setIsLoadingPreview] = useState(false);
  const [isSubmittingConfirm, setIsSubmittingConfirm] = useState(false);

  const [previewData, setPreviewData] = useState<RoomingListPreviewResponse | null>(null);
  const [importResult, setImportResult] = useState<RoomingListImportResultResponse | null>(null);
  const [activeTab, setActiveTab] = useState<TabType>('ALL');
  const [showGuideModal, setShowGuideModal] = useState(false);

  const fileInputRef = useRef<HTMLInputElement>(null);

  const resetState = useCallback(() => {
    setBookingNumberInput('');
    setIsLoadingBooking(false);
    setLookupError(null);
    setBooking(null);
    setSearchResults([]);
    setSelectedFile(null);
    setFileError(null);
    setIsLoadingPreview(false);
    setIsSubmittingConfirm(false);
    setPreviewData(null);
    setImportResult(null);
    setActiveTab('ALL');
    setShowGuideModal(false);
    setIsAvailableRoomsModalOpen(false);
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  }, []);

  const handleClose = () => {
    resetState();
    onClose();
  };

  const handleLookupBooking = useCallback(async (inputStr: string) => {
    const cleanStr = inputStr.trim();
    if (!cleanStr) {
      setLookupError('Vui lòng nhập mã đơn đặt phòng (VD: BK-...) hoặc số phòng để tra cứu');
      return;
    }

    setIsLoadingBooking(true);
    setLookupError(null);
    setBooking(null);
    setSearchResults([]);
    setPreviewData(null);
    setImportResult(null);

    try {
      // 1. Thử tìm theo mã đơn chính xác trước
      try {
        const res = await bookingService.getByNumber(cleanStr);
        if (res.result) {
          setBooking(res.result);
          setIsLoadingBooking(false);
          return;
        }
      } catch {
        // Tiếp tục tìm theo số phòng hoặc tìm kiếm tương đối
      }

      // 2. Tìm kiếm theo Số phòng (Room Number) qua API filter
      const filterByRoom = await bookingService.filter({
        roomNumber: cleanStr,
        size: 10,
      });

      const listByRoom = filterByRoom.result?.content || [];
      if (listByRoom.length === 1) {
        setBooking(listByRoom[0]);
        setIsLoadingBooking(false);
        return;
      } else if (listByRoom.length > 1) {
        setSearchResults(listByRoom);
        setIsLoadingBooking(false);
        return;
      }

      // 3. Nếu chưa thấy, tìm theo mã đơn tương đối
      const filterByNum = await bookingService.filter({
        bookingNumber: cleanStr,
        size: 10,
      });
      const listByNum = filterByNum.result?.content || [];
      if (listByNum.length === 1) {
        setBooking(listByNum[0]);
      } else if (listByNum.length > 1) {
        setSearchResults(listByNum);
      } else {
        setLookupError(
          `Không tìm thấy đơn đặt phòng nào với mã hoặc số phòng: "${cleanStr}". Vui lòng kiểm tra lại.`
        );
      }
    } catch (err: unknown) {
      console.error('Lỗi tra cứu booking:', err);
      setLookupError('Xảy ra lỗi trong quá trình tra cứu đơn đặt phòng.');
    } finally {
      setIsLoadingBooking(false);
    }
  }, []);

  // Tự động nạp booking khi mở modal với props truyền vào
  useEffect(() => {
    if (!isOpen) {
      return;
    }

    let isSubscribed = true;

    if (initialBookingNumber) {
      bookingService.getByNumber(initialBookingNumber.trim())
        .then((res) => {
          if (isSubscribed && res.result) {
            setBooking(res.result);
          }
        })
        .catch(() => {});
    } else if (initialBookingId) {
      bookingService.getById(initialBookingId)
        .then((res) => {
          if (isSubscribed && res.result) {
            setBooking(res.result);
          }
        })
        .catch(() => {});
    }

    return () => {
      isSubscribed = false;
    };
  }, [isOpen, initialBookingId, initialBookingNumber]);

  const handleOpenAvailableRoomsModal = async () => {
    setIsAvailableRoomsModalOpen(true);
    setIsLoadingAvailableRooms(true);
    setAvailableRoomsSearch('');
    setAvailableRoomsTypeTab('ALL');
    try {
      try {
        const hotelId = booking?.hotelId || contract?.hotelId;
        const res = await bookingService.getVacantRooms(hotelId);
        if (res.result && res.result.length > 0) {
          setAvailableRooms(res.result);
          setIsLoadingAvailableRooms(false);
          return;
        }
      } catch {
        // Fallback sang roomInstanceService
      }

      const hotelId = booking?.hotelId || contract?.hotelId;
      const res = await roomInstanceService.filter({
        currentStatus: 'READY',
        hotelId: hotelId ? Number(hotelId) : undefined,
        pageSize: 100,
      });
      if (res.result?.content) {
        setAvailableRooms(res.result.content);
      }
    } catch (err) {
      console.error('Lỗi lấy danh sách phòng trống:', err);
    } finally {
      setIsLoadingAvailableRooms(false);
    }
  };

  const handleCopyRoom = (roomNum: string) => {
    navigator.clipboard.writeText(roomNum);
    setCopiedRoomNumber(roomNum);
    setTimeout(() => setCopiedRoomNumber(null), 2000);
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFileError(null);
    setPreviewData(null);
    setImportResult(null);

    const file = e.target.files?.[0];
    if (!file) return;

    validateAndSetFile(file);
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  const handleRemoveFile = (e?: React.MouseEvent) => {
    if (e) e.stopPropagation();
    setSelectedFile(null);
    setFileError(null);
    setPreviewData(null);
    setImportResult(null);
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  const validateAndSetFile = (file: File) => {
    const isXlsx =
      file.name.toLowerCase().endsWith('.xlsx') ||
      file.type === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet';

    if (!isXlsx) {
      setFileError('Chỉ chấp nhận file định dạng Excel (.xlsx).');
      setSelectedFile(null);
      return;
    }

    const maxSize = 10 * 1024 * 1024; // 10MB
    if (file.size > maxSize) {
      setFileError('Dung lượng file vượt quá giới hạn 10MB.');
      setSelectedFile(null);
      return;
    }

    if (file.size === 0) {
      setFileError('File rỗng (0 bytes). Vui lòng chọn file có dữ liệu.');
      setSelectedFile(null);
      return;
    }

    setSelectedFile(file);
    setFileError(null);
  };

  const handleDrop = (e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      validateAndSetFile(e.dataTransfer.files[0]);
    }
  };

  const handleDragOver = (e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    e.stopPropagation();
  };

  const effectiveBookingId = booking?.id || initialBookingId || contract?.id;

  const handleUploadAndPreview = async () => {
    if (!effectiveBookingId) {
      setFileError('Vui lòng chọn đơn đặt phòng trước khi tải file.');
      return;
    }
    if (!selectedFile) {
      setFileError('Vui lòng chọn file Excel.');
      return;
    }

    setIsLoadingPreview(true);
    setFileError(null);
    setPreviewData(null);

    try {
      const res = await bookingService.previewRoomingList(effectiveBookingId, selectedFile);
      if (res.result) {
        setPreviewData(res.result);
        const errCount = res.result.errorCount ?? res.result.errorRowsCount ?? 0;
        if (errCount > 0) {
          setActiveTab('ERROR');
        } else {
          setActiveTab('ALL');
        }
      } else {
        setFileError(res.message || 'Không thể đọc dữ liệu file Excel.');
      }
    } catch (err: unknown) {
      console.error('Lỗi khi tải file rooming list:', err);
      const apiErr = err as { response?: { status?: number; data?: { message?: string } }; message?: string };
      if (apiErr?.response?.status === 404) {
        setFileError(
          'Lỗi kết nối Backend (HTTP 404): Không tìm thấy endpoint xử lý Rooming List. Vui lòng kiểm tra lại đơn đặt phòng.'
        );
      } else {
        const msg =
          apiErr?.response?.data?.message ||
          apiErr?.message ||
          'Có lỗi xảy ra khi đọc file Excel. Vui lòng kiểm tra lại cấu trúc file.';
        setFileError(msg);
      }
    } finally {
      setIsLoadingPreview(false);
    }
  };

  const handleConfirmAndImport = async () => {
    if (!effectiveBookingId) {
      setFileError('Vui lòng chọn đơn đặt phòng trước khi tải file.');
      return;
    }
    if (!selectedFile) {
      setFileError('Vui lòng chọn file Excel.');
      return;
    }

    setIsSubmittingConfirm(true);
    setFileError(null);
    setImportResult(null);

    try {
      // 1. Kiểm tra tính hợp lệ
      const previewRes = await bookingService.previewRoomingList(effectiveBookingId, selectedFile);
      const preview = previewRes.result;

      if (!preview) {
        setFileError('File không hợp lệ: Không thể đọc được dữ liệu từ file Excel.');
        return;
      }

      const validCount = preview.validCount ?? preview.validRowsCount ?? 0;
      const errorCount = preview.errorCount ?? preview.errorRowsCount ?? 0;

      if (errorCount > 0 || validCount === 0) {
        setPreviewData(preview);
        setActiveTab('ERROR');
        setFileError(
          `File không hợp lệ! Phát hiện ${errorCount} lỗi trên tổng số ${preview.totalRows} dòng dữ liệu. Vui lòng kiểm tra các dòng lỗi bên dưới.`
        );
        return;
      }

      // 2. File hợp lệ -> Xác nhận lưu
      const validRows = preview.rows.filter((r) => r.isValid ?? r.valid ?? false);
      const confirmRes = await bookingService.confirmRoomingList(effectiveBookingId, { guests: validRows });
      if (confirmRes.result) {
        setImportResult(confirmRes.result);
        setPreviewData(null);
        if (onSuccess) {
          onSuccess(confirmRes.result);
        }
      } else {
        setFileError(confirmRes.message || 'Lưu danh sách khách thất bại.');
      }
    } catch (err: unknown) {
      console.error('Lỗi khi kiểm tra và import file:', err);
      const apiErr = err as { response?: { status?: number; data?: { message?: string } }; message?: string };
      const msg =
        apiErr?.response?.data?.message ||
        apiErr?.message ||
        'Cấu trúc file không hợp lệ hoặc sai định dạng.';
      setFileError(`File không hợp lệ: ${msg}`);
      setPreviewData(null);
    } finally {
      setIsSubmittingConfirm(false);
    }
  };

  const filteredRows: RoomingListRowDto[] = (previewData?.rows || []).filter((row) => {
    const isRowValid = row.isValid ?? row.valid ?? false;
    if (activeTab === 'VALID') return isRowValid;
    if (activeTab === 'ERROR') return !isRowValid;
    return true;
  });

  const validCount = previewData ? (previewData.validCount ?? previewData.validRowsCount ?? 0) : 0;
  const errorCount = previewData ? (previewData.errorCount ?? previewData.errorRowsCount ?? 0) : 0;

  const onlyReadyRooms = useMemo(() => {
    return availableRooms.filter((room) => room.currentStatus === 'READY');
  }, [availableRooms]);

  const filteredAvailableRooms: RoomInstanceDto[] = useMemo(() => {
    return onlyReadyRooms
      .filter((room) => {
        if (availableRoomsTypeTab !== 'ALL' && (room.roomTypeCode || '').toUpperCase() !== availableRoomsTypeTab) {
          return false;
        }
        if (!availableRoomsSearch.trim()) return true;
        const q = availableRoomsSearch.trim().toLowerCase();
        const rNum = (room.roomNumber || '').toLowerCase();
        const tName = (room.roomTypeName || '').toLowerCase();
        const tCode = (room.roomTypeCode || '').toLowerCase();
        return rNum.includes(q) || tName.includes(q) || tCode.includes(q);
      })
      .sort((a, b) => {
        const codeA = (a.roomTypeCode || '').toUpperCase();
        const codeB = (b.roomTypeCode || '').toUpperCase();
        const orderA = ROOM_TYPE_HIERARCHY[codeA] || 99;
        const orderB = ROOM_TYPE_HIERARCHY[codeB] || 99;
        if (orderA !== orderB) return orderA - orderB;

        const numA = parseInt((a.roomNumber || '').replace(/\D/g, '') || '0', 10);
        const numB = parseInt((b.roomNumber || '').replace(/\D/g, '') || '0', 10);
        if (numA !== numB) return numA - numB;

        return (a.roomNumber || '').localeCompare(b.roomNumber || '');
      });
  }, [onlyReadyRooms, availableRoomsTypeTab, availableRoomsSearch]);

  const displayTitle = contract
    ? `Nhập danh sách khách: ${contract.contractNumber} (${contract.groupName})`
    : booking
      ? `Nhập danh sách khách: ${booking.bookingNumber}`
      : 'Nhập danh sách khách đoàn (Excel Rooming List)';

  const headerUtilityButtons = (
    <div className="flex items-center gap-2">
      <button
        type="button"
        onClick={handleOpenAvailableRoomsModal}
        className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl border border-neutral-300 hover:border-blue-500 hover:text-blue-600 bg-white text-neutral-700 text-xs font-semibold shadow-2xs transition-all cursor-pointer"
      >
        <Search className="w-3.5 h-3.5 text-blue-600" />
        <span>Kiểm tra phòng còn trống</span>
      </button>
      <button
        type="button"
        onClick={() => setShowGuideModal(true)}
        className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl border border-neutral-300 hover:border-red-500 hover:text-red-600 bg-white text-neutral-700 text-xs font-semibold shadow-2xs transition-all cursor-pointer"
      >
        <HelpCircle className="w-3.5 h-3.5 text-red-600" />
        <span>Quy chuẩn & Bố cục file Excel</span>
      </button>
    </div>
  );

  return (
    <>
      <Modal
        isOpen={isOpen}
        onClose={handleClose}
        title={displayTitle}
        description="Tải lên file Excel danh sách khách đoàn, kiểm tra lỗi từng dòng và đồng bộ vào phòng lưu trú."
        maxWidth="5xl"
        headerActions={headerUtilityButtons}
      >
        <div className="space-y-5 overflow-y-auto scrollbar-none px-1 py-1">
          {/* Kết quả sau khi Import thành công */}
          {importResult ? (
            <div className="p-6 bg-emerald-50 border border-emerald-200 rounded-2xl text-center space-y-4 animate-in fade-in">
              <div className="w-14 h-14 bg-emerald-100 text-emerald-600 rounded-full flex items-center justify-center mx-auto">
                <CheckCircle2 className="w-8 h-8" />
              </div>
              <div>
                <h3 className="text-lg font-bold text-emerald-900">Import Rooming List Thành Công!</h3>
                <p className="text-xs text-emerald-700 mt-1">
                  Danh sách khách lưu trú đã được lưu thành công vào hệ thống.
                </p>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 max-w-lg mx-auto text-left">
                <div className="bg-white p-3 rounded-xl border border-emerald-100 shadow-2xs">
                  <span className="text-[11px] text-neutral-500 font-medium">Đã thêm mới</span>
                  <p className="text-xl font-bold text-emerald-600">{importResult.importedCount} khách</p>
                </div>
                <div className="bg-white p-3 rounded-xl border border-emerald-100 shadow-2xs">
                  <span className="text-[11px] text-neutral-500 font-medium">Bỏ qua (Trùng lặp)</span>
                  <p className="text-xl font-bold text-amber-600">{importResult.skippedCount}</p>
                </div>
                <div className="bg-white p-3 rounded-xl border border-emerald-100 shadow-2xs">
                  <span className="text-[11px] text-neutral-500 font-medium">Bị từ chối (Lỗi)</span>
                  <p className="text-xl font-bold text-neutral-500">{importResult.errorCount}</p>
                </div>
              </div>

              <div className="pt-2 flex justify-center gap-3">
                <Button variant="primary" onClick={handleClose}>
                  <Check className="w-4 h-4 mr-1.5" />
                  Hoàn tất
                </Button>
              </div>
            </div>
          ) : (
            <>
              {/* BƯỚC 1: TRA CỨU ĐƠN (KHI CHƯA CHỌN ĐƠN / HỢP ĐỒNG) */}
              {!booking && !contract && (
                <div className="bg-neutral-50 p-4 rounded-2xl border border-neutral-200 space-y-3">
                  <label className="block text-xs font-bold text-neutral-800 flex items-center gap-1.5">
                    <Search className="w-4 h-4 text-red-600" />
                    <span>Bước 1: Tra cứu đơn đặt phòng theo Mã đơn hoặc Số phòng</span>
                  </label>

                  <div className="flex flex-col sm:flex-row gap-2.5 items-stretch sm:items-center">
                    <input
                      type="text"
                      placeholder="Nhập mã đơn (VD: BK-...) hoặc Số phòng (VD: 101, 201)..."
                      value={bookingNumberInput}
                      onChange={(e) => setBookingNumberInput(e.target.value)}
                      onKeyDown={(e) => {
                        if (e.key === 'Enter') handleLookupBooking(bookingNumberInput);
                      }}
                      className="flex-1 px-3.5 py-2 text-xs bg-white border border-neutral-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-red-500/20 focus:border-red-500 h-10 transition-all shadow-2xs"
                    />
                    <Button
                      variant="primary"
                      onClick={() => handleLookupBooking(bookingNumberInput)}
                      disabled={isLoadingBooking}
                      className="h-10 px-5 text-xs font-bold rounded-xl shrink-0 flex items-center justify-center gap-1.5 shadow-xs"
                    >
                      <Search className={`w-4 h-4 ${isLoadingBooking ? 'animate-spin' : ''}`} />
                      <span>{isLoadingBooking ? 'Đang tìm...' : 'Tìm kiếm'}</span>
                    </Button>
                  </div>

                  {lookupError && (
                    <div className="p-3 bg-red-50 border border-red-200 text-red-700 text-xs rounded-xl flex items-center gap-2">
                      <AlertCircle className="w-4 h-4 shrink-0" />
                      <span>{lookupError}</span>
                    </div>
                  )}

                  {/* KẾT QUẢ TÌM THẤY NHIỀU ĐƠN */}
                  {searchResults.length > 0 && (
                    <div className="p-3 bg-white rounded-xl border border-neutral-200 space-y-2 animate-in fade-in">
                      <p className="text-xs font-bold text-neutral-800">
                        Tìm thấy {searchResults.length} đơn đặt phòng. Bấm để chọn:
                      </p>
                      <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                        {searchResults.map((b) => (
                          <div
                            key={b.id}
                            onClick={() => {
                              setBooking(b);
                              setSearchResults([]);
                            }}
                            className="p-3 bg-neutral-50 rounded-xl border border-neutral-200 hover:border-red-500 hover:bg-white cursor-pointer transition-all space-y-1"
                          >
                            <div className="flex items-center justify-between">
                              <span className="font-mono font-bold text-xs px-2 py-0.5 rounded bg-neutral-900 text-white">
                                {b.bookingNumber}
                              </span>
                              <span className="text-[10px] font-bold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded border border-emerald-200">
                                {b.status}
                              </span>
                            </div>
                            <p className="text-xs font-semibold text-neutral-900">
                              {b.guestName} ({b.guestPhone})
                            </p>
                            <p className="text-[11px] text-neutral-500">
                              {b.hotelName}
                            </p>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}
                </div>
              )}

              {/* THÔNG TIN HỢP ĐỒNG ĐOÀN HOẶC BOOKING ĐÃ CHỌN */}
              {(booking || contract) && (
                <div className="p-4 bg-neutral-50 rounded-2xl border border-neutral-200 space-y-3">
                  <div className="flex flex-wrap items-center justify-between gap-3">
                    <div className="space-y-1">
                      <div className="flex items-center gap-2">
                        <span className="text-xs font-mono font-bold px-2.5 py-0.5 rounded-md bg-neutral-900 text-white">
                          {booking?.bookingNumber || contract?.contractNumber}
                        </span>
                        <span className="text-xs px-2 py-0.5 rounded-full font-bold bg-emerald-100 text-emerald-800">
                          {booking?.status || contract?.status}
                        </span>
                      </div>
                      <div className="flex flex-wrap items-center gap-4 text-xs text-neutral-700 pt-1">
                        <span className="flex items-center gap-1">
                          <User className="w-3.5 h-3.5 text-neutral-400" />
                          <strong>Đoàn / Đại diện:</strong>{' '}
                          {contract?.groupName || booking?.guestName || 'Khách đoàn B2B'}
                        </span>
                        <span className="flex items-center gap-1">
                          <Building className="w-3.5 h-3.5 text-neutral-400" />
                          <strong>Cơ sở:</strong> {contract?.hotelName || booking?.hotelName}
                        </span>
                        {contract?.totalRooms && (
                          <span className="flex items-center gap-1">
                            <BedDouble className="w-3.5 h-3.5 text-neutral-400" />
                            <strong>Số phòng block:</strong> {contract.totalRooms} phòng
                          </span>
                        )}
                      </div>
                    </div>

                    {!contract && (
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => {
                          setBooking(null);
                          setSearchResults([]);
                          setPreviewData(null);
                        }}
                      >
                        Đổi đơn khác
                      </Button>
                    )}
                  </div>

                  {/* CÁC NÚT TIỆN ÍCH HỖ TRỢ */}
                  <div className="flex flex-wrap items-center gap-2 pt-2 border-t border-neutral-200">
                    <button
                      type="button"
                      onClick={handleOpenAvailableRoomsModal}
                      className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-neutral-300 hover:border-blue-500 hover:text-blue-600 bg-white text-neutral-700 text-xs font-semibold shadow-2xs transition-all cursor-pointer"
                    >
                      <Search className="w-3.5 h-3.5 text-blue-600" />
                      <span>Kiểm tra phòng còn trống</span>
                    </button>
                    <button
                      type="button"
                      onClick={() => setShowGuideModal(true)}
                      className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-neutral-300 hover:border-red-500 hover:text-red-600 bg-white text-neutral-700 text-xs font-semibold shadow-2xs transition-all cursor-pointer"
                    >
                      <HelpCircle className="w-3.5 h-3.5 text-red-600" />
                      <span>Quy chuẩn & Bố cục file Excel</span>
                    </button>
                  </div>
                </div>
              )}

              {/* VÙNG UPLOAD FILE EXCEL */}
              {(booking || contract) && (
                <div className="bg-neutral-50 p-4 rounded-2xl border border-neutral-200 space-y-3">
                  <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-2">
                    <div>
                      <h4 className="text-xs font-bold text-neutral-900 flex items-center gap-1.5">
                        <FileSpreadsheet className="w-4 h-4 text-red-600" />
                        Bước 2: Chọn file Excel Rooming List
                      </h4>
                      <p className="text-[11px] text-neutral-500 mt-0.5">
                        Định dạng <strong>.xlsx</strong>, tối đa 500 dòng, &lt; 10MB. Sheet bắt buộc: <strong>RoomingList</strong>.
                      </p>
                    </div>
                  </div>

                  {/* KHUNG KÉO THẢ FILE */}
                  {!selectedFile ? (
                    <div
                      onDrop={handleDrop}
                      onDragOver={handleDragOver}
                      onClick={() => fileInputRef.current?.click()}
                      className="border-2 border-dashed border-neutral-300 hover:border-red-500 hover:bg-neutral-100/50 rounded-2xl p-6 text-center cursor-pointer transition-all duration-200 flex flex-col items-center justify-center space-y-2 group bg-white"
                    >
                      <div className="w-12 h-12 rounded-full bg-red-50 text-red-600 flex items-center justify-center group-hover:scale-110 transition-transform duration-200">
                        <Upload className="w-6 h-6" />
                      </div>
                      <div className="space-y-0.5">
                        <p className="text-xs font-bold text-neutral-800">
                          Kéo và thả file Excel vào đây, hoặc{' '}
                          <span className="text-red-600 hover:underline">chọn file từ máy tính</span>
                        </p>
                        <p className="text-[11px] text-neutral-400">
                          Chỉ hỗ trợ file .xlsx của Microsoft Excel (Sheet: RoomingList)
                        </p>
                      </div>
                      <input
                        type="file"
                        ref={fileInputRef}
                        onChange={handleFileChange}
                        accept=".xlsx, application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                        className="hidden"
                      />
                    </div>
                  ) : (
                    /* FILE ĐÃ CHỌN */
                    <div className="bg-white p-4 rounded-xl border border-neutral-200 flex flex-col sm:flex-row sm:items-center justify-between gap-3 shadow-2xs">
                      <div className="flex items-center gap-3">
                        <div className="w-10 h-10 rounded-lg bg-emerald-50 text-emerald-600 flex items-center justify-center shrink-0">
                          <FileCheck2 className="w-5 h-5" />
                        </div>
                        <div className="min-w-0">
                          <p className="text-xs font-bold text-neutral-900 truncate">
                            {selectedFile.name}
                          </p>
                          <p className="text-[11px] text-neutral-500">
                            {(selectedFile.size / 1024).toFixed(1)} KB • Sẵn sàng kiểm tra
                          </p>
                        </div>
                      </div>

                      <div className="flex items-center gap-2 shrink-0">
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={handleRemoveFile}
                          className="text-neutral-500 hover:text-red-600 border-neutral-300 text-xs"
                        >
                          <X className="w-3.5 h-3.5 mr-1" />
                          Hủy chọn
                        </Button>
                        <Button
                          variant="primary"
                          size="sm"
                          onClick={handleUploadAndPreview}
                          disabled={isLoadingPreview}
                          className="text-xs"
                        >
                          <RefreshCw className={`w-3.5 h-3.5 mr-1.5 ${isLoadingPreview ? 'animate-spin' : ''}`} />
                          {isLoadingPreview ? 'Đang đọc file...' : 'Kiểm tra file (Preview)'}
                        </Button>
                      </div>
                    </div>
                  )}

                  {fileError && (
                    <div className="p-3 bg-red-50 border border-red-200 text-red-700 text-xs rounded-xl flex items-center gap-2">
                      <AlertCircle className="w-4 h-4 shrink-0" />
                      <span>{fileError}</span>
                    </div>
                  )}
                </div>
              )}

              {/* BẢNG PREVIEW DỮ LIỆU EXCEL */}
              {previewData && (
                <div className="space-y-4 animate-in fade-in">
                  <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
                    <div className="flex items-center gap-2">
                      <h4 className="text-xs font-bold text-neutral-900">
                        Bước 3: Xem trước và kiểm tra tính hợp lệ
                      </h4>
                      <span className="text-[11px] font-semibold text-neutral-500">
                        (Tổng: {previewData.totalRows} dòng)
                      </span>
                    </div>

                    <div className="flex items-center gap-1.5 bg-neutral-100 p-1 rounded-xl text-xs">
                      <button
                        type="button"
                        onClick={() => setActiveTab('ALL')}
                        className={`px-3 py-1 rounded-lg font-bold transition-colors cursor-pointer ${
                          activeTab === 'ALL'
                            ? 'bg-white text-neutral-900 shadow-2xs'
                            : 'text-neutral-600 hover:text-neutral-900'
                        }`}
                      >
                        Tất cả ({previewData.totalRows})
                      </button>
                      <button
                        type="button"
                        onClick={() => setActiveTab('VALID')}
                        className={`px-3 py-1 rounded-lg font-bold transition-colors cursor-pointer ${
                          activeTab === 'VALID'
                            ? 'bg-emerald-600 text-white shadow-2xs'
                            : 'text-emerald-700 hover:bg-emerald-50'
                        }`}
                      >
                        Hợp lệ ({validCount})
                      </button>
                      <button
                        type="button"
                        onClick={() => setActiveTab('ERROR')}
                        className={`px-3 py-1 rounded-lg font-bold transition-colors cursor-pointer ${
                          activeTab === 'ERROR'
                            ? 'bg-red-600 text-white shadow-2xs'
                            : 'text-red-700 hover:bg-red-50'
                        }`}
                      >
                        Bị lỗi ({errorCount})
                      </button>
                    </div>
                  </div>

                  <div className="border border-neutral-200 rounded-xl overflow-hidden bg-white shadow-2xs">
                    <div className="overflow-x-auto max-h-[38vh]">
                      <table className="w-full text-left text-xs border-collapse">
                        <thead className="bg-neutral-100 text-neutral-700 font-bold border-b border-neutral-200 sticky top-0">
                          <tr>
                            <th className="p-2.5 w-14 text-center">Dòng</th>
                            <th className="p-2.5">Họ và tên</th>
                            <th className="p-2.5 w-20 text-center">Giới tính</th>
                            <th className="p-2.5 w-28 text-center">Ngày sinh</th>
                            <th className="p-2.5 w-32">CCCD / Hộ chiếu</th>
                            <th className="p-2.5 w-24 text-center">Số phòng</th>
                            <th className="p-2.5 w-28 text-center">Trạng thái</th>
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-neutral-100">
                          {filteredRows.map((row, idx) => {
                            const isRowValid = row.isValid ?? row.valid ?? false;
                            return (
                              <tr
                                key={idx}
                                className={isRowValid ? 'hover:bg-neutral-50/60' : 'bg-red-50/40 hover:bg-red-50/70'}
                              >
                                <td className="p-2.5 text-center font-mono text-neutral-500">
                                  {row.rowNumber || idx + 2}
                                </td>
                                <td className="p-2.5 font-bold text-neutral-900">
                                  {row.fullName}
                                </td>
                                <td className="p-2.5 text-center text-neutral-600">
                                  {row.gender || '-'}
                                </td>
                                <td className="p-2.5 text-center font-mono text-neutral-600">
                                  {row.dateOfBirth || '-'}
                                </td>
                                <td className="p-2.5 font-mono text-neutral-800">
                                  {row.idNumber || row.documentNumber || '-'}
                                </td>
                                <td className="p-2.5 text-center font-mono font-bold text-blue-700">
                                  {row.roomNumber || '-'}
                                </td>
                                <td className="p-2.5 text-center">
                                  {isRowValid ? (
                                    <span className="inline-flex items-center gap-1 text-[11px] font-bold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-full border border-emerald-200">
                                      <CheckCircle2 className="w-3.5 h-3.5" />
                                      Hợp lệ
                                    </span>
                                  ) : (
                                    <span
                                      className="inline-flex items-center gap-1 text-[11px] font-bold text-red-700 bg-red-100 px-2 py-0.5 rounded-full border border-red-200 cursor-help"
                                      title={
                                        row.errors && row.errors.length > 0
                                          ? typeof row.errors[0] === 'string'
                                            ? row.errors[0]
                                            : row.errors[0].message || row.errors[0].error || 'Lỗi dữ liệu'
                                          : 'Lỗi dữ liệu'
                                      }
                                    >
                                      <XCircle className="w-3.5 h-3.5" />
                                      {row.errors && row.errors.length > 0
                                        ? typeof row.errors[0] === 'string'
                                          ? row.errors[0]
                                          : row.errors[0].message || row.errors[0].error || 'Lỗi'
                                        : 'Lỗi'}
                                    </span>
                                  )}
                                </td>
                              </tr>
                            );
                          })}
                        </tbody>
                      </table>
                    </div>
                  </div>

                  {/* THANH NÚT XÁC NHẬN IMPORT */}
                  <div className="flex flex-col sm:flex-row items-center justify-between gap-3 pt-2 border-t border-neutral-200">
                    <div className="text-xs text-neutral-500">
                      {errorCount > 0 ? (
                        <span className="text-red-600 font-semibold flex items-center gap-1">
                          <AlertCircle className="w-4 h-4 shrink-0" />
                          Phát hiện {errorCount} dòng lỗi. Bạn cần sửa lại file Excel trước khi lưu.
                        </span>
                      ) : (
                        <span className="text-emerald-700 font-semibold flex items-center gap-1">
                          <CheckCircle2 className="w-4 h-4 shrink-0" />
                          Tất cả {validCount} dòng đều 100% hợp lệ, sẵn sàng lưu vào hệ thống!
                        </span>
                      )}
                    </div>

                    <div className="flex items-center gap-2">
                      <Button variant="outline" onClick={handleClose}>
                        Hủy bỏ
                      </Button>
                      <Button
                        variant="primary"
                        onClick={handleConfirmAndImport}
                        disabled={isSubmittingConfirm || errorCount > 0 || validCount === 0}
                        className="bg-emerald-600 hover:bg-emerald-700 text-white"
                      >
                        <Check className={`w-4 h-4 mr-1.5 ${isSubmittingConfirm ? 'animate-spin' : ''}`} />
                        {isSubmittingConfirm ? 'Đang lưu vào CSDL...' : `Xác nhận & Lưu ${validCount} khách`}
                      </Button>
                    </div>
                  </div>
                </div>
              )}
            </>
          )}
        </div>
      </Modal>

      {/* MODAL HƯỚNG DẪN & QUY CHUẨN TẠO FILE EXCEL */}
      <Modal
        isOpen={showGuideModal}
        onClose={() => setShowGuideModal(false)}
        title="Quy chuẩn & Bố cục tạo file Excel Rooming List"
        description="Bố cục bảng tính và các quy ước dữ liệu bắt buộc để hệ thống chấp thuận import"
        maxWidth="4xl"
      >
        <div className="space-y-6 max-h-[75vh] overflow-y-auto pr-1 text-xs">
          {/* BỐ CỤC BẢNG TÍNH MẪU MINH HỌA */}
          <div className="space-y-3">
            <h5 className="font-bold text-neutral-900 text-sm flex items-center gap-2">
              <FileSpreadsheet className="w-4 h-4 text-emerald-600" />
              1. Bố cục bảng tính mẫu (.xlsx) & Tải file mẫu
            </h5>
            
            <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 p-3.5 bg-emerald-50 border border-emerald-200 rounded-xl">
              <div className="space-y-0.5">
                <p className="font-bold text-emerald-950 text-xs flex items-center gap-1.5">
                  <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                  File Excel mẫu chuẩn gồm 9 cột và Sheet "RoomingList"
                </p>
                <p className="text-[11px] text-emerald-800">
                  Tải file mẫu về máy, mở bằng Excel hoặc Google Sheets và điền thông tin khách đoàn theo các cột có sẵn.
                </p>
              </div>
              <a
                href="/test-data/rooming-list-template.xlsx"
                download="rooming-list-template.xlsx"
                className="inline-flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-bold bg-emerald-600 hover:bg-emerald-700 text-white shadow-xs transition-all cursor-pointer shrink-0"
              >
                <FileSpreadsheet className="w-4 h-4 text-white" />
                Tải file Excel mẫu chuẩn (.xlsx)
              </a>
            </div>

            <p className="text-neutral-500 text-[11px]">
              Tên sheet dữ liệu bắt buộc là <strong>RoomingList</strong>. Hàng 1 là tiêu đề các cột cố định. Dữ liệu bắt đầu từ Hàng 2 (tối đa 500 dòng/lần tải lên).
            </p>

            <div className="border border-neutral-200 rounded-xl overflow-hidden shadow-xs bg-white">
              <div className="overflow-x-auto">
                <table className="w-full text-left border-collapse text-[11px]">
                  <thead className="bg-neutral-100 text-neutral-800 font-bold border-b border-neutral-200">
                    <tr>
                      <th className="p-2.5 border-r border-neutral-200 bg-neutral-200/60 w-10 text-center font-mono">#</th>
                      <th className="p-2.5 border-r border-neutral-200">Full Name *</th>
                      <th className="p-2.5 border-r border-neutral-200">Gender</th>
                      <th className="p-2.5 border-r border-neutral-200">Date of Birth</th>
                      <th className="p-2.5 border-r border-neutral-200">Nationality</th>
                      <th className="p-2.5 border-r border-neutral-200">ID Number</th>
                      <th className="p-2.5 border-r border-neutral-200">Phone</th>
                      <th className="p-2.5 border-r border-neutral-200">Email</th>
                      <th className="p-2.5 border-r border-neutral-200">Room Number</th>
                      <th className="p-2.5">Note</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-neutral-100 font-mono">
                    <tr className="bg-white">
                      <td className="p-2 border-r border-neutral-200 bg-neutral-50 text-center font-bold text-neutral-500">1</td>
                      <td className="p-2 border-r border-neutral-200 text-neutral-400 font-sans italic" colSpan={9}>
                        (Hàng 1 tiêu đề: Điền đúng tên tiếng Anh hoặc tiếng Việt chuẩn)
                      </td>
                    </tr>
                    <tr className="bg-emerald-50/40">
                      <td className="p-2 border-r border-neutral-200 bg-neutral-50 text-center font-bold text-neutral-500">2</td>
                      <td className="p-2 border-r border-neutral-200 font-sans font-bold text-neutral-900">Nguyễn Văn An</td>
                      <td className="p-2 border-r border-neutral-200 font-sans">Nam</td>
                      <td className="p-2 border-r border-neutral-200">12/03/1988</td>
                      <td className="p-2 border-r border-neutral-200 font-sans">Việt Nam</td>
                      <td className="p-2 border-r border-neutral-200">001088001234</td>
                      <td className="p-2 border-r border-neutral-200">0912345601</td>
                      <td className="p-2 border-r border-neutral-200 font-sans">an.nguyen@example.com</td>
                      <td className="p-2 border-r border-neutral-200 font-bold text-emerald-700 whitespace-nowrap">101A</td>
                      <td className="p-2 font-sans text-neutral-600">Trưởng đoàn</td>
                    </tr>
                    <tr className="bg-white">
                      <td className="p-2 border-r border-neutral-200 bg-neutral-50 text-center font-bold text-neutral-500">3</td>
                      <td className="p-2 border-r border-neutral-200 font-sans font-bold text-neutral-900">Trần Thị Bích</td>
                      <td className="p-2 border-r border-neutral-200 font-sans">Nữ</td>
                      <td className="p-2 border-r border-neutral-200">25/07/1990</td>
                      <td className="p-2 border-r border-neutral-200 font-sans">Việt Nam</td>
                      <td className="p-2 border-r border-neutral-200">001190002345</td>
                      <td className="p-2 border-r border-neutral-200">0912345602</td>
                      <td className="p-2 border-r border-neutral-200 font-sans">bich.tran@example.com</td>
                      <td className="p-2 border-r border-neutral-200 font-bold text-emerald-700 whitespace-nowrap">101A</td>
                      <td className="p-2 font-sans text-neutral-600">Thành viên đoàn</td>
                    </tr>
                  </tbody>
                </table>
              </div>
              <div className="bg-neutral-100 p-2.5 border-t border-neutral-200 flex items-center gap-2">
                <span className="px-2.5 py-1 bg-white rounded-md text-xs font-bold text-emerald-700 border border-neutral-300 shadow-2xs flex items-center gap-1.5">
                  <FileSpreadsheet className="w-3.5 h-3.5" />
                  Sheet: RoomingList
                </span>
                <span className="text-[11px] text-neutral-500">← Tên sheet bắt buộc viết liền không dấu</span>
              </div>
            </div>
          </div>

          {/* BẢNG QUY ƯỚC CÁC CỘT */}
          <div className="space-y-2">
            <h5 className="font-bold text-neutral-900 text-sm flex items-center gap-2">
              <CheckCircle2 className="w-4 h-4 text-emerald-600" />
              2. Bảng quy ước định dạng dữ liệu từng cột để được chấp thuận
            </h5>

            <div className="border border-neutral-200 rounded-xl overflow-hidden bg-white">
              <table className="w-full text-left border-collapse text-xs">
                <thead className="bg-neutral-50 text-neutral-700 font-semibold border-b border-neutral-200">
                  <tr>
                    <th className="p-2.5 w-28">Tên cột (Header)</th>
                    <th className="p-2.5 w-20 text-center">Bắt buộc</th>
                    <th className="p-2.5 w-44">Định dạng hợp lệ</th>
                    <th className="p-2.5">Quy ước kiểm tra & Chấp thuận</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-neutral-100">
                  <tr>
                    <td className="p-2.5 font-mono font-bold text-neutral-900">Full Name</td>
                    <td className="p-2.5 text-center">
                      <span className="px-2 py-0.5 rounded-full font-bold bg-red-100 text-red-800 text-[10px]">Có</span>
                    </td>
                    <td className="p-2.5 font-semibold text-neutral-700">Chuỗi ký tự (&le; 250)</td>
                    <td className="p-2.5 text-neutral-600">Họ và tên đầy đủ của khách. Tuyệt đối không để trống.</td>
                  </tr>
                  <tr>
                    <td className="p-2.5 font-mono font-bold text-neutral-900">Gender</td>
                    <td className="p-2.5 text-center text-neutral-400">Không</td>
                    <td className="p-2.5 text-neutral-700">Nam / Nữ / Khác</td>
                    <td className="p-2.5 text-neutral-600">Chấp nhận: Nam, Nữ, Khác hoặc MALE, FEMALE, OTHER (không phân biệt hoa/thường).</td>
                  </tr>
                  <tr>
                    <td className="p-2.5 font-mono font-bold text-neutral-900">Date of Birth</td>
                    <td className="p-2.5 text-center text-neutral-400">Không</td>
                    <td className="p-2.5 font-mono text-neutral-700">dd/MM/yyyy hoặc yyyy-MM-dd</td>
                    <td className="p-2.5 text-neutral-600">Ngày sinh hợp lệ trong quá khứ, năm sinh &ge; 1900. Không điền ngày ở tương lai hoặc ngày không tồn tại (như 31/02).</td>
                  </tr>
                  <tr>
                    <td className="p-2.5 font-mono font-bold text-neutral-900">Nationality</td>
                    <td className="p-2.5 text-center text-neutral-400">Không</td>
                    <td className="p-2.5 text-neutral-700">Chuỗi ký tự (&le; 100)</td>
                    <td className="p-2.5 text-neutral-600">Quốc tịch khách. Nếu để trống hệ thống sẽ tự động gán mặc định là &quot;Việt Nam&quot;.</td>
                  </tr>
                  <tr>
                    <td className="p-2.5 font-mono font-bold text-neutral-900">ID Number</td>
                    <td className="p-2.5 text-center text-neutral-400">Không</td>
                    <td className="p-2.5 font-mono text-neutral-700">CCCD (9-12 số) hoặc Hộ chiếu</td>
                    <td className="p-2.5 text-neutral-600"><strong>Phải duy nhất trong file</strong>. Nếu 2 dòng có cùng số CCCD/Hộ chiếu, hệ thống sẽ báo lỗi trùng lặp và từ chối.</td>
                  </tr>
                  <tr>
                    <td className="p-2.5 font-mono font-bold text-neutral-900">Phone</td>
                    <td className="p-2.5 text-center text-neutral-400">Không</td>
                    <td className="p-2.5 font-mono text-neutral-700">09xxxxxxxx hoặc +84xxxx</td>
                    <td className="p-2.5 text-neutral-600">Số điện thoại 10 số hợp lệ.</td>
                  </tr>
                  <tr>
                    <td className="p-2.5 font-mono font-bold text-neutral-900">Email</td>
                    <td className="p-2.5 text-center text-neutral-400">Không</td>
                    <td className="p-2.5 text-neutral-700">name@domain.com</td>
                    <td className="p-2.5 text-neutral-600">Định dạng email chuẩn RFC (chứa ký tự @ và tên miền hợp lệ).</td>
                  </tr>
                  <tr>
                    <td className="p-2.5 font-mono font-bold text-neutral-900">Room Number</td>
                    <td className="p-2.5 text-center text-neutral-400">Không</td>
                    <td className="p-2.5 font-bold text-emerald-700 font-mono">101A, 101B, 101C...</td>
                    <td className="p-2.5 text-neutral-600">Số phòng kèm hậu tố tòa/hạng phòng (VD: <strong>101A</strong>, <strong>101B</strong>, <strong>101C</strong>...). Tòa A (STD), Tòa B (SUP), Tòa C (DLX), Tòa D (STE), Tòa E (EXE), Tòa F (FAM). Hệ thống cũng tương thích với 101-A, 101 - STD hoặc 101. Có thể để trống nếu chưa xếp phòng.</td>
                  </tr>
                  <tr>
                    <td className="p-2.5 font-mono font-bold text-neutral-900">Note</td>
                    <td className="p-2.5 text-center text-neutral-400">Không</td>
                    <td className="p-2.5 text-neutral-700">Chuỗi ký tự (&le; 250)</td>
                    <td className="p-2.5 text-neutral-600">Ghi chú lưu trú (VD: Trưởng đoàn, ăn chay, khách VIP...).</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          {/* NGUYÊN TẮC CHẤP THUẬN IMPORT */}
          <div className="p-3.5 bg-blue-50 border border-blue-200 rounded-xl text-blue-900 space-y-1">
            <p className="font-bold text-xs flex items-center gap-1.5">
              <Info className="w-4 h-4 text-blue-600" />
              Nguyên tắc chấp thuận khi bấm Xác nhận Import:
            </p>
            <ul className="list-disc list-inside space-y-1 text-blue-800 text-[11px] pl-1">
              <li>File phải đúng định dạng Excel <code>.xlsx</code> (dung lượng dưới 10MB, tối đa 500 dòng).</li>
              <li>Tên sheet dữ liệu bắt buộc là <code>RoomingList</code>.</li>
              <li>Chỉ khi <strong>tất cả các dòng hợp lệ 100%</strong> (không có dòng nào vi phạm), hệ thống mới thực hiện lưu vào cơ sở dữ liệu. Nếu có bất kỳ dòng nào sai, hệ thống sẽ dừng lại và chỉ rõ dòng mấy, cột nào bị sai để bạn sửa.</li>
            </ul>
          </div>

          <div className="flex justify-end pt-2">
            <Button variant="primary" onClick={() => setShowGuideModal(false)}>
              Đã hiểu & Đóng
            </Button>
          </div>
        </div>
      </Modal>

      {/* MODAL TRA CỨU PHÒNG TRỐNG */}
      <Modal
        isOpen={isAvailableRoomsModalOpen}
        onClose={() => setIsAvailableRoomsModalOpen(false)}
        title="Danh sách phòng vật lý còn trống (Sẵn sàng - READY)"
        description="Tra cứu và sao chép số phòng để điền vào cột 'Room Number' trong file Excel."
        maxWidth="4xl"
      >
        <div className="space-y-4 max-h-[70vh] overflow-y-auto pr-1 text-xs">
          {/* Tab Loại phòng */}
          <div className="flex flex-wrap gap-1.5 p-1 bg-neutral-100 rounded-xl">
            {ROOM_TYPE_TABS.map((tab) => (
              <button
                key={tab.code}
                type="button"
                onClick={() => setAvailableRoomsTypeTab(tab.code)}
                className={`px-3 py-1.5 rounded-lg text-xs font-bold transition-all cursor-pointer ${
                  availableRoomsTypeTab === tab.code
                    ? 'bg-white text-neutral-900 shadow-2xs'
                    : 'text-neutral-600 hover:text-neutral-900'
                }`}
              >
                {tab.label}
              </button>
            ))}
          </div>

          {/* Thanh tìm kiếm */}
          <div className="relative">
            <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-neutral-400" />
            <input
              type="text"
              placeholder="Tìm theo số phòng, hạng phòng..."
              value={availableRoomsSearch}
              onChange={(e) => setAvailableRoomsSearch(e.target.value)}
              className="w-full pl-9 pr-3 py-2 text-xs bg-white border border-neutral-300 rounded-xl focus:outline-none focus:ring-1 focus:ring-blue-500"
            />
          </div>

          {/* Bảng danh sách phòng */}
          {isLoadingAvailableRooms ? (
            <div className="py-8 text-center text-neutral-500">
              <RefreshCw className="w-6 h-6 animate-spin mx-auto mb-2 text-blue-600" />
              <p>Đang tải danh sách phòng trống...</p>
            </div>
          ) : filteredAvailableRooms.length === 0 ? (
            <div className="py-8 text-center text-neutral-500 bg-neutral-50 rounded-xl border border-neutral-200">
              <p className="font-semibold text-neutral-700">Không tìm thấy phòng trống nào</p>
            </div>
          ) : (
            <div className="border border-neutral-200 rounded-xl overflow-hidden bg-white shadow-2xs max-h-60 overflow-y-auto">
              <table className="w-full text-left border-collapse text-xs">
                <thead className="bg-neutral-100 text-neutral-700 font-bold border-b border-neutral-200 sticky top-0">
                  <tr>
                    <th className="p-2.5">Số phòng</th>
                    <th className="p-2.5">Hạng phòng</th>
                    <th className="p-2.5 text-center">Sức chứa</th>
                    <th className="p-2.5 text-center">Thao tác</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-neutral-100">
                  {filteredAvailableRooms.map((room) => (
                    <tr key={room.id} className="hover:bg-blue-50/40">
                      <td className="p-2.5 font-mono font-bold text-neutral-900">
                        {room.roomNumber}
                      </td>
                      <td className="p-2.5 font-medium text-neutral-800">
                        {(() => {
                          const badge = ROOM_TYPE_BADGE_STYLE[(room.roomTypeCode || '').toUpperCase()];
                          return badge ? (
                            <span className={`inline-block px-2 py-0.5 rounded text-xs font-semibold border ${badge.bg} ${badge.text} ${badge.border}`}>
                              {room.roomTypeName || room.roomTypeCode}
                            </span>
                          ) : (
                            <span className="inline-block px-2 py-0.5 rounded text-xs font-semibold border bg-neutral-100 text-neutral-700 border-neutral-200">
                              {room.roomTypeName || room.roomTypeCode}
                            </span>
                          );
                        })()}
                      </td>
                      <td className="p-2.5 text-center text-neutral-600">
                        {room.maxTotalGuests || room.maxAdults || 2} người
                      </td>
                      <td className="p-2.5 text-center">
                        <button
                          type="button"
                          onClick={() => handleCopyRoom(room.roomNumber)}
                          className="inline-flex items-center gap-1 px-2.5 py-1 text-[11px] font-semibold rounded-lg border border-neutral-300 hover:border-blue-500 hover:text-blue-600 bg-white transition-all cursor-pointer shadow-2xs"
                        >
                          {copiedRoomNumber === room.roomNumber ? (
                            <>
                              <Check className="w-3.5 h-3.5 text-emerald-600" />
                              <span className="text-emerald-700 font-bold">Đã chép</span>
                            </>
                          ) : (
                            <>
                              <Copy className="w-3.5 h-3.5 text-neutral-500" />
                              <span>Chép số</span>
                            </>
                          )}
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          <div className="flex justify-end pt-2 border-t border-neutral-200">
            <Button variant="outline" onClick={() => setIsAvailableRoomsModalOpen(false)}>
              Đóng
            </Button>
          </div>
        </div>
      </Modal>
    </>
  );
};

export default RoomingListImportModal;
