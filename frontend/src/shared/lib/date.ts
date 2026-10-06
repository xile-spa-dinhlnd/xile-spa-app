/** Ngày giờ theo múi giờ nghiệp vụ (BR-02), hiển thị dd/MM/yyyy. */

export const BUSINESS_TIME_ZONE = 'Asia/Ho_Chi_Minh'

/** "2026-10-06" (ngày làm việc từ backend) → "06/10/2026". Không đi qua Date nên không lệch múi giờ. */
export function formatDate(isoDate: string): string {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(isoDate)
  if (!match) throw new Error(`Ngày không đúng định dạng yyyy-MM-dd: ${isoDate}`)
  const [, year, month, day] = match
  return `${day}/${month}/${year}`
}

const dateTimeFormat = new Intl.DateTimeFormat('en-GB', {
  timeZone: BUSINESS_TIME_ZONE,
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
  hour12: false,
})

/** Thời điểm (ISO có múi giờ) → "06/10/2026 14:30" theo giờ Việt Nam, dù máy người dùng ở múi giờ nào. */
export function formatDateTime(isoInstant: string): string {
  const date = new Date(isoInstant)
  if (Number.isNaN(date.getTime())) throw new Error(`Thời điểm không hợp lệ: ${isoInstant}`)
  return dateTimeFormat.format(date).replace(',', '')
}

const isoDateFormat = new Intl.DateTimeFormat('en-CA', {
  timeZone: BUSINESS_TIME_ZONE,
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
})

/** Ngày hôm nay theo giờ Việt Nam, dạng "yyyy-MM-dd" (giá trị mặc định cho ô chọn ngày làm việc). */
export function todayInBusinessZone(now: Date = new Date()): string {
  return isoDateFormat.format(now)
}
