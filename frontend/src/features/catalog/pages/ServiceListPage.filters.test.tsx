import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { Providers } from '@/app/providers'
import { ServiceListPage } from './ServiceListPage'

/**
 * Hồi quy cho báo lỗi "lọc xong thì danh sách trống, bỏ lọc vẫn trống": backend giả lập lọc thật
 * theo đúng tham số giao diện gửi, rồi kiểm tra tổ hợp tìm + nhóm + trạng thái + xóa lọc.
 */
const SERVICES = [
  { id: 1, name: 'Gội thử', groupId: 3, groupName: 'Massage trị liệu', active: true },
  { id: 2, name: 'Gội 60 phút', groupId: 1, groupName: 'Gội đầu dưỡng sinh', active: false },
  { id: 3, name: 'Massage cổ vai', groupId: 1, groupName: 'Gội đầu dưỡng sinh', active: true },
  { id: 4, name: 'Tẩy da chết', groupId: 2, groupName: 'Chăm sóc da', active: false },
].map((s) => ({
  ...s,
  durationMinutes: 30,
  listPrice: 100000,
  tourFee: 0,
  createdAt: '',
  updatedAt: '',
}))

const GROUPS = [
  { id: 1, name: 'Gội đầu dưỡng sinh', sortOrder: 10, active: true },
  { id: 2, name: 'Chăm sóc da', sortOrder: 20, active: false },
  { id: 3, name: 'Massage trị liệu', sortOrder: 30, active: true },
]

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

let failList = false
let listUrls: URL[] = []
let groupUrls: URL[] = []

