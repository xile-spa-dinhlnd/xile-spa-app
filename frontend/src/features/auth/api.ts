import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '@/shared/api/client'
import type {
  ChangePasswordRequest,
  ForgotPasswordRequest,
  LoginRequest,
  ResetPasswordRequest,
  UserResponse,
} from './types'

export const authKeys = {
  all: ['auth'] as const,
  me: ['auth', 'me'] as const,
}

/** Lấy thông tin tài khoản đang đăng nhập từ phiên làm việc hiện tại (FR-AUTH-01, 02). */
export function getMe(signal?: AbortSignal): Promise<UserResponse> {
  return api.get<UserResponse>('/api/auth/me', { signal })
}

/** Đăng nhập bằng email và mật khẩu (FR-AUTH-01). */
export function login(credentials: LoginRequest): Promise<UserResponse> {
  return api.post<UserResponse>('/api/auth/login', credentials)
}

/** Đăng xuất và thu hồi phiên (FR-AUTH-02). */
export function logout(): Promise<void> {
  return api.post<void>('/api/auth/logout')
}

/** Đổi mật khẩu tài khoản đang đăng nhập (FR-AUTH-04). */
export function changePassword(data: ChangePasswordRequest): Promise<void> {
  return api.post<void>('/api/auth/change-password', data)
}

/** Yêu cầu liên kết đặt lại mật khẩu qua email (FR-AUTH-03). */
export function forgotPassword(data: ForgotPasswordRequest): Promise<void> {
  return api.post<void>('/api/auth/forgot-password', data)
}

/** Đặt lại mật khẩu mới bằng mã xác thực từ email (FR-AUTH-03). */
export function resetPassword(data: ResetPasswordRequest): Promise<void> {
  return api.post<void>('/api/auth/reset-password', data)
}

/** Hook lấy thông tin người dùng đang đăng nhập. Không thử lại khi chưa có phiên (401). */
export function useCurrentUser() {
  return useQuery({
    queryKey: authKeys.me,
    queryFn: ({ signal }) => getMe(signal),
    retry: false,
    staleTime: 5 * 60 * 1000,
  })
}

/** Hook thực hiện đăng nhập. */
export function useLogin() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (credentials: LoginRequest) => login(credentials),
    onSuccess: (user) => {
      queryClient.setQueryData(authKeys.me, user)
      queryClient.invalidateQueries({ queryKey: authKeys.me })
    },
  })
}

/** Hook thực hiện đăng xuất và làm sạch bộ nhớ tạm. */
export function useLogout() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: () => logout(),
    onSuccess: () => {
      queryClient.setQueryData(authKeys.me, null)
      queryClient.clear()
    },
  })
}

/** Hook thực hiện đổi mật khẩu (FR-AUTH-04). */
export function useChangePassword() {
  return useMutation({
    mutationFn: (data: ChangePasswordRequest) => changePassword(data),
  })
}

/** Hook gửi yêu cầu đặt lại mật khẩu qua email (FR-AUTH-03). */
export function useForgotPassword() {
  return useMutation({
    mutationFn: (data: ForgotPasswordRequest) => forgotPassword(data),
  })
}

/** Hook thực hiện đặt lại mật khẩu mới từ mã xác thực (FR-AUTH-03). */
export function useResetPassword() {
  return useMutation({
    mutationFn: (data: ResetPasswordRequest) => resetPassword(data),
  })
}
