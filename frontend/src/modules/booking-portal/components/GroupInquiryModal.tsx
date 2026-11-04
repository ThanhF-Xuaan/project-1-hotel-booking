import React, { useState } from 'react';
import {
  Users,
  Building2,
  CalendarCheck2,
  Receipt,
  ShieldCheck,
  CheckCircle2,
  ArrowLeft,
  Send,
  Info,
} from 'lucide-react';
import { Modal } from '../../../core/components/ui/Modal';
import Button from '../../../core/components/ui/Button';

export interface GroupInquiryModalProps {
  isOpen: boolean;
  onClose: () => void;
  initialRoomsCount: number;
  onSwitchToFit: () => void;
  hotelName?: string;
  checkInDate?: string;
  checkOutDate?: string;
}

export const GroupInquiryModal: React.FC<GroupInquiryModalProps> = ({
  isOpen,
  onClose,
  initialRoomsCount,
  onSwitchToFit,
  hotelName = 'Hệ thống Khách sạn Grand',
  checkInDate,
  checkOutDate,
}) => {
  const [companyName, setCompanyName] = useState('');
  const [contactName, setContactName] = useState('');
  const [phone, setPhone] = useState('');
  const [email, setEmail] = useState('');
  const [roomsCount, setRoomsCount] = useState<number>(initialRoomsCount >= 6 ? initialRoomsCount : 6);
  const [notes, setNotes] = useState('');
  const [isSubmitted, setIsSubmitted] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!contactName.trim() || !phone.trim()) return;

    setIsSubmitting(true);
    // Simulate submission delay
    setTimeout(() => {
      setIsSubmitting(false);
      setIsSubmitted(true);
    }, 600);
  };

  const handleReset = () => {
    setIsSubmitted(false);
    onClose();
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Quy Định Đặt Phòng Khách Đoàn (GIT Inquiry)"
      description="Yêu cầu đặt từ 6 phòng trở lên thuộc chính sách Khách đoàn (Group Inclusive Tour)."
      maxWidth="2xl"
    >
      {isSubmitted ? (
        <div className="py-6 text-center space-y-4">
          <div className="w-16 h-16 bg-emerald-100 text-emerald-600 rounded-full flex items-center justify-center mx-auto shadow-sm">
            <CheckCircle2 className="w-10 h-10" />
          </div>
          <div className="space-y-1.5">
            <h3 className="text-xl font-extrabold text-neutral-900">
              Đã Tiếp Nhận Yêu Cầu Báo Giá Đoàn!
            </h3>
            <p className="text-sm text-neutral-600 max-w-md mx-auto leading-relaxed">
              Cảm ơn Quý khách <span className="font-bold text-neutral-800">{contactName}</span>. Bộ phận Kinh doanh Khách sạn sẽ trực tiếp liên hệ qua số điện thoại <span className="font-bold text-red-600">{phone}</span> trong vòng 2 giờ làm việc để gửi bảng chào giá ưu đãi và hợp đồng giữ phòng.
            </p>
          </div>

          <div className="bg-neutral-50 border border-neutral-200 rounded-2xl p-4 text-left text-xs space-y-1.5 max-w-md mx-auto mt-4 text-neutral-700">
            <div className="font-bold text-neutral-900 flex items-center gap-1.5 text-sm mb-2">
              <Building2 className="w-4 h-4 text-red-600" />
              <span>Tóm tắt nhu cầu đoàn:</span>
            </div>
            <div>• <span className="font-medium">Khách sạn:</span> {hotelName}</div>
            {checkInDate && checkOutDate && (
              <div>• <span className="font-medium">Thời gian:</span> {checkInDate} → {checkOutDate}</div>
            )}
            <div>• <span className="font-medium">Số lượng phòng:</span> {roomsCount} phòng</div>
            {companyName && <div>• <span className="font-medium">Đơn vị:</span> {companyName}</div>}
          </div>

          <div className="pt-4">
            <Button onClick={handleReset} className="w-full sm:w-auto px-8 h-11 font-bold">
              Đã hiểu & Đóng
            </Button>
          </div>
        </div>
      ) : (
        <div className="space-y-5 text-left">
          {/* Policy Highlight Box */}
          <div className="bg-red-50/80 border border-red-200 rounded-2xl p-4 space-y-2.5">
            <div className="flex items-center gap-2 text-red-700 font-extrabold text-sm">
              <Info className="w-4 h-4 shrink-0 text-red-600" />
              <span>Quyền Lợi & Chính Sách Khách Đoàn (Từ 6 Phòng Trở Lên)</span>
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5 text-xs text-neutral-700">
              <div className="flex items-start gap-2">
                <ShieldCheck className="w-4 h-4 text-red-600 shrink-0 mt-0.5" />
                <span><strong className="text-neutral-900">Chiết khấu hợp đồng:</strong> Hưởng mức giá ưu đãi dành riêng cho đoàn công ty/du lịch.</span>
              </div>
              <div className="flex items-start gap-2">
                <CalendarCheck2 className="w-4 h-4 text-red-600 shrink-0 mt-0.5" />
                <span><strong className="text-neutral-900">Giữ phòng độc quyền:</strong> Khóa Room Block cố định đến hạn Cut-off Date, chống trùng phòng.</span>
              </div>
              <div className="flex items-start gap-2">
                <Receipt className="w-4 h-4 text-red-600 shrink-0 mt-0.5" />
                <span><strong className="text-neutral-900">Quy định đặt cọc:</strong> Yêu cầu thanh toán trước 50% giá trị hợp đồng để xác nhận đoàn.</span>
              </div>
              <div className="flex items-start gap-2">
                <Users className="w-4 h-4 text-red-600 shrink-0 mt-0.5" />
                <span><strong className="text-neutral-900">Hóa đơn & Quyết toán:</strong> Hỗ trợ Master Folio, xuất hóa đơn VAT và dịch vụ tiệc/hội nghị.</span>
              </div>
            </div>
          </div>

          {/* Inquiry Form */}
          <form onSubmit={handleSubmit} className="space-y-3.5">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div>
                <label className="block text-xs font-bold text-neutral-700 mb-1">
                  Người đại diện liên hệ *
                </label>
                <input
                  type="text"
                  required
                  value={contactName}
                  onChange={(e) => setContactName(e.target.value)}
                  placeholder="Họ và tên người phụ trách"
                  className="w-full h-11 px-3.5 border border-neutral-300 rounded-xl text-sm focus:ring-2 focus:ring-red-600 focus:outline-none"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-neutral-700 mb-1">
                  Số điện thoại di động *
                </label>
                <input
                  type="tel"
                  required
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  placeholder="09xx xxx xxx"
                  className="w-full h-11 px-3.5 border border-neutral-300 rounded-xl text-sm focus:ring-2 focus:ring-red-600 focus:outline-none"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-neutral-700 mb-1">
                  Email nhận báo giá
                </label>
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="email@congty.com"
                  className="w-full h-11 px-3.5 border border-neutral-300 rounded-xl text-sm focus:ring-2 focus:ring-red-600 focus:outline-none"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-neutral-700 mb-1">
                  Tên công ty / Cơ quan / Đoàn
                </label>
                <input
                  type="text"
                  value={companyName}
                  onChange={(e) => setCompanyName(e.target.value)}
                  placeholder="Công ty CP Du Lịch..."
                  className="w-full h-11 px-3.5 border border-neutral-300 rounded-xl text-sm focus:ring-2 focus:ring-red-600 focus:outline-none"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-neutral-700 mb-1">
                  Số lượng phòng dự kiến *
                </label>
                <input
                  type="number"
                  min={6}
                  max={200}
                  required
                  value={roomsCount}
                  onChange={(e) => setRoomsCount(Math.max(6, parseInt(e.target.value) || 6))}
                  className="w-full h-11 px-3.5 border border-neutral-300 rounded-xl text-sm font-bold text-red-600 focus:ring-2 focus:ring-red-600 focus:outline-none"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-neutral-700 mb-1">
                  Khách sạn dự kiến
                </label>
                <input
                  type="text"
                  disabled
                  value={hotelName}
                  className="w-full h-11 px-3.5 border border-neutral-200 bg-neutral-100 rounded-xl text-sm text-neutral-600 font-medium"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-bold text-neutral-700 mb-1">
                Yêu cầu bổ sung (Ăn uống, Hội thảo, Xe đưa đón...)
              </label>
              <textarea
                rows={2}
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                placeholder="Ví dụ: Cần 1 phòng họp 50 người, tiệc gala tối ngày nhận phòng..."
                className="w-full p-3 border border-neutral-300 rounded-xl text-sm focus:ring-2 focus:ring-red-600 focus:outline-none"
              />
            </div>

            {/* Action Buttons */}
            <div className="flex flex-col-reverse sm:flex-row items-center justify-between gap-3 pt-3 border-t border-neutral-100">
              <button
                type="button"
                onClick={onSwitchToFit}
                className="w-full sm:w-auto h-11 px-4 text-xs font-bold text-neutral-600 hover:text-neutral-900 hover:bg-neutral-100 rounded-xl transition-colors flex items-center justify-center gap-1.5 cursor-pointer"
              >
                <ArrowLeft className="w-4 h-4" />
                <span>Quay lại đặt khách lẻ (Tối đa 5 phòng)</span>
              </button>

              <Button
                type="submit"
                disabled={isSubmitting}
                className="w-full sm:w-auto h-11 px-6 font-bold flex items-center justify-center gap-2 shadow-sm"
              >
                <Send className="w-4 h-4" />
                <span>{isSubmitting ? 'Đang gửi...' : 'Gửi Yêu Cầu Báo Giá Đoàn'}</span>
              </Button>
            </div>
          </form>
        </div>
      )}
    </Modal>
  );
};

