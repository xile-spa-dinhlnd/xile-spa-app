import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { Providers } from '@/app/providers'
import { ComboFormDrawer } from './ComboFormDrawer'
import type {
  Combo,
  ComboItem,
  ComboQuote,
  ComboQuoteRequest,
  ComboRequest,
  ServiceItem,
} from '../types'

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

const mockServices: ServiceItem[] = [
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
  {
    id: 2,
    name: 'Giãn cơ chân',
    groupId: 2,
    groupName: 'Giãn cơ',
    durationMinutes: 60,
    listPrice: 200000,
    tourFee: 30000,
    active: true,
    createdAt: '2026-10-11T03:00:00Z',
    updatedAt: '2026-10-11T03:00:00Z',
  },
  {
    id: 3,
    name: 'Xông hơi thảo dược',
    groupId: 1,
    groupName: 'Gội đầu',
    durationMinutes: 30,
    listPrice: 100000,
    tourFee: 10000,
    active: true,
    createdAt: '2026-10-11T03:00:00Z',
    updatedAt: '2026-10-11T03:00:00Z',
  },
  {
    id: 4,
    name: 'Ngâm chân muối khoáng',
    groupId: 1,
    groupName: 'Gội đầu',
    durationMinutes: 20,
    listPrice: 80000,
    tourFee: 0,
    active: false,
    createdAt: '2026-10-11T03:00:00Z',
    updatedAt: '2026-10-11T03:00:00Z',
  },
]

let fetchCalls: { url: string; method: string; body?: unknown }[] = []
let quoteHandler: ((body: ComboQuoteRequest | undefined) => Response) | null = null
let createHandler: ((body: ComboRequest | undefined) => Response) | null = null

function renderDrawer(props: { open?: boolean; combo?: Combo | null; onClose?: () => void } = {}) {
  const onClose = props.onClose ?? vi.fn()
  const result = render(
    <Providers>
      <ComboFormDrawer open={props.open ?? true} combo={props.combo} onClose={onClose} />
    </Providers>,
  )
  return { ...result, onClose }
}

