import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { Providers } from '@/app/providers'
import { ComboListPage } from './ComboListPage'
import type { Combo, ComboItem, ComboQuote, ComboQuoteRequest, ComboRequest } from '../types'

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

const mockCombos: Combo[] = [
  {
    id: 1,
    name: 'Combo Thư Giãn',
    price: 400000,
    tourFee: 60000,
    active: true,
    items: [
      {
        serviceId: 1,
        serviceName: 'Gội đầu thảo dược',
        serviceActive: true,
        listPrice: 180000,
        quantity: 1,
        lineTotal: 180000,
      },
      {
        serviceId: 2,
        serviceName: 'Giãn cơ chân',
        serviceActive: true,
        listPrice: 200000,
        quantity: 2,
        lineTotal: 400000,
      },
    ],
    retailTotal: 480000,
    savings: 80000,
    createdAt: '2026-10-11T03:00:00Z',
    updatedAt: '2026-10-11T03:00:00Z',
  },
  {
    id: 2,
    name: 'Combo Da Sáng',
    price: 300000,
    tourFee: 30000,
    active: true,
    items: [
      {
        serviceId: 3,
        serviceName: 'Xông hơi thảo dược',
        serviceActive: true,
        listPrice: 100000,
        quantity: 1,
        lineTotal: 100000,
      },
      {
        serviceId: 4,
        serviceName: 'Ngâm chân muối khoáng',
        serviceActive: false,
        listPrice: 80000,
        quantity: 1,
        lineTotal: 80000,
      },
    ],
    retailTotal: 250000,
    savings: -50000,
    createdAt: '2026-10-11T03:00:00Z',
    updatedAt: '2026-10-11T03:00:00Z',
  },
  {
    id: 3,
    name: 'Combo Cũ Đã Ngừng',
    price: 200000,
    tourFee: 20000,
    active: false,
    items: [
      {
        serviceId: 1,
        serviceName: 'Gội đầu thảo dược',
        serviceActive: true,
        listPrice: 180000,
        quantity: 1,
        lineTotal: 180000,
      },
    ],
    retailTotal: 180000,
    savings: -20000,
    createdAt: '2026-10-11T03:00:00Z',
    updatedAt: '2026-10-11T03:00:00Z',
  },
  {
    id: 4,
    name: "<script>alert('xss')</script>",
    price: 150000,
    tourFee: 15000,
    active: true,
    items: [
      {
        serviceId: 1,
        serviceName: 'Gội đầu thảo dược',
        serviceActive: true,
        listPrice: 180000,
        quantity: 1,
        lineTotal: 180000,
      },
    ],
    retailTotal: 180000,
    savings: 30000,
    createdAt: '2026-10-11T03:00:00Z',
    updatedAt: '2026-10-11T03:00:00Z',
  },
]

let combosList: Combo[] = []
let failList = false
let listUrls: URL[] = []

