import React, { useState, useEffect, useCallback } from 'react';
import {
  Brush,
  Sparkles,
  BedDouble,
  AlertTriangle,
  RefreshCw,
  CheckCircle2,
  Clock,
  User,
  Calendar,
  Wrench,
  ShieldAlert,
  LayoutGrid,
  ListFilter,
  Plus,
} from 'lucide-react';
import Button from '../../../core/components/ui/Button';
import Modal from '../../../core/components/ui/Modal';
import { getApiErrorMessage } from '../../../core/api/error';
import { operationService } from '../services/operation.service';
import type { RoomHousekeepingStatusResponse } from '../types/operation.types';
import MaintenanceBlockFormModal from '../components/maintenance-block/MaintenanceBlockFormModal';
import MaintenanceBlockList from '../components/maintenance-block/MaintenanceBlockList';

export const HousekeepingPage: React.FC = () => {
  const [hotelId, setHotelId] = useState<number>(1);
  const [mainTab, setMainTab] = useState<'GRID' | 'BLOCKS'>('GRID');
  const [selectedStatusTab, setSelectedStatusTab] = useState<string>('ALL');
  const [rooms, setRooms] = useState<RoomHousekeepingStatusResponse[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [actionSuccessMessage, setActionSuccessMessage] = useState('');
  const [refreshBlocksTrigger, setRefreshBlocksTrigger] = useState<number>(0);

  // Modal states
  const [isBlockModalOpen, setIsBlockModalOpen] = useState<boolean>(false);
  const [selectedRoomForBlock, setSelectedRoomForBlock] = useState<number | undefined>(undefined);
  const [occupiedWarningRoom, setOccupiedWarningRoom] = useState<RoomHousekeepingStatusResponse | null>(null);

  // Error modal state
  const [apiErrorModal, setApiErrorModal] = useState<{
    isOpen: boolean;
    title: string;
    message: string;
    code?: number;
  } | null>(null);

  const handleApiError = useCallback((err: unknown, defaultTitle = 'Không thể thực hiện thao tác') => {
    const errorInfo = getApiErrorMessage(err);
    setApiErrorModal({
      isOpen: true,
      title: defaultTitle,
      message: errorInfo.message,
      code: errorInfo.code,
    });
  }, []);

  const fetchRooms = useCallback(async () => {
    setIsLoading(true);
    try {
      const statusParam = selectedStatusTab === 'ALL' ? undefined : selectedStatusTab;
      const data = await operationService.getHousekeepingRooms(hotelId, statusParam);
      setRooms(data || []);
    } catch (err) {
      handleApiError(err, 'Lỗi tải danh sách buồng phòng');
    } finally {
      setIsLoading(false);
    }
  }, [hotelId, selectedStatusTab, handleApiError]);

  useEffect(() => {
    fetchRooms();
  }, [fetchRooms]);

  const handleUpdateStatus = async (roomInstanceId: number, newStatus: string) => {
    try {
      await operationService.updateRoomHousekeepingStatus(roomInstanceId, { status: newStatus });
      setActionSuccessMessage(`Đã cập nhật trạng thái phòng thành công: ${newStatus}`);
      setTimeout(() => setActionSuccessMessage(''), 3500);
      fetchRooms();
    } catch (err) {
      handleApiError(err, 'Lỗi cập nhật trạng thái phòng');
    }
  };

  const handleInitiateBlock = (room: RoomHousekeepingStatusResponse) => {
    if (room.currentStatus === 'OCCUPIED') {
      setOccupiedWarningRoom(room);
    } else {
      setSelectedRoomForBlock(room.roomInstanceId);
      setIsBlockModalOpen(true);
    }
  };

  const handleBlockSuccess = (msg: string) => {
    setActionSuccessMessage(msg);
    setTimeout(() => setActionSuccessMessage(''), 4000);
    fetchRooms();
    setRefreshBlocksTrigger((prev) => prev + 1);
  };

  // Status counters from full rooms list
  const countReady = rooms.filter((r) => r.currentStatus === 'READY').length;
  const countOccupied = rooms.filter((r) => r.currentStatus === 'OCCUPIED').length;
  const countDirty = rooms.filter((r) => r.currentStatus === 'DIRTY').length;
  const countCleaning = rooms.filter((r) => r.currentStatus === 'CLEANING').length;
  const countMaintenance = rooms.filter((r) => r.currentStatus === 'MAINTENANCE').length;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold text-neutral-900 tracking-tight flex items-center gap-2">
            <Brush className="w-7 h-7 text-red-600" />
            Sơ đồ Buồng phòng & Khóa bảo trì (Housekeeping & Maintenance)
          </h1>
          <p className="text-sm text-neutral-500 mt-1">
            Theo dõi trạng thái buồng phòng, xử lý vệ sinh và quản lý các lệnh khóa phòng bảo trì (OOO/OOS)
          </p>
        </div>
        <div className="flex items-center gap-3">
          <label className="text-xs font-semibold text-neutral-600">Khách sạn cơ sở:</label>
          <select
            value={hotelId}
            onChange={(e) => setHotelId(Number(e.target.value))}
            className="h-10 px-3 border border-neutral-300 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-red-600 bg-white"
          >
            <option value={1}>Khách sạn Grand Hà Nội</option>
            <option value={2}>Khách sạn Premier Đà Nẵng</option>
            <option value={3}>Khách sạn Boutique Sài Gòn</option>
          </select>
          <Button
            variant="primary"
            onClick={() => {
              setSelectedRoomForBlock(undefined);
              setIsBlockModalOpen(true);
            }}
            className="h-10 px-3.5 bg-red-600 hover:bg-red-700 text-xs font-semibold flex items-center gap-1.5 shadow-sm"
          >
            <Plus className="w-4 h-4" />
            Khóa phòng (OOO/OOS)
          </Button>
          <Button
            variant="secondary"
            onClick={fetchRooms}
            className="h-10 flex items-center gap-1.5 text-xs"
            disabled={isLoading}
          >
            <RefreshCw className={`w-4 h-4 ${isLoading ? 'animate-spin' : ''}`} />
            Làm mới
          </Button>
        </div>
      </div>

      {actionSuccessMessage && (
        <div className="p-3.5 bg-emerald-50 border border-emerald-200 rounded-xl text-emerald-800 text-sm flex items-center gap-2 animate-in fade-in">
          <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0" />
          {actionSuccessMessage}
        </div>
      )}

      {/* Main Tab Navigation */}
      <div className="flex items-center gap-2 border-b border-neutral-200">
        <button
          onClick={() => setMainTab('GRID')}
          className={`pb-3 px-4 text-sm font-bold flex items-center gap-2 border-b-2 transition-all cursor-pointer ${
            mainTab === 'GRID'
              ? 'border-red-600 text-red-600'
              : 'border-transparent text-neutral-500 hover:text-neutral-800'
          }`}
        >
          <LayoutGrid className="w-4 h-4" />
          Sơ đồ buồng phòng ({rooms.length})
        </button>

        <button
          onClick={() => setMainTab('BLOCKS')}
          className={`pb-3 px-4 text-sm font-bold flex items-center gap-2 border-b-2 transition-all cursor-pointer ${
            mainTab === 'BLOCKS'
              ? 'border-red-600 text-red-600'
              : 'border-transparent text-neutral-500 hover:text-neutral-800'
          }`}
        >
          <ListFilter className="w-4 h-4" />
          Danh sách khóa bảo trì (OOO / OOS)
        </button>
      </div>

      {/* Tab Content: Housekeeping Grid */}
      {mainTab === 'GRID' && (
        <div className="space-y-6">
          {/* Summary KPI Cards */}
          <div className="grid grid-cols-2 sm:grid-cols-5 gap-3">
            <div
              onClick={() => setSelectedStatusTab('READY')}
              className={`cursor-pointer p-4 rounded-2xl border transition-all ${
                selectedStatusTab === 'READY'
                  ? 'bg-emerald-50 border-emerald-500 shadow-xs'
                  : 'bg-white border-neutral-200 hover:border-emerald-300'
              }`}
            >
              <div className="text-xs font-semibold text-emerald-700 flex items-center gap-1.5">
                <Sparkles className="w-4 h-4" />
                Sẵn sàng đón khách
              </div>
              <div className="text-2xl font-bold text-neutral-900 mt-2">{countReady}</div>
            </div>

            <div
              onClick={() => setSelectedStatusTab('OCCUPIED')}
              className={`cursor-pointer p-4 rounded-2xl border transition-all ${
                selectedStatusTab === 'OCCUPIED'
                  ? 'bg-blue-50 border-blue-500 shadow-xs'
                  : 'bg-white border-neutral-200 hover:border-blue-300'
              }`}
            >
              <div className="text-xs font-semibold text-blue-700 flex items-center gap-1.5">
                <BedDouble className="w-4 h-4" />
                Đang có khách ở
              </div>
              <div className="text-2xl font-bold text-neutral-900 mt-2">{countOccupied}</div>
            </div>

            <div
              onClick={() => setSelectedStatusTab('DIRTY')}
              className={`cursor-pointer p-4 rounded-2xl border transition-all ${
                selectedStatusTab === 'DIRTY'
                  ? 'bg-rose-50 border-rose-500 shadow-xs'
                  : 'bg-white border-neutral-200 hover:border-rose-300'
              }`}
            >
              <div className="text-xs font-semibold text-rose-700 flex items-center gap-1.5">
                <Brush className="w-4 h-4" />
                Cần dọn dẹp (Dirty)
              </div>
              <div className="text-2xl font-bold text-neutral-900 mt-2">{countDirty}</div>
            </div>

            <div
              onClick={() => setSelectedStatusTab('CLEANING')}
              className={`cursor-pointer p-4 rounded-2xl border transition-all ${
                selectedStatusTab === 'CLEANING'
                  ? 'bg-amber-50 border-amber-500 shadow-xs'
                  : 'bg-white border-neutral-200 hover:border-amber-300'
              }`}
            >
              <div className="text-xs font-semibold text-amber-700 flex items-center gap-1.5">
                <Clock className="w-4 h-4" />
                Đang dọn dẹp
              </div>
              <div className="text-2xl font-bold text-neutral-900 mt-2">{countCleaning}</div>
            </div>

            <div
              onClick={() => setSelectedStatusTab('MAINTENANCE')}
              className={`cursor-pointer p-4 rounded-2xl border transition-all ${
                selectedStatusTab === 'MAINTENANCE'
                  ? 'bg-neutral-100 border-neutral-400 shadow-xs'
                  : 'bg-white border-neutral-200 hover:border-neutral-400'
              }`}
            >
              <div className="text-xs font-semibold text-neutral-700 flex items-center gap-1.5">
                <AlertTriangle className="w-4 h-4" />
                Đang bảo trì
              </div>
              <div className="text-2xl font-bold text-neutral-900 mt-2">{countMaintenance}</div>
            </div>
          </div>

          {/* Filter Tabs */}
          <div className="flex flex-wrap gap-2 border-b border-neutral-200 pb-3">
            {[
              { key: 'ALL', label: 'Tất cả phòng' },
              { key: 'READY', label: 'Sẵn sàng (READY)' },
              { key: 'OCCUPIED', label: 'Có khách (OCCUPIED)' },
              { key: 'DIRTY', label: 'Cần dọn (DIRTY)' },
              { key: 'CLEANING', label: 'Đang dọn (CLEANING)' },
              { key: 'MAINTENANCE', label: 'Bảo trì (MAINTENANCE)' },
            ].map((tab) => (
              <button
                key={tab.key}
                onClick={() => setSelectedStatusTab(tab.key)}
                className={`px-3.5 py-1.5 rounded-xl text-xs font-semibold transition-colors cursor-pointer ${
                  selectedStatusTab === tab.key
                    ? 'bg-red-600 text-white shadow-xs'
                    : 'bg-neutral-100 text-neutral-600 hover:bg-neutral-200'
                }`}
              >
                {tab.label}
              </button>
            ))}
          </div>

          {/* Room Grid */}
          {isLoading ? (
            <div className="py-24 text-center text-neutral-400">Đang tải sơ đồ phòng...</div>
          ) : rooms.length === 0 ? (
            <div className="py-24 text-center text-neutral-400">Không có phòng nào với trạng thái này.</div>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
              {rooms.map((room) => {
                const isReady = room.currentStatus === 'READY';
                const isOccupied = room.currentStatus === 'OCCUPIED';
                const isDirty = room.currentStatus === 'DIRTY';
                const isCleaning = room.currentStatus === 'CLEANING';
                const isMaintenance = room.currentStatus === 'MAINTENANCE';

                return (
                  <div
                    key={room.roomInstanceId}
                    className={`bg-white rounded-2xl border p-4.5 flex flex-col justify-between transition-all shadow-xs ${
                      isReady
                        ? 'border-emerald-200 hover:border-emerald-400'
                        : isOccupied
                        ? 'border-blue-200 hover:border-blue-400'
                        : isDirty
                        ? 'border-rose-200 hover:border-rose-400'
                        : isCleaning
                        ? 'border-amber-200 hover:border-amber-400'
                        : 'border-neutral-200 hover:border-neutral-400'
                    }`}
                  >
                    <div>
                      <div className="flex justify-between items-start">
                        <div>
                          <div className="text-xl font-extrabold text-neutral-900 tracking-tight">
                            P.{room.roomNumber}
                          </div>
                          <div className="text-xs font-medium text-neutral-500 mt-0.5">
                            {room.roomTypeName || 'Tiêu chuẩn'}
                          </div>
                        </div>
                        <span
                          className={`px-2.5 py-1 rounded-full text-[11px] font-bold ${
                            isReady
                              ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                              : isOccupied
                              ? 'bg-blue-50 text-blue-700 border border-blue-200'
                              : isDirty
                              ? 'bg-rose-50 text-rose-700 border border-rose-200'
                              : isCleaning
                              ? 'bg-amber-50 text-amber-700 border border-amber-200'
                              : 'bg-neutral-100 text-neutral-600 border border-neutral-300'
                          }`}
                        >
                          {isReady && 'Sẵn sàng'}
                          {isOccupied && 'Có khách'}
                          {isDirty && 'Cần dọn dẹp'}
                          {isCleaning && 'Đang dọn'}
                          {isMaintenance && 'Bảo trì'}
                        </span>
                      </div>

                      {isOccupied && (
                        <div className="mt-3 pt-3 border-t border-neutral-100 space-y-1 text-xs text-neutral-600">
                          <div className="flex items-center gap-1.5 font-medium text-neutral-800">
                            <User className="w-3.5 h-3.5 text-blue-600" />
                            <span>{room.guestName || 'Khách lưu trú'}</span>
                          </div>
                          {room.checkOutDate && (
                            <div className="flex items-center gap-1.5 text-neutral-500 text-[11px]">
                              <Calendar className="w-3.5 h-3.5 text-neutral-400" />
                              <span>Trả phòng: {room.checkOutDate}</span>
                            </div>
                          )}
                        </div>
                      )}
                    </div>

                    {/* Quick Action Buttons */}
                    <div className="mt-4 pt-3 border-t border-neutral-100 flex flex-wrap gap-1.5">
                      {isDirty && (
                        <button
                          onClick={() => handleUpdateStatus(room.roomInstanceId, 'CLEANING')}
                          className="flex-1 py-1.5 bg-amber-500 hover:bg-amber-600 text-white rounded-lg text-xs font-semibold flex items-center justify-center gap-1 transition-colors cursor-pointer"
                        >
                          <Brush className="w-3.5 h-3.5" />
                          Bắt đầu dọn
                        </button>
                      )}
                      {isCleaning && (
                        <button
                          onClick={() => handleUpdateStatus(room.roomInstanceId, 'READY')}
                          className="flex-1 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-xs font-semibold flex items-center justify-center gap-1 transition-colors cursor-pointer"
                        >
                          <Sparkles className="w-3.5 h-3.5" />
                          Đã sạch (Ready)
                        </button>
                      )}
                      {!isDirty && !isCleaning && !isOccupied && (
                        <>
                          {!isReady && (
                            <button
                              onClick={() => handleUpdateStatus(room.roomInstanceId, 'READY')}
                              className="px-2.5 py-1 bg-emerald-50 text-emerald-700 hover:bg-emerald-100 rounded-lg text-xs font-semibold transition-colors cursor-pointer"
                            >
                              Sẵn sàng
                            </button>
                          )}
                          <button
                            onClick={() => handleUpdateStatus(room.roomInstanceId, 'DIRTY')}
                            className="px-2.5 py-1 bg-rose-50 text-rose-700 hover:bg-rose-100 rounded-lg text-xs font-semibold transition-colors cursor-pointer"
                          >
                            Báo dọn
                          </button>
                        </>
                      )}

                      {/* Khóa bảo trì / Xem bảo trì button */}
                      {isMaintenance ? (
                        <button
                          onClick={() => setMainTab('BLOCKS')}
                          className="px-2.5 py-1 bg-neutral-100 text-neutral-700 hover:bg-neutral-200 rounded-lg text-xs font-semibold transition-colors flex items-center gap-1 cursor-pointer"
                        >
                          <Wrench className="w-3.5 h-3.5 text-neutral-500" />
                          Lệnh bảo trì
                        </button>
                      ) : (
                        <button
                          onClick={() => handleInitiateBlock(room)}
                          className="px-2.5 py-1 bg-neutral-50 hover:bg-neutral-100 text-neutral-700 border border-neutral-200 rounded-lg text-xs font-semibold transition-colors flex items-center gap-1 cursor-pointer"
                          title="Tạo lệnh khóa phòng bảo trì (OOO/OOS)"
                        >
                          <Wrench className="w-3.5 h-3.5 text-neutral-500" />
                          Khóa phòng
                        </button>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      )}

      {/* Tab Content: Maintenance Blocks List */}
      {mainTab === 'BLOCKS' && (
        <MaintenanceBlockList
          hotelId={hotelId}
          onOpenCreateModal={() => {
            setSelectedRoomForBlock(undefined);
            setIsBlockModalOpen(true);
          }}
          onError={(err) => handleApiError(err, 'Lỗi thao tác khóa bảo trì')}
          onSuccess={(msg) => {
            setActionSuccessMessage(msg);
            setTimeout(() => setActionSuccessMessage(''), 4000);
            fetchRooms();
          }}
          refreshTrigger={refreshBlocksTrigger}
        />
      )}

      {/* Modal: Form tạo lệnh khóa phòng */}
      <MaintenanceBlockFormModal
        isOpen={isBlockModalOpen}
        onClose={() => setIsBlockModalOpen(false)}
        onSuccess={handleBlockSuccess}
        onError={(err) => handleApiError(err, 'Lỗi tạo lệnh khóa phòng')}
        rooms={rooms}
        initialRoomInstanceId={selectedRoomForBlock}
      />

      {/* Modal cảnh báo phòng có khách lưu trú */}
      {occupiedWarningRoom && (
        <Modal
          isOpen={!!occupiedWarningRoom}
          onClose={() => setOccupiedWarningRoom(null)}
          title="Cảnh báo: Phòng đang có khách lưu trú"
          maxWidth="sm"
        >
          <div className="flex flex-col items-center text-center gap-3">
            <div className="w-12 h-12 rounded-full bg-amber-100 text-amber-700 flex items-center justify-center mb-1">
              <ShieldAlert className="w-6 h-6" />
            </div>
            <p className="text-sm text-neutral-700 leading-relaxed">
              Phòng <span className="font-bold text-neutral-900">P.{occupiedWarningRoom.roomNumber}</span> hiện đang có khách lưu trú
              {occupiedWarningRoom.guestName ? ` (${occupiedWarningRoom.guestName})` : ''}.
            </p>
            <p className="text-xs text-neutral-500 leading-relaxed">
              Khóa phòng OOO trong thời gian khách ở sẽ bị từ chối nếu thời gian trùng với booking. Bạn có chắc muốn tiếp tục mở biểu mẫu khóa phòng không?
            </p>
            <div className="flex items-center justify-end gap-3 w-full mt-4 pt-3 border-t border-neutral-100">
              <Button variant="secondary" onClick={() => setOccupiedWarningRoom(null)}>
                Đóng lại
              </Button>
              <Button
                variant="primary"
                onClick={() => {
                  const targetRoomId = occupiedWarningRoom.roomInstanceId;
                  setOccupiedWarningRoom(null);
                  setSelectedRoomForBlock(targetRoomId);
                  setIsBlockModalOpen(true);
                }}
                className="bg-amber-600 hover:bg-amber-700"
              >
                Tiếp tục tạo lệnh
              </Button>
            </div>
          </div>
        </Modal>
      )}

      {/* Modal Popup Lỗi theo chuẩn ApiResponse */}
      {apiErrorModal && (
        <Modal
          isOpen={apiErrorModal.isOpen}
          onClose={() => setApiErrorModal(null)}
          title={apiErrorModal.title}
          maxWidth="sm"
        >
          <div className="flex flex-col items-center text-center gap-3">
            <div className="w-12 h-12 rounded-full bg-red-100 text-red-600 flex items-center justify-center mb-1">
              <AlertTriangle className="w-6 h-6" />
            </div>
            {apiErrorModal.code && (
              <span className="text-xs font-mono font-semibold px-2 py-0.5 bg-neutral-100 text-neutral-600 rounded">
                Mã lỗi: {apiErrorModal.code}
              </span>
            )}
            <p className="text-sm text-neutral-700 leading-relaxed">
              {apiErrorModal.message}
            </p>
            <div className="flex justify-end w-full mt-4 pt-3 border-t border-neutral-100">
              <Button
                variant="primary"
                onClick={() => setApiErrorModal(null)}
                className="w-full bg-red-600 hover:bg-red-700"
              >
                Đã hiểu
              </Button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
};

export default HousekeepingPage;
