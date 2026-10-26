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

/**
 * Khối tổng giá lẻ và mức tiết kiệm của combo (FR-CMB-03).
 * Mọi số liệu do backend tính (POST /api/catalog/combos/quote), giao diện chỉ hiển thị và định dạng.
 */
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
        <p className={styles.emptyHint}>Chọn dịch vụ để xem tổng giá lẻ.</p>
      </div>
    )
  }

  if (hasInvalidQuantity) {
    return (
      <div className={styles.container}>
        <p className={styles.emptyHint}>Nhập số lượng từ 1 đến 99 để tính tổng.</p>
      </div>
    )
  }

  if (isError) {
    return (
      <div className={styles.container}>
        <div className={styles.errorBox}>
          <span>Không tính được tổng giá lẻ.</span>
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
        <span className={styles.label}>Tổng giá lẻ</span>
        <span className={styles.value}>{quote ? formatMoney(quote.retailTotal) : '—'}</span>
      </div>

      <div className={styles.summaryRow}>
        <span className={styles.label}>Giá combo</span>
        <span className={styles.value}>{hasPrice ? formatMoney(price) : '—'}</span>
      </div>

      <div className={styles.summaryRow}>
        <span className={styles.label}>Tiết kiệm</span>
        <span className={styles.value}>
          {savings === null || savings === undefined ? (
            '—'
          ) : savings >= 0 ? (
            <Typography.Text type="success" strong>
              {formatMoney(savings)}
            </Typography.Text>
          ) : (
            <Typography.Text type="warning" strong>
              {formatMoney(savings)}
            </Typography.Text>
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

      <div className={styles.subText}>Tính theo giá niêm yết hiện tại của dịch vụ.</div>
    </div>
  )
}
