import { createElement } from 'react'
import { QueryClientProvider } from '@tanstack/react-query'
import { renderHook, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createQueryClient } from '@/app/queryClient'
import { catalogKeys, useCreateService, useSetServiceActive, useUpdateService } from './api'
import type { ServiceItemRequest } from './types'

function mockFetchSuccess(body: unknown = {}) {
  return vi.fn().mockResolvedValue({
    ok: true,
    status: 200,
    headers: new Headers({ 'content-type': 'application/json' }),
    json: () => Promise.resolve(body),
    text: () => Promise.resolve(JSON.stringify(body)),
  })
}

describe('Làm mới combo khi đổi dịch vụ (api.ts)', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', mockFetchSuccess({ id: 1 }))
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
  })

  function createWrapper() {
    const queryClient = createQueryClient()
    const spy = vi.spyOn(queryClient, 'invalidateQueries')
    const wrapper = ({ children }: { children: React.ReactNode }) =>
      createElement(QueryClientProvider, { client: queryClient }, children)
    return { queryClient, spy, wrapper }
  }

  it('sửa dịch vụ làm mới cả dịch vụ và combo', async () => {
    const { spy, wrapper } = createWrapper()
    const { result } = renderHook(() => useUpdateService(), { wrapper })

    const body: ServiceItemRequest = {
      name: 'Gội đầu thảo dược',
      groupId: 1,
      durationMinutes: 60,
      listPrice: 200_000,
      tourFee: 30_000,
    }

    await result.current.mutateAsync({ id: 1, body })

    await waitFor(() => {
      expect(spy).toHaveBeenCalledWith({ queryKey: catalogKeys.services })
      expect(spy).toHaveBeenCalledWith({ queryKey: catalogKeys.combos })
    })
  })

  it('ngừng bán, bán lại dịch vụ làm mới cả dịch vụ và combo', async () => {
    const { spy, wrapper } = createWrapper()
    const { result } = renderHook(() => useSetServiceActive(), { wrapper })

    await result.current.mutateAsync({ id: 1, active: false })

    await waitFor(() => {
      expect(spy).toHaveBeenCalledWith({ queryKey: catalogKeys.services })
      expect(spy).toHaveBeenCalledWith({ queryKey: catalogKeys.combos })
    })
  })

  it('tạo dịch vụ không làm mới combo', async () => {
    const { spy, wrapper } = createWrapper()
    const { result } = renderHook(() => useCreateService(), { wrapper })

    const body: ServiceItemRequest = {
      name: 'Dịch vụ mới',
      groupId: 1,
      durationMinutes: 30,
      listPrice: 100_000,
      tourFee: 10_000,
    }

    await result.current.mutateAsync(body)

    await waitFor(() => {
      expect(spy).toHaveBeenCalledWith({ queryKey: catalogKeys.services })
      expect(spy).not.toHaveBeenCalledWith({ queryKey: catalogKeys.combos })
    })
  })
})
