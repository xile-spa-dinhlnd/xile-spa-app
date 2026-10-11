/**
 * Kiểu dữ liệu mô-đun xác thực, khớp DTO backend (S1-01, FR-AUTH-01, 02).
 */

export type UserRole = 'OWNER' | 'MANAGER' | 'STAFF'

export interface UserResponse {
  id: number
  email: string
  displayName: string
  role: UserRole
}

export interface LoginRequest {
  email: string
  password: string
}
