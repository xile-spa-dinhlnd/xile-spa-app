import { useLocation, useNavigate } from 'react-router'
import { ApiError, CLIENT_ERROR_CODES } from '@/shared/api/errors'
import { useLogin } from '../api'
import { AuthCard } from '../components/AuthCard'
import { LoginForm } from '../components/LoginForm'
import type { LoginRequest } from '../types'

/**
 * Trang đăng nhập chính của hệ thống Xile Spa (FR-AUTH-01).
 * Thiết kế trang nhã, responsive đa thiết bị (điện thoại, iPad, máy tính bàn).
 */
export function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const loginMutation = useLogin()

  // Xác định đường dẫn điều hướng sau khi đăng nhập thành công
  const stateFrom = (location.state as { from?: { pathname?: string } } | null)?.from?.pathname
  const searchParams = new URLSearchParams(location.search)
  const queryFrom = searchParams.get('from')
  const redirectTo = stateFrom || queryFrom || '/'

  const handleSubmit = (values: LoginRequest) => {
    loginMutation.mutate(values, {
      onSuccess: () => {
        navigate(redirectTo, { replace: true })
      },
    })
  }

  // Chuyển đổi lỗi API thành thông báo tiếng Việt an toàn (FR-AUTH-01, NFR-USA-02)
  let errorMessage: string | null = null
  if (loginMutation.isError) {
    const error = loginMutation.error
    if (error instanceof ApiError) {
      if (error.status === 401) {
        errorMessage = 'Email hoặc mật khẩu không đúng.'
      } else if (error.code === CLIENT_ERROR_CODES.network) {
        errorMessage = 'Không kết nối được máy chủ. Vui lòng kiểm tra lại mạng.'
      } else {
        errorMessage = error.message
      }
    } else {
      errorMessage = 'Đã xảy ra lỗi không xác định. Vui lòng thử lại sau.'
    }
  }

  return (
    <AuthCard title="Xile Spa & Giãn cơ" subtitle="Hệ thống quản trị tiệm">
      <LoginForm
        onSubmit={handleSubmit}
        onForgotPassword={() => navigate('/forgot-password')}
        isLoading={loginMutation.isPending}
        errorMessage={errorMessage}
      />
    </AuthCard>
  )
}
