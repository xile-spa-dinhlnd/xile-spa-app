/** Định dạng phân trang chung của backend (`PageResponse`). `page` bắt đầu từ 0. */
export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}
