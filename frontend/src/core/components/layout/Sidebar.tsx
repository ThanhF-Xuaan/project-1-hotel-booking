import React from 'react'
import { NavLink } from 'react-router-dom'
import { cn } from '../../utils/cn'
import {
  Globe,
  Building,
  Briefcase,
  Users,
  ShieldCheck,
  UserSquare2,
  Building2,
  FileSpreadsheet,
  LayoutDashboard,
  BedDouble,
  DoorOpen,
  TrendingUp,
  Megaphone,
  CalendarCheck,
  UserPlus,
  Receipt,
  UtensilsCrossed,
  BellRing,
  Brush,
  Moon,
} from 'lucide-react'

interface NavItem {
  label: string
  to: string
  icon: React.ReactNode
  badge?: string
}

interface NavGroup {
  groupName: string
  items: NavItem[]
}

const navGroups: NavGroup[] = [
  {
    groupName: 'TỔNG QUAN',
    items: [
      {
        label: 'Bàn làm việc (Dashboard)',
        to: '/dashboard',
        icon: <LayoutDashboard className="w-5 h-5" />,
      },
    ],
  },
  {
    groupName: 'CỔNG KHÁCH HÀNG',
    items: [
      {
        label: 'Giao diện Đặt phòng Portal',
        to: '/portal/search',
        icon: <Globe className="w-5 h-5" />,
      },
      {
        label: 'Tra cứu Đơn của Khách',
        to: '/portal/my-bookings',
        icon: <UserSquare2 className="w-5 h-5" />,
      },
    ],
  },
  {
    groupName: 'ĐẶT PHÒNG & LƯU TRÚ',
    items: [
      {
        label: 'Danh sách Đặt phòng',
        to: '/booking/list',
        icon: <CalendarCheck className="w-5 h-5" />,
      },
      {
        label: 'Tiếp nhận Khách vãng lai',
        to: '/booking/walk-in',
        icon: <UserPlus className="w-5 h-5" />,
      },
    ],
  },
  {
    groupName: 'VẬN HÀNH & NHÀ HÀNG',
    items: [
      {
        label: 'Thực đơn & Dịch vụ',
        to: '/operation/menus',
        icon: <UtensilsCrossed className="w-5 h-5" />,
      },
      {
        label: 'Gọi Dịch vụ Phòng',
        to: '/operation/room-service',
        icon: <BellRing className="w-5 h-5" />,
      },
      {
        label: 'Sơ đồ Buồng phòng',
        to: '/operation/housekeeping',
        icon: <Brush className="w-5 h-5" />,
      },
      {
        label: 'Đóng ngày (Night Audit)',
        to: '/operation/night-audit',
        icon: <Moon className="w-5 h-5" />,
      },
    ],
  },
  {
    groupName: 'TÀI CHÍNH & HÓA ĐƠN',
    items: [
      {
        label: 'Quản lý Folio & Thu ngân',
        to: '/finance/folios',
        icon: <Receipt className="w-5 h-5" />,
      },
    ],
  },
  {
    groupName: 'CƠ CẤU TỔ CHỨC',
    items: [
      {
        label: 'Khu vực / Vùng',
        to: '/organization/regions',
        icon: <Globe className="w-5 h-5" />,
      },
      {
        label: 'Khách sạn Cơ sở',
        to: '/organization/hotels',
        icon: <Building className="w-5 h-5" />,
      },
      {
        label: 'Phòng ban Tiêu chuẩn',
        to: '/organization/departments',
        icon: <Briefcase className="w-5 h-5" />,
      },
    ],
  },
  {
    groupName: 'ĐỊNH DANH & PHÂN QUYỀN',
    items: [
      {
        label: 'Nhân sự & Tài khoản',
        to: '/identity/staffs',
        icon: <Users className="w-5 h-5" />,
      },
      {
        label: 'Vai trò & Quyền hạn',
        to: '/identity/roles',
        icon: <ShieldCheck className="w-5 h-5" />,
      },
      {
        label: 'Hồ sơ Khách hàng',
        to: '/identity/guests',
        icon: <UserSquare2 className="w-5 h-5" />,
      },
      {
        label: 'Doanh nghiệp Đối tác',
        to: '/identity/companies',
        icon: <Building2 className="w-5 h-5" />,
      },
      {
        label: 'Nhật ký Kiểm toán',
        to: '/identity/audit-logs',
        icon: <FileSpreadsheet className="w-5 h-5" />,
      },
    ],
  },
  {
    groupName: 'QUẢN LÝ TỒN PHÒNG',
    items: [
      {
        label: 'Danh mục Loại phòng',
        to: '/inventory/room-types',
        icon: <BedDouble className="w-5 h-5" />,
      },
      {
        label: 'Phòng Vật lý Cơ sở',
        to: '/inventory/rooms',
        icon: <DoorOpen className="w-5 h-5" />,
      },
    ],
  },
  {
    groupName: 'CẤU HÌNH GIÁ & ƯU ĐÃI',
    items: [
      {
        label: 'Quy tắc Giá Động',
        to: '/pricing/rules',
        icon: <TrendingUp className="w-5 h-5" />,
      },
      {
        label: 'Chiến dịch Khuyến mại',
        to: '/pricing/campaigns',
        icon: <Megaphone className="w-5 h-5" />,
      },
    ],
  },
]

export const Sidebar: React.FC = () => {
  return (
    <aside className="w-64 bg-white border-r border-neutral-200 flex flex-col shrink-0 min-h-[calc(100vh-4rem)]">
      <div className="p-4 space-y-6 flex-1 overflow-y-auto">
        {navGroups.map((group) => (
          <div key={group.groupName} className="space-y-1.5">
            <h3 className="px-3 text-[11px] font-bold text-neutral-400 tracking-wider">
              {group.groupName}
            </h3>
            <div className="space-y-0.5">
              {group.items.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  className={({ isActive }) =>
                    cn(
                      'flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition-all duration-150',
                      isActive
                        ? 'bg-red-50 text-red-600 font-semibold'
                        : 'text-neutral-700 hover:bg-neutral-50 hover:text-neutral-900'
                    )
                  }
                >
                  <span className="shrink-0">{item.icon}</span>
                  <span className="truncate">{item.label}</span>
                </NavLink>
              ))}
            </div>
          </div>
        ))}
      </div>
    </aside>
  )
}

export default Sidebar
