import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import AdminLayout from './core/components/layout/AdminLayout'
import RegionListPage from './modules/organization/pages/RegionListPage'
import HotelListPage from './modules/organization/pages/HotelListPage'
import RoomTypeListPage from './modules/inventory/pages/RoomTypeListPage'
import RoomInstanceListPage from './modules/inventory/pages/RoomInstanceListPage'
import PricingRuleListPage from './modules/pricing/pages/PricingRuleListPage'
import CampaignListPage from './modules/pricing/pages/CampaignListPage'
import BookingListPage from './modules/booking/pages/BookingListPage'
import WalkInBookingPage from './modules/booking/pages/WalkInBookingPage'
import FolioPaymentPage from './modules/finance/pages/FolioPaymentPage'
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
        {/* Customer-facing Public Booking Portal */}
        <Route path="/portal">
          <Route index element={<Navigate to="/portal/search" replace />} />
          <Route path="search" element={<SearchPage />} />
          <Route path="checkout" element={<CheckoutPage />} />
          <Route path="my-bookings" element={<MyBookingsPage />} />
        </Route>

        {/* Internal Hotel Staff & Executive Admin Console */}
        <Route path="/" element={<AdminLayout />}>
          <Route index element={<Navigate to="/dashboard" replace />} />

          {/* Executive Dashboard */}
          <Route path="dashboard" element={<ManagementDashboard />} />

          {/* Booking Engine Module */}
          <Route path="booking/list" element={<BookingListPage />} />
          <Route path="booking/walk-in" element={<WalkInBookingPage />} />

          {/* Operation & F&B / Housekeeping Module */}
          <Route path="operation/menus" element={<MenuListPage />} />
          <Route path="operation/room-service" element={<RoomServiceOrderPage />} />
          <Route path="operation/housekeeping" element={<HousekeepingPage />} />
          <Route path="operation/night-audit" element={<NightAuditPage />} />

          {/* Finance & Folio Module */}
          <Route path="finance/folios" element={<FolioPaymentPage />} />

          {/* Organization Module */}
          <Route path="organization/regions" element={<RegionListPage />} />
          <Route path="organization/hotels" element={<HotelListPage />} />

          {/* Inventory Module */}
          <Route path="inventory/room-types" element={<RoomTypeListPage />} />
          <Route path="inventory/rooms" element={<RoomInstanceListPage />} />

          {/* Pricing Module */}
          <Route path="pricing/rules" element={<PricingRuleListPage />} />
          <Route path="pricing/campaigns" element={<CampaignListPage />} />

          {/* Fallback for other modules as they are developed */}
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
      </Routes>
    </BrowserRouter>
  )
}

export default App