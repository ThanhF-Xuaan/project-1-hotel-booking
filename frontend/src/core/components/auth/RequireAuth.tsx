import React from 'react'
import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { isAuthenticated } from '../../auth/auth'

/**
 * Chặn các tuyến (route) bên trong khu nhân viên: chưa đăng nhập → chuyển về /login.
 * Trang công khai (portal khách hàng, kết quả thanh toán, trang login) không bọc component này.
 */
export const RequireAuth: React.FC = () => {
  const location = useLocation()

  if (!isAuthenticated()) {
    // Ghi lại trang đang định vào để quay lại sau khi đăng nhập
    return (
      <Navigate
        to="/login"
        replace
        state={{ from: location.pathname + location.search }}
      />
    )
  }
  return <Outlet />
}

export default RequireAuth
