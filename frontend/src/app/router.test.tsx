import { render, screen } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { _resetRefreshStateForTests } from '@/shared/api/client'
import { Providers } from './providers'
import { routes } from './router'

const mockOwnerUser = {
  id: 1,
  email: 'owner@xilespa.vn',
  displayName: 'Chủ tiệm Xile',
  role: 'OWNER',
}

function mockResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

function renderAt(path: string) {
  const router = createMemoryRouter(routes, { initialEntries: [path] })
  render(
    <Providers>
      <RouterProvider router={router} />
    </Providers>,
  )
}

afterEach(() => {
  vi.unstubAllGlobals()
  _resetRefreshStateForTests()
})

describe('router', () => {
  it('chưa đăng nhập thì tự động chuyển hướng về /login khi vào trang tổng quan', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockImplementation((url: string) => {
        if (url.includes('/api/auth/me') || url.includes('/api/auth/refresh')) {
          return Promise.resolve(mockResponse({ code: 'UNAUTHORIZED' }, 401))
        }
        return Promise.resolve(mockResponse({}))
      }),
    )

    renderAt('/')

    expect(await screen.findByRole('button', { name: 'Đăng nhập' })).toBeInTheDocument()
    expect(screen.getByText('Xile Spa & Giãn cơ')).toBeInTheDocument()
  })

  it('đã đăng nhập thì hiển thị trang tổng quan và thông tin người dùng', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockImplementation((url: string) => {
        if (url.includes('/api/auth/me')) {
          return Promise.resolve(mockResponse(mockOwnerUser, 200))
        }
        if (url.includes('/actuator/health')) {
          return Promise.resolve(mockResponse({ status: 'UP' }, 200))
        }
        return Promise.resolve(mockResponse({}))
      }),
    )

    renderAt('/')

    expect(await screen.findByRole('heading', { name: 'Tổng quan' })).toBeInTheDocument()
    expect(screen.getByText('Chủ tiệm Xile')).toBeInTheDocument()
    expect(await screen.findByText('Máy chủ đang hoạt động bình thường.')).toBeInTheDocument()
  })

  it('đã đăng nhập mà vào /login thì tự động chuyển hướng về trang chủ /', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockImplementation((url: string) => {
        if (url.includes('/api/auth/me')) {
          return Promise.resolve(mockResponse(mockOwnerUser, 200))
        }
        if (url.includes('/actuator/health')) {
          return Promise.resolve(mockResponse({ status: 'UP' }, 200))
        }
        return Promise.resolve(mockResponse({}))
      }),
    )

    renderAt('/login')

    expect(await screen.findByRole('heading', { name: 'Tổng quan' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Đăng nhập' })).not.toBeInTheDocument()
  })

  it('đường dẫn lạ hiển thị trang 404 khi đã đăng nhập', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockImplementation((url: string) => {
        if (url.includes('/api/auth/me')) {
          return Promise.resolve(mockResponse(mockOwnerUser, 200))
        }
        return Promise.resolve(mockResponse({}))
      }),
    )

    renderAt('/khong-ton-tai')

    expect(await screen.findByText('Không tìm thấy trang')).toBeInTheDocument()
  })
})
