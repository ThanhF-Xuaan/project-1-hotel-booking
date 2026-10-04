import React, { useMemo } from 'react'
import { Building2, Bell, LogIn, LogOut, User, ShieldCheck } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { AuthService } from '../../../modules/auth/services/auth.service'
import { getUserProfile } from '../../../modules/auth/utils/token.utils'

export const Header: React.FC = () => {
  const navigate = useNavigate()
  const token = localStorage.getItem('auth_token')
  const isAuthenticated = !!token

  const userProfile = useMemo(() => {
    return isAuthenticated ? getUserProfile() : null
  }, [isAuthenticated, token])

  const handleLogout = async () => {
    try {
      await AuthService.logout()
    } catch (error) {
      console.error('Lỗi khi đăng xuất:', error)
    } finally {
      localStorage.removeItem('auth_token')
      navigate('/login')
    }
  }

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

        <div className="flex items-center">
          {isAuthenticated ? (
            /* User Avatar & Hover Dropdown */
            <div className="relative group py-2">
              <button
                type="button"
                className="w-10 h-10 rounded-xl bg-neutral-100 hover:bg-neutral-200 border border-neutral-200 flex items-center justify-center text-neutral-700 transition-all cursor-pointer shadow-2xs group-hover:ring-2 group-hover:ring-red-600/30 group-hover:border-red-500"
                aria-label="Tài khoản người dùng"
              >
                <User className="w-5 h-5 text-neutral-700" />
              </button>

              {/* Hover Dropdown Menu */}
              <div className="absolute right-0 top-full pt-1 w-72 z-50 transition-all duration-200 invisible opacity-0 translate-y-1 group-hover:visible group-hover:opacity-100 group-hover:translate-y-0 pointer-events-none group-hover:pointer-events-auto">
                <div className="bg-white rounded-2xl shadow-xl border border-neutral-200 p-4">
                  {/* User Profile Info */}
                  <div className="flex items-start gap-3">
                    <div className="w-11 h-11 rounded-xl bg-red-50 border border-red-100 flex items-center justify-center text-red-600 shrink-0">
                      <User className="w-6 h-6" />
                    </div>
                    <div className="flex-1 min-w-0">
                      <h4 className="text-sm font-bold text-neutral-900 truncate">
                        {userProfile?.name || 'Người dùng'}
                      </h4>
                      <p className="text-xs text-neutral-500 truncate mt-0.5">
                        {userProfile?.email || 'user@hotel.utc.edu.vn'}
                      </p>
                      <div className="mt-2 flex items-center gap-1.5">
                        <span className="inline-flex items-center gap-1 px-2 py-0.5 text-[11px] font-semibold bg-red-50 text-red-600 rounded-md border border-red-100">
                          <ShieldCheck className="w-3 h-3" />
                          {userProfile?.role || 'Nhân viên'}
                        </span>
                      </div>
                    </div>
                  </div>

                  {/* Divider */}
                  <div className="my-3 border-t border-neutral-100" />

                  {/* Logout Button */}
                  <button
                    type="button"
                    onClick={handleLogout}
                    className="w-full flex items-center gap-2.5 px-3 py-2 text-sm font-medium text-red-600 hover:bg-red-50 hover:text-red-700 rounded-xl transition-colors cursor-pointer"
                  >
                    <LogOut className="w-4 h-4" />
                    <span>Đăng xuất</span>
                  </button>
                </div>
              </div>
            </div>
          ) : (
            /* Login Button */
            <button
              type="button"
              onClick={() => navigate('/login')}
              className="flex items-center gap-2 px-3.5 py-2 rounded-xl bg-red-600 hover:bg-red-700 text-white shadow-xs font-semibold text-sm transition-colors cursor-pointer"
            >
              <LogIn className="w-4 h-4" />
              <span>Đăng nhập</span>
            </button>
          )}
        </div>
      </div>
    </header>
  )
}

export default Header
