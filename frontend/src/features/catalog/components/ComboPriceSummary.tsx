import { Alert, Button, Spin, Typography } from 'antd'
import { formatMoney } from '@/shared/lib/money'
import type { ComboQuote } from '../types'
import styles from './ComboPriceSummary.module.css'

interface ComboPriceSummaryProps {
  quote?: ComboQuote
  isFetching: boolean
  isError: boolean
  onRetry: () => void
  price?: number | null
  itemCount: number
  hasInvalidQuantity: boolean
}

/** Khối hiển thị tổng giá lẻ, giá combo và mức tiết kiệm (FR-CMB-03). */
export function ComboPriceSummary({
  quote,
  isFetching,
  isError,
  onRetry,
  price,
  itemCount,
  hasInvalidQuantity,
}: ComboPriceSummaryProps) {
  if (itemCount === 0) {
    return (
      <div className={styles.container}>
        <Typography.Text type="secondary" className={styles.emptyHint}>
          Chọn dịch vụ để xem tổng giá lẻ.
        </Typography.Text>
      </div>
    )
  }

  if (hasInvalidQuantity) {
    return (
      <div className={styles.container}>
        <Typography.Text type="secondary" className={styles.emptyHint}>
          Nhập số lượng từ 1 đến 99 để tính tổng.
        </Typography.Text>
      </div>
    )
  }

  if (isError) {
    return (
      <div className={styles.container}>
        <div className={styles.errorBox}>
          <Typography.Text type="danger">Không tính được tổng giá lẻ.</Typography.Text>
          <Button type="link" size="small" onClick={onRetry}>
            Thử lại
          </Button>
        </div>
      </div>
    )
  }

  const hasPrice = price !== undefined && price !== null
  const savings = quote?.savings

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <span className={styles.title}>Tổng kết giá</span>
        {isFetching && <Spin size="small" />}
      </div>

      <div className={styles.summaryRow}>
        <Typography.Text type="secondary" className={styles.label}>
          Tổng giá lẻ
        </Typography.Text>
        <Typography.Text strong className={styles.value}>
          {quote ? formatMoney(quote.retailTotal) : '—'}
        </Typography.Text>
      </div>

      <div className={styles.summaryRow}>
        <Typography.Text type="secondary" className={styles.label}>
          Giá combo
        </Typography.Text>
        <Typography.Text strong className={styles.value}>
          {hasPrice ? formatMoney(price) : '—'}
        </Typography.Text>
      </div>

      <div className={styles.summaryRow}>
        <Typography.Text type="secondary" className={styles.label}>
          Tiết kiệm
        </Typography.Text>
        <span className={styles.value}>
          {savings !== null && savings !== undefined ? (
            savings > 0 ? (
              <Typography.Text type="success">{formatMoney(savings)}</Typography.Text>
            ) : savings < 0 ? (
              <Typography.Text type="warning">{formatMoney(savings)}</Typography.Text>
            ) : (
              <Typography.Text>{formatMoney(savings)}</Typography.Text>
            )
          ) : (
            '—'
          )}
        </span>
      </div>

      {savings !== null && savings !== undefined && savings < 0 && (
        <Alert
          type="warning"
          showIcon
          title="Giá combo cao hơn tổng giá lẻ. Vẫn lưu được, nhưng khách mua lẻ sẽ rẻ hơn."
          className={styles.alert}
        />
      )}

      <Typography.Text type="secondary" className={styles.subText}>
        Tính theo giá niêm yết hiện tại của dịch vụ.
      </Typography.Text>
    </div>
  )
}
