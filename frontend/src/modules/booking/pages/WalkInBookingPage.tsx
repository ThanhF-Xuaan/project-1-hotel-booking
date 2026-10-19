import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Users,
  Building,
  CheckCircle2,
  Bed,
  CreditCard,
  UserCheck,
  ShieldCheck,
  ArrowRight,
  Sparkles,
  AlertCircle,
  FileText,
} from 'lucide-react';
import Button from '../../../core/components/ui/Button';
import WalkInBookingModal from '../components/WalkInBookingModal';

export const WalkInBookingPage: React.FC = () => {
  const navigate = useNavigate();
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [lastCreatedBooking, setLastCreatedBooking] = useState<string | null>(null);

  return (
    <div className="max-w-5xl mx-auto space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <div className="flex items-center gap-2">
            <span className="px-3 py-1 bg-red-100 text-red-700 font-bold text-xs rounded-full uppercase tracking-wider">
              Front Desk POS
            </span>
            <span className="text-xs text-neutral-500 font-medium">Bàn Lễ Tân Tiếp Đón</span>
          </div>
          <h1 className="text-2xl font-bold text-neutral-900 mt-1">
            Tiếp nhận Khách Vãng lai & Check-in Tức thì (Walk-In In-House)
          </h1>
          <p className="text-sm text-neutral-500 mt-1">
            Quy trình tiếp nhận khách vãng lai trực tiếp tại quầy, chỉ định phòng vật lý READY, tính toán phụ thu và chuyển trạng thái In-House tức thì.
          </p>
        </div>

        <Button
          variant="primary"
          size="lg"
          onClick={() => setIsModalOpen(true)}
          className="bg-red-600 hover:bg-red-700 text-white font-bold shadow-md hover:shadow-lg transition-all"
        >
          <Sparkles className="w-5 h-5 mr-2" />
          Mở quy trình Walk-In (5 Bước)
        </Button>
      </div>

      {/* Success Notification if created */}
      {lastCreatedBooking && (
        <div className="p-6 bg-emerald-50 border border-emerald-200 rounded-2xl text-emerald-900 space-y-3">
          <div className="flex items-center gap-3">
            <CheckCircle2 className="w-6 h-6 text-emerald-600 shrink-0" />
            <div>
              <h3 className="font-bold text-base">Đã hoàn tất thủ tục Walk-in & Check-in thành công!</h3>
              <p className="text-sm text-emerald-800">
                Mã đơn đặt phòng: <span className="font-mono font-bold text-emerald-950">{lastCreatedBooking}</span>. Toàn bộ phòng vật lý đã chuyển sang trạng thái <strong>OCCUPIED (Đang lưu trú)</strong> và đồng bộ hàng đợi công an lưu trú (BCA).
              </p>
            </div>
          </div>
          <div className="flex gap-3 pt-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => navigate('/booking/list')}
              className="bg-white border-emerald-300 text-emerald-800 hover:bg-emerald-100"
            >
              <FileText className="w-4 h-4 mr-1.5" />
              Xem trong danh sách Đơn đặt phòng
            </Button>
            <Button
              variant="primary"
              size="sm"
              onClick={() => {
                setLastCreatedBooking(null);
                setIsModalOpen(true);
              }}
              className="bg-emerald-700 hover:bg-emerald-800 text-white"
            >
              Tiếp tục đón lượt khách mới
            </Button>
          </div>
        </div>
      )}

      {/* Main Workflow Hero Card */}
      <div className="bg-gradient-to-br from-neutral-900 to-neutral-800 text-white p-8 rounded-3xl shadow-xl relative overflow-hidden">
        <div className="relative z-10 max-w-2xl space-y-4">
          <div className="inline-flex items-center gap-2 px-3 py-1 bg-red-600/30 border border-red-500/40 rounded-full text-xs font-semibold text-red-300">
            <ShieldCheck className="w-4 h-4" />
            Quy trình chuẩn Lễ tân & Đảm bảo Tính toàn vẹn Dữ liệu
          </div>
          <h2 className="text-2xl sm:text-3xl font-extrabold tracking-tight leading-snug">
            Quy trình 5 bước Xử lý Khách Vãng lai Chuẩn Khách Sạn
          </h2>
          <p className="text-neutral-300 text-sm leading-relaxed">
            Hệ thống hỗ trợ gán phòng vật lý nguyên tử, áp dụng khóa bi quan (Pessimistic Locking) tránh trùng lặp phòng, tự động kiểm tra phụ thu nhận phòng sớm / thêm người / giường phụ và đồng bộ hồ sơ lưu trú BCA tức thời.
          </p>
          <div className="pt-2">
            <Button
              variant="primary"
              size="lg"
              onClick={() => setIsModalOpen(true)}
              className="bg-red-600 hover:bg-red-700 text-white font-bold px-6 shadow-lg"
            >
              Bắt đầu tạo đơn ngay
              <ArrowRight className="w-5 h-5 ml-2" />
            </Button>
          </div>
        </div>

        {/* Decorative background shape */}
        <div className="absolute right-0 top-0 bottom-0 w-1/3 bg-radial from-red-600/20 to-transparent pointer-events-none" />
      </div>

      {/* 5-Step Process Summary Grid */}
      <div className="grid grid-cols-1 md:grid-cols-5 gap-4">
        <div className="bg-white p-5 rounded-2xl border border-neutral-200 shadow-xs space-y-2">
          <div className="w-8 h-8 rounded-xl bg-red-50 text-red-600 font-bold flex items-center justify-center text-sm">
            1
          </div>
          <div className="flex items-center gap-2 font-bold text-sm text-neutral-900">
            <Building className="w-4 h-4 text-neutral-600" />
            Lưu trú
          </div>
          <p className="text-xs text-neutral-500 leading-relaxed">
            Chọn khách sạn cơ sở, ngày nhận/trả phòng và giờ khách đến thực tế.
          </p>
        </div>

        <div className="bg-white p-5 rounded-2xl border border-neutral-200 shadow-xs space-y-2">
          <div className="w-8 h-8 rounded-xl bg-red-50 text-red-600 font-bold flex items-center justify-center text-sm">
            2
          </div>
          <div className="flex items-center gap-2 font-bold text-sm text-neutral-900">
            <Bed className="w-4 h-4 text-neutral-600" />
            Chọn & Gán phòng
          </div>
          <p className="text-xs text-neutral-500 leading-relaxed">
            Tối đa 5 phòng (nhiều hạng phòng). Chỉ cho phép gán phòng vật lý trạng thái READY.
          </p>
        </div>

        <div className="bg-white p-5 rounded-2xl border border-neutral-200 shadow-xs space-y-2">
          <div className="w-8 h-8 rounded-xl bg-red-50 text-red-600 font-bold flex items-center justify-center text-sm">
            3
          </div>
          <div className="flex items-center gap-2 font-bold text-sm text-neutral-900">
            <Users className="w-4 h-4 text-neutral-600" />
            Khai báo Khách
          </div>
          <p className="text-xs text-neutral-500 leading-relaxed">
            Người đại diện thanh toán, Doanh nghiệp bảo lãnh và danh sách khách lưu trú từng phòng (CCCD/Passport).
          </p>
        </div>

        <div className="bg-white p-5 rounded-2xl border border-neutral-200 shadow-xs space-y-2">
          <div className="w-8 h-8 rounded-xl bg-red-50 text-red-600 font-bold flex items-center justify-center text-sm">
            4
          </div>
          <div className="flex items-center gap-2 font-bold text-sm text-neutral-900">
            <CreditCard className="w-4 h-4 text-neutral-600" />
            Phụ thu & Dịch vụ
          </div>
          <p className="text-xs text-neutral-500 leading-relaxed">
            Tự động tính phụ thu nhận phòng sớm, người/giường thêm. Lễ tân có thể xác nhận hoặc miễn giảm.
          </p>
        </div>

        <div className="bg-white p-5 rounded-2xl border border-neutral-200 shadow-xs space-y-2">
          <div className="w-8 h-8 rounded-xl bg-red-50 text-red-600 font-bold flex items-center justify-center text-sm">
            5
          </div>
          <div className="flex items-center gap-2 font-bold text-sm text-neutral-900">
            <UserCheck className="w-4 h-4 text-neutral-600" />
            Thanh toán & Check-in
          </div>
          <p className="text-xs text-neutral-500 leading-relaxed">
            Thu 100% với đơn ≤ 2 phòng; cọc với đơn lớn. Chuyển phòng sang In-house tức thì.
          </p>
        </div>
      </div>

      {/* Rules & Policy Notices */}
      <div className="bg-neutral-50 border border-neutral-200 rounded-2xl p-6 space-y-4">
        <h3 className="font-bold text-neutral-900 text-sm flex items-center gap-2">
          <AlertCircle className="w-4 h-4 text-red-600" />
          Quy tắc Nghiệp vụ Bắt buộc Dành Cho Lễ Tân (Standard Operating Procedures)
        </h3>
        <ul className="grid grid-cols-1 md:grid-cols-2 gap-3 text-xs text-neutral-700">
          <li className="flex items-start gap-2 bg-white p-3 rounded-xl border border-neutral-200">
            <span className="font-bold text-red-600 shrink-0">•</span>
            <span>
              <strong>Bắt buộc thu 100% tiền phòng:</strong> Áp dụng cho khách vãng lai FIT hoặc đặt ít phòng (≤ 2 phòng). Hệ thống sẽ khóa tùy chọn đặt cọc nhằm ngăn ngừa rủi ro thất thoát doanh thu.
            </span>
          </li>
          <li className="flex items-start gap-2 bg-white p-3 rounded-xl border border-neutral-200">
            <span className="font-bold text-red-600 shrink-0">•</span>
            <span>
              <strong>Chỉ định phòng READY:</strong> Lễ tân chỉ được gán các phòng vật lý có trạng thái sạch sẽ và đã nghiệm thu bởi Buồng phòng (READY). Phòng DIRTY hoặc MAINTENANCE sẽ bị từ chối.
            </span>
          </li>
          <li className="flex items-start gap-2 bg-white p-3 rounded-xl border border-neutral-200">
            <span className="font-bold text-red-600 shrink-0">•</span>
            <span>
              <strong>Xác minh danh tính người lưu trú:</strong> Mọi khách thực tế lưu trú trong phòng cần được đối chiếu giấy tờ tùy thân (CCCD / Passport) để đảm bảo dữ liệu đồng bộ BCA hợp lệ.
            </span>
          </li>
          <li className="flex items-start gap-2 bg-white p-3 rounded-xl border border-neutral-200">
            <span className="font-bold text-red-600 shrink-0">•</span>
            <span>
              <strong>Quyền điều chỉnh / Miễn phụ thu:</strong> Khi miễn phụ thu nhận phòng sớm hoặc thêm người, Lễ tân bắt buộc phải cung cấp lý do cụ thể (VD: Khách VIP, thiện chí thương hiệu) để phục vụ kiểm toán đêm.
            </span>
          </li>
        </ul>
      </div>

      {/* Walk-In Booking Modal */}
      <WalkInBookingModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onSuccess={(bookingNumber) => {
          setIsModalOpen(false);
          setLastCreatedBooking(bookingNumber);
        }}
      />
    </div>
  );
};

export default WalkInBookingPage;
