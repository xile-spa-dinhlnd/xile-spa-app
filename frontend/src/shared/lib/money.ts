/**
 * Chỉ ĐỊNH DẠNG tiền để hiển thị. Không có hàm tính tiền ở giao diện: giảm giá, tổng, thực nhận...
 * đều do backend tính (`frontend/AGENTS.md`, BR-01).
 */

/** 1234000 → "1.234.000 đ". Tiền là số nguyên đồng; số không nguyên là lỗi lập trình. */
export function formatMoney(amount: number): string {
  if (!Number.isSafeInteger(amount)) {
    throw new Error(`Số tiền phải là số nguyên đồng, nhận được: ${amount}`)
  }
  const digits = Math.abs(amount)
    .toString()
    .replace(/\B(?=(\d{3})+(?!\d))/g, '.')
  return `${amount < 0 ? '-' : ''}${digits} đ`
}

/**
 * Đọc giá trị ô nhập tiền: chỉ giữ chữ số ("1.234.000 đ" → 1234000). Ô trống trả về null.
 * Không nhận số âm hay số thập phân.
 */
export function parseMoneyInput(input: string): number | null {
  const digits = input.replace(/\D/g, '')
  if (digits === '') return null
  const value = Number(digits)
  return Number.isSafeInteger(value) ? value : null
}
