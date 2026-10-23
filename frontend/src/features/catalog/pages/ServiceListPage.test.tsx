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
let servicesList = [service]
let currentGroupName = 'Gội đầu dưỡng sinh'

beforeEach(() => {
  currentGroupName = 'Gội đầu dưỡng sinh'
  servicesList = [
    {
      ...service,
      groupName: currentGroupName,
    },
    {
      id: 2,
      name: 'Đắp mặt nạ cũ',
      groupId: 99,
      groupName: 'Chăm sóc da cũ',
      durationMinutes: 30,
      listPrice: 50000,
      tourFee: 0,
      active: false,
      createdAt: '2026-10-11T03:00:00Z',
      updatedAt: '2026-10-11T03:00:00Z',
    },
  ]

  fetchMock = vi.fn(async (url: string, init?: RequestInit) => {
    if (url.startsWith('/api/catalog/service-groups/1') && init?.method === 'PUT') {
      currentGroupName = 'Gội đầu thảo dược'
      return json({ id: 1, name: 'Gội đầu thảo dược', sortOrder: 10, active: true })
    }
    if (url.startsWith('/api/catalog/service-groups')) {
      const isOnlyActive = url.includes('active=true')
      if (isOnlyActive) {
        return json([{ id: 1, name: currentGroupName, sortOrder: 10, active: true }])
      }
      return json([
        { id: 1, name: currentGroupName, sortOrder: 10, active: true },
        { id: 99, name: 'Chăm sóc da cũ', sortOrder: 99, active: false },
      ])
    }
    if (url.startsWith('/api/catalog/services/2') && init?.method === 'PUT') {
      return json(
        {
          code: 'SERVICE_GROUP_NOT_FOUND',
          message: 'Nhóm dịch vụ không tồn tại hoặc đã ngừng dùng.',
          fieldErrors: [],
          path: '/api/catalog/services/2',
          timestamp: '',
        },
        400,
      )
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
      const content = servicesList.map((s) =>
        s.id === 1 ? { ...s, groupName: currentGroupName } : s,
      )
      return json({ content, page: 0, size: 20, totalElements: content.length, totalPages: 1 })
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
    const editBtns = await screen.findAllByRole('button', { name: /Sửa/ })
    fireEvent.click(editBtns[0])

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

  it('sửa dịch vụ thuộc nhóm đã ngừng dùng: hiện lựa chọn bị khóa và báo lỗi nếu giữ nguyên', async () => {
    renderPage()
    await screen.findByText('Đắp mặt nạ cũ')

    // Bấm Sửa dịch vụ thứ hai (Đắp mặt nạ cũ)
    const editBtns = await screen.findAllByRole('button', { name: /Sửa/ })
    fireEvent.click(editBtns[1])

    const dialog = await screen.findByRole('dialog')
    // Ô chọn nhóm hiển thị label "Chăm sóc da cũ (ngừng dùng)" thay vì số id
    expect(await within(dialog).findByText('Chăm sóc da cũ (ngừng dùng)')).toBeInTheDocument()

    // Bấm Lưu giữ nguyên nhóm đã ngừng dùng -> backend báo lỗi 400 gắn vào ô nhóm
    fireEvent.click(within(dialog).getByRole('button', { name: 'Lưu' }))
    expect(
      await within(dialog).findByText('Nhóm dịch vụ không tồn tại hoặc đã ngừng dùng.'),
    ).toBeInTheDocument()
  })

  it('đổi tên nhóm thì danh sách dịch vụ hiện tên mới', async () => {
    renderPage()
    expect(await screen.findByText('Gội 30 phút')).toBeInTheDocument()

    // Mở drawer quản lý nhóm
    fireEvent.click(screen.getByRole('button', { name: 'Nhóm dịch vụ' }))
    expect(await screen.findByText('Quản lý nhóm dịch vụ')).toBeInTheDocument()

    // Sửa tên nhóm trong drawer
    const drawer = screen.getByRole('dialog')
    const editGroupBtns = within(drawer).getAllByRole('button', { name: /Sửa/ })
    fireEvent.click(editGroupBtns[0])

    const groupNameInput = within(drawer).getByPlaceholderText('Ví dụ: Gội đầu dưỡng sinh')
    fireEvent.change(groupNameInput, { target: { value: 'Gội đầu thảo dược' } })
    fireEvent.click(within(drawer).getByRole('button', { name: 'Lưu' }))

    // Chỉ đếm chữ ngoài drawer: danh sách nhóm trong drawer cũng hiện tên mới
    await waitFor(() => {
      const inServiceList = screen
        .queryAllByText(/Gội đầu thảo dược/)
        .filter((el) => !drawer.contains(el))
      expect(inServiceList.length).toBeGreaterThan(0)
    })
  })
})
