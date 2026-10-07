import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '@/shared/api/client'
import type { LoginRequest, UserResponse } from './types'

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