beforeEach(() => {
  failList = false
  listUrls = []
  groupUrls = []
  vi.stubGlobal(
    'fetch',
    vi.fn(async (input: string) => {
      const url = new URL(input, 'http://localhost')
      if (url.pathname === '/api/catalog/service-groups') {
        groupUrls.push(url)
        return json(GROUPS)
      }
      if (url.pathname === '/api/catalog/services') {
        listUrls.push(url)
        if (failList) {
          return json(
            {
              code: 'INTERNAL_ERROR',
              message: 'Hệ thống gặp lỗi, vui lòng thử lại sau.',
              fieldErrors: [],
              path: '',
              timestamp: '',
            },
            500,
          )
        }
        const q = url.searchParams.get('q')?.toLowerCase()
        const groupId = url.searchParams.get('groupId')
        const active = url.searchParams.get('active')
        const content = SERVICES.filter(
          (s) =>
            (!q || s.name.toLowerCase().includes(q)) &&
            (!groupId || String(s.groupId) === groupId) &&
            (active === null || String(s.active) === active),
        )
        return json({ content, page: 0, size: 20, totalElements: content.length, totalPages: 1 })
      }
      return json({}, 404)
    }),
  )
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

const lastParams = () => Object.fromEntries(listUrls[listUrls.length - 1].searchParams)

async function choose(selectLabel: string, optionText: string) {
  fireEvent.mouseDown(screen.getByLabelText(selectLabel))
  const option = await waitFor(() => {
    const found = document.querySelector(`.ant-select-item-option[title="${optionText}"]`)
    if (!found) throw new Error('chưa thấy lựa chọn ' + optionText)
    return found as HTMLElement
  })
  fireEvent.click(option)
}

const names = () =>
  screen
    .queryAllByText(/Gội thử|Gội 60 phút|Massage cổ vai|Tẩy da chết/)
    .map((el) => el.textContent)

describe('ServiceListPage: tìm và lọc', () => {
  it('mặc định chỉ hiện dịch vụ đang bán, không gửi q và groupId', async () => {
    renderPage()
    expect(await screen.findByText('Gội thử')).toBeInTheDocument()
    expect(screen.getByText('Massage cổ vai')).toBeInTheDocument()
    expect(screen.queryByText('Gội 60 phút')).not.toBeInTheDocument()
    expect(lastParams()).toEqual({ active: 'true', page: '0', size: '20' })
  })

  it('tổ hợp tìm + nhóm + trạng thái, rồi xóa lọc thì hiện lại đầy đủ', async () => {
    renderPage()
    await screen.findByText('Gội thử')

    // Tìm "gội": chỉ còn "Gội thử" (đang bán)
    fireEvent.change(screen.getByLabelText('Tìm theo tên'), { target: { value: 'gội' } })
    await waitFor(() => expect(lastParams().q).toBe('gội'))
    await waitFor(() => expect(names()).toEqual(['Gội thử']))

    // Thêm lọc nhóm "Gội đầu dưỡng sinh": không còn dịch vụ đang bán nào khớp
    await choose('Lọc theo nhóm', 'Gội đầu dưỡng sinh')
    await waitFor(() => expect(lastParams()).toMatchObject({ q: 'gội', groupId: '1', page: '0' }))
    expect(await screen.findByText('Không có dịch vụ khớp bộ lọc hiện tại')).toBeInTheDocument()

    // Đổi trạng thái sang "Tất cả": thấy "Gội 60 phút" (đã ngừng bán) trong nhóm đó
    await choose('Lọc theo trạng thái', 'Tất cả')
    await waitFor(() => expect(lastParams()).not.toHaveProperty('active'))
    await waitFor(() => expect(names()).toEqual(['Gội 60 phút']))

    // Xóa lọc: quay về mặc định, thấy lại cả hai dịch vụ đang bán
    // (khóa truy vấn trở về mặc định nên dữ liệu có thể lấy từ bộ nhớ đệm, không nhất thiết gọi lại API)
    fireEvent.click(screen.getAllByRole('button', { name: /Xóa bộ lọc/ })[0])
    await waitFor(() => expect(names().sort()).toEqual(['Gội thử', 'Massage cổ vai']))
    expect(screen.getByLabelText('Tìm theo tên')).toHaveValue('')
    expect(screen.queryByRole('button', { name: /Xóa bộ lọc/ })).not.toBeInTheDocument()
  })

  it('xóa chữ trong ô tìm thì bỏ hẳn tham số q', async () => {
    renderPage()
    await screen.findByText('Gội thử')
    const box = screen.getByLabelText('Tìm theo tên')

    fireEvent.change(box, { target: { value: 'massage' } })
    await waitFor(() => expect(names()).toEqual(['Massage cổ vai']))
    fireEvent.change(box, { target: { value: '   ' } })
    // Khóa truy vấn quay về mặc định (không q): lấy lại từ bộ nhớ đệm hoặc gọi lại, đều phải đủ 2 dịch vụ.
    await waitFor(() => expect(names().sort()).toEqual(['Gội thử', 'Massage cổ vai']))
    expect(listUrls.every((u) => u.searchParams.get('q') !== '')).toBe(true)
  })

  it('lỗi tải danh sách hiện thông báo rõ ràng và nút thử lại', { timeout: 15000 }, async () => {
    failList = true
    renderPage()

    // Lỗi 5xx được tự thử lại 2 lần trước khi báo, nên chờ lâu hơn mặc định.
    const alert = await screen.findByRole('alert', {}, { timeout: 8000 })
    expect(within(alert).getByText('Không tải được danh sách dịch vụ')).toBeInTheDocument()

    failList = false
    fireEvent.click(within(alert).getByRole('button', { name: 'Thử lại' }))
    expect(await screen.findByText('Gội thử')).toBeInTheDocument()
  })

  it('ô lọc nhóm lấy tất cả nhóm, nhóm ngừng dùng có nhãn (ngừng dùng)', async () => {
    renderPage()

    await screen.findByText('Gội thử')

    // Ô lọc dùng useAllServiceGroups (không gửi param active)
    expect(groupUrls.some((u) => u.searchParams.get('active') === null)).toBe(true)

    // Lọc theo nhóm ngừng dùng và trạng thái "Tất cả": ra đúng dịch vụ của nhóm đó
    await choose('Lọc theo nhóm', 'Chăm sóc da (ngừng dùng)')
    await choose('Lọc theo trạng thái', 'Tất cả')
    await waitFor(() => expect(lastParams()).toMatchObject({ groupId: '2' }))
    await waitFor(() => expect(names()).toEqual(['Tẩy da chết']))
  })
})
