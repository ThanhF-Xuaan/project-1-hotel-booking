import type { FC } from 'react'
import { ArrowLeft, ShieldCheck, AlertTriangle } from 'lucide-react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import Button from '../../../core/components/ui/Button'

export const PrivacyPage: FC = () => {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const requestedHotelId = searchParams.get('hotelId')
  const hotelId = requestedHotelId && /^[1-9]\d*$/.test(requestedHotelId)
    && Number(requestedHotelId) <= 32767
    ? Number(requestedHotelId)
    : null

  return (
    <main className="min-h-screen bg-neutral-50 px-4 py-8 sm:py-12">
      <article className="mx-auto max-w-3xl rounded-3xl border border-neutral-200 bg-white p-6 shadow-sm sm:p-10">
        <button
          type="button"
          onClick={() => navigate(-1)}
          className="mb-6 inline-flex items-center gap-2 text-sm font-semibold text-neutral-600 hover:text-red-700 cursor-pointer"
        >
          <ArrowLeft className="h-4 w-4" />
          Quay lại
        </button>

        <div className="flex items-start gap-3">
          <div className="rounded-xl bg-red-50 p-2 text-red-700">
            <ShieldCheck className="h-6 w-6" />
          </div>
          <div>
            <span className="inline-flex rounded-full bg-amber-50 px-3 py-1 text-xs font-bold text-amber-800 border border-amber-200">
              Môi trường Demo Học thuật — Dữ liệu Thử nghiệm
            </span>
            <h1 className="mt-2 text-2xl font-extrabold tracking-tight text-neutral-900 sm:text-3xl">
              Chính Sách Xử Lý Dữ Liệu & Quyền Riêng Tư
            </h1>
            <p className="text-xs text-neutral-500 mt-1">
              Tuân thủ nguyên tắc bảo vệ dữ liệu theo Luật Bảo vệ dữ liệu cá nhân số 91/2025/QH15 và Nghị định 356/2025/NĐ-CP
            </p>
          </div>
        </div>

        {/* Cảnh báo quan trọng */}
        <div className="mt-6 rounded-2xl border border-amber-300 bg-amber-50 p-4 text-xs md:text-sm text-amber-950 flex items-start gap-3">
          <AlertTriangle className="w-5 h-5 text-amber-700 shrink-0 mt-0.5" />
          <div className="space-y-1">
            <p className="font-bold text-amber-900">
              Khuyến cáo quan trọng dành cho người dùng thử nghiệm:
            </p>
            <p className="text-amber-800 leading-relaxed">
              SmartStay là hệ thống được xây dựng phục vụ nghiên cứu và trình diễn đồ án tốt nghiệp tại Trường Đại học GTVT (UTC). Người dùng vui lòng <strong>tuyệt đối không cung cấp thông tin định danh cá nhân thật, số thẻ thanh toán thật hoặc hình ảnh giấy tờ tùy thân (CCCD/Hộ chiếu) thực tế</strong> khi trải nghiệm hệ thống trên môi trường công khai này.
            </p>
          </div>
        </div>

        <div className="mt-7 space-y-6 text-sm leading-7 text-neutral-700">
          <section>
            <h2 className="font-bold text-neutral-900 text-base">1. Mục đích thu thập và phạm vi xử lý dữ liệu</h2>
            <p>
              Dữ liệu người dùng cung cấp (tên đăng nhập, email thử nghiệm, họ tên khách đặt phòng) chỉ được sử dụng để thực hiện các chức năng demo của hệ thống SmartStay, bao gồm:
            </p>
            <ul className="list-disc pl-5 mt-2 space-y-1 text-neutral-600">
              <li>Đăng nhập, phân quyền quản trị nội bộ qua Keycloak IAM.</li>
              <li>Tạo mã đặt phòng thử nghiệm và mô phỏng luồng thanh toán qua cổng VNPAY Sandbox.</li>
              <li>Hỗ trợ phân tích lịch sử lưu trú để gợi ý hạng phòng cá nhân hóa (Personalization AI) khi bạn cấp sự đồng ý (Consent).</li>
            </ul>
          </section>

          <section>
            <h2 className="font-bold text-neutral-900 text-base">2. Hạ tầng lưu trữ và các bên tham gia xử lý</h2>
            <p>
              Hệ thống vận hành trên hạ tầng đám mây Oracle Cloud Infrastructure (OCI). Dữ liệu được lưu trữ trong cơ sở dữ liệu PostgreSQL và Object Storage MinIO nội bộ thuộc mạng Docker riêng biệt.
            </p>
            <p className="mt-1">
              Các chức năng thanh toán sử dụng API Sandbox của nhà cung cấp giải pháp thanh toán (VNPAY Sandbox) để trả lời kết quả thử nghiệm mà không ghi nhận bất kỳ giao dịch tài chính thực tế nào.
            </p>
          </section>

          <section>
            <h2 className="font-bold text-neutral-900 text-base">3. Thời gian lưu trữ và xóa dữ liệu</h2>
            <p>
              Dữ liệu được lưu trữ trong thời gian cần thiết cho mục đích trình diễn và bảo vệ đồ án của nhóm nghiên cứu. Toàn bộ dữ liệu đặt phòng thử nghiệm sẽ được làm sạch hoặc đặt lại theo chu kỳ bảo trì của hệ thống.
            </p>
          </section>

          <section>
            <h2 className="font-bold text-neutral-900 text-base">4. Quyền của chủ thể dữ liệu (Cấp và Rút Consent)</h2>
            <p>
              Bạn có toàn quyền rút lại sự đồng ý (Consent) phân tích lịch sử đặt phòng bất cứ lúc nào tại trang{' '}
              <Link
                className="font-semibold text-red-600 underline underline-offset-2 hover:text-red-700"
                to={hotelId === null ? '/portal/search' : `/portal/personalization?hotelId=${hotelId}`}
              >
                Gợi ý hạng phòng cá nhân hóa
              </Link>
              . Sau khi rút, hệ thống lập tức ngừng đọc lịch sử đặt phòng để tạo gợi ý mới.
            </p>
          </section>
        </div>

        <div className="mt-8 pt-6 border-t border-neutral-200 flex flex-wrap items-center justify-between gap-4">
          <Button onClick={() => navigate('/portal/search')}>
            Về Trang Tìm Phòng
          </Button>
          <span className="text-xs text-neutral-400">
            Hệ thống SmartStay • Đồ án Tốt nghiệp UTC
          </span>
        </div>
      </article>
    </main>
  )
}

export default PrivacyPage
