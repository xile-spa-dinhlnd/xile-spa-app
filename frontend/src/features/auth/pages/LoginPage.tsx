import { Card, Typography } from 'antd'
import { useLocation, useNavigate } from 'react-router'
import { ApiError, CLIENT_ERROR_CODES } from '@/shared/api/errors'
import { useLogin } from '../api'
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
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: 'linear-gradient(145deg, #f0f5f1 0%, #e6efe8 100%)',
        padding: '24px 16px',
      }}
    >
      <Card
        variant="borderless"
        style={{
          width: '100%',
          maxWidth: 420,
          borderRadius: 16,
          boxShadow: '0 8px 30px rgba(0, 0, 0, 0.06)',
          padding: '12px 8px',
        }}
      >
        <div style={{ textAlign: 'center', marginBottom: 20 }}>
          <img
            src="/logo.png"
            alt="Xile Beauty & Spa"
            style={{
              maxHeight: 110,
              maxWidth: '80%',
              objectFit: 'contain',
              marginBottom: 8,
              display: 'inline-block',
            }}
          />
          <Typography.Title level={4} style={{ margin: 0, color: '#1f2d24', fontWeight: 600 }}>
            Xile Spa & Giãn cơ
          </Typography.Title>
          <Typography.Text type="secondary" style={{ fontSize: 13 }}>
            Hệ thống quản trị tiệm
          </Typography.Text>
        </div>

        <LoginForm
          onSubmit={handleSubmit}
          isLoading={loginMutation.isPending}
          errorMessage={errorMessage}
        />
      </Card>
    </div>
  )
}
