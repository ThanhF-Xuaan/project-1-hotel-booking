import { lazy, Suspense } from 'react'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import ProtectedRoute from './core/components/auth/ProtectedRoute'

const AdminLayout = lazy(() => import('./core/components/layout/AdminLayout'))
const LoginPage = lazy(() => import('./modules/auth/pages/LoginPage'))
const RegionListPage = lazy(() => import('./modules/organization/pages/RegionListPage'))
const HotelListPage = lazy(() => import('./modules/organization/pages/HotelListPage'))
const RoomTypeListPage = lazy(() => import('./modules/inventory/pages/RoomTypeListPage'))
const RoomInstanceListPage = lazy(() => import('./modules/inventory/pages/RoomInstanceListPage'))
const PricingRuleListPage = lazy(() => import('./modules/pricing/pages/PricingRuleListPage'))
const CampaignListPage = lazy(() => import('./modules/pricing/pages/CampaignListPage'))
const BookingListPage = lazy(() => import('./modules/booking/pages/BookingListPage'))
const WalkInBookingPage = lazy(() => import('./modules/booking/pages/WalkInBookingPage'))
const PolicyManagementPage = lazy(() => import('./modules/booking/pages/PolicyManagementPage'))
const FolioPaymentPage = lazy(() => import('./modules/finance/pages/FolioPaymentPage'))
const PaymentResultPage = lazy(() => import('./modules/finance/pages/PaymentResultPage'))
const MenuListPage = lazy(() => import('./modules/operation/pages/MenuListPage')
  .then((module) => ({ default: module.MenuListPage })))
const RoomServiceOrderPage = lazy(() => import('./modules/operation/pages/RoomServiceOrderPage')
  .then((module) => ({ default: module.RoomServiceOrderPage })))
const HousekeepingPage = lazy(() => import('./modules/operation/pages/HousekeepingPage')
  .then((module) => ({ default: module.HousekeepingPage })))
const NightAuditPage = lazy(() => import('./modules/operation/pages/NightAuditPage')
  .then((module) => ({ default: module.NightAuditPage })))
const ManagementDashboard = lazy(() => import('./modules/dashboard/pages/ManagementDashboard')
  .then((module) => ({ default: module.ManagementDashboard })))
const SearchPage = lazy(() => import('./modules/booking-portal/pages/SearchPage'))
const CheckoutPage = lazy(() => import('./modules/booking-portal/pages/CheckoutPage'))
const MyBookingsPage = lazy(() => import('./modules/booking-portal/pages/MyBookingsPage'))
const RoomDetailsPage = lazy(() => import('./modules/booking-portal/pages/RoomDetailsPage'))
const PersonalizationPage = lazy(() => import('./modules/booking-portal/pages/PersonalizationPage'))
const PrivacyPage = lazy(() => import('./modules/booking-portal/pages/PrivacyPage'))