beforeEach(() => {
  fetchCalls = []
  quoteHandler = null
  createHandler = null

  vi.stubGlobal(
    'fetch',
    vi.fn(async (input: string, init?: RequestInit) => {
      const url = new URL(input, 'http://localhost')
      const method = init?.method ?? 'GET'
      const body = init?.body ? JSON.parse(String(init.body)) : undefined
      fetchCalls.push({ url: url.pathname, method, body })

      if (url.pathname === '/api/catalog/services') {
        const activeOnly = url.searchParams.get('active') === 'true'
        const q = url.searchParams.get('q')?.toLowerCase()
        let items = mockServices
        if (activeOnly) items = items.filter((s) => s.active)
        if (q) items = items.filter((s) => s.name.toLowerCase().includes(q))
        return json({
          content: items,
          page: 0,
          size: 20,
          totalElements: items.length,
          totalPages: 1,
        })
      }

      if (url.pathname === '/api/catalog/combos/quote' && method === 'POST') {
        const quoteReq = body as ComboQuoteRequest | undefined
        if (quoteHandler) return quoteHandler(quoteReq)
        const quoteItems: ComboItem[] = (quoteReq?.items ?? []).map((it) => {
          const s = mockServices.find((srv) => srv.id === it.serviceId)
          const listPrice = s ? s.listPrice : 100000
          return {
            serviceId: it.serviceId,
            serviceName: s ? s.name : `Dịch vụ #${it.serviceId}`,
            serviceActive: s ? s.active : true,
            listPrice,
            quantity: it.quantity,
            lineTotal: listPrice * it.quantity,
          }
        })
        const retailTotal = quoteItems.reduce((acc, curr) => acc + curr.lineTotal, 0)
        const savings =
          quoteReq?.price !== null && quoteReq?.price !== undefined
            ? retailTotal - quoteReq.price
            : null
        const res: ComboQuote = {
          items: quoteItems,
          retailTotal,
          savings,
        }
        return json(res, 200)
      }

      if (url.pathname === '/api/catalog/combos' && method === 'POST') {
        const createReq = body as ComboRequest | undefined
        if (createHandler) return createHandler(createReq)
        const created: Combo = {
          id: 10,
          name: createReq?.name ?? '',
          price: createReq?.price ?? 0,
          tourFee: createReq?.tourFee ?? 0,
          active: true,
          items: (createReq?.items ?? []).map((it) => ({
            serviceId: it.serviceId,
            serviceName: `Dịch vụ ${it.serviceId}`,
            serviceActive: true,
            listPrice: 150000,
            quantity: it.quantity,
            lineTotal: 150000 * it.quantity,
          })),
          retailTotal: 450000,
          savings: 50000,
          createdAt: '2026-10-26T10:00:00Z',
          updatedAt: '2026-10-26T10:00:00Z',
        }
        return json(created, 201)
      }

      return json({}, 404)
    }),
  )
})

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('ComboFormDrawer - Thêm combo', () => {
  it('chọn hai dịch vụ, nhập giá rồi tạo: gửi đúng body, báo Đã thêm combo.', async () => {
    const { onClose } = renderDrawer()

    // 1. Nhập tên combo
    fireEvent.change(screen.getByPlaceholderText('Ví dụ: Combo Thư Giãn'), {
      target: { value: '  Combo Thư Giãn  ' },
    })

    // 2. Chọn dịch vụ 1
    const select = screen.getByRole('combobox', { name: 'Thêm dịch vụ vào combo' })
    fireEvent.mouseDown(select)
    const opt1 = await screen.findByText(/Gội đầu thảo dược/)
    fireEvent.click(opt1)

    // 3. Chọn dịch vụ 2
    fireEvent.mouseDown(select)
    const opt2 = await screen.findByText(/Giãn cơ chân/)
    fireEvent.click(opt2)

    // 4. Nhập số lượng cho dịch vụ 2 (dòng 2) thành 2
    const qty2Input = await screen.findByLabelText('Số lượng Giãn cơ chân')
    fireEvent.change(qty2Input, { target: { value: '2' } })

    // 5. Nhập giá combo
    const priceInput = screen.getByLabelText('Giá combo')
    fireEvent.change(priceInput, { target: { value: '450000' } })

    // 6. Bấm Tạo combo
    const submitBtn = screen.getByRole('button', { name: 'Tạo combo' })
    fireEvent.click(submitBtn)

    await waitFor(() => {
      expect(onClose).toHaveBeenCalled()
    })

    const postCall = fetchCalls.find((c) => c.url === '/api/catalog/combos' && c.method === 'POST')
    expect(postCall).toBeDefined()
    expect(postCall?.body).toEqual({
      name: 'Combo Thư Giãn',
      price: 450000,
      tourFee: null,
      items: [
        { serviceId: 1, quantity: 1 },
        { serviceId: 2, quantity: 2 },
      ],
    })
    expect(await screen.findByText('Đã thêm combo.')).toBeInTheDocument()
  })

  it('form sai thì báo lỗi dưới từng ô và không gửi request', async () => {
    const { onClose } = renderDrawer()

    const submitBtn = screen.getByRole('button', { name: 'Tạo combo' })
    fireEvent.click(submitBtn)

    expect(await screen.findByText('Tên combo không được để trống')).toBeInTheDocument()
    expect(screen.getByText('Vui lòng nhập giá combo')).toBeInTheDocument()
    expect(screen.getByText('Combo phải có ít nhất một dịch vụ')).toBeInTheDocument()

    const postCalls = fetchCalls.filter(
      (c) => c.url === '/api/catalog/combos' && c.method === 'POST',
    )
    expect(postCalls.length).toBe(0)
    expect(onClose).not.toHaveBeenCalled()
  })

  it('đủ 20 dòng thì khóa ô chọn dịch vụ', async () => {
    // Tạo sẵn combo có 20 dòng
    const items20 = Array.from({ length: 20 }, (_, idx) => ({
      serviceId: idx + 1,
      serviceName: `Dịch vụ ${idx + 1}`,
      serviceActive: true,
      listPrice: 100000,
      quantity: 1,
      lineTotal: 100000,
    }))
    const combo20: Combo = {
      id: 1,
      name: 'Combo 20 món',
      price: 1500000,
      tourFee: 0,
      active: true,
      items: items20,
      retailTotal: 2000000,
      savings: 500000,
      createdAt: '2026-10-26T10:00:00Z',
      updatedAt: '2026-10-26T10:00:00Z',
    }

    renderDrawer({ combo: combo20 })

    expect(screen.getByText('Combo tối đa 20 dịch vụ.')).toBeInTheDocument()
    const select = screen.getByRole('combobox', { name: 'Thêm dịch vụ vào combo' })
    expect(select).toBeDisabled()
  })

  it('dịch vụ đã chọn bị khóa trong ô chọn', async () => {
    renderDrawer()

    // Chọn dịch vụ 1
    const select = screen.getByRole('combobox', { name: 'Thêm dịch vụ vào combo' })
    fireEvent.mouseDown(select)
    const opt1 = await screen.findByText(/Gội đầu thảo dược/)
    fireEvent.click(opt1)

    expect(await screen.findByRole('button', { name: 'Bỏ Gội đầu thảo dược' })).toBeInTheDocument()

    // Mở lại dropdown
    fireEvent.mouseDown(select)
    await screen.findByText(/Giãn cơ chân/)

    // Tìm option Gội đầu thảo dược xem có disabled không
    await waitFor(() => {
      const options = document.querySelectorAll('.ant-select-item-option')
      const opt1Element = Array.from(options).find((el) =>
        el.textContent?.includes('Gội đầu thảo dược'),
      )
      expect(opt1Element).toHaveClass('ant-select-item-option-disabled')
    })
  })

  it('trùng tên: lỗi hiện dưới ô Tên', async () => {
    createHandler = () =>
      json(
        {
          code: 'COMBO_NAME_DUPLICATE',
          message: 'Tên combo đã tồn tại.',
          fieldErrors: [],
          path: '/api/catalog/combos',
          timestamp: '',
        },
        409,
      )

    const { onClose } = renderDrawer()

    fireEvent.change(screen.getByPlaceholderText('Ví dụ: Combo Thư Giãn'), {
      target: { value: 'Combo Đã Có' },
    })

    const select = screen.getByRole('combobox', { name: 'Thêm dịch vụ vào combo' })
    fireEvent.mouseDown(select)
    const opt1 = await screen.findByText(/Gội đầu thảo dược/)
    fireEvent.click(opt1)

    fireEvent.change(screen.getByLabelText('Giá combo'), { target: { value: '300000' } })

    fireEvent.click(screen.getByRole('button', { name: 'Tạo combo' }))

    expect(await screen.findByText('Tên combo đã tồn tại.')).toBeInTheDocument()
    expect(onClose).not.toHaveBeenCalled()
  })

  it('dịch vụ ngừng bán: lỗi hiện ở khu Dịch vụ trong combo', async () => {
    createHandler = () =>
      json(
        {
          code: 'COMBO_SERVICE_UNAVAILABLE',
          message: 'Dịch vụ đã ngừng bán: Gội đầu thảo dược.',
          fieldErrors: [],
          path: '/api/catalog/combos',
          timestamp: '',
        },
        400,
      )

    const { onClose } = renderDrawer()

    fireEvent.change(screen.getByPlaceholderText('Ví dụ: Combo Thư Giãn'), {
      target: { value: 'Combo Test' },
    })

    const select = screen.getByRole('combobox', { name: 'Thêm dịch vụ vào combo' })
    fireEvent.mouseDown(select)
    const opt1 = await screen.findByText(/Gội đầu thảo dược/)
    fireEvent.click(opt1)

    fireEvent.change(screen.getByLabelText('Giá combo'), { target: { value: '300000' } })

    fireEvent.click(screen.getByRole('button', { name: 'Tạo combo' }))

    expect(await screen.findByText('Dịch vụ đã ngừng bán: Gội đầu thảo dược.')).toBeInTheDocument()
    expect(onClose).not.toHaveBeenCalled()
  })

  it('tên chỉ có ký tự vô hình: lỗi fieldErrors của backend hiện dưới ô Tên', async () => {
    createHandler = () =>
      json(
        {
          code: 'VALIDATION_FAILED',
          message: 'Dữ liệu không hợp lệ.',
          fieldErrors: [{ field: 'name', message: 'Tên combo không được để trống' }],
          path: '/api/catalog/combos',
          timestamp: '',
        },
        400,
      )

    renderDrawer()

    fireEvent.change(screen.getByPlaceholderText('Ví dụ: Combo Thư Giãn'), {
      target: { value: '​‌‍' }, // ký tự zero-width
    })

    const select = screen.getByRole('combobox', { name: 'Thêm dịch vụ vào combo' })
    fireEvent.mouseDown(select)
    const opt1 = await screen.findByText(/Gội đầu thảo dược/)
    fireEvent.click(opt1)

    fireEvent.change(screen.getByLabelText('Giá combo'), { target: { value: '300000' } })

    fireEvent.click(screen.getByRole('button', { name: 'Tạo combo' }))

    expect(await screen.findByText('Tên combo không được để trống')).toBeInTheDocument()
  })

  it('fieldErrors items[1].quantity hiện dưới ô số lượng dòng 2', async () => {
    createHandler = () =>
      json(
        {
          code: 'VALIDATION_FAILED',
          message: 'Dữ liệu không hợp lệ.',
          fieldErrors: [{ field: 'items[1].quantity', message: 'Số lượng tối đa là 99' }],
          path: '/api/catalog/combos',
          timestamp: '',
        },
        400,
      )

    renderDrawer()

    fireEvent.change(screen.getByPlaceholderText('Ví dụ: Combo Thư Giãn'), {
      target: { value: 'Combo Test' },
    })

    const select = screen.getByRole('combobox', { name: 'Thêm dịch vụ vào combo' })
    fireEvent.mouseDown(select)
    const opt1 = await screen.findByText(/Gội đầu thảo dược/)
    fireEvent.click(opt1)

    fireEvent.mouseDown(select)
    const opt2 = await screen.findByText(/Giãn cơ chân/)
    fireEvent.click(opt2)

    fireEvent.change(screen.getByLabelText('Giá combo'), { target: { value: '300000' } })

    fireEvent.click(screen.getByRole('button', { name: 'Tạo combo' }))

    expect(await screen.findByText('Số lượng tối đa là 99')).toBeInTheDocument()
  })

  it('fieldErrors trường lạ hiện bằng message.error', async () => {
    createHandler = () =>
      json(
        {
          code: 'VALIDATION_FAILED',
          message: 'Dữ liệu không hợp lệ.',
          fieldErrors: [{ field: 'unknownField', message: 'Trường này không hợp lệ' }],
          path: '/api/catalog/combos',
          timestamp: '',
        },
        400,
      )

    renderDrawer()

    fireEvent.change(screen.getByPlaceholderText('Ví dụ: Combo Thư Giãn'), {
      target: { value: 'Combo Test' },
    })

    const select = screen.getByRole('combobox', { name: 'Thêm dịch vụ vào combo' })
    fireEvent.mouseDown(select)
    const opt1 = await screen.findByText(/Gội đầu thảo dược/)
    fireEvent.click(opt1)

    fireEvent.change(screen.getByLabelText('Giá combo'), { target: { value: '300000' } })

    fireEvent.click(screen.getByRole('button', { name: 'Tạo combo' }))

    expect(await screen.findByText('unknownField: Trường này không hợp lệ')).toBeInTheDocument()
  })
})

