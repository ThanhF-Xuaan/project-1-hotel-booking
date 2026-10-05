import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import {
  Building2,
  AlertCircle,
  Eye,
  EyeOff,
  ShieldCheck,
  TrendingUp,
  BedDouble,
  Star,
  Sparkles,
  HelpCircle,
} from 'lucide-react'
import { AuthService } from '../services/auth.service'

const loginSchema = z.object({
  username: z.string().min(1, 'Tên đăng nhập không được để trống'),
  password: z.string().min(1, 'Mật khẩu không được để trống'),
})

type LoginFormValues = z.infer<typeof loginSchema>

export const LoginPage: React.FC = () => {
  const navigate = useNavigate()
  const [errorMsg, setErrorMsg] = useState<string | null>(null)
  const [showPassword, setShowPassword] = useState(false)

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
  })

  const onSubmit = async (data: LoginFormValues) => {
    try {
      setErrorMsg(null)
      const res = await AuthService.login({
        username: data.username.trim(),
        password: data.password.trim(),
      })

      if (res.code === 200 && res.result) {
        localStorage.setItem('auth_token', res.result.access_token)
        // Lưu kèm refresh_token để client.ts tự làm mới phiên khi access token hết hạn
        if (res.result.refresh_token) {
          localStorage.setItem('auth_refresh_token', res.result.refresh_token)
        }
        navigate('/dashboard')
      } else {
        setErrorMsg(res.message || 'Đăng nhập thất bại')
      }
    } catch (error: any) {
      console.error(error)
      setErrorMsg(error?.response?.data?.message || 'Có lỗi xảy ra khi kết nối đến máy chủ')
    }
  }

  return (
    <div className="min-h-screen bg-white flex flex-col lg:flex-row">
      <div className="hidden lg:flex lg:w-1/2 bg-neutral-50 border-r border-neutral-200 p-12 xl:p-16 flex-col justify-between relative overflow-hidden">
        <div className="absolute -top-24 -left-24 w-96 h-96 bg-red-100/50 rounded-full blur-3xl pointer-events-none" />
        <div className="absolute -bottom-24 -right-24 w-96 h-96 bg-neutral-200/50 rounded-full blur-3xl pointer-events-none" />
        <div className="relative z-10">
          <div className="flex items-center gap-3">
            <div className="w-12 h-12 rounded-2xl bg-red-600 text-white flex items-center justify-center shadow-sm">
              <Building2 className="w-6 h-6" />
            </div>
            <div>
              <h1 className="text-xl font-bold text-neutral-900 tracking-tight leading-none">
                UTC HOTEL SYSTEM
              </h1>
              <span className="text-[11px] font-semibold text-red-600 uppercase tracking-widest">
                Modular Monolith Enterprise
              </span>
            </div>
          </div>
        </div>


        <div className="relative my-auto py-10 flex items-center justify-center">
          <div className="w-[340px] bg-white rounded-3xl p-5 shadow-xl border border-neutral-200/80 relative z-20 transition-transform duration-300 hover:scale-[1.02]">
            <div className="h-44 rounded-2xl bg-gradient-to-tr from-neutral-800 to-neutral-700 relative overflow-hidden flex flex-col justify-between p-4 text-white">
              <div className="flex justify-between items-center">
                <span className="bg-red-600/90 backdrop-blur-xs text-white text-[11px] font-bold px-2.5 py-1 rounded-full uppercase tracking-wider">
                  Deluxe Ocean View
                </span>
                <span className="flex items-center gap-1 bg-black/40 backdrop-blur-xs px-2 py-0.5 rounded-full text-xs font-medium text-amber-300">
                  <Star className="w-3.5 h-3.5 fill-amber-300" /> 4.9
                </span>
              </div>
              <div>
                <span className="text-xs text-neutral-300 font-light">UTC Grand Hà Nội</span>
                <h3 className="text-base font-bold leading-tight">Phòng Tổng Thống Hạng Sang</h3>
              </div>
            </div>

            <div className="mt-4 flex items-center justify-between">
              <div>
                <span className="text-[11px] text-neutral-500 uppercase font-semibold">Giá tiêu chuẩn</span>
                <p className="text-lg font-bold text-neutral-900">
                  3.250.000 <span className="text-xs font-normal text-neutral-500">VNĐ/đêm</span>
                </p>
              </div>
              <span className="inline-flex items-center gap-1 text-xs font-medium text-emerald-700 bg-emerald-50 px-2.5 py-1 rounded-lg border border-emerald-200">
                <BedDouble className="w-3.5 h-3.5" /> Sẵn sàng
              </span>
            </div>
          </div>

          <div className="absolute -left-4 bottom-4 bg-white/95 backdrop-blur-md rounded-2xl p-4 shadow-lg border border-neutral-200 z-30 flex items-center gap-3 animate-pulse duration-1000">
            <div className="w-10 h-10 rounded-xl bg-red-50 text-red-600 flex items-center justify-center">
              <TrendingUp className="w-5 h-5" />
            </div>
            <div>
              <p className="text-[11px] text-neutral-500 font-medium">Tỉ lệ lấp đầy hôm nay</p>
              <p className="text-base font-bold text-neutral-900">98.5% <span className="text-[11px] text-emerald-600 font-semibold">(+12%)</span></p>
            </div>
          </div>

          <div className="absolute -right-2 top-4 bg-white/95 backdrop-blur-md rounded-2xl px-4 py-2.5 shadow-lg border border-neutral-200 z-30 flex items-center gap-2">
            <div className="w-7 h-7 rounded-lg bg-red-600 text-white flex items-center justify-center">
              <Sparkles className="w-4 h-4" />
            </div>
            <div>
              <p className="text-xs font-bold text-neutral-900">AI Concierge 2.0</p>
              <p className="text-[10px] text-neutral-500">Tự động hóa vận hành</p>
            </div>
          </div>

          <div className="absolute -top-6 left-12 w-12 h-12 rounded-full bg-red-50 border-2 border-white shadow-md flex items-center justify-center text-red-600 z-30">
            <ShieldCheck className="w-6 h-6" />
          </div>
        </div>

        <div className="relative z-10 text-neutral-600 text-sm">
          <p className="font-semibold text-neutral-900">Hệ thống Quản lý Khách sạn Nội bộ Tập trung</p>
          <p className="text-xs text-neutral-500 mt-1">
            Đồng bộ dữ liệu thời gian thực tích hợp AI tư vấn đặt phòng và tự động hóa chuỗi đa phân hệ.
          </p>
        </div>
      </div>

      <div className="w-full lg:w-1/2 flex items-center justify-center p-6 sm:p-12 lg:p-16">
        <div className="w-full max-w-[420px]">
          <div className="lg:hidden flex items-center gap-3 mb-8">
            <div className="w-10 h-10 rounded-xl bg-red-600 text-white flex items-center justify-center shadow-xs">
              <Building2 className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-lg font-bold text-neutral-900 leading-none">UTC HOTEL SYSTEM</h2>
              <span className="text-[10px] font-semibold text-red-600 uppercase">Modular Monolith</span>
            </div>
          </div>

          <div className="mb-8">
            <h2 className="text-2xl sm:text-3xl font-bold tracking-tight text-neutral-900">
              Đăng nhập hệ thống
            </h2>
            <p className="mt-2 text-sm text-neutral-600">
              Nhập tài khoản định danh IAM để truy cập bảng điều khiển vận hành.
            </p>
          </div>

          {errorMsg && (
            <div className="mb-6 bg-red-50 border border-red-200 text-red-600 px-4 py-3 rounded-xl text-sm flex items-start gap-3">
              <AlertCircle className="w-5 h-5 shrink-0 mt-0.5" />
              <span>{errorMsg}</span>
            </div>
          )}

          <form className="space-y-5" onSubmit={handleSubmit(onSubmit)}>
            <div>
              <label
                htmlFor="username"
                className="block text-sm font-semibold text-neutral-900 mb-1.5"
              >
                Tên đăng nhập hoặc Email
              </label>
              <input
                id="username"
                type="text"
                autoComplete="username"
                {...register('username')}
                className={`block w-full h-[48px] rounded-xl border px-4 text-sm text-neutral-900 placeholder:text-neutral-400 focus:outline-none focus:ring-2 focus:ring-red-600 transition-all ${errors.username
                  ? 'border-red-500 bg-red-50/50'
                  : 'border-neutral-300 hover:border-neutral-400 focus:border-red-600 bg-white'
                  }`}
                placeholder="Nhập tên đăng nhập (ví dụ: admin)"
              />
              {errors.username && (
                <p className="mt-1.5 text-xs font-medium text-red-600">{errors.username.message}</p>
              )}
            </div>

            <div>
              <div className="flex justify-between items-center mb-1.5">
                <label
                  htmlFor="password"
                  className="block text-sm font-semibold text-neutral-900"
                >
                  Mật khẩu
                </label>
              </div>
              <div className="relative">
                <input
                  id="password"
                  type={showPassword ? 'text' : 'password'}
                  autoComplete="current-password"
                  {...register('password')}
                  className={`block w-full h-[48px] rounded-xl border pl-4 pr-12 text-sm text-neutral-900 placeholder:text-neutral-400 focus:outline-none focus:ring-2 focus:ring-red-600 transition-all ${errors.password
                    ? 'border-red-500 bg-red-50/50'
                    : 'border-neutral-300 hover:border-neutral-400 focus:border-red-600 bg-white'
                    }`}
                  placeholder="Nhập mật khẩu của bạn"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3.5 top-1/2 -translate-y-1/2 text-neutral-400 hover:text-neutral-700 transition-colors p-1"
                  aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                >
                  {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
              {errors.password && (
                <p className="mt-1.5 text-xs font-medium text-red-600">{errors.password.message}</p>
              )}
            </div>

            <button
              type="submit"
              disabled={isSubmitting}
              className="w-full h-[48px] mt-2 flex items-center justify-center rounded-xl bg-red-600 hover:bg-red-700 active:bg-red-800 text-white text-sm font-bold shadow-xs hover:shadow-md transition-all disabled:opacity-70 disabled:cursor-not-allowed cursor-pointer"
            >
              {isSubmitting ? (
                <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
              ) : (
                'Đăng nhập'
              )}
            </button>
          </form>

          <div className="mt-5 text-center">
            <button
              type="button"
              onClick={() => alert('Vui lòng liên hệ Quản trị viên hệ thống (Admin) để được cấp lại mật khẩu.')}
              className="text-sm font-medium text-neutral-600 hover:text-red-600 transition-colors cursor-pointer"
            >
              Quên mật khẩu?
            </button>
          </div>

          <div className="my-6 relative flex items-center justify-center">
            <div className="w-full border-t border-neutral-200" />
            <span className="absolute bg-white px-3 text-xs font-medium text-neutral-400 uppercase tracking-wider">
              Nội bộ
            </span>
          </div>

          <button
            type="button"
            onClick={() => alert('Hệ thống quản lý nội bộ dành riêng cho Cán bộ & Nhân viên chuỗi khách sạn UTC. Khách hàng vui lòng truy cập cổng đặt phòng công khai.')}
            className="w-full h-[48px] rounded-xl border border-neutral-300 hover:bg-neutral-50 text-neutral-800 text-sm font-semibold transition-colors flex items-center justify-center gap-2 cursor-pointer shadow-2xs"
          >
            <HelpCircle className="w-4 h-4 text-neutral-500" />
            <span>Hướng dẫn & Quy chế truy cập</span>
          </button>
          <div className="mt-8 text-center">
            <p className="text-[11px] text-neutral-400">
              © 2026 UTC Hotel System • Được bảo vệ bởi Keycloak IAM & Spring Security
            </p>
          </div>
        </div>
      </div>
    </div>
  )
}

export default LoginPage