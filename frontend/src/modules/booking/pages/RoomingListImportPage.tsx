import React, { useState, useEffect, useRef, useMemo } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import {
  FileSpreadsheet,
  Upload,
  Search,
  CheckCircle2,
  XCircle,
  AlertCircle,
  RefreshCw,
  Check,
  ArrowLeft,
  Building,
  User,
  BedDouble,
  X,
  FileCheck2,
  HelpCircle,
  Info,
  Copy,
} from 'lucide-react';
import Button from '../../../core/components/ui/Button';
import { Modal } from '../../../core/components/ui/Modal';
import bookingService from '../services/booking.service';
import roomInstanceService from '../../inventory/services/roomInstance.service';
import type { RoomInstanceDto } from '../../inventory/types/inventory.types';
import type {
  BookingResponse,
  RoomingListPreviewResponse,
  RoomingListImportResultResponse,
  RoomingListRowDto,
} from '../types/booking.types';

type TabType = 'ALL' | 'VALID' | 'ERROR';

// Thứ tự phân cấp chuẩn của các hạng phòng (Room Type Hierarchy)
const ROOM_TYPE_HIERARCHY: Record<string, number> = {
  STD: 1, // Standard Room
  SUP: 2, // Superior Room
  DLX: 3, // Deluxe Room
  STE: 4, // Suite
  EXE: 5, // Executive Suite
  FAM: 6, // Family Room
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

export const RoomingListImportPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  const queryBookingNumber = searchParams.get('bookingNumber') || '';

  // Booking Lookup State
  const [bookingNumberInput, setBookingNumberInput] = useState(queryBookingNumber);
  const [isLoadingBooking, setIsLoadingBooking] = useState(false);
  const [booking, setBooking] = useState<BookingResponse | null>(null);
  const [searchResults, setSearchResults] = useState<BookingResponse[]>([]);
  const [lookupError, setLookupError] = useState<string | null>(null);

  // Guide Modal State
  const [isGuideModalOpen, setIsGuideModalOpen] = useState(false);

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

  const fileInputRef = useRef<HTMLInputElement>(null);

  // Load booking if passed in URL query param
  useEffect(() => {
    if (queryBookingNumber) {
      handleLookupBooking(queryBookingNumber);
    }
  }, [queryBookingNumber]);

  const handleLookupBooking = async (inputStr: string) => {
    const cleanStr = inputStr.trim();
    if (!cleanStr) return;

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
      } catch (ignored) {
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
          `Không tìm thấy đơn đặt phòng nào với mã đơn hoặc số phòng: "${cleanStr}". Vui lòng kiểm tra lại.`
        );
      }
    } catch (err: any) {
      console.error('Lỗi tra cứu booking:', err);
      setLookupError('Xảy ra lỗi trong quá trình tra cứu đơn đặt phòng.');
    } finally {
      setIsLoadingBooking(false);
    }
  };

  const handleOpenAvailableRoomsModal = async () => {
    setIsAvailableRoomsModalOpen(true);
    setIsLoadingAvailableRooms(true);
    setAvailableRoomsSearch('');
    setAvailableRoomsTypeTab('ALL');
    try {
      try {
        const res = await bookingService.getVacantRooms(booking?.hotelId);
        if (res.result && res.result.length > 0) {
          setAvailableRooms(res.result);
          setIsLoadingAvailableRooms(false);
          return;
        }
      } catch (e) {
        // Fallback sang roomInstanceService nếu cần
      }

      const res = await roomInstanceService.filter({
        currentStatus: 'READY',
        hotelId: booking?.hotelId,
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

  const handleUploadAndPreview = async () => {
    if (!booking) {
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
      const res = await bookingService.previewRoomingList(booking.id, selectedFile);
      if (res.result) {
        setPreviewData(res.result);
        const errCount = res.result.errorCount ?? (res.result as any).errorRowsCount ?? 0;
        if (errCount > 0) {
          setActiveTab('ERROR');
        } else {
          setActiveTab('ALL');
        }
      } else {
        setFileError(res.message || 'Không thể đọc dữ liệu file Excel.');
      }
    } catch (err: any) {
      console.error('Lỗi khi tải file rooming list:', err);
      if (err?.response?.status === 404) {
        setFileError(
          'Lỗi kết nối Backend (HTTP 404): Server Backend (Spring Boot) chưa được khởi động lại để nhận các API mới. Bạn vui lòng restart lại Backend (Spring Boot) rồi thử lại nhé!'
        );
      } else {
        const msg =
          err?.response?.data?.message ||
          err?.message ||
          'Có lỗi xảy ra khi đọc file Excel. Vui lòng kiểm tra lại cấu trúc file.';
        setFileError(msg);
      }
    } finally {
      setIsLoadingPreview(false);
    }
  };

  const handleConfirmAndImport = async () => {
    if (!booking) {
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
      // 1. Gửi file lên để kiểm tra tính hợp lệ
      const previewRes = await bookingService.previewRoomingList(booking.id, selectedFile);
      const preview = previewRes.result;

      if (!preview) {
        setFileError('File không hợp lệ: Không thể đọc được dữ liệu từ file Excel.');
        return;
      }

      const validCount = preview.validCount ?? (preview as any).validRowsCount ?? 0;
      const errorCount = preview.errorCount ?? (preview as any).errorRowsCount ?? 0;

      // 2. Nếu có bất kỳ dòng nào bị lỗi hoặc không có dòng hợp lệ -> Báo lỗi, KHÔNG import
      if (errorCount > 0 || validCount === 0) {
        setPreviewData(preview); // Hiển thị chi tiết lỗi từng dòng để người dùng sửa file
        setActiveTab('ERROR'); // Tự động chuyển sang tab Lỗi để người dùng thấy ngay
        setFileError(
          `File không hợp lệ! Phát hiện ${errorCount} lỗi trên tổng số ${preview.totalRows} dòng dữ liệu. Vui lòng kiểm tra các dòng lỗi chi tiết bên dưới.`
        );
        return;
      }

      // 3. File 100% hợp lệ -> Lưu vào hệ thống
      const validRows = preview.rows.filter((r) => r.isValid ?? r.valid ?? true);
      const confirmRes = await bookingService.confirmRoomingList(booking.id, { guests: validRows });
      if (confirmRes.result) {
        setImportResult(confirmRes.result);
        setPreviewData(null);
      } else {
        setFileError(confirmRes.message || 'Lưu danh sách khách thất bại.');
      }
    } catch (err: any) {
      console.error('Lỗi khi kiểm tra và import file:', err);
      if (err?.response?.status === 404) {
        setFileError(
          'Lỗi kết nối Backend (HTTP 404): Server Backend (Spring Boot) chưa được khởi động lại để nhận các API mới. Bạn vui lòng restart lại Backend (Spring Boot) rồi thử lại nhé!'
        );
      } else {
        const msg =
          err?.response?.data?.message ||
          err?.message ||
          'Cấu trúc file không hợp lệ hoặc sai định dạng.';
        setFileError(`File không hợp lệ: ${msg}`);
      }
      setPreviewData(null);
    } finally {
      setIsSubmittingConfirm(false);
    }
  };

  const handleConfirmImport = async () => {
    if (!booking || !previewData) return;
    const validCount = previewData.validCount ?? (previewData as any).validRowsCount ?? 0;
    if (validCount === 0) return;

    const validRows = previewData.rows.filter((r) => r.isValid ?? r.valid ?? false);
    if (validRows.length === 0) return;

    setIsSubmittingConfirm(true);
    try {
      const res = await bookingService.confirmRoomingList(booking.id, { guests: validRows });
      if (res.result) {
        setImportResult(res.result);
      } else {
        alert(res.message || 'Lưu danh sách khách thất bại.');
      }
    } catch (err: any) {
      console.error('Lỗi khi xác nhận import:', err);
      const msg =
        err?.response?.data?.message ||
        err?.message ||
        'Xảy ra lỗi trong quá trình lưu danh sách khách.';
      alert(msg);
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

  // Chỉ lấy các phòng vật lý đang ở trạng thái READY (Trống/Sẵn sàng)
  // Bỏ qua toàn bộ các phòng đã đặt (OCCUPIED), dọn dẹp (CLEANING), bảo trì (MAINTENANCE)
  const onlyReadyRooms = useMemo(() => {
    return availableRooms.filter((room) => room.currentStatus === 'READY');
  }, [availableRooms]);

  // Đếm số lượng phòng trống theo từng hạng phòng cho thanh Tab
  const roomTypeCounts = useMemo(() => {
    const counts: Record<string, number> = { ALL: onlyReadyRooms.length };
    onlyReadyRooms.forEach((r) => {
      const code = (r.roomTypeCode || '').toUpperCase();
      counts[code] = (counts[code] || 0) + 1;
    });
    return counts;
  }, [onlyReadyRooms]);

  // Danh sách phòng trống hiển thị: Lọc theo tab Loại phòng, từ khóa tìm kiếm
  // Sắp xếp BẮT BUỘC: Mã loại phòng trước (STD -> SUP -> DLX -> STE -> EXE -> FAM), sau đó đến Số phòng tăng dần
  const filteredAvailableRooms: RoomInstanceDto[] = useMemo(() => {
    return onlyReadyRooms
      .filter((room) => {
        // 1. Lọc theo tab Loại phòng
        if (availableRoomsTypeTab !== 'ALL' && (room.roomTypeCode || '').toUpperCase() !== availableRoomsTypeTab) {
          return false;
        }

        // 2. Lọc theo từ khóa tìm kiếm (số phòng, tên hạng phòng, mã hạng phòng)
        if (!availableRoomsSearch.trim()) return true;
        const q = availableRoomsSearch.trim().toLowerCase();
        const rNum = (room.roomNumber || '').toLowerCase();
        const tName = (room.roomTypeName || '').toLowerCase();
        const tCode = (room.roomTypeCode || '').toLowerCase();
        return rNum.includes(q) || tName.includes(q) || tCode.includes(q);
      })
      .sort((a, b) => {
        // 1. Sắp xếp ưu tiên: Mã loại phòng (STD -> SUP -> DLX -> STE -> EXE -> FAM)
        const codeA = (a.roomTypeCode || '').toUpperCase();
        const codeB = (b.roomTypeCode || '').toUpperCase();
        const orderA = ROOM_TYPE_HIERARCHY[codeA] || 99;
        const orderB = ROOM_TYPE_HIERARCHY[codeB] || 99;
        if (orderA !== orderB) {
          return orderA - orderB;
        }

        // 2. Tiếp theo sắp xếp theo Số phòng tăng dần (ví dụ: 101 -> 102 -> 103 -> 201 -> 202)
        const numA = parseInt((a.roomNumber || '').replace(/\D/g, '') || '0', 10);
        const numB = parseInt((b.roomNumber || '').replace(/\D/g, '') || '0', 10);
        if (numA !== numB) {
          return numA - numB;
        }

        return (a.roomNumber || '').localeCompare(b.roomNumber || '');
      });
  }, [onlyReadyRooms, availableRoomsTypeTab, availableRoomsSearch]);

  return (
    <div className="space-y-6">
      {/* HEADER */}
      <div className="flex flex-col lg:flex-row justify-between items-start lg:items-center gap-4 bg-white p-5 rounded-2xl border border-neutral-200 shadow-xs">
        <div>
          <button
            onClick={() => navigate('/booking/list')}
            className="inline-flex items-center text-xs font-semibold text-neutral-500 hover:text-neutral-900 mb-1.5 transition-colors cursor-pointer"
          >
            <ArrowLeft className="w-3.5 h-3.5 mr-1" />
            Quay lại Danh sách Đặt phòng
          </button>
          <h1 className="text-xl sm:text-2xl font-bold text-neutral-900 flex items-center gap-2">
            <FileSpreadsheet className="w-6 h-6 sm:w-7 sm:h-7 text-red-600 shrink-0" />
            Nhập danh sách khách đoàn (Excel Rooming List)
          </h1>
          <p className="text-xs sm:text-sm text-neutral-500 mt-0.5">
            Tải lên file Excel danh sách khách, kiểm tra lỗi hợp lệ theo từng dòng và đồng bộ vào phòng lưu trú.
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2.5 shrink-0 w-full sm:w-auto">
          <button
            type="button"
            onClick={handleOpenAvailableRoomsModal}
            className="inline-flex items-center justify-center gap-2 px-4 py-2 text-xs font-semibold rounded-xl border border-neutral-300 bg-white hover:bg-blue-50/60 hover:border-blue-500 hover:text-blue-600 transition-all shadow-2xs cursor-pointer h-10 w-full sm:w-auto whitespace-nowrap"
          >
            <Search className="w-4 h-4 text-blue-600 shrink-0" />
            <span>Kiểm tra phòng còn trống</span>
          </button>
          <button
            type="button"
            onClick={() => setIsGuideModalOpen(true)}
            className="inline-flex items-center justify-center gap-2 px-4 py-2 text-xs font-semibold rounded-xl border border-neutral-300 bg-white hover:bg-red-50/60 hover:border-red-500 hover:text-red-600 transition-all shadow-2xs cursor-pointer h-10 w-full sm:w-auto whitespace-nowrap"
          >
            <HelpCircle className="w-4 h-4 text-neutral-600 shrink-0" />
            <span>Quy chuẩn & Bố cục file Excel</span>
          </button>
        </div>
      </div>

      {/* TRA CỨU ĐƠN ĐẶT PHÒNG */}
      <div className="bg-white p-5 rounded-2xl border border-neutral-200 shadow-xs space-y-4">
        <label className="block text-xs font-bold text-neutral-800 flex items-center gap-1.5">
          <Search className="w-4 h-4 text-red-600" />
          <span>Bước 1: Tra cứu đơn đặt phòng theo Mã đơn hoặc Số phòng</span>
        </label>

        <div className="flex flex-col sm:flex-row gap-2.5 items-stretch sm:items-center max-w-xl">
          <div className="flex-1">
            <input
              type="text"
              placeholder="Nhập mã đơn (VD: BK-...) hoặc Số phòng (VD: 101, 201)..."
              value={bookingNumberInput}
              onChange={(e) => setBookingNumberInput(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter') handleLookupBooking(bookingNumberInput);
              }}
              className="w-full px-3.5 py-2 text-xs bg-white border border-neutral-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-red-500/20 focus:border-red-500 h-10 transition-all shadow-2xs"
            />
          </div>
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

        {/* KẾT QUẢ TÌM THẤY NHIỀU ĐƠN (KHI TÌM THEO SỐ PHÒNG) */}
        {searchResults.length > 0 && (
          <div className="p-4 bg-neutral-50 rounded-xl border border-neutral-200 space-y-2.5 animate-in fade-in">
            <p className="text-xs font-bold text-neutral-800 flex items-center gap-1.5">
              <Search className="w-3.5 h-3.5 text-red-600" />
              Tìm thấy {searchResults.length} đơn đặt phòng liên quan tới "{bookingNumberInput}". Nhấp để chọn đơn:
            </p>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
              {searchResults.map((b) => {
                const rooms = b.bookingDetails
                  ?.flatMap((d) => d.bookingRooms || [])
                  .filter((r) => r.roomNumber)
                  .map((r) => r.roomNumber)
                  .join(', ') || 'Chưa xếp phòng';

                return (
                  <div
                    key={b.id}
                    onClick={() => {
                      setBooking(b);
                      setSearchResults([]);
                    }}
                    className="p-3.5 bg-white rounded-xl border border-neutral-200 hover:border-red-500 hover:shadow-xs cursor-pointer transition-all space-y-1.5"
                  >
                    <div className="flex items-center justify-between">
                      <span className="font-mono font-bold text-xs px-2 py-0.5 rounded bg-neutral-900 text-white">
                        {b.bookingNumber}
                      </span>
                      <span className="text-[11px] font-semibold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-full border border-emerald-200">
                        {b.status}
                      </span>
                    </div>
                    <p className="text-xs font-semibold text-neutral-900">
                      Khách đại diện: {b.guestName} ({b.guestPhone})
                    </p>
                    <p className="text-[11px] text-neutral-500">
                      Khách sạn: {b.hotelName} • Phòng: <strong className="text-emerald-700">{rooms}</strong>
                    </p>
                  </div>
                );
              })}
            </div>
          </div>
        )}

        {/* THÔNG TIN BOOKING ĐÃ CHỌN KÈM GỢI Ý CÁC PHÒNG */}
        {booking && (
          <div className="p-4 bg-neutral-50 rounded-xl border border-neutral-200 space-y-3 animate-in fade-in">
            <div className="flex flex-wrap items-center justify-between gap-4">
              <div className="space-y-1">
                <div className="flex items-center gap-2">
                  <span className="text-xs font-mono font-bold px-2 py-0.5 rounded-md bg-neutral-900 text-white">
                    {booking.bookingNumber}
                  </span>
                  <span className="text-xs font-semibold text-neutral-600 uppercase">
                    {booking.bookingType}
                  </span>
                  <span className="text-xs px-2 py-0.5 rounded-full font-bold bg-emerald-100 text-emerald-800">
                    {booking.status}
                  </span>
                </div>
                <div className="flex flex-wrap items-center gap-4 text-xs text-neutral-600 pt-1">
                  <span className="flex items-center gap-1">
                    <User className="w-3.5 h-3.5 text-neutral-400" />
                    <strong>Khách đại diện:</strong> {booking.guestName} ({booking.guestPhone})
                  </span>
                  <span className="flex items-center gap-1">
                    <Building className="w-3.5 h-3.5 text-neutral-400" />
                    <strong>Khách sạn:</strong> {booking.hotelName}
                  </span>
                </div>
              </div>

              <Button
                variant="outline"
                size="sm"
                onClick={() => {
                  setBooking(null);
                  setSearchResults([]);
                  setPreviewData(null);
                  setImportResult(null);
                }}
              >
                Chọn đơn khác
              </Button>
            </div>

            {/* GỢI Ý CÁC PHÒNG TRONG ĐƠN ĐỂ ĐIỀN VÀO FILE EXCEL */}
            <div className="pt-3 border-t border-neutral-200">
              <div className="flex items-center gap-1.5 text-xs font-bold text-neutral-800 mb-2">
                <BedDouble className="w-4 h-4 text-red-600" />
                <span>Gợi ý các phòng trong đơn này (Dùng để điền vào cột 'Room Number' trong file Excel):</span>
              </div>

              {(() => {
                const assignedPhysicalRooms =
                  booking.bookingDetails?.flatMap((d) =>
                    (d.bookingRooms || [])
                      .filter((r) => r.roomNumber)
                      .map((r) => ({
                        roomNumber: r.roomNumber!,
                        roomTypeName: d.roomTypeName,
                      }))
                  ) || [];

                if (assignedPhysicalRooms.length > 0) {
                  return (
                    <div className="space-y-2">
                      <div className="flex flex-wrap gap-2">
                        {assignedPhysicalRooms.map((r, i) => (
                          <span
                            key={i}
                            className="inline-flex items-center gap-1.5 px-3 py-1 rounded-lg bg-emerald-50 text-emerald-900 border border-emerald-200 text-xs font-medium whitespace-nowrap"
                          >
                            <span className="font-mono font-bold text-emerald-700 bg-emerald-100 px-2 py-0.5 rounded whitespace-nowrap inline-block">
                              Phòng {r.roomNumber}
                            </span>
                            <span className="text-[11px] text-neutral-600 whitespace-nowrap">({r.roomTypeName})</span>
                          </span>
                        ))}
                      </div>
                      <p className="text-[11px] text-neutral-500">
                        💡 <em>Gợi ý:</em> Điền đúng các mã số phòng trên (ví dụ: <strong>101 - STD</strong>, <strong>101 - SUP</strong>, <strong>101 - DLX</strong>...) vào cột <strong>Room Number</strong> trong file Excel để hệ thống tự động phân biệt và xếp khách vào đúng phòng vật lý.
                      </p>
                    </div>
                  );
                }

                return (
                  <div className="text-xs text-amber-800 bg-amber-50 border border-amber-200 p-2.5 rounded-lg">
                    Đơn này đã đặt{' '}
                    {booking.bookingDetails?.map((d) => `${d.quantity}x ${d.roomTypeName}`).join(', ')}{' '}
                    nhưng <strong>chưa được xếp số phòng vật lý cụ thể</strong>. Bạn có thể để trống cột <em>Room Number</em> trong Excel.
                  </div>
                );
              })()}
            </div>
          </div>
        )}
      </div>

      {/* VÙNG CHỌN VÀ NỘP FILE (Khi đã chọn booking) */}
      {booking && (
        <div className="bg-white p-6 rounded-2xl border border-neutral-200 shadow-xs space-y-5">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-neutral-100 pb-3">
            <div>
              <h4 className="text-sm font-bold text-neutral-900 flex items-center gap-2">
                <Upload className="w-4 h-4 text-red-600" />
                Bước 2: Nộp file Excel Rooming List
              </h4>
              <p className="text-xs text-neutral-500 mt-0.5">
                Kéo thả file để xem tên, sau đó bấm <strong>Xác nhận Import</strong> để hệ thống kiểm tra và lưu vào đơn đặt phòng.
              </p>
            </div>
            <div className="flex items-center gap-2">
              <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-neutral-100 text-neutral-600 border border-neutral-200">
                <FileSpreadsheet className="w-3.5 h-3.5 text-neutral-500" />
                Định dạng: <strong>.xlsx</strong> (&lt; 10MB, sheet: <strong>RoomingList</strong>)
              </span>
            </div>
          </div>

          {/* NẾU CHƯA CHỌN FILE: KHUNG KÉO THẢ PHONG CÁCH GOOGLE DRIVE */}
          {!selectedFile ? (
            <div
              onDrop={handleDrop}
              onDragOver={handleDragOver}
              onClick={() => fileInputRef.current?.click()}
              className="border-2 border-dashed border-neutral-300 hover:border-red-500 hover:bg-neutral-50/60 rounded-2xl p-10 text-center cursor-pointer transition-all duration-200 flex flex-col items-center justify-center space-y-3 group"
            >
              <div className="w-14 h-14 rounded-2xl bg-red-50 text-red-600 flex items-center justify-center group-hover:scale-105 transition-transform">
                <Upload className="w-7 h-7" />
              </div>

              <div>
                <p className="text-sm font-semibold text-neutral-800">
                  Kéo và thả file Excel vào đây, hoặc{' '}
                  <span className="text-red-600 font-bold hover:underline">duyệt từ máy tính</span>
                </p>
                <p className="text-xs text-neutral-400 mt-1">
                  Chỉ chấp nhận file .xlsx có sheet tên <strong>RoomingList</strong>, tối đa 500 dòng
                </p>
              </div>
            </div>
          ) : (
            /* NẾU ĐÃ CHỌN FILE: THẺ TỆP ĐÍNH KÈM (ATTACHMENT CARD) PHONG CÁCH DRIVE */
            <div className="space-y-4">
              <div className="flex items-center justify-between p-4 rounded-xl border border-neutral-200 bg-neutral-50/70 hover:bg-neutral-50 transition-colors">
                <div className="flex items-center gap-3.5 min-w-0">
                  <div className="w-12 h-12 rounded-xl bg-emerald-100 text-emerald-700 flex items-center justify-center shrink-0">
                    <FileSpreadsheet className="w-6 h-6" />
                  </div>
                  <div className="min-w-0">
                    <div className="flex items-center gap-2">
                      <p className="text-sm font-bold text-neutral-900 truncate" title={selectedFile.name}>
                        {selectedFile.name}
                      </p>
                      <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-semibold bg-emerald-100 text-emerald-800">
                        <FileCheck2 className="w-3 h-3 mr-1" />
                        Đã đính kèm
                      </span>
                    </div>
                    <p className="text-xs text-neutral-500 mt-0.5">
                      {(selectedFile.size / 1024).toFixed(1)} KB • Microsoft Excel (.xlsx)
                    </p>
                  </div>
                </div>

                <div className="flex items-center gap-2">
                  <button
                    onClick={handleRemoveFile}
                    disabled={isSubmittingConfirm || isLoadingPreview}
                    className="p-2 text-neutral-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                    title="Hủy file này để chọn file khác"
                  >
                    <X className="w-5 h-5" />
                  </button>
                </div>
              </div>

              {/* THÔNG BÁO LỖI NẾU FILE KHÔNG HỢP LỆ */}
              {fileError && (
                <div className="p-4 bg-red-50 border border-red-200 text-red-800 text-xs rounded-xl flex flex-col gap-3 animate-in fade-in">
                  <div className="flex items-start gap-2.5">
                    <AlertCircle className="w-5 h-5 shrink-0 text-red-600 mt-0.5" />
                    <div className="space-y-0.5">
                      <p className="font-bold text-sm text-red-900">File không hợp lệ!</p>
                      <p className="text-red-700 leading-relaxed">{fileError}</p>
                    </div>
                  </div>

                  {/* Danh sách các lỗi chi tiết: Dòng mấy, cột nào, thiếu hoặc sai gì */}
                  {previewData && previewData.errors && previewData.errors.length > 0 && (
                    <div className="bg-white/90 border border-red-200 rounded-xl p-3.5 space-y-2 max-h-64 overflow-y-auto">
                      <p className="font-bold text-red-900 text-xs flex items-center gap-1.5">
                        <XCircle className="w-4 h-4 text-red-600 shrink-0" />
                        Danh sách chi tiết các lỗi cần sửa trong file ({previewData.errors.length} lỗi):
                      </p>
                      <div className="space-y-1.5">
                        {previewData.errors.map((err, idx) => {
                          const colName = err.column || err.field || 'Dữ liệu';
                          const errMsg =
                            err.error ||
                            err.message ||
                            (typeof err === 'string' ? err : 'Lỗi không xác định');

                          return (
                            <div
                              key={idx}
                              className="p-2 rounded-lg bg-red-50/70 border border-red-100 flex flex-wrap sm:flex-nowrap items-start gap-2 text-xs"
                            >
                              <span className="font-mono font-bold px-2 py-0.5 rounded bg-red-200 text-red-950 shrink-0 text-[11px]">
                                Dòng {err.rowNumber}
                              </span>
                              <span className="font-bold text-neutral-900 shrink-0">
                                Cột [{colName}]:
                              </span>
                              <span className="text-red-700 font-medium">{errMsg}</span>
                            </div>
                          );
                        })}
                      </div>
                    </div>
                  )}
                </div>
              )}

              {/* NÚT THAO TÁC XÁC NHẬN NỘP / IMPORT */}
              <div className="flex flex-col sm:flex-row items-center justify-between gap-3 pt-3 border-t border-neutral-200">
                <span className="text-xs text-neutral-500 font-medium">
                  💡 Bấm <strong>Xem trước dữ liệu</strong> để kiểm tra lỗi từng dòng, hoặc bấm <strong>Xác nhận Import</strong> để nộp file vào đơn.
                </span>

                <div className="flex items-center gap-2.5 w-full sm:w-auto">
                  <Button
                    variant="outline"
                    onClick={handleUploadAndPreview}
                    disabled={isSubmittingConfirm || isLoadingPreview}
                    className="flex-1 sm:flex-none h-10 px-4 text-xs font-semibold rounded-xl border-neutral-300 shadow-2xs"
                  >
                    <RefreshCw className={`w-4 h-4 mr-1.5 ${isLoadingPreview ? 'animate-spin text-blue-600' : 'text-neutral-500'}`} />
                    <span>{isLoadingPreview ? 'Đang đọc...' : 'Xem trước dữ liệu (Preview)'}</span>
                  </Button>

                  <Button
                    variant="primary"
                    onClick={handleConfirmAndImport}
                    disabled={isSubmittingConfirm || isLoadingPreview}
                    className="flex-1 sm:flex-none h-10 px-6 text-xs font-bold rounded-xl shadow-xs"
                  >
                    {isSubmittingConfirm ? (
                      <>
                        <RefreshCw className="w-4 h-4 mr-1.5 animate-spin" />
                        <span>Đang kiểm tra & Import...</span>
                      </>
                    ) : (
                      <>
                        <Check className="w-4 h-4 mr-1.5" />
                        <span>Xác nhận Import</span>
                      </>
                    )}
                  </Button>
                </div>
              </div>
            </div>
          )}

          <input
            ref={fileInputRef}
            type="file"
            accept=".xlsx,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            onChange={handleFileChange}
            className="hidden"
          />
        </div>
      )}

      {/* KẾT QUẢ IMPORT THÀNH CÔNG */}
      {importResult && (
        <div className="bg-emerald-50 border border-emerald-200 rounded-2xl p-6 text-center space-y-4">
          <div className="w-16 h-16 bg-emerald-100 text-emerald-600 rounded-full flex items-center justify-center mx-auto">
            <CheckCircle2 className="w-10 h-10" />
          </div>
          <div>
            <h3 className="text-xl font-bold text-emerald-900">Import Rooming List Thành Công!</h3>
            <p className="text-sm text-emerald-700 mt-1">
              Dữ liệu danh sách khách lưu trú đã được lưu thành công vào đơn{' '}
              <strong>{booking?.bookingNumber}</strong>.
            </p>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 max-w-lg mx-auto text-left">
            <div className="bg-white p-4 rounded-xl border border-emerald-100 shadow-xs">
              <span className="text-xs text-neutral-500 font-medium">Đã thêm mới</span>
              <p className="text-3xl font-bold text-emerald-600">{importResult.importedCount}</p>
            </div>
            <div className="bg-white p-4 rounded-xl border border-emerald-100 shadow-xs">
              <span className="text-xs text-neutral-500 font-medium">Bỏ qua (Trùng lặp)</span>
              <p className="text-3xl font-bold text-amber-600">{importResult.skippedCount}</p>
            </div>
            <div className="bg-white p-4 rounded-xl border border-emerald-100 shadow-xs">
              <span className="text-xs text-neutral-500 font-medium">Lỗi bị từ chối</span>
              <p className="text-3xl font-bold text-neutral-500">{importResult.errorCount}</p>
            </div>
          </div>

          <div className="pt-4 flex justify-center gap-3">
            <Button variant="outline" onClick={() => navigate('/booking/list')}>
              Quay về Danh sách đặt phòng
            </Button>
            <Button
              variant="primary"
              onClick={() => {
                setImportResult(null);
                setPreviewData(null);
                setSelectedFile(null);
              }}
            >
              Tiếp tục Import file khác
            </Button>
          </div>
        </div>
      )}

      {/* VÙNG PREVIEW VÀ CONFIRM (Khi chưa confirm) */}
      {previewData && !importResult && (
        <div className="bg-white p-5 rounded-2xl border border-neutral-200 shadow-xs space-y-4">
          {(() => {
            const previewValidCount =
              previewData.validCount ?? (previewData as any).validRowsCount ?? 0;
            const previewErrorCount =
              previewData.errorCount ?? (previewData as any).errorRowsCount ?? 0;
            const previewTotalRows = previewData.totalRows || 0;

            return (
              <>
                <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
                  <div>
                    <h4 className="text-base font-bold text-neutral-900">
                      Bước 3: Xem trước dữ liệu và kiểm tra lỗi từng dòng
                    </h4>
                    <p className="text-xs text-neutral-500 mt-0.5">
                      Xem xét cẩn thận các dòng hợp lệ và các dòng bị lỗi trước khi xác nhận lưu vào hệ thống.
                    </p>
                  </div>

                  <Button
                    variant="primary"
                    onClick={handleConfirmImport}
                    disabled={previewValidCount === 0 || isSubmittingConfirm}
                  >
                    <Check className="w-4 h-4 mr-1.5" />
                    {isSubmittingConfirm
                      ? 'Đang lưu vào hệ thống...'
                      : `Xác nhận Import (${previewValidCount} khách hợp lệ)`}
                  </Button>
                </div>

                {/* KPI CARDS */}
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                  <div className="bg-neutral-50 p-4 rounded-xl border border-neutral-200 flex items-center justify-between">
                    <div>
                      <span className="text-xs font-medium text-neutral-500">Tổng số dòng đọc được</span>
                      <p className="text-2xl font-bold text-neutral-900">{previewTotalRows}</p>
                    </div>
                    <FileSpreadsheet className="w-8 h-8 text-neutral-400" />
                  </div>

                  <div className="bg-emerald-50/50 p-4 rounded-xl border border-emerald-200 flex items-center justify-between">
                    <div>
                      <span className="text-xs font-medium text-emerald-700">Dòng hợp lệ</span>
                      <p className="text-2xl font-bold text-emerald-600">{previewValidCount}</p>
                    </div>
                    <CheckCircle2 className="w-8 h-8 text-emerald-500" />
                  </div>

                  <div className="bg-red-50/50 p-4 rounded-xl border border-red-200 flex items-center justify-between">
                    <div>
                      <span className="text-xs font-medium text-red-700">Dòng bị lỗi</span>
                      <p className="text-2xl font-bold text-red-600">{previewErrorCount}</p>
                    </div>
                    <XCircle className="w-8 h-8 text-red-500" />
                  </div>
                </div>

                {/* FILTER TABS */}
                <div className="flex flex-wrap items-center justify-between gap-3 border-b border-neutral-200 pb-2">
                  <div className="flex gap-2">
                    <button
                      onClick={() => setActiveTab('ALL')}
                      className={`px-3.5 py-1.5 text-xs font-semibold rounded-lg transition-colors ${
                        activeTab === 'ALL'
                          ? 'bg-neutral-900 text-white'
                          : 'bg-neutral-100 text-neutral-600 hover:bg-neutral-200'
                      }`}
                    >
                      Tất cả ({previewTotalRows})
                    </button>
                    <button
                      onClick={() => setActiveTab('VALID')}
                      className={`px-3.5 py-1.5 text-xs font-semibold rounded-lg transition-colors flex items-center gap-1 ${
                        activeTab === 'VALID'
                          ? 'bg-emerald-600 text-white'
                          : 'bg-emerald-50 text-emerald-700 hover:bg-emerald-100'
                      }`}
                    >
                      <Check className="w-3.5 h-3.5" />
                      Hợp lệ ({previewValidCount})
                    </button>
                    <button
                      onClick={() => setActiveTab('ERROR')}
                      className={`px-3.5 py-1.5 text-xs font-semibold rounded-lg transition-colors flex items-center gap-1 ${
                        activeTab === 'ERROR'
                          ? 'bg-red-600 text-white'
                          : 'bg-red-50 text-red-700 hover:bg-red-100'
                      }`}
                    >
                      <AlertCircle className="w-3.5 h-3.5" />
                      Lỗi ({previewErrorCount})
                    </button>
                  </div>

                  <span className="text-xs text-neutral-500">
                    Hiển thị <strong>{filteredRows.length}</strong> / {previewTotalRows} dòng
                  </span>
                </div>

                {/* TABLE PREVIEW */}
                <div className="border border-neutral-200 rounded-xl overflow-hidden bg-white">
                  <div className="overflow-x-auto">
                    <table className="w-full text-left border-collapse text-xs">
                      <thead className="bg-neutral-50 text-neutral-600 font-semibold border-b border-neutral-200">
                        <tr>
                          <th className="p-3 w-16 text-center whitespace-nowrap">Dòng</th>
                          <th className="p-3 w-36 whitespace-nowrap">Số phòng</th>
                          <th className="p-3 whitespace-nowrap min-w-[140px]">Họ và tên</th>
                          <th className="p-3 w-24 text-center whitespace-nowrap">Giới tính</th>
                          <th className="p-3 w-28 text-center whitespace-nowrap">Ngày sinh</th>
                          <th className="p-3 whitespace-nowrap min-w-[130px]">CCCD / Hộ chiếu</th>
                          <th className="p-3 whitespace-nowrap min-w-[110px]">Điện thoại</th>
                          <th className="p-3 whitespace-nowrap min-w-[140px]">Email</th>
                          <th className="p-3 text-center w-28 whitespace-nowrap">Trạng thái</th>
                          <th className="p-3 min-w-[280px]">Lỗi sai chi tiết (Cột nào & Thiếu/Sai gì)</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-neutral-100">
                        {filteredRows.length === 0 ? (
                          <tr>
                            <td colSpan={10} className="p-8 text-center text-neutral-400">
                              Không có dữ liệu trong tab này.
                            </td>
                          </tr>
                        ) : (
                          filteredRows.map((row) => {
                            const isRowValid = row.isValid ?? row.valid ?? false;
                            const hasErrors =
                              !isRowValid && row.errors && row.errors.length > 0;

                            return (
                              <tr
                                key={row.rowNumber}
                                className={`transition-colors ${
                                  isRowValid
                                    ? 'hover:bg-neutral-50'
                                    : 'bg-red-50/40 hover:bg-red-50/70'
                                }`}
                              >
                                <td className="p-3 text-center font-mono font-bold text-neutral-700 whitespace-nowrap">
                                  Dòng {row.rowNumber}
                                </td>
                                <td className="p-3 font-semibold text-neutral-800 whitespace-nowrap">
                                  {row.roomNumber ? (
                                    <span className="inline-block whitespace-nowrap px-2.5 py-1 rounded-md bg-neutral-100 border border-neutral-300 text-neutral-900 font-mono font-bold text-xs shadow-2xs">
                                      {row.roomNumber}
                                    </span>
                                  ) : (
                                    <span className="text-neutral-400 italic text-xs">Chưa gán</span>
                                  )}
                                </td>
                                <td className="p-3 font-medium text-neutral-900">
                                  {row.fullName || '—'}
                                </td>
                                <td className="p-3 text-neutral-600">
                                  {row.genderRaw || row.gender || '—'}
                                </td>
                                <td className="p-3 text-neutral-600">
                                  {row.dateOfBirthRaw || row.dateOfBirth || '—'}
                                </td>
                                <td className="p-3 font-mono text-neutral-700">
                                  {row.idNumber || row.documentNumber || '—'}
                                </td>
                                <td className="p-3 text-neutral-600">{row.phone || '—'}</td>
                                <td className="p-3 text-neutral-600">{row.email || '—'}</td>
                                <td className="p-3 text-center">
                                  {isRowValid ? (
                                    <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-100 text-emerald-800">
                                      <Check className="w-3 h-3 mr-0.5" />
                                      Hợp lệ
                                    </span>
                                  ) : (
                                    <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-bold bg-red-100 text-red-800">
                                      <AlertCircle className="w-3 h-3 mr-0.5" />
                                      {row.errors?.length || 1} Lỗi
                                    </span>
                                  )}
                                </td>
                                <td className="p-3">
                                  {hasErrors ? (
                                    <div className="space-y-1">
                                      {row.errors?.map((err, errIdx) => {
                                        const errMsg =
                                          typeof err === 'string'
                                            ? err
                                            : err.error || err.message;
                                        const errCol =
                                          typeof err === 'string'
                                            ? ''
                                            : err.column || err.field;
                                        return (
                                          <div
                                            key={errIdx}
                                            className="text-red-900 bg-red-100/90 px-2 py-1 rounded-md text-[11px] font-medium border border-red-200 flex items-start gap-1.5"
                                          >
                                            {errCol && (
                                              <span className="font-bold text-red-950 shrink-0">
                                                Cột [{errCol}]:
                                              </span>
                                            )}
                                            <span className="text-red-800">{errMsg}</span>
                                          </div>
                                        );
                                      })}
                                    </div>
                                  ) : (
                                    <span className="text-emerald-700 text-xs italic">
                                      ✓ Hợp lệ
                                    </span>
                                  )}
                                </td>
                              </tr>
                            );
                          })
                        )}
                      </tbody>
                    </table>
                  </div>
                </div>
              </>
            );
          })()}
        </div>
      )}

      {/* MODAL HƯỚNG DẪN & QUY CHUẨN TẠO FILE EXCEL */}
      <Modal
        isOpen={isGuideModalOpen}
        onClose={() => setIsGuideModalOpen(false)}
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
                      <td className="p-2 border-r border-neutral-200 font-bold text-emerald-700 whitespace-nowrap">101 - STD</td>
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
                      <td className="p-2 border-r border-neutral-200 font-bold text-emerald-700 whitespace-nowrap">101 - STD</td>
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
                    <td className="p-2.5 text-neutral-600">Quốc tịch khách. Nếu để trống hệ thống sẽ tự động gán mặc định là "Việt Nam".</td>
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
                    <td className="p-2.5 font-bold text-emerald-700 font-mono">101 - STD, 101 - SUP...</td>
                    <td className="p-2.5 text-neutral-600">Số phòng kèm mã loại phòng (VD: <strong>101 - STD</strong>, <strong>101 - SUP</strong>, <strong>101 - DLX</strong>...) để phân biệt chính xác giữa các hạng phòng. Hệ thống cũng chấp nhận số phòng ngắn (VD: 101). Có thể để trống nếu chưa xếp phòng.</td>
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
            <Button variant="primary" onClick={() => setIsGuideModalOpen(false)}>
              Đã hiểu & Đóng
            </Button>
          </div>
        </div>
      </Modal>

      {/* MODAL KIỂM TRA PHÒNG CÒN TRỐNG */}
      <Modal
        isOpen={isAvailableRoomsModalOpen}
        onClose={() => setIsAvailableRoomsModalOpen(false)}
        title="Kiểm tra phòng còn trống (Available Rooms)"
        description={
          booking
            ? `Danh sách phòng sẵn sàng đón khách của khách sạn "${booking.hotelName}". Các phòng đã đặt (OCCUPIED) hoặc bảo trì đã được tự động loại bỏ.`
            : 'Danh sách các phòng vật lý đang ở trạng thái Trống & Sẵn sàng (READY). Các phòng đã đặt hoặc đang bảo trì đã được tự động loại bỏ.'
        }
        maxWidth="4xl"
      >
        <div className="space-y-4 max-h-[75vh] overflow-y-auto pr-1 text-xs">
          {/* THANH TAB PHÂN LOẠI HẠNG PHÒNG */}
          <div className="flex flex-wrap gap-1.5 border-b border-neutral-200 pb-2">
            {ROOM_TYPE_TABS.map((tab) => {
              const count = roomTypeCounts[tab.code] || 0;
              const isActive = availableRoomsTypeTab === tab.code;
              return (
                <button
                  key={tab.code}
                  type="button"
                  onClick={() => setAvailableRoomsTypeTab(tab.code)}
                  className={`px-3 py-1.5 text-xs font-semibold rounded-lg transition-all cursor-pointer flex items-center gap-1.5 ${
                    isActive
                      ? 'bg-blue-600 text-white shadow-xs'
                      : 'bg-neutral-100 text-neutral-600 hover:bg-neutral-200 hover:text-neutral-900'
                  }`}
                >
                  <span>{tab.label}</span>
                  <span
                    className={`text-[10px] px-1.5 py-0.2 rounded-full font-bold ${
                      isActive ? 'bg-white/20 text-white' : 'bg-neutral-200 text-neutral-700'
                    }`}
                  >
                    {count}
                  </span>
                </button>
              );
            })}
          </div>

          {/* Thanh tìm kiếm & thống kê */}
          <div className="flex flex-col sm:flex-row gap-3 items-stretch sm:items-center justify-between bg-neutral-50 p-3.5 rounded-xl border border-neutral-200">
            <div className="relative flex-1 max-w-sm">
              <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-neutral-400" />
              <input
                type="text"
                placeholder="Tìm theo số phòng (VD: 101 STD, 101 - SUP), hạng phòng..."
                value={availableRoomsSearch}
                onChange={(e) => setAvailableRoomsSearch(e.target.value)}
                className="w-full pl-9 pr-3 py-1.5 text-xs bg-white border border-neutral-300 rounded-lg focus:outline-none focus:ring-1 focus:ring-blue-500"
              />
            </div>

            <div className="flex items-center gap-3 text-neutral-600">
              <span className="font-semibold">
                Phòng trống khả dụng:{' '}
                <strong className="text-emerald-700 text-sm font-bold">
                  {filteredAvailableRooms.length}
                </strong>{' '}
                phòng
              </span>
              <span className="text-neutral-300">|</span>
              <span>
                Tổng sức chứa:{' '}
                <strong className="text-blue-700 font-bold">
                  {filteredAvailableRooms.reduce((sum, r) => sum + (r.maxTotalGuests || r.maxAdults || 2), 0)}
                </strong>{' '}
                người
              </span>
            </div>
          </div>

          {/* Danh sách phòng */}
          {isLoadingAvailableRooms ? (
            <div className="py-12 text-center text-neutral-500">
              <RefreshCw className="w-6 h-6 animate-spin mx-auto mb-2 text-blue-600" />
              <p>Đang tải danh sách phòng trống...</p>
            </div>
          ) : filteredAvailableRooms.length === 0 ? (
            <div className="py-12 text-center text-neutral-500 bg-neutral-50 rounded-xl border border-neutral-200">
              <AlertCircle className="w-8 h-8 mx-auto mb-2 text-amber-500" />
              <p className="font-semibold text-neutral-800">Không tìm thấy phòng trống nào phù hợp</p>
              <p className="text-[11px] text-neutral-500 mt-1">Hiện không có phòng nào ở trạng thái Sẵn sàng (READY) khớp với điều kiện lọc.</p>
            </div>
          ) : (
            <div className="border border-neutral-200 rounded-xl overflow-hidden bg-white shadow-2xs">
              <div className="overflow-x-auto">
                <table className="w-full text-left border-collapse text-xs min-w-[720px]">
                  <thead className="bg-neutral-100 text-neutral-700 font-semibold border-b border-neutral-200">
                    <tr>
                      <th className="p-3 w-36 whitespace-nowrap">Số phòng</th>
                      <th className="p-3 whitespace-nowrap">Hạng phòng</th>
                      <th className="p-3 w-24 text-center whitespace-nowrap">Tầng</th>
                      <th className="p-3 w-48 text-center whitespace-nowrap">Sức chứa tối đa</th>
                      <th className="p-3 w-36 text-center whitespace-nowrap">Trạng thái</th>
                      <th className="p-3 w-28 text-center whitespace-nowrap">Hành động</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-neutral-100">
                    {filteredAvailableRooms.map((room) => {
                      const baseDigits = (room.roomNumber || '').split(' - ')[0].trim();
                      const floor = baseDigits.length >= 3
                        ? `Tầng ${baseDigits.slice(0, -2)}`
                        : 'Tầng 1';
                      const maxA = room.maxAdults || 2;
                      const maxC = room.maxChildren || 0;
                      const maxT = room.maxTotalGuests || (maxA + maxC);
                      const code = (room.roomTypeCode || '').toUpperCase();
                      const style = ROOM_TYPE_BADGE_STYLE[code] || { bg: 'bg-neutral-100', text: 'text-neutral-700', border: 'border-neutral-200' };

                      return (
                        <tr key={room.id} className="hover:bg-blue-50/40 transition-colors">
                          <td className="p-3 whitespace-nowrap font-bold font-mono text-neutral-900 text-sm">
                            <span className="inline-block whitespace-nowrap font-mono font-bold text-xs px-2.5 py-1 rounded-md bg-neutral-100 border border-neutral-300 shadow-2xs text-neutral-900 tracking-wide">
                              {room.roomNumber}
                            </span>
                          </td>
                          <td className="p-3 whitespace-nowrap">
                            <div className="inline-flex items-center gap-2 whitespace-nowrap">
                              <span className={`px-2 py-0.5 rounded font-bold text-[11px] border whitespace-nowrap ${style.bg} ${style.text} ${style.border}`}>
                                {room.roomTypeCode}
                              </span>
                              <span className="font-semibold text-neutral-800 whitespace-nowrap">
                                {room.roomTypeName || room.roomTypeCode}
                              </span>
                            </div>
                          </td>
                          <td className="p-3 text-center text-neutral-600 font-medium whitespace-nowrap">
                            {floor}
                          </td>
                          <td className="p-3 text-center whitespace-nowrap">
                            <span className="inline-flex items-center gap-1 font-semibold text-emerald-800 bg-emerald-50 px-2.5 py-1 rounded-full border border-emerald-200 whitespace-nowrap">
                              Tối đa {maxT} người ({maxA} lớn{maxC > 0 ? `, ${maxC} trẻ` : ''})
                            </span>
                          </td>
                          <td className="p-3 text-center whitespace-nowrap">
                            <span className="inline-flex items-center gap-1 font-bold text-[11px] text-emerald-700 bg-emerald-100/80 px-2.5 py-1 rounded-full border border-emerald-300 whitespace-nowrap">
                              <CheckCircle2 className="w-3.5 h-3.5 shrink-0" />
                              Sẵn sàng (Trống)
                            </span>
                          </td>
                          <td className="p-3 text-center whitespace-nowrap">
                            <button
                              type="button"
                              onClick={() => handleCopyRoom(room.roomNumber)}
                              className="inline-flex items-center gap-1 px-2.5 py-1 text-[11px] font-semibold rounded-lg border border-neutral-300 hover:border-blue-500 hover:text-blue-600 bg-white transition-all cursor-pointer shadow-2xs whitespace-nowrap"
                            >
                              {copiedRoomNumber === room.roomNumber ? (
                                <>
                                  <Check className="w-3.5 h-3.5 text-emerald-600 shrink-0" />
                                  <span className="text-emerald-700 font-bold whitespace-nowrap">Đã chép</span>
                                </>
                              ) : (
                                <>
                                  <Copy className="w-3.5 h-3.5 text-neutral-500 shrink-0" />
                                  <span className="whitespace-nowrap">Chép số</span>
                                </>
                              )}
                            </button>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          <div className="flex justify-between items-center pt-2 border-t border-neutral-200">
            <span className="text-neutral-500 text-[11px]">
              Mẹo: Các phòng được sắp xếp theo Mã loại phòng (STD &rarr; SUP &rarr; DLX &rarr; STE &rarr; EXE &rarr; FAM) rồi đến Số phòng. Bấm <strong>Chép số</strong> để dán vào file Excel.
            </span>
            <Button variant="outline" onClick={() => setIsAvailableRoomsModalOpen(false)}>
              Đóng
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
};

export default RoomingListImportPage;