describe('ComboFormDrawer - Tính thử giá', () => {
  it('hiện tổng giá lẻ, thành tiền từng dòng và tiết kiệm từ kết quả tính thử', async () => {
    renderDrawer()

    // Chọn dịch vụ 1: 180.000 đ
    const select = screen.getByRole('combobox', { name: 'Thêm dịch vụ vào combo' })
    fireEvent.mouseDown(select)
    const opt1 = await screen.findByText(/Gội đầu thảo dược/)
    fireEvent.click(opt1)

    // Chọn dịch vụ 2: 200.000 đ
    fireEvent.mouseDown(select)
    const opt2 = await screen.findByText(/Giãn cơ chân/)
    fireEvent.click(opt2)

    // Nhập giá combo: 300.000 đ
    fireEvent.change(screen.getByLabelText('Giá combo'), { target: { value: '300000' } })

    // Đợi quote query chạy và hiển thị kết quả
    await waitFor(() => {
      // Tổng giá lẻ: 180.000 + 200.000 = 380.000 đ
      expect(screen.getByText('380.000 đ')).toBeInTheDocument()
      // Tiết kiệm: 380.000 - 300.000 = 80.000 đ
      expect(screen.getByText('80.000 đ')).toBeInTheDocument()
    })

    // Thành tiền từng dòng
    expect(screen.getByText('180.000 đ × 1 =')).toBeInTheDocument()
    expect(screen.getByText('200.000 đ × 1 =')).toBeInTheDocument()
  })

  it('số lượng trống thì không gọi tính thử và hiện gợi ý nhập số lượng', async () => {
    renderDrawer()

    const select = screen.getByRole('combobox', { name: 'Thêm dịch vụ vào combo' })
    fireEvent.mouseDown(select)
    const opt1 = await screen.findByText(/Gội đầu thảo dược/)
    fireEvent.click(opt1)

    const qtyInput = await screen.findByLabelText('Số lượng Gội đầu thảo dược')
    fireEvent.change(qtyInput, { target: { value: '' } })

    expect(await screen.findByText('Nhập số lượng từ 1 đến 99 để tính tổng.')).toBeInTheDocument()
  })

  it('chưa chọn dịch vụ thì không gọi tính thử', async () => {
    renderDrawer()

    expect(screen.getByText('Chọn dịch vụ để xem tổng giá lẻ.')).toBeInTheDocument()
    const quoteCalls = fetchCalls.filter((c) => c.url === '/api/catalog/combos/quote')
    expect(quoteCalls.length).toBe(0)
  })

  it('chưa nhập giá thì tiết kiệm hiện —', async () => {
    renderDrawer()

    const select = screen.getByRole('combobox', { name: 'Thêm dịch vụ vào combo' })
    fireEvent.mouseDown(select)
    const opt1 = await screen.findByText(/Gội đầu thảo dược/)
    fireEvent.click(opt1)

    const summary = await screen.findByText('Tổng kết giá')
    const summaryContainer = summary.closest('div')?.parentElement
    expect(summaryContainer).toBeDefined()
    expect(await within(summaryContainer!).findByText('180.000 đ')).toBeInTheDocument()
    expect(within(summaryContainer!).getAllByText('—').length).toBe(2)
  })

  it('tiết kiệm âm hiện cảnh báo giá combo cao hơn tổng giá lẻ', async () => {
    renderDrawer()

    const select = screen.getByRole('combobox', { name: 'Thêm dịch vụ vào combo' })
    fireEvent.mouseDown(select)
    const opt1 = await screen.findByText(/Gội đầu thảo dược/) // 180.000 đ
    fireEvent.click(opt1)

    // Đặt giá combo = 200.000 đ (cao hơn giá lẻ 180.000 đ)
    fireEvent.change(screen.getByLabelText('Giá combo'), { target: { value: '200000' } })

    expect(
      await screen.findByText(
        'Giá combo cao hơn tổng giá lẻ. Vẫn lưu được, nhưng khách mua lẻ sẽ rẻ hơn.',
      ),
    ).toBeInTheDocument()
  })

  it('tính thử lỗi thì hiện Không tính được tổng giá lẻ và Thử lại, vẫn lưu được', async () => {
    quoteHandler = () =>
      json(
        {
          code: 'BAD_REQUEST',
          message: 'Lỗi tính toán.',
        },
        400,
      )

    renderDrawer()

    fireEvent.change(screen.getByPlaceholderText('Ví dụ: Combo Thư Giãn'), {
      target: { value: 'Combo Thử Lại' },
    })

    const select = screen.getByRole('combobox', { name: 'Thêm dịch vụ vào combo' })
    fireEvent.mouseDown(select)
    const opt1 = await screen.findByText(/Gội đầu thảo dược/)
    fireEvent.click(opt1)

    fireEvent.change(screen.getByLabelText('Giá combo'), { target: { value: '200000' } })

    expect(await screen.findByText('Không tính được tổng giá lẻ.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Thử lại' })).toBeInTheDocument()

    // Bấm Tạo combo vẫn lưu được
    fireEvent.click(screen.getByRole('button', { name: 'Tạo combo' }))
    expect(await screen.findByText('Đã thêm combo.')).toBeInTheDocument()
  })

  it('tính thử không làm mới danh sách combo', async () => {
    renderDrawer()

    const select = screen.getByRole('combobox', { name: 'Thêm dịch vụ vào combo' })
    fireEvent.mouseDown(select)
    const opt1 = await screen.findByText(/Gội đầu thảo dược/)
    fireEvent.click(opt1)

    await waitFor(() => {
      const quoteCalls = fetchCalls.filter((c) => c.url === '/api/catalog/combos/quote')
      expect(quoteCalls.length).toBeGreaterThan(0)
    })

    const comboListCalls = fetchCalls.filter(
      (c) => c.url === '/api/catalog/combos' && c.method === 'GET',
    )
    expect(comboListCalls.length).toBe(0)
  })
})
