import { describe, expect, it } from 'vitest'
import { formatMoney, parseMoneyInput } from './money'

describe('formatMoney', () => {
  it.each([
    [0, '0 đ'],
    [999, '999 đ'],
    [69_000, '69.000 đ'],
    [1_234_000, '1.234.000 đ'],
    [-201_560, '-201.560 đ'],
  ])('%i → %s', (amount, expected) => {
    expect(formatMoney(amount)).toBe(expected)
  })

  it('từ chối số không nguyên (tiền là số nguyên đồng)', () => {
    expect(() => formatMoney(1.5)).toThrow()
  })
})

describe('parseMoneyInput', () => {
  it('chỉ giữ chữ số', () => {
    expect(parseMoneyInput('1.234.000 đ')).toBe(1_234_000)
  })

  it('ô trống trả về null', () => {
    expect(parseMoneyInput('  ')).toBeNull()
  })

  it('bỏ dấu trừ, không nhận số âm', () => {
    expect(parseMoneyInput('-500')).toBe(500)
  })
})
