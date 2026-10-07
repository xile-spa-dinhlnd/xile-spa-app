import { Spin } from 'antd'
import { Navigate, Outlet, useLocation } from 'react-router'
import { useCurrentUser } from '../api'

/**
 * Component bảo vệ các trang nghiệp vụ nội bộ (FR-AUTH-01).
 * Nếu chưa đăng nhập hoặc phiên hết hạn, tự động chuyển hướng về trang /login.
 */
export function ProtectedRoute() {
  const { data: user, isPending, isError } = useCurrentUser()
  const location = useLocation()

  if (isPending) {
    return (
      <div
        style={{
          minHeight: '100vh',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          background: '#f8faf9',
        }}
      >
        <Spin size="large" />
      </div>
    )
  }

  if (isError || !user) {
    const fromPath = location.pathname + location.search
    return (
      <Navigate
        to={`/login?from=${encodeURIComponent(fromPath)}`}
        state={{ from: location }}
        replace
      />
    )
  }

  return <Outlet />
}
