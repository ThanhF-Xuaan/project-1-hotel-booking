import React from 'react'
import { Link } from 'react-router-dom'
import { Building2, ShieldCheck, Info, ExternalLink } from 'lucide-react'

export const PublicFooter: React.FC = () => {
  return (
    <footer className="bg-neutral-900 text-neutral-300 pt-12 pb-8 border-t border-neutral-800">
      <div className="max-w-7xl mx-auto px-4 md:px-6">
        {/* Top Disclaimer Box - Pháp lý & An toàn thông tin demo */}
        <div className="mb-10 rounded-2xl bg-neutral-800/80 border border-neutral-700/80 p-5 md:p-6 text-neutral-200">
          <div className="flex items-start gap-3.5">
            <div className="p-2 rounded-xl bg-red-600/20 text-red-400 shrink-0 mt-0.5">
              <Info className="w-5 h-5" />
            </div>
            <div className="space-y-1.5 text-xs md:text-sm leading-relaxed">
              <div className="flex flex-wrap items-center gap-2">
                <span className="font-bold text-white text-sm">
                  Thông báo Dự án Nghiên cứu & Trình diễn Đồ án (Đại học Giao thông vận tải - UTC)
                </span>
                <span className="px-2 py-0.5 rounded-full bg-amber-500/20 text-amber-300 text-[11px] font-semibold border border-amber-500/30">
                  Môi trường Demo Thử nghiệm
                </span>
              </div>
              <p className="text-neutral-300">
                Hệ thống <strong>SmartStay</strong> được xây dựng phục vụ mục đích nghiên cứu học thuật và trình diễn đồ án môn học / đồ án tốt nghiệp tại Trường Đại học GTVT (UTC). Toàn bộ danh mục phòng nghỉ, biểu phí và giao dịch thanh toán trực tuyến đang sử dụng môi trường giả lập (VNPAY Sandbox), hoàn toàn không phát sinh giao dịch tài chính thật.
              </p>
              <p className="text-amber-200/90 text-[12px] font-medium">
                ⚠️ Cảnh báo an toàn dữ liệu: Người dùng vui lòng <strong>không cung cấp giấy tờ tùy thân (CCCD/Hộ chiếu) thật hoặc thông tin định danh nhạy cảm</strong> khi trải nghiệm phiên bản demo công khai.
              </p>
            </div>
          </div>
        </div>

        {/* Middle Navigation & Info */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-8 pb-8 border-b border-neutral-800">
          <div className="md:col-span-2 space-y-3">
            <div className="flex items-center gap-2.5">
              <div className="w-9 h-9 rounded-xl bg-red-600 text-white flex items-center justify-center font-bold">
                <Building2 className="w-5 h-5" />
              </div>
              <span className="text-lg font-extrabold text-white tracking-tight">SMARTSTAY</span>
            </div>
            <p className="text-xs text-neutral-400 leading-relaxed max-w-md">
              Nền tảng Quản trị & Đặt phòng Khách sạn Thông minh tích hợp Trợ lý Trí tuệ nhân tạo (AI Advisor). Kiến trúc phân tầng Modular Monolith trên nền Java 21, Spring Boot và React Vite.
            </p>
            <div className="flex items-center gap-2 text-xs text-neutral-400">
              <ShieldCheck className="w-4 h-4 text-emerald-400" />
              <span>Bảo vệ quyền dữ liệu theo Luật 91/2025/QH15 & NĐ 356/2025/NĐ-CP</span>
            </div>
          </div>

          <div>
            <h4 className="text-xs font-bold uppercase tracking-wider text-white mb-3">
              Cổng Dịch Vụ
            </h4>
            <ul className="space-y-2 text-xs">
              <li>
                <Link to="/portal/search" className="hover:text-red-400 transition-colors">
                  Tìm kiếm & Đặt phòng
                </Link>
              </li>
              <li>
                <Link to="/portal/my-bookings" className="hover:text-red-400 transition-colors">
                  Tra cứu Đặt phòng của tôi
                </Link>
              </li>
              <li>
                <Link to="/login" className="hover:text-red-400 transition-colors inline-flex items-center gap-1">
                  Cổng Quản trị Nội bộ
                  <ExternalLink className="w-3 h-3 text-neutral-500" />
                </Link>
              </li>
            </ul>
          </div>

          <div>
            <h4 className="text-xs font-bold uppercase tracking-wider text-white mb-3">
              Pháp Lý & Học Thuật
            </h4>
            <ul className="space-y-2 text-xs">
              <li>
                <Link to="/portal/privacy" className="hover:text-red-400 transition-colors">
                  Chính sách Dữ liệu Cá nhân
                </Link>
              </li>
              <li>
                <a
                  href="https://utc.edu.vn"
                  target="_blank"
                  rel="noreferrer"
                  className="hover:text-red-400 transition-colors inline-flex items-center gap-1"
                >
                  Trường Đại học GTVT (UTC)
                  <ExternalLink className="w-3 h-3 text-neutral-500" />
                </a>
              </li>
              <li className="text-neutral-500">
                Khoa Công nghệ Thông tin
              </li>
            </ul>
          </div>
        </div>

        {/* Bottom Copyright */}
        <div className="pt-6 flex flex-col sm:flex-row items-center justify-between gap-3 text-xs text-neutral-500">
          <p>© 2026 SmartStay System • Đại học Giao thông vận tải (UTC) • All rights reserved.</p>
          <p className="text-[11px] text-neutral-600">
            Hạ tầng OCI Cloud • Nginx Gateway • Keycloak IAM Security
          </p>
        </div>
      </div>
    </footer>
  )
}

export default PublicFooter

