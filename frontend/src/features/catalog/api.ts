import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '@/shared/api/client'
import type { PageResponse } from '@/shared/api/page'
import type { ServiceGroup, ServiceItem, ServiceItemRequest, ServiceSearchParams } from './types'

export const catalogKeys = {
  all: ['catalog'] as const,
  services: ['catalog', 'services'] as const,
  serviceList: (params: ServiceSearchParams) => ['catalog', 'services', params] as const,
  groups: ['catalog', 'service-groups'] as const,
}

/** Danh sách dịch vụ: tìm, lọc, phân trang phía server (FR-SVC-01). */
export function useServices(params: ServiceSearchParams) {
  return useQuery({
    queryKey: catalogKeys.serviceList(params),
    queryFn: ({ signal }) =>
      api.get<PageResponse<ServiceItem>>('/api/catalog/services', {
        signal,
        query: { ...params },
      }),
    placeholderData: keepPreviousData,
  })
}

/** Nhóm dịch vụ đang dùng, cho ô chọn. */
export function useServiceGroups() {
  return useQuery({
    queryKey: catalogKeys.groups,
    queryFn: ({ signal }) => api.get<ServiceGroup[]>('/api/catalog/service-groups', { signal }),
    staleTime: 5 * 60 * 1000,
  })
}

function useInvalidateServices() {
  const queryClient = useQueryClient()
  return () => queryClient.invalidateQueries({ queryKey: catalogKeys.services })
}

/** Tạo dịch vụ (FR-SVC-02). */
export function useCreateService() {
  const invalidate = useInvalidateServices()
  return useMutation({
    mutationFn: (body: ServiceItemRequest) => api.post<ServiceItem>('/api/catalog/services', body),
    onSuccess: invalidate,
  })
}

/** Sửa dịch vụ (FR-SVC-03). */
export function useUpdateService() {
  const invalidate = useInvalidateServices()
  return useMutation({
    mutationFn: ({ id, body }: { id: number; body: ServiceItemRequest }) =>
      api.put<ServiceItem>(`/api/catalog/services/${id}`, body),
    onSuccess: invalidate,
  })
}

/** Ngừng bán hoặc bán lại (FR-SVC-04). */
export function useSetServiceActive() {
  const invalidate = useInvalidateServices()
  return useMutation({
    mutationFn: ({ id, active }: { id: number; active: boolean }) =>
      api.post<ServiceItem>(`/api/catalog/services/${id}/${active ? 'activate' : 'deactivate'}`),
    onSuccess: invalidate,
  })
}
