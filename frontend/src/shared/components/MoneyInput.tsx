import { InputNumber } from 'antd'
import type { CSSProperties } from 'react'
import styles from './MoneyInput.module.css'

const MAX_MONEY = 1_000_000_000

interface MoneyInputProps {
  value?: number | null
  onChange?: (value: number | null) => void
  onBlur?: () => void
  id?: string
  placeholder?: string
  status?: 'error' | 'warning'
  disabled?: boolean
  style?: CSSProperties
  'aria-label'?: string
}

/** 1234000 → "1.234.000" khi hiển thị trong ô nhập. */
function formatDigits(value: string | number | undefined): string {
  if (value === undefined || value === '') return ''
  return String(value).replace(/\B(?=(\d{3})+(?!\d))/g, '.')
}

/**
 * Ô nhập tiền (đồng): chỉ nhận chữ số, hiển thị "1.234.000 đ", giá trị trả ra là số nguyên ≥ 0
 * hoặc null khi để trống. Chỉ nhập và định dạng, KHÔNG tính tiền (frontend/AGENTS.md).
 */
export function MoneyInput({ value, onChange, style, ...rest }: MoneyInputProps) {
  return (
    <InputNumber<number>
      {...rest}
      value={value ?? null}
      onChange={(next) => onChange?.(next === null ? null : Math.trunc(next))}
      min={0}
      max={MAX_MONEY}
      precision={0}
      step={1000}
      controls={false}
      inputMode="numeric"
      formatter={(v) => formatDigits(v)}
      parser={(text) => Number((text ?? '').replace(/\D/g, '') || 0)}
      suffix="đ"
      className={styles.input}
      style={style}
    />
  )
}
