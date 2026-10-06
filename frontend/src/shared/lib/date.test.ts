import { describe, expect, it } from 'vitest'
import { formatDate, formatDateTime, todayInBusinessZone } from './date'

describe('formatDate', () => {
  it('đổi yyyy-MM-dd sang dd/MM/yyyy', () => {
    expect(formatDate('2026-10-06')).toBe('06/10/2026')
  })

  it('báo lỗi khi sai định dạng', () => {
    expect(() => formatDate('06/10/2026')).toThrow()
  })
})

describe('formatDateTime', () => {
  it('hiển thị theo giờ Việt Nam', () => {
    // 17:30 UTC ngày 5/10 là 00:30 ngày 6/10 ở Việt Nam
    expect(formatDateTime('2026-10-05T17:30:00Z')).toBe('06/10/2026 00:30')
  })
})

describe('todayInBusinessZone', () => {
  it('lấy ngày theo giờ Việt Nam, không theo máy', () => {
    expect(todayInBusinessZone(new Date('2026-10-05T17:30:00Z'))).toBe('2026-10-06')
  })
})