beforeEach(() => {
  combosList = [...mockCombos]
  failList = false
  listUrls = []

  vi.stubGlobal(
    'fetch',
    vi.fn(async (input: string, init?: RequestInit) => {
      const url = new URL(input, 'http://localhost')
      const method = init?.method ?? 'GET'

      if (url.pathname === '/api/catalog/combos' && method === 'GET') {
        listUrls.push(url)
        if (failList) {
          return json(
            {
              code: 'INTERNAL_ERROR',
              message: 'Hệ thống gặp lỗi, vui lòng thử lại sau.',
              fieldErrors: [],
              path: '/api/catalog/combos',
              timestamp: '',
            },
            500,
          )
        }

        const q = url.searchParams.get('q')?.toLowerCase()
        const active = url.searchParams.get('active')
        const filtered = combosList.filter((c) => {
          if (q && !c.name.toLowerCase().includes(q)) return false
          if (active !== null && String(c.active) !== active) return false
          return true
        })

        const page = Number(url.searchParams.get('page') ?? '0')
        const size = Number(url.searchParams.get('size') ?? '20')
        const start = page * size
        const paged = filtered.slice(start, start + size)

        return json({
          content: paged,
          page,
          size,
          totalElements: filtered.length,
          totalPages: Math.max(1, Math.ceil(filtered.length / size)),
        })
      }

      if (url.pathname === '/api/catalog/combos' && method === 'POST') {
        const body = JSON.parse(String(init?.body)) as ComboRequest
        const newCombo: Combo = {
          id: 99,
          name: body.name,
          price: body.price,
          tourFee: body.tourFee ?? 0,
          active: true,
          items: body.items.map((it) => ({
            serviceId: it.serviceId,
            serviceName: `Dịch vụ #${it.serviceId}`,
            serviceActive: true,
            listPrice: 180000,
            quantity: it.quantity,
            lineTotal: 180000 * it.quantity,
          })),
          retailTotal: 180000,
          savings: 180000 - body.price,
          createdAt: '2026-10-26T12:00:00Z',
          updatedAt: '2026-10-26T12:00:00Z',
        }
        combosList.push(newCombo)
        return json(newCombo, 201)
      }

      if (url.pathname === '/api/catalog/services') {
        return json({
          content: [
            {
              id: 1,
              name: 'Gội đầu thảo dược',
              groupId: 1,
              groupName: 'Gội đầu',
              durationMinutes: 45,
              listPrice: 180000,
              tourFee: 20000,
              active: true,
              createdAt: '2026-10-11T03:00:00Z',
              updatedAt: '2026-10-11T03:00:00Z',
            },
          ],
          page: 0,
          size: 20,
          totalElements: 1,
          totalPages: 1,
        })
      }

      if (url.pathname === '/api/catalog/combos/quote') {
        const body = JSON.parse(String(init?.body)) as ComboQuoteRequest
        const quoteItems: ComboItem[] = body.items.map((it) => ({
          serviceId: it.serviceId,
          serviceName: 'Gội đầu thảo dược',
          serviceActive: true,
          listPrice: 180000,
          quantity: it.quantity,
          lineTotal: 180000 * it.quantity,
        }))
        const retailTotal = quoteItems.reduce((acc, curr) => acc + curr.lineTotal, 0)
        const quote: ComboQuote = {
          items: quoteItems,
          retailTotal,
          savings:
            body.price !== null && body.price !== undefined ? retailTotal - body.price : null,
        }
        return json(quote, 200)
      }

      return json({}, 404)
    }),
  )
})

afterEach(() => {
  vi.unstubAllGlobals()
})

