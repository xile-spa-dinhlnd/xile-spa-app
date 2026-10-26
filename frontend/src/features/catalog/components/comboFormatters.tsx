import { Tag, Typography } from 'antd'
import { formatMoney } from '@/shared/lib/money'
import type { ComboItem } from '../types'

/** Định dạng danh sách thành phần theo thứ tự backend trả về (FR-CMB-04). */
export function formatComboItems(items: ComboItem[]): string {
  if (!items || items.length === 0) return '—'
  return items
    .map((item) => {
      const qtyPart = item.quantity > 1 ? ` × ${item.quantity}` : ''
      const inactivePart = !item.serviceActive ? ' (ngừng bán)' : ''
      return `${item.serviceName}${qtyPart}${inactivePart}`
    })
    .join(', ')
}

/** Hiển thị mức tiết kiệm, nếu âm hiển thị cảnh báo (FR-CMB-03). */
export function renderSavings(savings: number) {
  const formatted = formatMoney(savings)
  if (savings < 0) {
    return <Typography.Text type="warning">{formatted}</Typography.Text>
  }
  return formatted
}

/** Nhãn trạng thái đang bán hoặc ngừng bán. */
export function comboStatusTag(active: boolean) {
  return active ? <Tag color="success">Đang bán</Tag> : <Tag>Ngừng bán</Tag>
}
