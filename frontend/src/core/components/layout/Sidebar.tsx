import React, { useMemo } from 'react'
import { useLocation } from 'react-router-dom'
import { hasAnyRole } from '../../../modules/auth/utils/token.utils'
import { LODGING_ROLES } from '../../../modules/operation/types/lodging.types'
import { PAYROLL_IMPORT_ROLES } from '../../../modules/finance/services/payroll.service'
import { UTILITY_READ_ROLES } from '../../../modules/operation/types/utility.types'
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
  Gauge,
  Percent,
  Calendar,
} from 'lucide-react'
import { BranchedMenu, type BranchedMenuItem } from '../ui/BranchedMenu'

interface NavItem {
  label: string
  to: string
  icon: React.ReactNode
  badge?: string
  roles?: string[]
}

interface NavGroup {
  groupName: string
  icon?: React.ReactNode
  items: NavItem[]
}

const navGroups: NavGroup[] = [
  {
    groupName: 'TỔNG QUAN',
    icon: <LayoutDashboard className="w-4 h-4" />,
    items: [
      {
        label: 'Bàn làm việc (Dashboard)',
        to: '/dashboard',
        icon: <LayoutDashboard className="w-4 h-4" />,
      },
      {
        label: 'Dự báo lấp đầy (Demo)',
        to: '/ai/demo/occupancy',
        icon: <TrendingUp aria-hidden="true" className="w-4 h-4" />,
        badge: 'Demo',
        roles: ['CHAIN_ADMIN'],
      },
      {
        label: 'Điện/nước bất thường (Demo)',
        to: '/ai/demo/utility-anomalies',
        icon: <TrendingUp aria-hidden="true" className="w-4 h-4" />,
        badge: 'Demo',
        roles: ['CHAIN_ADMIN'],
      },
      {
        label: 'Phân phòng Group (Demo)',
        to: '/ai/demo/group-room-assignment',
        icon: <BedDouble aria-hidden="true" className="w-4 h-4" />,
        badge: 'Demo',
        roles: ['CHAIN_ADMIN'],
      },
    ],
  },
  {
    groupName: 'ĐẶT PHÒNG & LƯU TRÚ',
    icon: <CalendarCheck className="w-4 h-4" />,
    items: [
      {
        label: 'Danh sách Đặt phòng',
        to: '/booking/list',
        icon: <CalendarCheck className="w-4 h-4" />,
        roles: [
          'CHAIN_ADMIN',
          'CHAIN_EXECUTIVE',
          'REGION_MANAGER',
          'PROPERTY_MANAGER',
          'RECEPTIONIST',
          'SALES_GROUP',
          'FINANCE',
        ],
      },
      {
        label: 'Tiếp nhận khách đoàn',
        to: '/admin/groups',
        icon: <Users className="w-4 h-4" />,
        roles: [
          'CHAIN_ADMIN',
          'CHAIN_EXECUTIVE',
          'REGION_MANAGER',
          'PROPERTY_MANAGER',
          'RECEPTIONIST',
          'SALES_GROUP',
          'FINANCE',
        ],
      },
      {
        label: 'Tiếp nhận Khách vãng lai',
        to: '/booking/walk-in',
        icon: <UserPlus className="w-4 h-4" />,
        roles: ['CHAIN_ADMIN', 'PROPERTY_MANAGER', 'RECEPTIONIST'],
      },
      {
        label: 'Chính sách Hủy & No-Show',
        to: '/booking/policies',
        icon: <ShieldCheck className="w-4 h-4" />,
        roles: ['CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER'],
      },
      {
        label: 'Tra cứu Đơn của Khách',
        to: '/booking/lookup',
        icon: <UserSquare2 className="w-4 h-4" />,
        roles: [
          'CHAIN_ADMIN',
          'CHAIN_EXECUTIVE',
          'REGION_MANAGER',
          'PROPERTY_MANAGER',
          'RECEPTIONIST',
          'SALES_GROUP',
          'FINANCE',
        ],
      },
    ],
  },
  {
    groupName: 'VẬN HÀNH & NHÀ HÀNG',
    icon: <UtensilsCrossed className="w-4 h-4" />,
    items: [
      {
        label: 'Thực đơn & Dịch vụ',
        to: '/operation/menus',
        icon: <UtensilsCrossed className="w-4 h-4" />,
        roles: ['CHAIN_ADMIN', 'PROPERTY_MANAGER', 'F_AND_B', 'RECEPTIONIST'],
      },
      {
        label: 'Gọi Dịch vụ Phòng',
        to: '/operation/room-service',
        icon: <BellRing className="w-4 h-4" />,
        roles: ['CHAIN_ADMIN', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'F_AND_B'],
      },
      {
        label: 'Sơ đồ Buồng phòng',
        to: '/operation/housekeeping',
        icon: <Brush className="w-4 h-4" />,
        roles: ['CHAIN_ADMIN', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'HOUSEKEEPING'],
      },
      {
        label: 'Khai báo lưu trú',
        to: '/operation/lodging-queue',
        icon: <UserSquare2 className="w-4 h-4" />,
        roles: LODGING_ROLES,
      },
      {
        label: 'Điện/Nước',
        to: '/operation/utility',
        icon: <Gauge className="w-4 h-4" />,
        roles: UTILITY_READ_ROLES,
      },
      {
        label: 'Đóng ngày (Night Audit)',
        to: '/operation/night-audit',
        icon: <Moon className="w-4 h-4" />,
        roles: ['CHAIN_ADMIN', 'PROPERTY_MANAGER', 'FINANCE'],
      },
    ],
  },
  {
    groupName: 'TÀI CHÍNH & HÓA ĐƠN',
    icon: <Receipt className="w-4 h-4" />,
    items: [
      {
        label: 'Nhập bảng lương',
        to: '/finance/payroll-import',
        icon: <FileSpreadsheet className="w-4 h-4" />,
        roles: PAYROLL_IMPORT_ROLES,
      },
      {
        label: 'Quản lý Folio & Thu ngân',
        to: '/finance/folios',
        icon: <Receipt className="w-4 h-4" />,
        roles: [
          'CHAIN_ADMIN',
          'CHAIN_EXECUTIVE',
          'REGION_MANAGER',
          'PROPERTY_MANAGER',
          'RECEPTIONIST',
          'FINANCE',
        ],
      },
    ],
  },
  {
    groupName: 'CƠ CẤU TỔ CHỨC',
    icon: <Building className="w-4 h-4" />,
    items: [
      {
        label: 'Khu vực / Vùng',
        to: '/organization/regions',
        icon: <Globe className="w-4 h-4" />,
        roles: ['CHAIN_ADMIN', 'CHAIN_EXECUTIVE', 'REGION_MANAGER'],
      },
      {
        label: 'Khách sạn Cơ sở',
        to: '/organization/hotels',
        icon: <Building className="w-4 h-4" />,
        roles: [
          'CHAIN_ADMIN',
          'CHAIN_EXECUTIVE',
          'REGION_MANAGER',
          'PROPERTY_MANAGER',
        ],
      },
      {
        label: 'Phòng ban Tiêu chuẩn',
        to: '/organization/departments',
        icon: <Briefcase className="w-4 h-4" />,
        roles: [
          'CHAIN_ADMIN',
          'CHAIN_EXECUTIVE',
          'REGION_MANAGER',
          'PROPERTY_MANAGER',
        ],
      },
    ],
  },
  {
    groupName: 'ĐỊNH DANH & PHÂN QUYỀN',
    icon: <ShieldCheck className="w-4 h-4" />,
    items: [
      {
        label: 'Nhân sự & Tài khoản',
        to: '/identity/staffs',
        icon: <Users className="w-4 h-4" />,
        roles: ['CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER'],
      },
      {
        label: 'Vai trò & Quyền hạn',
        to: '/identity/roles',
        icon: <ShieldCheck className="w-4 h-4" />,
        roles: ['CHAIN_ADMIN'],
      },
      {
        label: 'Hồ sơ Khách hàng',
        to: '/identity/guests',
        icon: <UserSquare2 className="w-4 h-4" />,
        roles: ['CHAIN_ADMIN', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'SALES_GROUP'],
      },
      {
        label: 'Doanh nghiệp Đối tác',
        to: '/identity/companies',
        icon: <Building2 className="w-4 h-4" />,
        roles: [
          'CHAIN_ADMIN',
          'REGION_MANAGER',
          'PROPERTY_MANAGER',
          'SALES_GROUP',
        ],
      },
      {
        label: 'Nhật ký Kiểm toán',
        to: '/identity/audit-logs',
        icon: <FileSpreadsheet className="w-4 h-4" />,
        roles: ['CHAIN_ADMIN', 'CHAIN_EXECUTIVE'],
      },
    ],
  },
  {
    groupName: 'QUẢN LÝ TỒN PHÒNG',
    icon: <BedDouble className="w-4 h-4" />,
    items: [
      {
        label: 'Danh mục Loại phòng',
        to: '/inventory/room-types',
        icon: <BedDouble className="w-4 h-4" />,
        roles: ['CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER'],
      },
      {
        label: 'Phòng Vật lý Cơ sở',
        to: '/inventory/rooms',
        icon: <DoorOpen className="w-4 h-4" />,
        roles: [
          'CHAIN_ADMIN',
          'REGION_MANAGER',
          'PROPERTY_MANAGER',
          'RECEPTIONIST',
          'HOUSEKEEPING',
          'ENGINEERING',
        ],
      },
    ],
  },
  {
    groupName: 'CẤU HÌNH GIÁ & ƯU ĐÃI',
    icon: <TrendingUp className="w-4 h-4" />,
    items: [
      {
        label: 'Quy tắc Giá Động',
        to: '/pricing/rules',
        icon: <TrendingUp className="w-4 h-4" />,
        roles: ['CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER'],
      },
      {
        label: 'Chiến dịch Khuyến mại',
        to: '/pricing/campaigns',
        icon: <Megaphone className="w-4 h-4" />,
        roles: ['CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER'],
      },
      {
        label: 'Chính sách Độ tuổi',
        to: '/pricing/age-policies',
        icon: <Users className="w-4 h-4" />,
        roles: ['CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER'],
      },
      {
        label: 'Lịch Ngày Lễ Tết',
        to: '/pricing/holiday-calendars',
        icon: <Calendar className="w-4 h-4" />,
        roles: ['CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER'],
      },
      {
        label: 'Danh mục Thuế',
        to: '/pricing/tax-categories',
        icon: <Receipt className="w-4 h-4" />,
        roles: ['CHAIN_ADMIN', 'FINANCE'],
      },
      {
        label: 'Quy tắc Thuế VAT',
        to: '/pricing/vat-rules',
        icon: <Percent className="w-4 h-4" />,
        roles: ['CHAIN_ADMIN', 'FINANCE'],
      },
    ],
  },
]

export const Sidebar: React.FC = () => {
  const location = useLocation()

  const menuItems = useMemo<BranchedMenuItem[]>(() => {
    const list: BranchedMenuItem[] = []
    for (const group of navGroups) {
      const allowedItems = group.items.filter((item) => hasAnyRole(item.roles))
      if (allowedItems.length > 0) {
        list.push({
          label: group.groupName,
          value: group.groupName,
          icon: group.icon,
          children: allowedItems.map((item) => ({
            label: item.label,
            value: item.to,
            to: item.to,
            icon: item.icon,
            badge: item.badge,
          })),
        })
      }
    }
    return list
  }, [])

  return (
    <aside className="w-64 bg-white border-r border-neutral-200 flex flex-col shrink-0 min-h-[calc(100vh-4rem)] select-none">
      <div className="py-4 px-3 flex-1 overflow-y-auto">
        <BranchedMenu
          items={menuItems}
          active={location.pathname}
          defaultOpen="active"
          color="#171717"
          accentColor="#dc2626"
          lineColor="#e5e7eb"
          rowHeight={36}
          indent={36}
          trunk={14}
          radius={9}
          fontSize={13}
        />
      </div>
    </aside>
  )
}

export default Sidebar
