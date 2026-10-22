/** Kiểu dữ liệu khớp DTO backend module staff (S2-05). */

export type StaffRole = 'OWNER' | 'THERAPIST'

export const STAFF_ROLE_LABELS: Record<StaffRole, string> = {
  OWNER: 'Chủ tiệm',
  THERAPIST: 'KTV',
}

export interface Staff {
  id: number
  fullName: string
  role: StaffRole
  /** false nghĩa là đã nghỉ. */
  active: boolean
  createdAt: string
  updatedAt: string
}

/** Thân request tạo và sửa (POST, PUT dùng chung). */
export interface StaffRequest {
  fullName: string
  role: StaffRole
}

export interface StaffSearchParams {
  q?: string
  role?: StaffRole
  active?: boolean
  page: number
  size: number
}
