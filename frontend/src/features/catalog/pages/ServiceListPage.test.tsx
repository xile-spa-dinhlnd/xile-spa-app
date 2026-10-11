import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { Providers } from '@/app/providers'
import { ServiceListPage } from './ServiceListPage'

const service = {
  id: 1,
  name: 'Gội 30 phút',
  groupId: 1,
  groupName: 'Gội đầu dưỡng sinh',
  durationMinutes: 30,
  listPrice: 69000,
  tourFee: 10000,
  active: true,
  createdAt: '2026-10-11T03:00:00Z',
  updatedAt: '2026-10-11T03:00:00Z',
}

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

let fetchMock: ReturnType<typeof vi.fn>

beforeEach(() => {
  fetchMock = vi.fn(async (url: string, init?: RequestInit) => {
    if (url.startsWith('/api/catalog/service-groups')) {
      return json([{ id: 1, name: 'Gội đầu dưỡng sinh', sortOrder: 10 }])
    }
    if (url.startsWith('/api/catalog/services/1') && init?.method === 'PUT') {
      return json(
        {
          code: 'SERVICE_NAME_DUPLICATE',
          message: 'Tên dịch vụ đã tồn tại.',
          fieldErrors: [],
          path: '/api/catalog/services/1',
          timestamp: '',
        },
        409,
      )
    }
    if (url.startsWith('/api/catalog/services')) {
      return json({ content: [service], page: 0, size: 20, totalElements: 1, totalPages: 1 })
    }
    return json({}, 404)
  })
  vi.stubGlobal('fetch', fetchMock)
})

afterEach(() => vi.unstubAllGlobals())

function renderPage() {
  render(
    <Providers>
      <MemoryRouter>
        <ServiceListPage />
      </MemoryRouter>
    </Providers>,
  )
}

describe('ServiceListPage', () => {
  it('hiển thị dịch vụ với tiền đã định dạng, mặc định lọc đang bán', async () => {
    renderPage()

    expect(await screen.findByText('Gội 30 phút')).toBeInTheDocument()
    expect(screen.getByText('69.000 đ')).toBeInTheDocument()
    expect(screen.getByText(/10\.000 đ/)).toBeInTheDocument()
    const listCall = fetchMock.mock.calls.find(([url]) => String(url).includes('/services?'))
    expect(String(listCall?.[0])).toContain('active=true')
  })

  it('lỗi trùng tên từ backend hiển thị ngay dưới ô tên', async () => {
    renderPage()
    fireEvent.click(await screen.findByRole('button', { name: /Sửa/ }))

    const nameInput = await screen.findByPlaceholderText('Ví dụ: Gội 30 phút')
    await waitFor(() => expect(nameInput).toHaveValue('Gội 30 phút'))
    fireEvent.change(nameInput, { target: { value: 'Gội 60 phút' } })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu' }))

    const dialog = await screen.findByRole('dialog')
    expect(await within(dialog).findByText('Tên dịch vụ đã tồn tại.')).toBeInTheDocument()
    const put = fetchMock.mock.calls.find(([, init]) => init?.method === 'PUT')
    expect(JSON.parse(String(put?.[1]?.body))).toMatchObject({
      name: 'Gội 60 phút',
      listPrice: 69000,
    })
  })
})
