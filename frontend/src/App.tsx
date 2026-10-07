import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import AdminLayout from './core/components/layout/AdminLayout'
import ProtectedRoute from './core/components/auth/ProtectedRoute'
import LoginPage from './modules/auth/pages/LoginPage'
import RegionListPage from './modules/organization/pages/RegionListPage'
import HotelListPage from './modules/organization/pages/HotelListPage'
import RoomTypeListPage from './modules/inventory/pages/RoomTypeListPage'
import RoomInstanceListPage from './modules/inventory/pages/RoomInstanceListPage'
import PricingRuleListPage from './modules/pricing/pages/PricingRuleListPage'
import CampaignListPage from './modules/pricing/pages/CampaignListPage'
import BookingListPage from './modules/booking/pages/BookingListPage'
import WalkInBookingPage from './modules/booking/pages/WalkInBookingPage'
import FolioPaymentPage from './modules/finance/pages/FolioPaymentPage'
import PaymentResultPage from './modules/finance/pages/PaymentResultPage'
import { MenuListPage } from './modules/operation/pages/MenuListPage'
import { RoomServiceOrderPage } from './modules/operation/pages/RoomServiceOrderPage'
import { HousekeepingPage } from './modules/operation/pages/HousekeepingPage'
import { NightAuditPage } from './modules/operation/pages/NightAuditPage'
import { ManagementDashboard } from './modules/dashboard/pages/ManagementDashboard'
import { SearchPage } from './modules/booking-portal/pages/SearchPage'
import { CheckoutPage } from './modules/booking-portal/pages/CheckoutPage'
import { MyBookingsPage } from './modules/booking-portal/pages/MyBookingsPage'

export function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Đăng nhập (công khai — không qua ProtectedRoute) */}
        <Route path="/login" element={<LoginPage />} />

        {/* Trang kết quả trả về từ cổng thanh toán (VNPay return / MoMo redirect) */}
        <Route path="/payment/result" element={<PaymentResultPage />} />

        {/* Customer-facing Public Booking Portal */}
        <Route path="/portal">
          <Route index element={<Navigate to="/portal/search" replace />} />
          <Route path="search" element={<SearchPage />} />
          <Route path="checkout" element={<CheckoutPage />} />
          <Route path="my-bookings" element={<MyBookingsPage />} />
        </Route>

        {/* Internal Hotel Staff & Executive Admin Console (bắt buộc đăng nhập) */}
        <Route element={<ProtectedRoute />}>
          <Route path="/" element={<AdminLayout />}>
            <Route index element={<Navigate to="/dashboard" replace />} />

            {/* Dashboard chung cho toàn bộ nhân sự */}
            <Route path="dashboard" element={<ManagementDashboard />} />

            {/* Phân hệ Đặt phòng */}
            <Route element={<ProtectedRoute roles={['CHAIN_ADMIN', 'CHAIN_EXECUTIVE', 'REGION_MANAGER', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'SALES_GROUP', 'FINANCE']} />}>
              <Route path="booking/list" element={<BookingListPage />} />
            </Route>
            <Route element={<ProtectedRoute roles={['CHAIN_ADMIN', 'PROPERTY_MANAGER', 'RECEPTIONIST']} />}>
              <Route path="booking/walk-in" element={<WalkInBookingPage />} />
            </Route>

            {/* Phân hệ Vận hành & Nhà hàng */}
            <Route element={<ProtectedRoute roles={['CHAIN_ADMIN', 'PROPERTY_MANAGER', 'F_AND_B', 'RECEPTIONIST']} />}>
              <Route path="operation/menus" element={<MenuListPage />} />
              <Route path="operation/room-service" element={<RoomServiceOrderPage />} />
            </Route>
            <Route element={<ProtectedRoute roles={['CHAIN_ADMIN', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'HOUSEKEEPING']} />}>
              <Route path="operation/housekeeping" element={<HousekeepingPage />} />
            </Route>
            <Route element={<ProtectedRoute roles={['CHAIN_ADMIN', 'PROPERTY_MANAGER', 'FINANCE']} />}>
              <Route path="operation/night-audit" element={<NightAuditPage />} />
            </Route>

            {/* Phân hệ Tài chính */}
            <Route element={<ProtectedRoute roles={['CHAIN_ADMIN', 'CHAIN_EXECUTIVE', 'REGION_MANAGER', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'FINANCE']} />}>
              <Route path="finance/folios" element={<FolioPaymentPage />} />
            </Route>

            {/* Phân hệ Cơ cấu tổ chức */}
            <Route element={<ProtectedRoute roles={['CHAIN_ADMIN', 'CHAIN_EXECUTIVE', 'REGION_MANAGER']} />}>
              <Route path="organization/regions" element={<RegionListPage />} />
            </Route>
            <Route element={<ProtectedRoute roles={['CHAIN_ADMIN', 'CHAIN_EXECUTIVE', 'REGION_MANAGER', 'PROPERTY_MANAGER']} />}>
              <Route path="organization/hotels" element={<HotelListPage />} />
            </Route>

            {/* Phân hệ Quản lý Tồn phòng */}
            <Route element={<ProtectedRoute roles={['CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER']} />}>
              <Route path="inventory/room-types" element={<RoomTypeListPage />} />
            </Route>
            <Route element={<ProtectedRoute roles={['CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'HOUSEKEEPING', 'ENGINEERING']} />}>
              <Route path="inventory/rooms" element={<RoomInstanceListPage />} />
            </Route>

            {/* Phân hệ Giá & Khuyến mại */}
            <Route element={<ProtectedRoute roles={['CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER']} />}>
              <Route path="pricing/rules" element={<PricingRuleListPage />} />
              <Route path="pricing/campaigns" element={<CampaignListPage />} />
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
    </BrowserRouter>
  )
}

export default App
