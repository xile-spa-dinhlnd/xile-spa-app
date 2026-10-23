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
  List,
  Pagination,
  Popconfirm,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  type TableColumnsType,
} from 'antd'
import { useState } from 'react'
import { ApiError } from '@/shared/api/errors'
import { useDebounce } from '@/shared/hooks/useDebounce'
import { formatMoney } from '@/shared/lib/money'
import { useAllServiceGroups, useServices, useSetServiceActive } from '../api'
import { ServiceFormDrawer } from '../components/ServiceFormDrawer'
import { ServiceGroupDrawer } from '../components/ServiceGroupDrawer'
import type { ServiceItem } from '../types'
import styles from './ServiceListPage.module.css'

const PAGE_SIZE = 20
type StatusFilter = 'all' | 'active' | 'inactive'

/** Bảng giá dịch vụ (FR-SVC-01 đến 04). Máy tính: bảng; điện thoại: danh sách thẻ. */
export function ServiceListPage() {
  const screens = Grid.useBreakpoint()
  const isDesktop = screens.md ?? true
  const { message } = App.useApp()

  const [search, setSearch] = useState('')
  const [groupId, setGroupId] = useState<number | undefined>()
  const [status, setStatus] = useState<StatusFilter>('active')
  const [page, setPage] = useState(0)
  const [editing, setEditing] = useState<ServiceItem | null>(null)
  const [formOpen, setFormOpen] = useState(false)
  const [groupDrawerOpen, setGroupDrawerOpen] = useState(false)
  const q = useDebounce(search.trim(), 300)

  const groups = useAllServiceGroups()
  const services = useServices({
    q: q || undefined,
    groupId,
    active: status === 'all' ? undefined : status === 'active',
    page,
    size: PAGE_SIZE,
  })
  const setActive = useSetServiceActive()

  const hasFilters = search.trim() !== '' || groupId !== undefined || status !== 'active'
  const clearFilters = () => {
    setSearch('')
    setGroupId(undefined)
    setStatus('active')
    setPage(0)
  }
  // Danh sách trống mà đang lọc: nói rõ là do bộ lọc và cho xóa lọc nhanh.
  const emptyText = (
    <Empty
      description={hasFilters ? 'Không có dịch vụ khớp bộ lọc hiện tại' : 'Chưa có dịch vụ nào'}
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
  const openEdit = (service: ServiceItem) => {
    setEditing(service)
    setFormOpen(true)
  }
  const toggleActive = async (service: ServiceItem) => {
    try {
      await setActive.mutateAsync({ id: service.id, active: !service.active })
      message.success(service.active ? 'Đã ngừng bán dịch vụ.' : 'Đã bán lại dịch vụ.')
    } catch (error) {
      message.error(error instanceof ApiError ? error.message : 'Không thực hiện được thao tác.')
    }
  }

  const actions = (service: ServiceItem) => (
    <Space wrap>
      <Button icon={<EditOutlined />} onClick={() => openEdit(service)}>
        Sửa
      </Button>
      <Popconfirm
        title={service.active ? 'Ngừng bán dịch vụ?' : 'Bán lại dịch vụ?'}
        description={
          service.active
            ? 'Dịch vụ sẽ không còn để chọn khi nhập giao dịch. Giao dịch cũ không bị ảnh hưởng.'
            : 'Dịch vụ sẽ xuất hiện lại khi nhập giao dịch.'
        }
        okText={service.active ? 'Ngừng bán' : 'Bán lại'}
        cancelText="Hủy"
        okButtonProps={{ danger: service.active }}
        onConfirm={() => toggleActive(service)}
      >
        <Button danger={service.active}>{service.active ? 'Ngừng bán' : 'Bán lại'}</Button>
      </Popconfirm>
    </Space>
  )

  const statusTag = (active: boolean) =>
    active ? <Tag color="success">Đang bán</Tag> : <Tag>Ngừng bán</Tag>

  const columns: TableColumnsType<ServiceItem> = [
    { title: 'Tên dịch vụ', dataIndex: 'name', key: 'name' },
    { title: 'Nhóm', dataIndex: 'groupName', key: 'group' },
    {
      title: 'Thời lượng',
      dataIndex: 'durationMinutes',
      key: 'duration',
      render: (v: number | null) => (v ? `${v} phút` : '—'),
    },
    {
      title: 'Giá niêm yết',
      dataIndex: 'listPrice',
      key: 'price',
      align: 'right',
      render: (v: number) => formatMoney(v),
    },
    {
      title: 'Tiền tour',
      dataIndex: 'tourFee',
      key: 'tour',
      align: 'right',
      render: (v: number) => formatMoney(v),
    },
    { title: 'Trạng thái', dataIndex: 'active', key: 'active', render: statusTag },
    { title: '', key: 'actions', render: (_, s) => actions(s) },
  ]

  const data = services.data
  return (
    <>
      <Flex justify="space-between" align="center" wrap gap={12} className={styles.header}>
        <Typography.Title level={3} className={styles.title}>
          Dịch vụ
        </Typography.Title>
        <Space wrap>
          <Button size="large" onClick={() => setGroupDrawerOpen(true)}>
            Nhóm dịch vụ
          </Button>
          <Button type="primary" size="large" icon={<PlusOutlined />} onClick={openCreate}>
            Thêm dịch vụ
          </Button>
        </Space>
      </Flex>

      <Flex wrap gap={8} className={styles.filterBar}>
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
          className={styles.searchInput}
        />
        <Select
          allowClear
          size="large"
          placeholder="Tất cả nhóm"
          aria-label="Lọc theo nhóm"
          value={groupId}
          onChange={(v) => {
            setGroupId(v)
            setPage(0)
          }}
          options={(groups.data ?? []).map((g) => ({
            value: g.id,
            label: g.active ? g.name : `${g.name} (ngừng dùng)`,
          }))}
          className={styles.groupSelect}
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
            { value: 'active', label: 'Đang bán' },
            { value: 'inactive', label: 'Ngừng bán' },
            { value: 'all', label: 'Tất cả' },
          ]}
          className={styles.statusSelect}
        />
        {hasFilters && (
          <Button size="large" icon={<ClearOutlined />} onClick={clearFilters}>
            Xóa bộ lọc
          </Button>
        )}
      </Flex>

      {services.isError && (
        <Alert
          type="error"
          showIcon
          className={styles.errorAlert}
          title="Không tải được danh sách dịch vụ"
          description={services.error.message}
          action={
            <Button size="small" onClick={() => services.refetch()}>
              Thử lại
            </Button>
          }
        />
      )}

      {isDesktop ? (
        <Table<ServiceItem>
          rowKey="id"
          columns={columns}
          dataSource={data?.content}
          loading={services.isFetching}
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
        <>
          <List<ServiceItem>
            loading={services.isFetching}
            dataSource={data?.content ?? []}
            locale={{ emptyText }}
            renderItem={(s) => (
              <Card key={s.id} size="small" className={styles.serviceCard}>
                <Flex justify="space-between" align="start" gap={8}>
                  <Typography.Text strong>{s.name}</Typography.Text>
                  {statusTag(s.active)}
                </Flex>
                <Typography.Paragraph type="secondary" className={styles.cardSubtext}>
                  {s.groupName}
                  {s.durationMinutes ? ` · ${s.durationMinutes} phút` : ''}
                </Typography.Paragraph>
                <Typography.Paragraph className={styles.cardPrice}>
                  Giá: <b>{formatMoney(s.listPrice)}</b> · Tour: {formatMoney(s.tourFee)}
                </Typography.Paragraph>
                {actions(s)}
              </Card>
            )}
          />
          {(data?.totalPages ?? 0) > 1 && (
            <Pagination
              align="center"
              current={(data?.page ?? 0) + 1}
              pageSize={PAGE_SIZE}
              total={data?.totalElements ?? 0}
              onChange={(p) => setPage(p - 1)}
            />
          )}
        </>
      )}

      <ServiceFormDrawer open={formOpen} service={editing} onClose={() => setFormOpen(false)} />
      <ServiceGroupDrawer open={groupDrawerOpen} onClose={() => setGroupDrawerOpen(false)} />
    </>
  )
}
