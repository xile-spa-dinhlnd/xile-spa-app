import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { Providers } from '@/app/providers'
import { StaffListPage } from './StaffListPage'

const lan = {
  id: 1,
  fullName: 'Lan',
  role: 'THERAPIST',
  active: true,
  createdAt: '2026-10-15T03:00:00Z',
  updatedAt: '2026-10-15T03:00:00Z',
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
    if (url.startsWith('/api/staff') && init?.method === 'POST') {
      return json(
        {
          code: 'STAFF_NAME_DUPLICATE',
          message: 'Đã có nhân viên đang làm trùng tên.',
          fieldErrors: [],
          path: '/api/staff',
          timestamp: '',
        },
        409,
      )
    }
    if (url.startsWith('/api/staff')) {
      return json({ content: [lan], page: 0, size: 20, totalElements: 1, totalPages: 1 })
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
        <StaffListPage />
      </MemoryRouter>
    </Providers>,
  )
}

describe('StaffListPage', () => {
  it('hiển thị nhân viên với vai trò tiếng Việt, mặc định lọc đang làm', async () => {
    renderPage()

    expect(await screen.findByText('Lan')).toBeInTheDocument()
    expect(screen.getByText('KTV')).toBeInTheDocument()
    expect(screen.getAllByText('Đang làm').length).toBeGreaterThan(0)
    const listCall = fetchMock.mock.calls.find(([url]) => String(url).startsWith('/api/staff?'))
    expect(String(listCall?.[0])).toContain('active=true')
  })

  it('lỗi trùng tên từ backend hiển thị ngay dưới ô tên', async () => {
    renderPage()
    fireEvent.click(await screen.findByRole('button', { name: /Thêm nhân viên/ }))

    const nameInput = await screen.findByPlaceholderText('Ví dụ: Lan')
    fireEvent.change(nameInput, { target: { value: 'Lan' } })
    const dialog = await screen.findByRole('dialog')
    fireEvent.click(within(dialog).getByRole('button', { name: 'Thêm nhân viên' }))

    expect(
      await within(dialog).findByText('Đã có nhân viên đang làm trùng tên.'),
    ).toBeInTheDocument()
    await waitFor(() => {
      const post = fetchMock.mock.calls.find(([, init]) => init?.method === 'POST')
      expect(JSON.parse(String(post?.[1]?.body))).toEqual({ fullName: 'Lan', role: 'THERAPIST' })
    })
  })
})
