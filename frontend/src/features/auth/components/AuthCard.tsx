import { Card, Typography } from 'antd'
import type { ReactNode } from 'react'
import styles from './AuthCard.module.css'

interface AuthCardProps {
  title: string
  subtitle: string
  children: ReactNode
}

/** Khung chung của trang đăng nhập, quên mật khẩu, đặt lại mật khẩu: nền, thẻ, logo, tiêu đề. */
export function AuthCard({ title, subtitle, children }: AuthCardProps) {
  return (
    <div className={styles.container}>
      <Card variant="borderless" className={styles.card}>
        <div className={styles.brandHeader}>
          <img src="/logo.png" alt="Xile Beauty & Spa" className={styles.logo} />
          <Typography.Title level={4} className={styles.brandTitle}>
            {title}
          </Typography.Title>
          <Typography.Text type="secondary" className={styles.brandSubtitle}>
            {subtitle}
          </Typography.Text>
        </div>
        {children}
      </Card>
    </div>
  )
}
