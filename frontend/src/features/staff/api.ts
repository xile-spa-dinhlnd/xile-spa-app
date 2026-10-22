import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '@/shared/api/client'
import type { PageResponse } from '@/shared/api/page'
import type { Staff, StaffRequest, StaffSearchParams } from './types'

export const staffKeys = {
  all: ['staff'] as const,
  list: (params: StaffSearchParams) => ['staff', 'list', params] as const,
}

/** Danh sách nhân viên: tìm, lọc, phân trang phía server (FR-STF-01). */
export function useStaffList(params: StaffSearchParams) {
  return useQuery({
    queryKey: staffKeys.list(params),
    queryFn: ({ signal }) =>
      api.get<PageResponse<Staff>>('/api/staff', { signal, query: { ...params } }),
    placeholderData: keepPreviousData,
  })
}

function useInvalidateStaff() {
  const queryClient = useQueryClient()
  return () => queryClient.invalidateQueries({ queryKey: staffKeys.all })
}

export function useCreateStaff() {
  const invalidate = useInvalidateStaff()
  return useMutation({
    mutationFn: (body: StaffRequest) => api.post<Staff>('/api/staff', body),
    onSuccess: invalidate,
  })
}

export function useUpdateStaff() {
  const invalidate = useInvalidateStaff()
  return useMutation({
    mutationFn: ({ id, body }: { id: number; body: StaffRequest }) =>
      api.put<Staff>(`/api/staff/${id}`, body),
    onSuccess: invalidate,
  })
}

/** Cho nghỉ hoặc cho làm lại. */
export function useSetStaffActive() {
  const invalidate = useInvalidateStaff()
  return useMutation({
    mutationFn: ({ id, active }: { id: number; active: boolean }) =>
      api.post<Staff>(`/api/staff/${id}/${active ? 'activate' : 'deactivate'}`),
    onSuccess: invalidate,
  })
}
