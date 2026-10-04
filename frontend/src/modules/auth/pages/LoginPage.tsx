import React, { useState } from 'react'
import { useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import { Building2, KeyRound, Lock, AlertCircle, Info } from 'lucide-react'
import Button from '../../../core/components/ui/Button'
import Input from '../../../core/components/ui/Input'
import { login } from '../../../core/auth/auth'

/**
 * Trang đăng nhập nhân viên (Keycloak).
 * - ?session=expired → hiển thị banner "phiên hết hạn" (client.ts tự chuyển về đây khi token không refresh được)
 * - state.from → quay lại trang đang xem sau khi đăng nhập thành công
 */
export const LoginPage: React.FC = () => {
  const navigate = useNavigate()
  const location = useLocation()
  const [searchParams] = useSearchParams()

  const from = (location.state as { from?: string } | null)?.from || '/dashboard'
  const sessionExpired = searchParams.get('session') === 'expired'

  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(false)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!username.trim() || !password) {
      setError('Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu')
      return
    }
    setIsLoading(true)
    setError(null)
    try {
      await login(username.trim(), password)
      navigate(from, { replace: true })
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Đăng nhập thất bại, vui lòng thử lại')
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-neutral-50 flex items-center justify-center px-4">
      <div className="w-full max-w-md">
        {/* Brand */}
        <div className="flex flex-col items-center mb-6">
          <div className="w-14 h-14 rounded-2xl bg-red-600 text-white flex items-center justify-center shadow-sm mb-3">
            <Building2 className="w-7 h-7" />
          </div>
          <h1 className="text-2xl font-bold text-neutral-900 tracking-tight">UTC HOTEL SYSTEM</h1>
          <span className="text-xs font-medium text-red-600 uppercase tracking-widest mt-1">
            Modular Monolith Enterprise
          </span>
        </div>

        <div className="bg-white border border-neutral-200 rounded-2xl shadow-sm p-6 md:p-8">
          <h2 className="text-lg font-bold text-neutral-900 mb-1">Đăng nhập</h2>
          <p className="text-sm text-neutral-500 mb-5">
            Dùng tài khoản nhân viên để truy cập hệ thống quản lý
          </p>

          {sessionExpired && (
            <div className="flex items-start gap-2 mb-4 p-3 rounded-xl bg-amber-50 border border-amber-200 text-sm text-amber-800">
              <Info className="w-4 h-4 mt-0.5 shrink-0" />
              <span>Phiên đăng nhập đã hết hạn — vui lòng đăng nhập lại để tiếp tục.</span>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <Input
              label="Tên đăng nhập"
              placeholder="VD: admin"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              autoComplete="username"
              autoFocus
              leftIcon={<KeyRound className="w-4 h-4" />}
            />
            <Input
              label="Mật khẩu"
              type="password"
              placeholder="••••••••"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoComplete="current-password"
              leftIcon={<Lock className="w-4 h-4" />}
            />

            {error && (
              <div className="flex items-start gap-2 p-3 rounded-xl bg-red-50 border border-red-200 text-sm text-red-700">
                <AlertCircle className="w-4 h-4 mt-0.5 shrink-0" />
                <span>{error}</span>
              </div>
            )}

            <Button type="submit" className="w-full" size="lg" isLoading={isLoading}>
              Đăng nhập
            </Button>
          </form>
        </div>

        <p className="text-center text-xs text-neutral-400 mt-4">
          Quản lý Folio · Đặt phòng · Thanh toán (VNPay / MoMo / CASH)
        </p>
      </div>
    </div>
  )
}

export default LoginPage
