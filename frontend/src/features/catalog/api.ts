import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '@/shared/api/client'
import type { PageResponse } from '@/shared/api/page'
import type {
  Combo,
  ComboQuote,
  ComboQuoteRequest,
  ComboRequest,
  ServiceGroup,
  ServiceGroupRequest,
  ServiceItem,
  ServiceItemRequest,
  ServiceSearchParams,
} from './types'

export const catalogKeys = {
  all: ['catalog'] as const,
  services: ['catalog', 'services'] as const,
  serviceList: (params: ServiceSearchParams) => ['catalog', 'services', params] as const,
  groups: ['catalog', 'service-groups'] as const,
  combos: ['catalog', 'combos'] as const,
  comboQuote: (body: ComboQuoteRequest | null) => ['catalog', 'combos', 'quote', body] as const,
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

/** Nhóm dịch vụ đang dùng, cho ô chọn. Màn quản lý nhóm (S2-06) lấy cả nhóm ngừng dùng bằng key khác. */
export function useServiceGroups() {
  return useQuery({
    queryKey: [...catalogKeys.groups, { active: true }] as const,
    queryFn: ({ signal }) =>
      api.get<ServiceGroup[]>('/api/catalog/service-groups', { signal, query: { active: true } }),
    staleTime: 5 * 60 * 1000,
  })
}

/** Toàn bộ nhóm dịch vụ (cả đang dùng và ngừng dùng), cho ô lọc bảng giá và màn quản lý nhóm (S2-06). */
export function useAllServiceGroups() {
  return useQuery({
    queryKey: [...catalogKeys.groups, {}] as const,
    queryFn: ({ signal }) => api.get<ServiceGroup[]>('/api/catalog/service-groups', { signal }),
    staleTime: 5 * 60 * 1000,
  })
}

function useInvalidateGroups() {
  const queryClient = useQueryClient()
  return () => queryClient.invalidateQueries({ queryKey: catalogKeys.groups })
}

/** Tạo nhóm dịch vụ (FR-SVC-05). */
export function useCreateServiceGroup() {
  const invalidate = useInvalidateGroups()
  return useMutation({
    mutationFn: (body: ServiceGroupRequest) =>
      api.post<ServiceGroup>('/api/catalog/service-groups', body),
    onSuccess: invalidate,
  })
}

/** Sửa nhóm dịch vụ (FR-SVC-05). Làm mới cả danh sách dịch vụ vì hiển thị groupName. */
export function useUpdateServiceGroup() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, body }: { id: number; body: ServiceGroupRequest }) =>
      api.put<ServiceGroup>(`/api/catalog/service-groups/${id}`, body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: catalogKeys.groups })
      queryClient.invalidateQueries({ queryKey: catalogKeys.services })
    },
  })
}

/** Ngừng dùng hoặc dùng lại nhóm dịch vụ (FR-SVC-05, BR-19). */
export function useSetServiceGroupActive() {
  const invalidate = useInvalidateGroups()
  return useMutation({
    mutationFn: ({ id, active }: { id: number; active: boolean }) =>
      api.post<ServiceGroup>(
        `/api/catalog/service-groups/${id}/${active ? 'activate' : 'deactivate'}`,
      ),
    onSuccess: invalidate,
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

/** Gọi API tính thử giá combo trực tiếp (FR-CMB-03). */
export function comboQuote(body: ComboQuoteRequest) {
  return api.post<ComboQuote>('/api/catalog/combos/quote', body)
}

/** Tính thử tổng giá lẻ và mức tiết kiệm của combo (FR-CMB-03). */
export function useComboQuote(body: ComboQuoteRequest | null, options?: { enabled?: boolean }) {
  return useQuery({
    queryKey: catalogKeys.comboQuote(body),
    queryFn: ({ signal }) => api.post<ComboQuote>('/api/catalog/combos/quote', body!, { signal }),
    enabled: Boolean(body) && (options?.enabled ?? true),
    placeholderData: keepPreviousData,
  })
}

function useInvalidateCombos() {
  const queryClient = useQueryClient()
  return () => queryClient.invalidateQueries({ queryKey: catalogKeys.combos })
}

/** Tạo combo mới (FR-CMB-01). */
export function useCreateCombo() {
  const invalidate = useInvalidateCombos()
  return useMutation({
    mutationFn: (body: ComboRequest) => api.post<Combo>('/api/catalog/combos', body),
    onSuccess: invalidate,
  })
}
