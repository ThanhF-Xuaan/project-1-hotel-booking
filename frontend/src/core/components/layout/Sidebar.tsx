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
  Bed,
  Sparkles,
  LayoutGrid,
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
  Clock,
  Image as ImageIcon,
  Sliders,
  Tag,
  Coins,
  Layers,
  Ticket,
  PanelLeftClose,
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
        label: 'Dự báo tỷ lệ lấp đầy',
        to: '/ai/occupancy',
        icon: <TrendingUp aria-hidden="true" className="w-4 h-4" />,
        badge: 'AI',
        roles: ['CHAIN_ADMIN', 'CHAIN_EXECUTIVE', 'REGION_MANAGER', 'PROPERTY_MANAGER'],
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
        label: 'Danh mục gốc chuỗi',
        to: '/operation/master-items',
        icon: <Layers className="w-4 h-4" />,
        roles: ['CHAIN_ADMIN', 'CHAIN_EXECUTIVE', 'REGION_MANAGER', 'PROPERTY_MANAGER', 'F_AND_B'],
      },
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
      {
        label: 'Tác vụ Lập lịch & Nền',
        to: '/operation/schedulers',
        icon: <Clock className="w-4 h-4" />,
        roles: ['CHAIN_ADMIN', 'CHAIN_EXECUTIVE', 'PROPERTY_MANAGER'],
      },
      {
        label: 'Kho Đa phương tiện',
        to: '/operation/media',
        icon: <ImageIcon className="w-4 h-4" />,
        roles: ['CHAIN_ADMIN', 'CHAIN_EXECUTIVE', 'REGION_MANAGER', 'PROPERTY_MANAGER'],
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
        label: 'Cấu hình Hạng phòng Cơ sở',
        to: '/inventory/hotel-room-types',
        icon: <Building2 className="w-4 h-4" />,
        roles: ['CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER'],
      },
      {
        label: 'Ma trận Phân bổ Cơ sở',
        to: '/inventory/room-type-matrix',
        icon: <LayoutGrid className="w-4 h-4" />,
        roles: ['CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER'],
      },
      {
        label: 'Danh mục Tiêu chuẩn Giường',
        to: '/inventory/beds',
        icon: <Bed className="w-4 h-4" />,
        roles: ['CHAIN_ADMIN'],
      },
      {
        label: 'Danh mục Tiện ích Phòng',
        to: '/inventory/features',
        icon: <Sparkles className="w-4 h-4" />,
        roles: ['CHAIN_ADMIN'],
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
        label: 'Loại Quy tắc Giá',
        to: '/pricing/rule-types',
        icon: <Sliders className="w-4 h-4" />,
        roles: ['CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER'],
      },
      {
        label: 'Chiến dịch Khuyến mại',
        to: '/pricing/campaigns',
        icon: <Megaphone className="w-4 h-4" />,
        roles: ['CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER'],
      },
      {
        label: 'Quy tắc Giảm giá',
        to: '/pricing/discount-rules',
        icon: <Percent className="w-4 h-4" />,
        roles: ['CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER'],
      },
      {
        label: 'Mã Giảm Giá & Voucher',
        to: '/pricing/vouchers',
        icon: <Ticket className="w-4 h-4" />,
        roles: ['CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER'],
      },
      {
        label: 'Loại Quy tắc Giảm giá',
        to: '/pricing/discount-rule-types',
        icon: <Tag className="w-4 h-4" />,
        roles: ['CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER'],
      },
      {
        label: 'Quy tắc Phụ thu',
        to: '/pricing/surcharge-rules',
        icon: <Coins className="w-4 h-4" />,
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

export interface SidebarProps {
  isCollapsed?: boolean
  onToggle?: () => void
  isMobile?: boolean
  onCloseMobile?: () => void
}

export const Sidebar: React.FC<SidebarProps> = ({
  isCollapsed,
  onToggle,
  isMobile,
  onCloseMobile,
}) => {
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
    <aside
      className={`bg-white border-r border-neutral-200 flex flex-col shrink-0 select-none transition-all duration-300 ease-in-out ${
        isMobile
          ? 'h-full w-72 max-w-[85vw] shadow-2xl'
          : isCollapsed
          ? 'w-0 min-w-0 border-r-0 opacity-0 overflow-hidden pointer-events-none h-[calc(100vh-4rem)]'
          : 'w-64 min-w-[16rem] opacity-100 h-[calc(100vh-4rem)]'
      }`}
    >
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

      {isMobile ? (
        <div className="p-3 border-t border-neutral-200 bg-neutral-50/60 lg:hidden">
          <button
            type="button"
            onClick={onCloseMobile}
            className="w-full flex items-center justify-center gap-2 px-3 py-2 text-xs font-semibold text-neutral-600 hover:text-neutral-900 hover:bg-neutral-100 rounded-xl transition-colors cursor-pointer"
          >
            <PanelLeftClose className="w-4 h-4 text-neutral-500" />
            <span>Đóng menu</span>
          </button>
        </div>
      ) : onToggle && !isCollapsed ? (
        <div className="p-3 border-t border-neutral-200 bg-neutral-50/60">
          <button
            type="button"
            onClick={onToggle}
            className="w-full flex items-center justify-between px-3 py-2 text-xs font-semibold text-neutral-600 hover:text-neutral-900 hover:bg-neutral-100 rounded-xl transition-colors cursor-pointer"
            title="Thu gọn menu bên trái (Alt + S)"
          >
            <span className="flex items-center gap-2">
              <PanelLeftClose className="w-4 h-4 text-neutral-500" />
              <span>Thu gọn menu</span>
            </span>
            <span className="text-[10px] text-neutral-400 font-mono">Alt + S</span>
          </button>
        </div>
      ) : null}
    </aside>
  )
}

export default Sidebar
