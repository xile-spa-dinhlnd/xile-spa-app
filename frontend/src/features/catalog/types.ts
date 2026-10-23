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
