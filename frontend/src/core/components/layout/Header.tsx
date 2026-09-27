import React from 'react'
import { Building2, User, Bell } from 'lucide-react'

export const Header: React.FC = () => {
  return (
    <header className="h-16 bg-white border-b border-neutral-200 px-4 md:px-6 flex items-center justify-between sticky top-0 z-30 shadow-xs">
      <div className="flex items-center gap-3">
        <div className="w-10 h-10 rounded-xl bg-red-600 text-white flex items-center justify-center shadow-xs">
          <Building2 className="w-5 h-5" />
        </div>
        <div>
          <h1 className="text-base md:text-lg font-bold text-neutral-900 tracking-tight leading-none">
            UTC HOTEL SYSTEM
          </h1>
          <span className="text-[11px] font-medium text-red-600 uppercase tracking-wider">
            Modular Monolith Enterprise
          </span>
        </div>
      </div>

      <div className="flex items-center gap-3 md:gap-4">
        <button
          type="button"
          aria-label="Thông báo hệ thống"
          className="p-2 text-neutral-500 hover:text-neutral-800 hover:bg-neutral-100 rounded-xl transition-colors cursor-pointer"
        >
          <Bell className="w-5 h-5" />
        </button>

        <div className="h-8 w-px bg-neutral-200 hidden sm:block" />

        <div className="flex items-center gap-2.5">
          <div className="w-9 h-9 rounded-xl bg-neutral-100 text-neutral-700 flex items-center justify-center border border-neutral-200">
            <User className="w-4 h-4" />
          </div>
          <div className="hidden md:flex flex-col text-left">
            <span className="text-sm font-semibold text-neutral-900 leading-tight">Admin Toàn Chuỗi</span>
            <span className="text-[11px] font-medium text-neutral-500">ROLE_CHAIN_ADMIN</span>
          </div>
        </div>
      </div>
    </header>
  )
}

export default Header
