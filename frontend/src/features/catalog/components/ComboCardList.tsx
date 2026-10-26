import { EditOutlined } from '@ant-design/icons'
import { Button, Card, Flex, List, Pagination, Space, Typography } from 'antd'
import type { ReactNode } from 'react'
import { formatMoney } from '@/shared/lib/money'
import type { Combo } from '../types'
import { comboStatusTag, formatComboItems, renderSavings } from './comboFormatters'
import styles from './ComboCardList.module.css'

export interface ComboCardListProps {
  combos?: Combo[]
  loading?: boolean
  emptyText: ReactNode
  page: number
  totalElements: number
  totalPages: number
  pageSize: number
  onPageChange: (page: number) => void
  onEdit: (combo: Combo) => void
}

/** Danh sách thẻ combo cho màn hình điện thoại (FR-CMB-04). */
export function ComboCardList({
  combos,
  loading,
  emptyText,
  page,
  totalElements,
  totalPages,
  pageSize,
  onPageChange,
  onEdit,
}: ComboCardListProps) {
  return (
    <>
      <List<Combo>
        loading={loading}
        dataSource={combos ?? []}
        locale={{ emptyText }}
        renderItem={(combo) => (
          <Card key={combo.id} size="small" className={styles.comboCard}>
            <Flex justify="space-between" align="start" gap={8}>
              <Typography.Text strong>{combo.name}</Typography.Text>
              {comboStatusTag(combo.active)}
            </Flex>
            <Typography.Paragraph type="secondary" className={styles.cardItems}>
              {formatComboItems(combo.items)}
            </Typography.Paragraph>
            <Typography.Paragraph className={styles.cardLine}>
              Giá combo: <b>{formatMoney(combo.price)}</b> · Giá lẻ:{' '}
              {formatMoney(combo.retailTotal)}
            </Typography.Paragraph>
            <Typography.Paragraph className={styles.cardLine}>
              Tiết kiệm: {renderSavings(combo.savings)} · Tour: {formatMoney(combo.tourFee)}
            </Typography.Paragraph>
            <Space wrap>
              <Button icon={<EditOutlined />} onClick={() => onEdit(combo)}>
                Sửa
              </Button>
            </Space>
          </Card>
        )}
      />
      {totalPages > 1 && (
        <Pagination
          align="center"
          current={page + 1}
          pageSize={pageSize}
          total={totalElements}
          onChange={(p) => onPageChange(p - 1)}
        />
      )}
    </>
  )
}
