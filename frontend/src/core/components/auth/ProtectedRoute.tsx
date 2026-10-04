import React from 'react'
import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { hasAnyRole } from '../../../modules/auth/utils/token.utils'
import { ShieldAlert } from 'lucide-react'

interface ProtectedRouteProps {
  roles?: string[]
}

export const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ roles }) => {
  const location = useLocation()
  const token = localStorage.getItem('auth_token')

  // Nếu chưa đăng nhập, chuyển hướng về trang login
  if (!token) {
    return <Navigate to="/login" state={{ from: location }} replace />
  }

  // Nếu có yêu cầu role cụ thể mà người dùng không có quyền
  if (roles && roles.length > 0 && !hasAnyRole(roles)) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[60vh] text-center p-6">
        <div className="w-16 h-16 rounded-2xl bg-red-50 text-red-600 flex items-center justify-center mb-4">
          <ShieldAlert className="w-8 h-8" />
        </div>
        <h2 className="text-2xl font-bold text-neutral-900">403 - Quyền truy cập bị hạn chế</h2>
        <p className="text-neutral-500 mt-2 max-w-md text-sm">
          Tài khoản của bạn không có vai trò phù hợp để truy cập vào phân hệ này. Vui lòng liên hệ Quản trị viên hệ thống để được cấp quyền.
        </p>
      </div>
    )
  }

  return <Outlet />
}

export default ProtectedRoute
