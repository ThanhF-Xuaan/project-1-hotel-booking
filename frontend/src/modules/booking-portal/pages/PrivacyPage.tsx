import type { FC } from 'react'
import { ArrowLeft, ShieldCheck } from 'lucide-react'
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
          className="mb-6 inline-flex items-center gap-2 text-sm font-semibold text-neutral-600 hover:text-red-700"
        >
          <ArrowLeft className="h-4 w-4" />
          Quay lại
        </button>

        <div className="flex items-start gap-3">
          <div className="rounded-xl bg-red-50 p-2 text-red-700"><ShieldCheck className="h-5 w-5" /></div>
          <div>
            <span className="inline-flex rounded-full bg-amber-50 px-3 py-1 text-xs font-bold text-amber-800">
              Demo — dữ liệu giả lập
            </span>
            <h1 className="mt-3 text-2xl font-extrabold tracking-tight text-neutral-900 sm:text-3xl">
              Quyền riêng tư trong demo U14
            </h1>
          </div>
        </div>

        <div className="mt-7 space-y-6 text-sm leading-7 text-neutral-700">
          <section>
            <h2 className="font-bold text-neutral-900">Mục đích</h2>
            <p>
              Khi bạn đồng ý, demo dùng lịch sử booking FIT một phòng đã hoàn tất, đứng tên hồ sơ được liên kết,
              tại khách sạn bạn chọn trong 12 tháng gần nhất để xếp hạng hạng phòng có thể bạn quan tâm.
              Phân tích chỉ chạy khi consent còn hiệu lực; lịch sử trước thời điểm cấp consent cũng nằm trong phạm vi này.
            </p>
          </section>

          <section>
            <h2 className="font-bold text-neutral-900">Dữ liệu và nơi lưu trong demo</h2>
            <p>
              Đây là dữ liệu tổng hợp dành cho kiểm thử local, không phải dữ liệu khách thật. Fixture của demo nằm trong
              PostgreSQL và Keycloak của sandbox acceptance local. Dữ liệu có thể còn trong Docker volume sau khi dừng
              ứng dụng và được giữ tới khi sandbox được reset hoặc khôi phục từ snapshot.
            </p>
            <p className="mt-2">
              Gợi ý chỉ mang tính tham khảo; không thay đổi giá, khuyến mại, tình trạng phòng hoặc đặt phòng.
            </p>
          </section>

          <section>
            <h2 className="font-bold text-neutral-900">Cấp và rút consent</h2>
            <p>
              Bạn có thể rút consent bất cứ lúc nào tại trang{' '}
              <Link
                className="font-semibold text-red-700 underline underline-offset-2"
                to={hotelId === null ? '/portal/search' : `/portal/personalization?hotelId=${hotelId}`}
              >
                gợi ý hạng phòng cá nhân hóa
              </Link>
              . Sau khi rút, demo không tiếp tục đọc lịch sử để tạo gợi ý mới.
            </p>
          </section>

          <p className="rounded-xl border border-amber-200 bg-amber-50 p-4 text-amber-950">
            Nội dung này chỉ áp dụng cho demo dùng dữ liệu giả lập; không phải chính sách quyền riêng tư cho khách hàng thật.
          </p>
        </div>

        <Button className="mt-8" onClick={() => navigate('/portal/search')}>
          Về tìm phòng
        </Button>
      </article>
    </main>
  )
}

export default PrivacyPage
