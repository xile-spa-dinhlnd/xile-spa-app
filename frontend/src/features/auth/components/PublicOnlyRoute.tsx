import { Spin } from 'antd'
import { Navigate, Outlet } from 'react-router'
import { useCurrentUser } from '../api'
import styles from './PublicOnlyRoute.module.css'

/**
 * Component dành riêng cho trang công khai như /login.
 * Nếu người dùng đã có phiên đăng nhập hợp lệ, tự động chuyển hướng về trang chủ /.
 */
export function PublicOnlyRoute() {
  const { data: user, isPending } = useCurrentUser()

  if (isPending) {
    return (
      <div className={styles.loadingContainer}>
        <Spin size="large" />
      </div>
    )
  }

  if (user) {
    return <Navigate to="/" replace />
  }

  return <Outlet />
}
