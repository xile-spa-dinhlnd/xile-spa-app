import { render, screen } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { Providers } from './providers'
import { routes } from './router'

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
})

describe('router', () => {
  it('trang tổng quan hiển thị trạng thái máy chủ từ backend', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify({ status: 'UP' }), {
          headers: { 'Content-Type': 'application/json' },
        }),
      ),
    )

    renderAt('/')

    expect(screen.getByRole('heading', { name: 'Tổng quan' })).toBeInTheDocument()
    expect(await screen.findByText('Máy chủ đang hoạt động bình thường.')).toBeInTheDocument()
  })

  it('đường dẫn lạ hiển thị trang 404', () => {
    vi.stubGlobal('fetch', vi.fn())

    renderAt('/khong-ton-tai')

    expect(screen.getByText('Không tìm thấy trang')).toBeInTheDocument()
  })
})
