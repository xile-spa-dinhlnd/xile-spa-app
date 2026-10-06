import { Alert, Card, Skeleton, Typography } from 'antd'
import { useServerHealth } from '../api'

export function DashboardPage() {
  const health = useServerHealth()

  return (
    <>
      <Typography.Title level={3}>Tổng quan</Typography.Title>
      <Card title="Trạng thái hệ thống" style={{ maxWidth: 480 }}>
        {health.isPending ? (
          <Skeleton active paragraph={false} />
        ) : health.isError ? (
          <Alert type="error" showIcon message={health.error.message} />
        ) : health.data.status === 'UP' ? (
          <Alert type="success" showIcon message="Máy chủ đang hoạt động bình thường." />
        ) : (
          <Alert type="warning" showIcon message="Máy chủ đang gặp sự cố." />
        )}
      </Card>
    </>
  )
}
