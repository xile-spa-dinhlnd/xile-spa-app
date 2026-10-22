import { Spin } from 'antd'
import { Navigate, Outlet, useLocation } from 'react-router'
import { useCurrentUser } from '../api'
import styles from './ProtectedRoute.module.css'

/**
 * Component bảo vệ các trang nghiệp vụ nội bộ (FR-AUTH-01).
 * Nếu chưa đăng nhập hoặc phiên hết hạn, tự động chuyển hướng về trang /login.
 */
export function ProtectedRoute() {
  const { data: user, isPending, isError } = useCurrentUser()
  const location = useLocation()

  if (isPending) {
    return (
      <div className={styles.loadingContainer}>
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
