import { QueryClient } from '@tanstack/react-query'
import { ApiError } from '@/shared/api/errors'

export function createQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 30_000,
        // Lỗi 4xx (sai dữ liệu, chưa đăng nhập, không có quyền) thử lại cũng vô ích.
        retry: (failureCount, error) =>
          !(error instanceof ApiError && error.isClientError) && failureCount < 2,
        refetchOnWindowFocus: false,
      },
      mutations: { retry: false },
    },
  })
}
