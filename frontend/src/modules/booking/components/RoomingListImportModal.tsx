import React, { useState, useRef } from 'react';
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
  Info,
} from 'lucide-react';
import { Modal } from '../../../core/components/ui/Modal';
import Button from '../../../core/components/ui/Button';
import bookingService from '../services/booking.service';
import type {
  RoomingListPreviewResponse,
  RoomingListImportResultResponse,
  RoomingListRowDto,
} from '../types/booking.types';

interface RoomingListImportModalProps {
  isOpen: boolean;
  onClose: () => void;
  bookingId: number;
  bookingNumber: string;
  onSuccess?: (result: RoomingListImportResultResponse) => void;
}

type TabType = 'ALL' | 'VALID' | 'ERROR';

export const RoomingListImportModal: React.FC<RoomingListImportModalProps> = ({
  isOpen,
  onClose,
  bookingId,
  bookingNumber,
  onSuccess,
}) => {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [fileError, setFileError] = useState<string | null>(null);
  const [isLoadingPreview, setIsLoadingPreview] = useState(false);
  const [isSubmittingConfirm, setIsSubmittingConfirm] = useState(false);

  const [previewData, setPreviewData] = useState<RoomingListPreviewResponse | null>(null);
  const [importResult, setImportResult] = useState<RoomingListImportResultResponse | null>(null);
  const [activeTab, setActiveTab] = useState<TabType>('ALL');
  const [showGuideModal, setShowGuideModal] = useState(false);

  const fileInputRef = useRef<HTMLInputElement>(null);

  const resetState = () => {
    setSelectedFile(null);
    setFileError(null);
    setIsLoadingPreview(false);
    setIsSubmittingConfirm(false);
    setPreviewData(null);
    setImportResult(null);
    setActiveTab('ALL');
    setShowGuideModal(false);
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  const handleClose = () => {
    resetState();
    onClose();
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
    if (!selectedFile) {
      setFileError('Vui lòng chọn file Excel.');
      return;
    }

    setIsLoadingPreview(true);
    setFileError(null);
    setPreviewData(null);

    try {
      const res = await bookingService.previewRoomingList(bookingId, selectedFile);
      if (res.result) {
        setPreviewData(res.result);
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
    if (!selectedFile) {
      setFileError('Vui lòng chọn file Excel.');
      return;
    }

    setIsSubmittingConfirm(true);
    setFileError(null);
    setImportResult(null);

    try {
      // 1. Gửi file lên để kiểm tra tính hợp lệ
      const previewRes = await bookingService.previewRoomingList(bookingId, selectedFile);
      const preview = previewRes.result;

      if (!preview) {
        setFileError('File không hợp lệ: Không thể đọc được dữ liệu từ file Excel.');
        return;
      }

      const validCount = preview.validCount ?? (preview as any).validRowsCount ?? 0;
      const errorCount = preview.errorCount ?? (preview as any).errorRowsCount ?? 0;

      // 2. Nếu có lỗi dữ liệu hoặc không có dòng hợp lệ -> Báo lỗi, KHÔNG import
      if (errorCount > 0 || validCount === 0) {
        setPreviewData(preview); // Bung bảng chi tiết lỗi để người dùng xem và sửa file
        setActiveTab('ERROR');
        setFileError(
          `File không hợp lệ! Phát hiện ${errorCount} lỗi trên tổng số ${preview.totalRows} dòng dữ liệu. Vui lòng kiểm tra các dòng lỗi bên dưới.`
        );
        return;
      }

      // 3. File 100% hợp lệ -> Lưu vào hệ thống
      const validRows = preview.rows.filter((r) => r.isValid ?? r.valid ?? false);
      const confirmRes = await bookingService.confirmRoomingList(bookingId, { guests: validRows });
      if (confirmRes.result) {
        setImportResult(confirmRes.result);
        setPreviewData(null);
        if (onSuccess) {
          onSuccess(confirmRes.result);
        }
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
    if (!previewData) return;
    const validCount = previewData.validCount ?? (previewData as any).validRowsCount ?? 0;
    if (validCount === 0) return;

    const validRows = previewData.rows.filter((r) => r.isValid ?? r.valid ?? false);
    if (validRows.length === 0) return;

    setIsSubmittingConfirm(true);
    try {
      const res = await bookingService.confirmRoomingList(bookingId, { guests: validRows });
      if (res.result) {
        setImportResult(res.result);
        if (onSuccess) {
          onSuccess(res.result);
        }
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

  const validCount = previewData ? (previewData.validCount ?? (previewData as any).validRowsCount ?? 0) : 0;
  const errorCount = previewData ? (previewData.errorCount ?? (previewData as any).errorRowsCount ?? 0) : 0;

  return (
    <>
      <Modal
        isOpen={isOpen}
        onClose={handleClose}
        title="Nhập danh sách khách đoàn (Excel Rooming List)"
        description={`Mã đơn đặt phòng: ${bookingNumber} (ID: ${bookingId})`}
        maxWidth="5xl"
      >
        <div className="space-y-6 max-h-[78vh] overflow-y-auto px-1 py-1">
          {/* Kết quả sau khi Import thành công */}
          {importResult ? (
            <div className="p-6 bg-emerald-50 border border-emerald-200 rounded-2xl text-center space-y-4">
              <div className="w-16 h-16 bg-emerald-100 text-emerald-600 rounded-full flex items-center justify-center mx-auto">
                <CheckCircle2 className="w-10 h-10" />
              </div>
              <div>
                <h3 className="text-xl font-bold text-emerald-900">Import thành công!</h3>
                <p className="text-sm text-emerald-700 mt-1">
                  Danh sách khách lưu trú đã được lưu thành công vào đơn đặt phòng{' '}
                  <strong>{bookingNumber}</strong>.
                </p>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 max-w-lg mx-auto text-left">
                <div className="bg-white p-3 rounded-xl border border-emerald-100 shadow-xs">
                  <span className="text-xs text-neutral-500 font-medium">Đã thêm mới</span>
                  <p className="text-2xl font-bold text-emerald-600">{importResult.importedCount}</p>
                </div>
                <div className="bg-white p-3 rounded-xl border border-emerald-100 shadow-xs">
                  <span className="text-xs text-neutral-500 font-medium">Bỏ qua (Trùng lặp)</span>
                  <p className="text-2xl font-bold text-amber-600">{importResult.skippedCount}</p>
                </div>
                <div className="bg-white p-3 rounded-xl border border-emerald-100 shadow-xs">
                  <span className="text-xs text-neutral-500 font-medium">Bị từ chối (Lỗi)</span>
                  <p className="text-2xl font-bold text-neutral-500">{importResult.errorCount}</p>
                </div>
              </div>

              <div className="pt-4 flex justify-center gap-3">
                <Button variant="primary" onClick={handleClose}>
                  <Check className="w-4 h-4 mr-1.5" />
                  Hoàn tất
                </Button>
              </div>
            </div>
          ) : (
            <>
              {/* VÙNG UPLOAD FILE */}
              <div className="bg-neutral-50 p-5 rounded-2xl border border-neutral-200 space-y-4">
                <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
                  <div>
                    <h4 className="text-sm font-bold text-neutral-900 flex items-center gap-2">
                      <FileSpreadsheet className="w-4 h-4 text-red-600" />
                      Bước 1: Chọn file Excel Rooming List
                    </h4>
                    <p className="text-xs text-neutral-500 mt-0.5">
                      Hỗ trợ file định dạng <strong>.xlsx</strong>, tối đa 500 dòng, kích thước &lt; 10MB. Sheet: <strong>RoomingList</strong>.
                    </p>
                  </div>

                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => setShowGuideModal(true)}
                    className="text-xs text-neutral-700 bg-white hover:bg-neutral-100 font-medium border-neutral-300"
                  >
                    <HelpCircle className="w-3.5 h-3.5 mr-1.5 text-red-600" />
                    Quy chuẩn & Bố cục file
                  </Button>
                </div>

                {/* NẾU CHƯA CHỌN FILE: KHUNG KÉO THẢ STYLE GOOGLE DRIVE */}
                {!selectedFile ? (
                  <div
                    onDrop={handleDrop}
                    onDragOver={handleDragOver}
                    onClick={() => fileInputRef.current?.click()}
                    className="border-2 border-dashed border-neutral-300 hover:border-red-500 hover:bg-neutral-50/50 rounded-2xl p-8 text-center cursor-pointer transition-all duration-200 flex flex-col items-center justify-center space-y-2 group"
                  >
                    <div className="w-12 h-12 rounded-xl bg-red-50 text-red-600 flex items-center justify-center group-hover:scale-105 transition-transform">
                      <Upload className="w-6 h-6" />
                    </div>

                    <div>
                      <p className="text-sm font-semibold text-neutral-800">
                        Kéo và thả file Excel vào đây, hoặc{' '}
                        <span className="text-red-600 font-bold hover:underline">duyệt từ máy tính</span>
                      </p>
                      <p className="text-xs text-neutral-400 mt-0.5">
                        File .xlsx có sheet <strong>RoomingList</strong>, tối đa 500 dòng
                      </p>
                    </div>
                  </div>
                ) : (
                  /* NẾU ĐÃ CHỌN FILE: THẺ TỆP ĐÍNH KÈM STYLE DRIVE */
                  <div className="space-y-4">
                    <div className="flex items-center justify-between p-3.5 rounded-xl border border-neutral-200 bg-white shadow-xs">
                      <div className="flex items-center gap-3 min-w-0">
                        <div className="w-10 h-10 rounded-lg bg-emerald-100 text-emerald-700 flex items-center justify-center shrink-0">
                          <FileSpreadsheet className="w-5 h-5" />
                        </div>
                        <div className="min-w-0">
                          <div className="flex items-center gap-2">
                            <p className="text-sm font-bold text-neutral-900 truncate" title={selectedFile.name}>
                              {selectedFile.name}
                            </p>
                            <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-semibold bg-emerald-100 text-emerald-800">
                              <FileCheck2 className="w-3 h-3 mr-0.5" />
                              Đã đính kèm
                            </span>
                          </div>
                          <p className="text-xs text-neutral-500 mt-0.5">
                            {(selectedFile.size / 1024).toFixed(1)} KB • Microsoft Excel (.xlsx)
                          </p>
                        </div>
                      </div>

                      <button
                        onClick={handleRemoveFile}
                        disabled={isSubmittingConfirm || isLoadingPreview}
                        className="p-1.5 text-neutral-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                        title="Hủy file này để chọn file khác"
                      >
                        <X className="w-4 h-4" />
                      </button>
                    </div>

                    {/* Thông báo lỗi nếu file không hợp lệ */}
                    {fileError && (
                      <div className="p-3.5 bg-red-50 border border-red-200 text-red-800 text-xs rounded-xl flex flex-col gap-2.5 animate-in fade-in">
                        <div className="flex items-start gap-2.5">
                          <AlertCircle className="w-4 h-4 shrink-0 text-red-600 mt-0.5" />
                          <div className="space-y-0.5">
                            <p className="font-bold text-xs text-red-900">File không hợp lệ!</p>
                            <p className="text-red-700 leading-relaxed">{fileError}</p>
                          </div>
                        </div>

                        {/* Danh sách các lỗi chi tiết: Dòng mấy, cột nào, thiếu hoặc sai gì */}
                        {previewData && previewData.errors && previewData.errors.length > 0 && (
                          <div className="bg-white/90 border border-red-200 rounded-lg p-3 space-y-1.5 max-h-56 overflow-y-auto">
                            <p className="font-bold text-red-900 text-xs flex items-center gap-1.5">
                              <XCircle className="w-3.5 h-3.5 text-red-600 shrink-0" />
                              Chi tiết các lỗi trong file ({previewData.errors.length} lỗi):
                            </p>
                            <div className="space-y-1">
                              {previewData.errors.map((err, idx) => (
                                <div
                                  key={idx}
                                  className="p-1.5 rounded bg-red-50/70 border border-red-100 flex items-start gap-1.5 text-xs"
                                >
                                  <span className="font-mono font-bold px-1.5 py-0.5 rounded bg-red-200 text-red-950 shrink-0 text-[10px]">
                                    Dòng {err.rowNumber}
                                  </span>
                                  <span className="font-bold text-neutral-900 shrink-0">
                                    Cột [{err.column || err.field || 'Dữ liệu'}]:
                                  </span>
                                  <span className="text-red-700 font-medium">{err.error || err.message}</span>
                                </div>
                              ))}
                            </div>
                          </div>
                        )}
                      </div>
                    )}

                    {/* Nút hành động Xác nhận Import & Xem trước */}
                    <div className="flex flex-col sm:flex-row items-center justify-end gap-2.5 pt-1">
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={handleUploadAndPreview}
                        disabled={isSubmittingConfirm || isLoadingPreview}
                        className="w-full sm:w-auto"
                      >
                        <RefreshCw
                          className={`w-3.5 h-3.5 mr-1.5 ${isLoadingPreview ? 'animate-spin' : ''}`}
                        />
                        {isLoadingPreview ? 'Đang đọc...' : 'Xem trước (Preview)'}
                      </Button>

                      <Button
                        variant="primary"
                        size="sm"
                        onClick={handleConfirmAndImport}
                        disabled={isSubmittingConfirm || isLoadingPreview}
                        className="w-full sm:w-auto px-5 font-bold"
                      >
                        {isSubmittingConfirm ? (
                          <>
                            <RefreshCw className="w-3.5 h-3.5 mr-1.5 animate-spin" />
                            Đang kiểm tra & Import...
                          </>
                        ) : (
                          <>
                            <Check className="w-3.5 h-3.5 mr-1.5" />
                            Xác nhận Import
                          </>
                        )}
                      </Button>
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

              {/* VÙNG KẾT QUẢ PREVIEW */}
              {previewData && (
                <div className="space-y-4">
                  {/* Thanh thống kê KPI */}
                  <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                    <div className="bg-white p-4 rounded-xl border border-neutral-200 shadow-xs flex items-center justify-between">
                      <div>
                        <span className="text-xs font-medium text-neutral-500">Tổng số dòng đọc được</span>
                        <p className="text-2xl font-bold text-neutral-900">{previewData.totalRows}</p>
                      </div>
                      <FileSpreadsheet className="w-8 h-8 text-neutral-400" />
                    </div>

                    <div className="bg-white p-4 rounded-xl border border-emerald-200 shadow-xs flex items-center justify-between">
                      <div>
                        <span className="text-xs font-medium text-emerald-700">Dòng hợp lệ (Sẵn sàng import)</span>
                        <p className="text-2xl font-bold text-emerald-600">{validCount}</p>
                      </div>
                      <CheckCircle2 className="w-8 h-8 text-emerald-400" />
                    </div>

                    <div className="bg-white p-4 rounded-xl border border-red-200 shadow-xs flex items-center justify-between">
                      <div>
                        <span className="text-xs font-medium text-red-700">Dòng bị lỗi</span>
                        <p className="text-2xl font-bold text-red-600">{errorCount}</p>
                      </div>
                      <XCircle className="w-8 h-8 text-red-400" />
                    </div>
                  </div>

                  {/* Filter Tabs */}
                  <div className="flex flex-wrap items-center justify-between gap-3 border-b border-neutral-200 pb-2">
                    <div className="flex gap-2">
                      <button
                        onClick={() => setActiveTab('ALL')}
                        className={`px-3 py-1.5 text-xs font-semibold rounded-lg transition-colors ${
                          activeTab === 'ALL'
                            ? 'bg-neutral-900 text-white'
                            : 'bg-neutral-100 text-neutral-600 hover:bg-neutral-200'
                        }`}
                      >
                        Tất cả ({previewData.totalRows})
                      </button>
                      <button
                        onClick={() => setActiveTab('VALID')}
                        className={`px-3 py-1.5 text-xs font-semibold rounded-lg transition-colors flex items-center gap-1 ${
                          activeTab === 'VALID'
                            ? 'bg-emerald-600 text-white'
                            : 'bg-emerald-50 text-emerald-700 hover:bg-emerald-100'
                        }`}
                      >
                        <Check className="w-3.5 h-3.5" />
                        Hợp lệ ({validCount})
                      </button>
                      <button
                        onClick={() => setActiveTab('ERROR')}
                        className={`px-3 py-1.5 text-xs font-semibold rounded-lg transition-colors flex items-center gap-1 ${
                          activeTab === 'ERROR'
                            ? 'bg-red-600 text-white'
                            : 'bg-red-50 text-red-700 hover:bg-red-100'
                        }`}
                      >
                        <AlertCircle className="w-3.5 h-3.5" />
                        Lỗi ({errorCount})
                      </button>
                    </div>

                    <span className="text-xs text-neutral-500">
                      Hiển thị <strong>{filteredRows.length}</strong> / {previewData.totalRows} dòng
                    </span>
                  </div>

                  {/* BẢNG PREVIEW DATA */}
                  <div className="border border-neutral-200 rounded-xl overflow-hidden bg-white">
                    <div className="overflow-x-auto max-h-[350px]">
                      <table className="w-full text-left border-collapse text-xs">
                        <thead className="bg-neutral-50 text-neutral-600 font-semibold sticky top-0 border-b border-neutral-200">
                          <tr>
                            <th className="p-3 w-14 text-center">Dòng</th>
                            <th className="p-3 w-20">Phòng</th>
                            <th className="p-3">Họ và tên</th>
                            <th className="p-3 w-20">Giới tính</th>
                            <th className="p-3 w-24">Ngày sinh</th>
                            <th className="p-3">CCCD / Hộ chiếu</th>
                            <th className="p-3">Điện thoại</th>
                            <th className="p-3">Email</th>
                            <th className="p-3 text-center w-24">Trạng thái</th>
                            <th className="p-3 min-w-[240px]">Lỗi sai chi tiết (Cột nào & Thiếu/Sai gì)</th>
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-neutral-100">
                          {filteredRows.length === 0 ? (
                            <tr>
                              <td colSpan={10} className="p-6 text-center text-neutral-400">
                                Không có dòng dữ liệu nào trong tab này.
                              </td>
                            </tr>
                          ) : (
                            filteredRows.map((row) => {
                              const isRowValid = row.isValid ?? row.valid ?? false;
                              const hasErrors = !isRowValid && row.errors && row.errors.length > 0;
                              const docNo = row.idNumber || row.documentNumber || '—';

                              return (
                                <tr
                                  key={row.rowNumber}
                                  className={`transition-colors ${
                                    isRowValid ? 'hover:bg-neutral-50' : 'bg-red-50/40 hover:bg-red-50/70'
                                  }`}
                                >
                                  <td className="p-3 text-center font-mono font-bold text-neutral-700">
                                    Dòng {row.rowNumber}
                                  </td>
                                  <td className="p-3 font-semibold text-neutral-800">
                                    {row.roomNumber ? (
                                      <span className="px-2 py-0.5 rounded-md bg-neutral-100 text-neutral-700">
                                        {row.roomNumber}
                                      </span>
                                    ) : (
                                      <span className="text-neutral-400 italic">Chưa gán</span>
                                    )}
                                  </td>
                                  <td className="p-3 font-medium text-neutral-900">{row.fullName || '—'}</td>
                                  <td className="p-3 text-neutral-600">{row.gender || '—'}</td>
                                  <td className="p-3 text-neutral-600">{row.dateOfBirth || '—'}</td>
                                  <td className="p-3 font-mono text-neutral-700">{docNo}</td>
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
                                          const colName = typeof err === 'string' ? '' : (err.column || err.field || '');
                                          const errMsg = typeof err === 'string' ? err : (err.error || err.message || '');
                                          return (
                                            <div
                                              key={errIdx}
                                              className="text-red-900 bg-red-100/90 px-2 py-1 rounded-md text-[11px] font-medium border border-red-200 flex items-start gap-1.5"
                                            >
                                              {colName && (
                                                <span className="font-bold text-red-950 shrink-0">
                                                  Cột [{colName}]:
                                                </span>
                                              )}
                                              <span className="text-red-800">{errMsg}</span>
                                            </div>
                                          );
                                        })}
                                      </div>
                                    ) : (
                                      <span className="text-emerald-700 text-xs italic">✓ Hợp lệ</span>
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

                  {/* Cảnh báo các dòng lỗi sẽ bị bỏ qua */}
                  {errorCount > 0 && (
                    <div className="p-3 bg-amber-50 border border-amber-200 text-amber-800 text-xs rounded-xl flex items-start gap-2">
                      <AlertCircle className="w-4 h-4 text-amber-600 shrink-0 mt-0.5" />
                      <div>
                        <span className="font-semibold">Lưu ý nghiệp vụ:</span> Có{' '}
                        <strong>{errorCount}</strong> dòng bị lỗi sẽ không được import
                        vào hệ thống. Hệ thống sẽ chỉ tiến hành import{' '}
                        <strong>{validCount}</strong> khách hợp lệ.
                      </div>
                    </div>
                  )}
                </div>
              )}

              {/* ACTION FOOTER */}
              <div className="flex items-center justify-between pt-4 border-t border-neutral-100">
                <Button variant="outline" onClick={handleClose} disabled={isSubmittingConfirm}>
                  Hủy bỏ
                </Button>

                {previewData && (
                  <Button
                    variant="primary"
                    onClick={handleConfirmImport}
                    disabled={validCount === 0 || isSubmittingConfirm}
                  >
                    <Check className="w-4 h-4 mr-1.5" />
                    {isSubmittingConfirm
                      ? 'Đang lưu vào hệ thống...'
                      : `Xác nhận Import (${validCount} khách hợp lệ)`}
                  </Button>
                )}
              </div>
            </>
          )}
        </div>
      </Modal>

      {/* POPUP HƯỚNG DẪN BỐ CỤC FILE EXCEL & QUY ƯỚC */}
      <Modal
        isOpen={showGuideModal}
        onClose={() => setShowGuideModal(false)}
        title="Quy chuẩn & Bố cục file Excel Rooming List chuẩn"
        description="Định dạng cột, tên sheet và các quy ước bắt buộc để file được hệ thống kiểm tra chấp thuận"
        maxWidth="4xl"
      >
        <div className="space-y-6 max-h-[75vh] overflow-y-auto pr-1">
          {/* MÔ PHỎNG BẢNG EXCEL MINH HỌA */}
          <div className="space-y-3">
            <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 p-3.5 bg-emerald-50 border border-emerald-200 rounded-xl">
              <div className="space-y-0.5">
                <p className="font-bold text-emerald-950 text-xs flex items-center gap-1.5">
                  <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                  Tải file Excel mẫu gồm 9 cột chuẩn để điền thông tin:
                </p>
                <p className="text-[11px] text-emerald-800">
                  File gồm đúng 9 cột chuẩn và Sheet <strong>RoomingList</strong> theo quy định của hệ thống.
                </p>
              </div>
              <a
                href="/test-data/rooming-list-template.xlsx"
                download="rooming-list-template.xlsx"
                className="inline-flex items-center gap-2 px-3.5 py-2 rounded-xl text-xs font-bold bg-emerald-600 hover:bg-emerald-700 text-white shadow-xs transition-all cursor-pointer shrink-0"
              >
                <FileSpreadsheet className="w-4 h-4 text-white" />
                Tải file Excel mẫu chuẩn (.xlsx)
              </a>
            </div>

            <div className="flex items-center justify-between">
              <span className="text-xs font-bold text-neutral-800 uppercase tracking-wider flex items-center gap-1.5">
                <FileSpreadsheet className="w-4 h-4 text-emerald-600" />
                Mô phỏng bố cục bảng Excel thực tế (Sheet: RoomingList)
              </span>
              <span className="text-[11px] font-semibold text-emerald-800 bg-emerald-100 px-2 py-0.5 rounded-full">
                Tên Sheet bắt buộc: RoomingList
              </span>
            </div>

            <div className="border-2 border-neutral-300 rounded-xl overflow-hidden shadow-xs bg-white text-xs">
              <div className="bg-neutral-100 border-b border-neutral-200 px-3 py-1.5 font-mono text-[11px] text-neutral-600 flex items-center justify-between">
                <span>Excel Spreadsheet: [Dòng 1 = Tiêu đề cột]</span>
                <span className="text-neutral-500 font-sans">9 Cột tiêu chuẩn từ A đến I</span>
              </div>
              <div className="overflow-x-auto">
                <table className="w-full text-left border-collapse">
                  <thead>
                    <tr className="bg-emerald-700 text-white font-semibold divide-x divide-emerald-600 text-[11px]">
                      <th className="p-2 text-center w-12 bg-emerald-800">Cột</th>
                      <th className="p-2 w-14">A</th>
                      <th className="p-2 w-28">B (*)</th>
                      <th className="p-2 w-20">C</th>
                      <th className="p-2 w-24">D</th>
                      <th className="p-2 w-28">E</th>
                      <th className="p-2 w-28">F</th>
                      <th className="p-2 w-28">G</th>
                      <th className="p-2 w-32">H</th>
                      <th className="p-2 w-28">I</th>
                    </tr>
                    <tr className="bg-emerald-50 text-emerald-950 font-bold divide-x divide-emerald-200 border-b border-emerald-200 text-xs">
                      <th className="p-2 text-center bg-emerald-100 font-mono text-[11px]">Dòng 1</th>
                      <th className="p-2 text-neutral-700 font-semibold">Room</th>
                      <th className="p-2 text-red-700 font-bold">Full Name</th>
                      <th className="p-2 text-neutral-700 font-semibold">Gender</th>
                      <th className="p-2 text-neutral-700 font-semibold">Date of Birth</th>
                      <th className="p-2 text-neutral-700 font-semibold">Nationality</th>
                      <th className="p-2 text-neutral-700 font-semibold">ID / Passport</th>
                      <th className="p-2 text-neutral-700 font-semibold">Phone</th>
                      <th className="p-2 text-neutral-700 font-semibold">Email</th>
                      <th className="p-2 text-neutral-700 font-semibold">Note</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-neutral-200 font-mono text-[11px]">
                    <tr className="hover:bg-neutral-50 divide-x divide-neutral-200">
                      <td className="p-2 text-center bg-neutral-100 text-neutral-500 font-sans">Dòng 2</td>
                      <td className="p-2 text-neutral-800 font-sans font-bold text-emerald-700">101 - STD</td>
                      <td className="p-2 font-bold text-neutral-900 font-sans">Nguyễn Văn An</td>
                      <td className="p-2 font-sans">Nam</td>
                      <td className="p-2">15/05/1990</td>
                      <td className="p-2 font-sans">Việt Nam</td>
                      <td className="p-2 text-neutral-800">001090123456</td>
                      <td className="p-2">0912345678</td>
                      <td className="p-2 text-neutral-800 font-sans">an.nguyen@gmail.com</td>
                      <td className="p-2 font-sans text-neutral-500">Khách VIP</td>
                    </tr>
                    <tr className="hover:bg-neutral-50 divide-x divide-neutral-200">
                      <td className="p-2 text-center bg-neutral-100 text-neutral-500 font-sans">Dòng 3</td>
                      <td className="p-2 text-neutral-800 font-sans font-bold text-emerald-700">101 - STD</td>
                      <td className="p-2 font-bold text-neutral-900 font-sans">Trần Thị Bình</td>
                      <td className="p-2 font-sans">Nữ</td>
                      <td className="p-2">20/10/1992</td>
                      <td className="p-2 font-sans">Việt Nam</td>
                      <td className="p-2 text-neutral-800">001192987654</td>
                      <td className="p-2">0987654321</td>
                      <td className="p-2 text-neutral-800 font-sans">binh.tran@gmail.com</td>
                      <td className="p-2 font-sans text-neutral-500">—</td>
                    </tr>
                    <tr className="hover:bg-neutral-50 divide-x divide-neutral-200">
                      <td className="p-2 text-center bg-neutral-100 text-neutral-500 font-sans">Dòng 4</td>
                      <td className="p-2 text-neutral-800 font-sans font-bold text-emerald-700">102 - SUP</td>
                      <td className="p-2 font-bold text-neutral-900 font-sans">Lê Hoàng Cường</td>
                      <td className="p-2 font-sans">Nam</td>
                      <td className="p-2">02/09/1988</td>
                      <td className="p-2 font-sans">Việt Nam</td>
                      <td className="p-2 text-neutral-800">038088001122</td>
                      <td className="p-2">0903112233</td>
                      <td className="p-2 text-neutral-800 font-sans">cuong.le@corp.vn</td>
                      <td className="p-2 font-sans text-neutral-500">Trưởng đoàn</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>

          {/* QUY ƯỚC ĐIỀN ĐỦ VÀ HỢP LỆ ĐỂ CHẤP THUẬN */}
          <div className="space-y-3">
            <h4 className="text-xs font-bold text-neutral-900 uppercase tracking-wider flex items-center gap-1.5">
              <Info className="w-4 h-4 text-blue-600" />
              Quy ước chi tiết từng cột để hệ thống chấp thuận
            </h4>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-xs">
              <div className="p-3 rounded-xl border border-neutral-200 bg-neutral-50/50 space-y-1">
                <div className="flex items-center gap-1.5">
                  <span className="font-bold text-neutral-900">1. Cột B - Full Name</span>
                  <span className="text-[10px] bg-red-100 text-red-700 font-bold px-1.5 py-0.5 rounded">
                    Bắt buộc
                  </span>
                </div>
                <p className="text-neutral-600">
                  Họ và tên khách. Không được để trống. Độ dài từ 2 đến 100 ký tự.
                </p>
              </div>

              <div className="p-3 rounded-xl border border-neutral-200 bg-neutral-50/50 space-y-1">
                <div className="flex items-center gap-1.5">
                  <span className="font-bold text-neutral-900">2. Cột A - Room</span>
                  <span className="text-[10px] bg-blue-100 text-blue-700 font-semibold px-1.5 py-0.5 rounded">
                    Tùy chọn
                  </span>
                </div>
                <p className="text-neutral-600">
                  Số phòng kèm mã loại phòng (VD: <code>101 - STD</code>, <code>101 - SUP</code>, <code>101 - DLX</code> hoặc điền <code>101</code>). Nếu để trống, lễ tân có thể gán sau khi check-in.
                </p>
              </div>

              <div className="p-3 rounded-xl border border-neutral-200 bg-neutral-50/50 space-y-1">
                <div className="flex items-center gap-1.5">
                  <span className="font-bold text-neutral-900">3. Cột C - Gender</span>
                  <span className="text-[10px] bg-blue-100 text-blue-700 font-semibold px-1.5 py-0.5 rounded">
                    Tùy chọn
                  </span>
                </div>
                <p className="text-neutral-600">
                  Giới tính: <code>Nam</code>, <code>Nữ</code>, <code>Khác</code> (hoặc <code>Male</code>, <code>Female</code>, <code>Other</code>).
                </p>
              </div>

              <div className="p-3 rounded-xl border border-neutral-200 bg-neutral-50/50 space-y-1">
                <div className="flex items-center gap-1.5">
                  <span className="font-bold text-neutral-900">4. Cột D - Date of Birth</span>
                  <span className="text-[10px] bg-blue-100 text-blue-700 font-semibold px-1.5 py-0.5 rounded">
                    Tùy chọn
                  </span>
                </div>
                <p className="text-neutral-600">
                  Định dạng ngày chuẩn: <code>DD/MM/YYYY</code> hoặc <code>YYYY-MM-DD</code>. Ngày sinh không được lớn hơn ngày hiện tại.
                </p>
              </div>

              <div className="p-3 rounded-xl border border-neutral-200 bg-neutral-50/50 space-y-1">
                <div className="flex items-center gap-1.5">
                  <span className="font-bold text-neutral-900">5. Cột F - ID / Passport</span>
                  <span className="text-[10px] bg-blue-100 text-blue-700 font-semibold px-1.5 py-0.5 rounded">
                    Tùy chọn
                  </span>
                </div>
                <p className="text-neutral-600">
                  CCCD / CMND hoặc số Hộ chiếu. Định dạng 9 đến 12 số (CCCD) hoặc chuỗi ký tự hợp lệ.
                </p>
              </div>

              <div className="p-3 rounded-xl border border-neutral-200 bg-neutral-50/50 space-y-1">
                <div className="flex items-center gap-1.5">
                  <span className="font-bold text-neutral-900">6. Cột G - Phone &amp; Cột H - Email</span>
                  <span className="text-[10px] bg-blue-100 text-blue-700 font-semibold px-1.5 py-0.5 rounded">
                    Tùy chọn
                  </span>
                </div>
                <p className="text-neutral-600">
                  Số điện thoại Việt Nam (10 số, bắt đầu bằng 0) và Email đúng định dạng (nếu có nhập).
                </p>
              </div>
            </div>
          </div>

          {/* QUY TRÌNH DUYỆT 100% CỦA HỆ THỐNG */}
          <div className="p-4 bg-emerald-50 border border-emerald-200 rounded-xl text-xs space-y-2">
            <p className="font-bold text-emerald-900 flex items-center gap-1.5">
              <CheckCircle2 className="w-4 h-4 text-emerald-600" />
              Điều kiện để hệ thống chấp thuận và thực hiện Import:
            </p>
            <ul className="list-disc pl-5 space-y-1 text-emerald-800">
              <li>File phải là định dạng <strong>.xlsx</strong> của Microsoft Excel.</li>
              <li>Tên Sheet chứa dữ liệu bắt buộc phải là <strong>RoomingList</strong>.</li>
              <li>Dòng 1 là hàng tiêu đề với các cột nêu trên. Dữ liệu bắt đầu từ Dòng 2.</li>
              <li>
                Toàn bộ các dòng đều hợp lệ (<strong>0 dòng lỗi</strong>). Khi nhấn nút <strong>Xác nhận Import</strong>, nếu phát hiện bất kỳ dòng nào sai định dạng, hệ thống sẽ báo đỏ và chỉ ra chính xác Dòng mấy, Cột nào và Lỗi gì để bạn sửa lại.
              </li>
            </ul>
          </div>

          <div className="flex justify-end pt-2">
            <Button variant="primary" onClick={() => setShowGuideModal(false)}>
              Đã hiểu quy chuẩn
            </Button>
          </div>
        </div>
      </Modal>
    </>
  );
};

export default RoomingListImportModal;
