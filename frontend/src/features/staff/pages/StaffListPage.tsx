import { ClearOutlined, EditOutlined, PlusOutlined, SearchOutlined } from '@ant-design/icons'
import {
  Alert,
  App,
  Button,
  Card,
  Empty,
  Flex,
  Grid,
  Input,
  Pagination,
  Popconfirm,
  Select,
  Space,
  Spin,
  Table,
  Tag,
  Typography,
  type TableColumnsType,
} from 'antd'
import { useState } from 'react'
import { ApiError } from '@/shared/api/errors'
import { useDebounce } from '@/shared/hooks/useDebounce'
import { useSetStaffActive, useStaffList } from '../api'
import { StaffFormDrawer } from '../components/StaffFormDrawer'
import { STAFF_ROLE_LABELS, type Staff, type StaffRole } from '../types'

const PAGE_SIZE = 20
type StatusFilter = 'all' | 'active' | 'inactive'

/** Danh sách nhân viên (FR-STF-01). Máy tính: bảng; điện thoại: danh sách thẻ. */
export function StaffListPage() {
  const screens = Grid.useBreakpoint()
  const isDesktop = screens.md ?? true
  const { message } = App.useApp()

  const [search, setSearch] = useState('')
  const [role, setRole] = useState<StaffRole | undefined>()
  const [status, setStatus] = useState<StatusFilter>('active')
  const [page, setPage] = useState(0)
  const [editing, setEditing] = useState<Staff | null>(null)
  const [formOpen, setFormOpen] = useState(false)
  const q = useDebounce(search.trim(), 300)

  const staffList = useStaffList({
    q: q || undefined,
    role,
    active: status === 'all' ? undefined : status === 'active',
    page,
    size: PAGE_SIZE,
  })
  const setActive = useSetStaffActive()

  const hasFilters = search.trim() !== '' || role !== undefined || status !== 'active'
  const clearFilters = () => {
    setSearch('')
    setRole(undefined)
    setStatus('active')
    setPage(0)
  }
  // Danh sách trống mà đang lọc: nói rõ là do bộ lọc và cho xóa lọc nhanh.
  const emptyText = (
    <Empty
      description={hasFilters ? 'Không có nhân viên khớp bộ lọc hiện tại' : 'Chưa có nhân viên nào'}
    >
      {hasFilters && (
        <Button icon={<ClearOutlined />} onClick={clearFilters}>
          Xóa bộ lọc
        </Button>
      )}
    </Empty>
  )

  const openCreate = () => {
    setEditing(null)
    setFormOpen(true)
  }
  const openEdit = (staff: Staff) => {
    setEditing(staff)
    setFormOpen(true)
  }
  const toggleActive = async (staff: Staff) => {
    try {
      await setActive.mutateAsync({ id: staff.id, active: !staff.active })
      message.success(staff.active ? 'Đã chuyển sang đã nghỉ.' : 'Đã cho làm lại.')
    } catch (error) {
      message.error(error instanceof ApiError ? error.message : 'Không thực hiện được thao tác.')
    }
  }

  const actions = (staff: Staff) => (
    <Space wrap>
      <Button icon={<EditOutlined />} onClick={() => openEdit(staff)}>
        Sửa
      </Button>
      <Popconfirm
        title={staff.active ? 'Chuyển sang đã nghỉ?' : 'Cho làm lại?'}
        description={
          staff.active
            ? 'Nhân viên sẽ không còn để chọn khi nhập giao dịch mới. Giao dịch cũ không bị ảnh hưởng.'
            : 'Nhân viên sẽ xuất hiện lại khi nhập giao dịch.'
        }
        okText={staff.active ? 'Đã nghỉ' : 'Làm lại'}
        cancelText="Hủy"
        okButtonProps={{ danger: staff.active }}
        onConfirm={() => toggleActive(staff)}
      >
        <Button danger={staff.active}>{staff.active ? 'Cho nghỉ' : 'Làm lại'}</Button>
      </Popconfirm>
    </Space>
  )

  const statusTag = (active: boolean) =>
    active ? <Tag color="success">Đang làm</Tag> : <Tag>Đã nghỉ</Tag>

  const columns: TableColumnsType<Staff> = [
    { title: 'Tên nhân viên', dataIndex: 'fullName', key: 'name' },
    {
      title: 'Vai trò',
      dataIndex: 'role',
      key: 'role',
      render: (r: StaffRole) => STAFF_ROLE_LABELS[r],
    },
    { title: 'Trạng thái', dataIndex: 'active', key: 'active', render: statusTag },
    { title: '', key: 'actions', render: (_, s) => actions(s) },
  ]

  const data = staffList.data
  return (
    <>
      <Flex justify="space-between" align="center" wrap gap={12} style={{ marginBottom: 16 }}>
        <Typography.Title level={3} style={{ margin: 0 }}>
          Nhân viên
        </Typography.Title>
        <Button type="primary" size="large" icon={<PlusOutlined />} onClick={openCreate}>
          Thêm nhân viên
        </Button>
      </Flex>

      <Flex wrap gap={8} style={{ marginBottom: 16 }}>
        <Input
          allowClear
          size="large"
          prefix={<SearchOutlined />}
          placeholder="Tìm theo tên"
          aria-label="Tìm theo tên"
          value={search}
          onChange={(e) => {
            setSearch(e.target.value)
            setPage(0)
          }}
          style={{ flex: '1 1 220px' }}
        />
        <Select
          allowClear
          size="large"
          placeholder="Tất cả vai trò"
          aria-label="Lọc theo vai trò"
          value={role}
          onChange={(v) => {
            setRole(v)
            setPage(0)
          }}
          options={Object.entries(STAFF_ROLE_LABELS).map(([value, label]) => ({ value, label }))}
          style={{ flex: '1 1 160px' }}
        />
        <Select
          size="large"
          aria-label="Lọc theo trạng thái"
          value={status}
          onChange={(v) => {
            setStatus(v)
            setPage(0)
          }}
          options={[
            { value: 'active', label: 'Đang làm' },
            { value: 'inactive', label: 'Đã nghỉ' },
            { value: 'all', label: 'Tất cả' },
          ]}
          style={{ flex: '1 1 140px' }}
        />
        {hasFilters && (
          <Button size="large" icon={<ClearOutlined />} onClick={clearFilters}>
            Xóa bộ lọc
          </Button>
        )}
      </Flex>

      {staffList.isError && (
        <Alert
          type="error"
          showIcon
          style={{ marginBottom: 16 }}
          title="Không tải được danh sách nhân viên"
          description={staffList.error.message}
          action={
            <Button size="small" onClick={() => staffList.refetch()}>
              Thử lại
            </Button>
          }
        />
      )}

      {isDesktop ? (
        <Table<Staff>
          rowKey="id"
          columns={columns}
          dataSource={data?.content}
          loading={staffList.isFetching}
          locale={{ emptyText }}
          pagination={{
            current: (data?.page ?? 0) + 1,
            pageSize: PAGE_SIZE,
            total: data?.totalElements ?? 0,
            showSizeChanger: false,
            onChange: (p) => setPage(p - 1),
          }}
        />
      ) : (
        <Spin spinning={staffList.isFetching}>
          {data && data.content.length === 0 && emptyText}
          <Flex vertical gap={12}>
            {data?.content.map((s) => (
              <Card key={s.id} size="small">
                <Flex justify="space-between" align="start" gap={8}>
                  <Typography.Text strong>{s.fullName}</Typography.Text>
                  {statusTag(s.active)}
                </Flex>
                <Typography.Paragraph type="secondary" style={{ margin: '4px 0 12px' }}>
                  {STAFF_ROLE_LABELS[s.role]}
                </Typography.Paragraph>
                {actions(s)}
              </Card>
            ))}
          </Flex>
          {(data?.totalPages ?? 0) > 1 && (
            <Pagination
              align="center"
              style={{ marginTop: 12 }}
              current={(data?.page ?? 0) + 1}
              pageSize={PAGE_SIZE}
              total={data?.totalElements ?? 0}
              onChange={(p) => setPage(p - 1)}
            />
          )}
        </Spin>
      )}

      <StaffFormDrawer open={formOpen} staff={editing} onClose={() => setFormOpen(false)} />
    </>
  )
}