export function App() {
  return (
    <BrowserRouter>
      <Suspense fallback={
        <div role="status" className="min-h-screen flex items-center justify-center text-sm text-neutral-500">
          Đang tải trang...
        </div>
      }>
        <Routes>
          {/* Đăng nhập (công khai — không qua ProtectedRoute) */}
          <Route path="/login" element={<LoginPage />} />

          {/* Trang kết quả trả về từ cổng thanh toán (VNPay return / MoMo redirect) */}
          <Route path="/payment/result" element={<PaymentResultPage />} />

          {/* Customer-facing Public Booking Portal (công khai) */}
          <Route path="/" element={<Navigate to="/portal/search" replace />} />
          <Route path="/portal">
            <Route index element={<Navigate to="/portal/search" replace />} />
            <Route path="search" element={<SearchPage />} />
            <Route path="checkout" element={<CheckoutPage />} />
            <Route path="my-bookings" element={<MyBookingsPage />} />
            <Route path="hotels/:hotelId/room-types/:hotelRoomTypeId" element={<RoomDetailsPage />} />
            <Route path="privacy" element={<PrivacyPage />} />
            <Route element={<ProtectedRoute roles={['CUSTOMER']} />}>
              <Route path="personalization" element={<PersonalizationPage />} />
            </Route>
          </Route>

          {/* Internal Hotel Staff & Executive Admin Console (bắt buộc đăng nhập) */}
          <Route element={<ProtectedRoute />}>
            <Route element={<AdminLayout />}>
              {/* Dashboard chung cho toàn bộ nhân sự */}
              <Route path="/dashboard" element={<ManagementDashboard />} />

              {/* Phân hệ Đặt phòng */}
              <Route element={<ProtectedRoute roles={['CHAIN_ADMIN', 'CHAIN_EXECUTIVE', 'REGION_MANAGER', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'SALES_GROUP', 'FINANCE']} />}>
                <Route path="/booking/list" element={<BookingListPage />} />
              </Route>
              <Route element={<ProtectedRoute roles={['CHAIN_ADMIN', 'PROPERTY_MANAGER', 'RECEPTIONIST']} />}>
                <Route path="/booking/walk-in" element={<WalkInBookingPage />} />
              </Route>
              <Route element={<ProtectedRoute roles={['CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER']} />}>
                <Route path="booking/policies" element={<PolicyManagementPage />} />
              </Route>

              {/* Phân hệ Vận hành & Nhà hàng */}
              <Route element={<ProtectedRoute roles={['CHAIN_ADMIN', 'PROPERTY_MANAGER', 'F_AND_B', 'RECEPTIONIST']} />}>
                <Route path="/operation/menus" element={<MenuListPage />} />
                <Route path="/operation/room-service" element={<RoomServiceOrderPage />} />
              </Route>
              <Route element={<ProtectedRoute roles={['CHAIN_ADMIN', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'HOUSEKEEPING']} />}>
                <Route path="/operation/housekeeping" element={<HousekeepingPage />} />
              </Route>
              <Route element={<ProtectedRoute roles={['CHAIN_ADMIN', 'PROPERTY_MANAGER', 'FINANCE']} />}>
                <Route path="/operation/night-audit" element={<NightAuditPage />} />
              </Route>

              {/* Phân hệ Tài chính */}
              <Route element={<ProtectedRoute roles={['CHAIN_ADMIN', 'CHAIN_EXECUTIVE', 'REGION_MANAGER', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'FINANCE']} />}>
                <Route path="/finance/folios" element={<FolioPaymentPage />} />
              </Route>

              {/* Phân hệ Cơ cấu tổ chức */}
              <Route element={<ProtectedRoute roles={['CHAIN_ADMIN', 'CHAIN_EXECUTIVE', 'REGION_MANAGER']} />}>
                <Route path="/organization/regions" element={<RegionListPage />} />
              </Route>
              <Route element={<ProtectedRoute roles={['CHAIN_ADMIN', 'CHAIN_EXECUTIVE', 'REGION_MANAGER', 'PROPERTY_MANAGER']} />}>
                <Route path="/organization/hotels" element={<HotelListPage />} />
              </Route>

              {/* Phân hệ Quản lý Tồn phòng */}
              <Route element={<ProtectedRoute roles={['CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER']} />}>
                <Route path="/inventory/room-types" element={<RoomTypeListPage />} />
              </Route>
              <Route element={<ProtectedRoute roles={['CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'HOUSEKEEPING', 'ENGINEERING']} />}>
                <Route path="/inventory/rooms" element={<RoomInstanceListPage />} />
              </Route>

              {/* Phân hệ Giá & Khuyến mại */}
              <Route element={<ProtectedRoute roles={['CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER']} />}>
                <Route path="/pricing/rules" element={<PricingRuleListPage />} />
                <Route path="/pricing/campaigns" element={<CampaignListPage />} />
              </Route>

              {/* Fallback cho các module chưa triển khai */}
              <Route
                path="*"
                element={
                  <div className="flex flex-col items-center justify-center min-h-[50vh] text-center">
                    <h3 className="text-xl font-bold text-neutral-800">Mô-đun đang được phát triển</h3>
                    <p className="text-sm text-neutral-500 mt-1">
                      Tính năng này thuộc các giai đoạn tiếp theo trong lộ trình Modular Monolith.
                    </p>
                  </div>
                }
              />
            </Route>
          </Route>
        </Routes>
      </Suspense>
    </BrowserRouter>
  )
}

export default App
