import { ClearOutlined, PlusOutlined, SearchOutlined } from '@ant-design/icons'
import { Alert, Button, Empty, Flex, Grid, Input, Select, Typography } from 'antd'
import { useState } from 'react'
import { useDebounce } from '@/shared/hooks/useDebounce'
import { useCombos } from '../api'
import { ComboCardList } from '../components/ComboCardList'
import { ComboFormDrawer } from '../components/ComboFormDrawer'
import { ComboTable } from '../components/ComboTable'
import type { Combo } from '../types'
import styles from './ComboListPage.module.css'

const PAGE_SIZE = 20
type StatusFilter = 'all' | 'active' | 'inactive'

/**
 * Trang danh sách combo (FR-CMB-01, FR-CMB-04).
 * Hỗ trợ tìm kiếm theo tên, lọc theo trạng thái, phân trang, và hiển thị bảng hoặc thẻ theo kích thước màn hình.
 */
export function ComboListPage() {
  const screens = Grid.useBreakpoint()
  const isDesktop = screens.md ?? true

  const [search, setSearch] = useState('')
  const [status, setStatus] = useState<StatusFilter>('active')
  const [page, setPage] = useState(0)
  const [editing, setEditing] = useState<Combo | null>(null)
  const [formOpen, setFormOpen] = useState(false)

  const debouncedSearch = useDebounce(search.trim(), 300)

  const combosQuery = useCombos({
    q: debouncedSearch || undefined,
    active: status === 'all' ? undefined : status === 'active',
    page,
    size: PAGE_SIZE,
  })

  const hasFilters = search.trim() !== '' || status !== 'active'
  const clearFilters = () => {
    setSearch('')
    setStatus('active')
    setPage(0)
  }

  // Khi lỗi tải: không hiển thị trạng thái trống (Empty) để tránh trùng thông điệp với Alert lỗi (UX Guidelines mục 2).
  const emptyText = combosQuery.isError ? (
    <></>
  ) : (
    <Empty description={hasFilters ? 'Không có combo khớp bộ lọc hiện tại' : 'Chưa có combo nào'}>
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

  const openEdit = (combo: Combo) => {
    setEditing(combo)
    setFormOpen(true)
  }

  const data = combosQuery.data

  return (
    <div>
      <Flex justify="space-between" align="center" wrap gap={12} className={styles.header}>
        <Typography.Title level={3} className={styles.title}>
          Combo
        </Typography.Title>
        <Button type="primary" icon={<PlusOutlined />} size="large" onClick={openCreate}>
          Thêm combo
        </Button>
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

      {combosQuery.isError && (
        <Alert
          type="error"
          showIcon
          className={styles.errorAlert}
          title="Không tải được danh sách combo"
          description={combosQuery.error.message}
          action={
            <Button size="small" onClick={() => combosQuery.refetch()}>
              Thử lại
            </Button>
          }
        />
      )}

      {isDesktop ? (
        <ComboTable
          combos={data?.content}
          loading={combosQuery.isFetching}
          emptyText={emptyText}
          page={data?.page ?? page}
          totalElements={data?.totalElements ?? 0}
          pageSize={PAGE_SIZE}
          onPageChange={(newPage) => setPage(newPage)}
          onEdit={openEdit}
        />
      ) : (
        <ComboCardList
          combos={data?.content}
          loading={combosQuery.isFetching}
          emptyText={emptyText}
          page={data?.page ?? page}
          totalElements={data?.totalElements ?? 0}
          totalPages={data?.totalPages ?? 0}
          pageSize={PAGE_SIZE}
          onPageChange={(newPage) => setPage(newPage)}
          onEdit={openEdit}
        />
      )}

      <ComboFormDrawer open={formOpen} combo={editing} onClose={() => setFormOpen(false)} />
    </div>
  )
}
