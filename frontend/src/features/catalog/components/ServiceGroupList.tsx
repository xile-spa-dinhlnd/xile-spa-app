import { EditOutlined } from '@ant-design/icons'
import { Alert, App, Button, Empty, List, Popconfirm, Space, Spin, Tag, Typography } from 'antd'
import { ApiError } from '@/shared/api/errors'
import { useAllServiceGroups, useSetServiceGroupActive } from '../api'
import type { ServiceGroup } from '../types'
import styles from './ServiceGroupList.module.css'

interface ServiceGroupListProps {
  onEdit: (group: ServiceGroup) => void
}

/** Danh sách nhóm dịch vụ trong drawer quản lý (FR-SVC-05): sửa, ngừng dùng, dùng lại. */
export function ServiceGroupList({ onEdit }: ServiceGroupListProps) {
  const { message } = App.useApp()
  const groups = useAllServiceGroups()
  const setActive = useSetServiceGroupActive()

  const toggleGroupActive = async (group: ServiceGroup) => {
    try {
      await setActive.mutateAsync({ id: group.id, active: !group.active })
      message.success(group.active ? 'Đã ngừng dùng nhóm dịch vụ.' : 'Đã dùng lại nhóm dịch vụ.')
    } catch (error) {
      message.error(error instanceof ApiError ? error.message : 'Không thực hiện được thao tác.')
    }
  }

  if (groups.isPending) return <Spin />

  if (groups.isError) {
    return (
      <Alert
        type="error"
        showIcon
        className={styles.errorAlert}
        title="Không tải được danh sách nhóm"
        description={groups.error.message}
        action={
          <Button size="small" onClick={() => groups.refetch()}>
            Thử lại
          </Button>
        }
      />
    )
  }

  return (
    <List<ServiceGroup>
      dataSource={groups.data}
      locale={{ emptyText: <Empty description="Chưa có nhóm dịch vụ nào" /> }}
      renderItem={(g) => (
        <List.Item key={g.id} className={styles.listItem}>
          <div className={styles.listMeta}>
            <Space>
              <Typography.Text strong>{g.name}</Typography.Text>
              <Tag color={g.active ? 'success' : undefined}>
                {g.active ? 'Đang dùng' : 'Ngừng dùng'}
              </Tag>
            </Space>
            <Typography.Text className={styles.listSubtext}>Thứ tự: {g.sortOrder}</Typography.Text>
          </div>
          <div className={styles.listActions}>
            <Button icon={<EditOutlined />} size="small" onClick={() => onEdit(g)}>
              Sửa
            </Button>
            {g.active ? (
              <Popconfirm
                title="Ngừng dùng nhóm dịch vụ?"
                description="Nhóm sẽ không còn để chọn khi thêm hoặc sửa dịch vụ. Nhóm còn dịch vụ đang bán thì phải ngừng bán hoặc chuyển dịch vụ sang nhóm khác trước."
                okText="Ngừng dùng"
                cancelText="Hủy"
                okButtonProps={{ danger: true }}
                onConfirm={() => toggleGroupActive(g)}
              >
                <Button danger size="small">
                  Ngừng dùng
                </Button>
              </Popconfirm>
            ) : (
              <Popconfirm
                title="Dùng lại nhóm dịch vụ?"
                description="Nhóm sẽ xuất hiện lại trong ô chọn nhóm."
                okText="Dùng lại"
                cancelText="Hủy"
                onConfirm={() => toggleGroupActive(g)}
              >
                <Button size="small">Dùng lại</Button>
              </Popconfirm>
            )}
          </div>
        </List.Item>
      )}
    />
  )
}
