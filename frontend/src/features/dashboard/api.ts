import { useQuery } from '@tanstack/react-query'
import { api } from '@/shared/api/client'
import type { HealthResponse } from './types'

export const dashboardKeys = {
  health: ['dashboard', 'health'] as const,
}

/** Trạng thái máy chủ (health endpoint, FR-SYS-03). Dùng để kiểm tra frontend nối được backend. */
export function useServerHealth() {
  return useQuery({
    queryKey: dashboardKeys.health,
    queryFn: ({ signal }) => api.get<HealthResponse>('/actuator/health', { signal }),
  })
}
