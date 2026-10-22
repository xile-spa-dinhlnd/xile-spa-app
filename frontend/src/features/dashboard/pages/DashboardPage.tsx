import { Alert, Card, Skeleton, Typography } from 'antd'
import { useServerHealth } from '../api'
import styles from './DashboardPage.module.css'

export function DashboardPage() {
  const health = useServerHealth()

  return (
    <>
      <Typography.Title level={3}>Tổng quan</Typography.Title>
      <Card title="Trạng thái hệ thống" className={styles.card}>
        {health.isPending ? (
          <Skeleton active paragraph={false} />
        ) : health.isError ? (
          <Alert type="error" showIcon title={health.error.message} />
        ) : health.data.status === 'UP' ? (
          <Alert type="success" showIcon title="Máy chủ đang hoạt động bình thường." />
        ) : (
          <Alert type="warning" showIcon title="Máy chủ đang gặp sự cố." />
        )}
      </Card>
    </>
  )
}