function renderPage() {
  return render(
    <Providers>
      <MemoryRouter>
        <ComboListPage />
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

describe('ComboListPage - Danh sách, tìm, lọc', () => {
  it('mặc định chỉ hiện combo đang bán, tiền đã định dạng, thành phần có × số lượng', async () => {
    renderPage()

    // Kiểm tra tên combo đang bán hiện
    expect(await screen.findByText('Combo Thư Giãn')).toBeInTheDocument()
    expect(screen.getByText('Combo Da Sáng')).toBeInTheDocument()

    // Combo đã ngừng bán không hiện ở bộ lọc mặc định
    expect(screen.queryByText('Combo Cũ Đã Ngừng')).not.toBeInTheDocument()

    // Tiền đã định dạng
    expect(screen.getByText('400.000 đ')).toBeInTheDocument()
    expect(screen.getByText(/Giá lẻ: 480\.000 đ/)).toBeInTheDocument()
    expect(screen.getByText(/Tiết kiệm: 80\.000 đ/)).toBeInTheDocument()
    expect(screen.getByText(/Tour: 60\.000 đ/)).toBeInTheDocument()

    // Thành phần có × số lượng
    expect(screen.getByText('Gội đầu thảo dược, Giãn cơ chân × 2')).toBeInTheDocument()

    // Kiểm tra URL gửi lên có active=true và không có q
    expect(lastParams().active).toBe('true')
    expect(lastParams()).not.toHaveProperty('q')
  })

  it('thêm combo xong thì danh sách tải lại', async () => {
    renderPage()
    expect(await screen.findByText('Combo Thư Giãn')).toBeInTheDocument()

    const listCallsBefore = listUrls.length

    // Mở drawer Thêm combo
    fireEvent.click(screen.getByRole('button', { name: /Thêm combo/ }))
    const dialog = await screen.findByRole('dialog')
    expect(within(dialog).getByText('Thêm combo')).toBeInTheDocument()

    // Điền form
    fireEvent.change(within(dialog).getByPlaceholderText('Ví dụ: Combo Thư Giãn'), {
      target: { value: 'Combo Mới Cứng' },
    })

    const select = within(dialog).getByRole('combobox', { name: 'Thêm dịch vụ vào combo' })
    fireEvent.mouseDown(select)
    const opt = await waitFor(() => {
      const found = document.querySelector(
        '.ant-select-dropdown:not(.ant-select-dropdown-hidden) .ant-select-item-option',
      )
      if (!found) throw new Error('chưa thấy lựa chọn dịch vụ')
      return found as HTMLElement
    })
    fireEvent.click(opt)

    fireEvent.change(within(dialog).getByLabelText('Giá combo'), { target: { value: '150000' } })

    // Bấm Tạo combo
    fireEvent.click(within(dialog).getByRole('button', { name: 'Tạo combo' }))

    // Danh sách được tải lại (có GET /combos sau POST /combos)
    await waitFor(() => {
      expect(listUrls.length).toBeGreaterThan(listCallsBefore)
    })
    expect(await screen.findByText('Combo Mới Cứng')).toBeInTheDocument()
  })

  it('tìm theo tên rồi xóa chữ thì bỏ hẳn tham số q', async () => {
    renderPage()
    expect(await screen.findByText('Combo Thư Giãn')).toBeInTheDocument()

    const searchInput = screen.getByLabelText('Tìm theo tên')

    // Tìm "thư giãn"
    fireEvent.change(searchInput, { target: { value: 'thư giãn' } })
    await waitFor(() => {
      expect(lastParams().q).toBe('thư giãn')
    })
    expect(screen.getByText('Combo Thư Giãn')).toBeInTheDocument()
    expect(screen.queryByText('Combo Da Sáng')).not.toBeInTheDocument()

    // Xóa chữ
    fireEvent.change(searchInput, { target: { value: '   ' } })
    await waitFor(() => {
      expect(screen.getByText('Combo Da Sáng')).toBeInTheDocument()
    })
    expect(listUrls.every((u) => u.searchParams.get('q') !== '')).toBe(true)
  })

  it('lọc theo trạng thái Ngừng bán và Tất cả', async () => {
    renderPage()
    expect(await screen.findByText('Combo Thư Giãn')).toBeInTheDocument()

    // Chọn "Ngừng bán"
    await choose('Lọc theo trạng thái', 'Ngừng bán')
    await waitFor(() => {
      expect(lastParams().active).toBe('false')
    })
    expect(await screen.findByText('Combo Cũ Đã Ngừng')).toBeInTheDocument()
    expect(screen.queryByText('Combo Thư Giãn')).not.toBeInTheDocument()

    // Chọn "Tất cả"
    await choose('Lọc theo trạng thái', 'Tất cả')
    await waitFor(() => {
      expect(lastParams()).not.toHaveProperty('active')
    })
    expect(await screen.findByText('Combo Thư Giãn')).toBeInTheDocument()
    expect(screen.getByText('Combo Cũ Đã Ngừng')).toBeInTheDocument()
  })

  it('đổi bộ lọc thì về trang đầu', async () => {
    // Tạo danh sách nhiều hơn 20 phần tử để có phân trang
    combosList = Array.from({ length: 25 }, (_, i) => ({
      id: 100 + i,
      name: `Combo số ${i + 1}`,
      price: 100000,
      tourFee: 10000,
      active: true,
      items: [],
      retailTotal: 100000,
      savings: 0,
      createdAt: '2026-10-11T03:00:00Z',
      updatedAt: '2026-10-11T03:00:00Z',
    }))

    renderPage()
    expect(await screen.findByText('Combo số 1')).toBeInTheDocument()

    // Chuyển sang trang 2 (page 1)
    const page2Btn = screen.getByRole('listitem', { name: '2' })
    fireEvent.click(page2Btn)
    await waitFor(() => {
      expect(lastParams().page).toBe('1')
    })

    // Đổi bộ lọc tìm kiếm -> trang về 0
    const searchInput = screen.getByLabelText('Tìm theo tên')
    fireEvent.change(searchInput, { target: { value: 'Combo số 2' } })
    await waitFor(() => {
      expect(lastParams().page).toBe('0')
    })
  })

  it('tiết kiệm âm hiện -50.000 đ màu cảnh báo', async () => {
    renderPage()

    // Combo Da Sáng có savings = -50.000 đ
    const savingsEl = await screen.findByText('-50.000 đ')
    expect(savingsEl).toBeInTheDocument()
    expect(savingsEl).toHaveClass('ant-typography-warning')
  })

  it('dịch vụ đã ngừng bán trong thành phần có chữ (ngừng bán)', async () => {
    renderPage()

    // Combo Da Sáng có dịch vụ "Ngâm chân muối khoáng" đã ngừng bán
    expect(
      await screen.findByText(/Xông hơi thảo dược, Ngâm chân muối khoáng \(ngừng bán\)/),
    ).toBeInTheDocument()
  })

  it('tên combo chứa <script> hiện như chữ', async () => {
    renderPage()

    // Tên combo chứa <script> không thực thi và hiển thị nguyên vẹn như text
    expect(await screen.findByText("<script>alert('xss')</script>")).toBeInTheDocument()
  })

  it('chưa có combo thì hiện Chưa có combo nào', async () => {
    combosList = []
    renderPage()

    expect(await screen.findByText('Chưa có combo nào')).toBeInTheDocument()
  })

  it('đang lọc mà trống thì hiện Không có combo khớp bộ lọc hiện tại và Xóa bộ lọc', async () => {
    renderPage()
    expect(await screen.findByText('Combo Thư Giãn')).toBeInTheDocument()

    // Lọc theo từ khóa không tồn tại
    fireEvent.change(screen.getByLabelText('Tìm theo tên'), { target: { value: 'không tồn tại' } })

    expect(await screen.findByText('Không có combo khớp bộ lọc hiện tại')).toBeInTheDocument()

    const clearBtns = screen.getAllByRole('button', { name: /Xóa bộ lọc/ })
    expect(clearBtns.length).toBeGreaterThan(0)

    // Bấm Xóa bộ lọc
    fireEvent.click(clearBtns[0])
    expect(await screen.findByText('Combo Thư Giãn')).toBeInTheDocument()
    expect(screen.getByLabelText('Tìm theo tên')).toHaveValue('')
    expect(screen.queryByRole('button', { name: /Xóa bộ lọc/ })).not.toBeInTheDocument()
  })

  it(
    'lỗi tải hiện Không tải được danh sách combo và Thử lại gọi lại API',
    { timeout: 15000 },
    async () => {
      failList = true
      renderPage()

      const alert = await screen.findByRole('alert', {}, { timeout: 8000 })
      expect(within(alert).getByText('Không tải được danh sách combo')).toBeInTheDocument()

      failList = false
      fireEvent.click(within(alert).getByRole('button', { name: 'Thử lại' }))

      expect(await screen.findByText('Combo Thư Giãn')).toBeInTheDocument()
    },
  )

  it('bấm Sửa mở drawer với dữ liệu điền sẵn', async () => {
    renderPage()
    expect(await screen.findByText('Combo Thư Giãn')).toBeInTheDocument()

    const editBtns = screen.getAllByRole('button', { name: /Sửa/ })
    fireEvent.click(editBtns[0])

    const drawer = await screen.findByRole('dialog')
    expect(within(drawer).getByText('Sửa combo')).toBeInTheDocument()
    expect(within(drawer).getByPlaceholderText('Ví dụ: Combo Thư Giãn')).toHaveValue(
      'Combo Thư Giãn',
    )
  })
})
