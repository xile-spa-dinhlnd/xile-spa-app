import { EditOutlined } from '@ant-design/icons'
import { Button, Space, Table, Typography, type TableColumnsType } from 'antd'
import type { ReactNode } from 'react'
import { formatMoney } from '@/shared/lib/money'
import type { Combo } from '../types'
import { comboStatusTag, formatComboItems, renderSavings } from './comboFormatters'
import styles from './ComboTable.module.css'

export interface ComboTableProps {
  combos?: Combo[]
  loading?: boolean
  emptyText: ReactNode
  page: number
  totalElements: number
  pageSize: number
  onPageChange: (page: number) => void
  onEdit: (combo: Combo) => void
}

/** Bảng danh sách combo cho màn hình máy tính và iPad (FR-CMB-04). */
export function ComboTable({
  combos,
  loading,
  emptyText,
  page,
  totalElements,
  pageSize,
  onPageChange,
  onEdit,
}: ComboTableProps) {
  const columns: TableColumnsType<Combo> = [
    {
      title: 'Tên combo',
      dataIndex: 'name',
      key: 'name',
      render: (_: unknown, record: Combo) => (
        <div>
          <Typography.Text strong>{record.name}</Typography.Text>
          <div className={styles.itemsSubtext}>
            <Typography.Text type="secondary">{formatComboItems(record.items)}</Typography.Text>
          </div>
        </div>
      ),
    },
    {
      title: 'Giá combo',
      dataIndex: 'price',
      key: 'price',
      align: 'right',
      render: (v: number) => formatMoney(v),
    },
    {
      title: 'Tổng giá lẻ',
      dataIndex: 'retailTotal',
      key: 'retailTotal',
      align: 'right',
      render: (v: number) => formatMoney(v),
    },
    {
      title: 'Tiết kiệm',
      dataIndex: 'savings',
      key: 'savings',
      align: 'right',
      render: (v: number) => renderSavings(v),
    },
    {
      title: 'Tiền tour',
      dataIndex: 'tourFee',
      key: 'tourFee',
      align: 'right',
      render: (v: number) => formatMoney(v),
    },
    {
      title: 'Trạng thái',
      dataIndex: 'active',
      key: 'active',
      render: (active: boolean) => comboStatusTag(active),
    },
    {
      title: '',
      key: 'actions',
      render: (_: unknown, combo: Combo) => (
        <Space wrap>
          <Button icon={<EditOutlined />} onClick={() => onEdit(combo)}>
            Sửa
          </Button>
        </Space>
      ),
    },
  ]

  return (
    <Table<Combo>
      rowKey="id"
      columns={columns}
      dataSource={combos}
      loading={loading}
      locale={{ emptyText }}
      pagination={{
        current: page + 1,
        pageSize,
        total: totalElements,
        showSizeChanger: false,
        onChange: (p) => onPageChange(p - 1),
      }}
    />
  )
}
