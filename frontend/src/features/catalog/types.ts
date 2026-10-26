/** Kiểu dữ liệu khớp DTO backend module catalog (S1-05). Tiền là số nguyên đồng. */

export interface ServiceGroup {
  id: number
  name: string
  sortOrder: number
  active: boolean
}

/** Thân request tạo và sửa nhóm dịch vụ (S2-06). */
export interface ServiceGroupRequest {
  name: string
  sortOrder: number
}

export interface ServiceItem {
  id: number
  name: string
  groupId: number
  groupName: string
  durationMinutes: number | null
  listPrice: number
  tourFee: number
  active: boolean
  createdAt: string
  updatedAt: string
}

/** Thân request tạo và sửa (POST, PUT dùng chung; PUT là thay thế toàn bộ). */
export interface ServiceItemRequest {
  name: string
  groupId: number
  durationMinutes: number | null
  listPrice: number
  tourFee: number | null
}

export interface ServiceSearchParams {
  q?: string
  groupId?: number
  active?: boolean
  page: number
  size: number
}

/** Thành phần trong một combo (ComboItemResponse từ backend). */
export interface ComboItem {
  serviceId: number
  serviceName: string
  serviceActive: boolean
  listPrice: number
  quantity: number
  lineTotal: number
}

/** Thông tin combo đầy đủ (ComboResponse từ backend). Tiền là số nguyên đồng. */
export interface Combo {
  id: number
  name: string
  price: number
  tourFee: number
  active: boolean
  items: ComboItem[]
  retailTotal: number
  savings: number
  createdAt: string
  updatedAt: string
}

/** Thành phần gửi lên khi tạo, sửa combo hoặc tính thử (ComboItemRequest). */
export interface ComboItemRequest {
  serviceId: number
  quantity: number
}

/** Thân request tạo và sửa combo (POST, PUT dùng chung). */
export interface ComboRequest {
  name: string
  price: number
  tourFee?: number | null
  items: ComboItemRequest[]
}

/** Thân request tính thử giá combo (POST /api/catalog/combos/quote). */
export interface ComboQuoteRequest {
  price?: number | null
  items: ComboItemRequest[]
}

/** Kết quả tính thử từ backend (ComboQuoteResponse). `savings` là null khi chưa nhập `price`. */
export interface ComboQuote {
  items: ComboItem[]
  retailTotal: number
  savings: number | null
}

/** Tham số tìm kiếm, lọc danh sách combo. */
export interface ComboSearchParams {
  q?: string
  active?: boolean
  page: number
  size: number
}
