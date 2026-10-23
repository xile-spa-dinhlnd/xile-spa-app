import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { Providers } from '@/app/providers'
import { ServiceGroupDrawer } from './ServiceGroupDrawer'
import type { ServiceGroup } from '../types'

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

const mockGroups: ServiceGroup[] = [
  { id: 1, name: 'Gội đầu dưỡng sinh', sortOrder: 10, active: true },
  { id: 2, name: 'Chăm sóc da', sortOrder: 20, active: false },
]

let groupsData: ServiceGroup[] = []
let failGet = false
let fetchCalls: { url: string; method: string; body?: unknown }[] = []

beforeEach(() => {
  groupsData = [...mockGroups]
  failGet = false
  fetchCalls = []

  vi.stubGlobal(
    'fetch',
    vi.fn(async (input: string, init?: RequestInit) => {
      const url = new URL(input, 'http://localhost')
      const method = init?.method ?? 'GET'
      const body = init?.body ? JSON.parse(String(init.body)) : undefined
      fetchCalls.push({ url: url.pathname, method, body })

      if (url.pathname === '/api/catalog/service-groups') {
        if (method === 'GET') {
          if (failGet) {
            return json(
              {
                code: 'INTERNAL_ERROR',
                message: 'Không thể tải nhóm dịch vụ.',
                fieldErrors: [],
                path: url.pathname,
                timestamp: '',
              },
              500,
            )
          }
          return json(groupsData)
        }
        if (method === 'POST') {
          if (body?.name === 'Trùng tên') {
            return json(
              {
                code: 'SERVICE_GROUP_NAME_DUPLICATE',
                message: 'Tên nhóm dịch vụ đã tồn tại.',
                fieldErrors: [],
                path: url.pathname,
                timestamp: '',
              },
              409,
            )
          }
          const created: ServiceGroup = {
            id: 99,
            name: body.name,
            sortOrder: body.sortOrder,
            active: true,
          }
          groupsData.push(created)
          return json(created, 201)
        }
      }

      if (url.pathname.match(/^\/api\/catalog\/service-groups\/\d+$/) && method === 'PUT') {
        const id = Number(url.pathname.split('/').pop())
        const updated: ServiceGroup = {
          id,
          name: body.name,
          sortOrder: body.sortOrder,
          active: true,
        }
        groupsData = groupsData.map((g) => (g.id === id ? updated : g))
        return json(updated, 200)
      }

      if (url.pathname === '/api/catalog/service-groups/1/deactivate' && method === 'POST') {
        return json(
          {
            code: 'SERVICE_GROUP_IN_USE',
            message:
              'Nhóm còn dịch vụ đang bán. Hãy ngừng bán hoặc chuyển các dịch vụ sang nhóm khác trước.',
            fieldErrors: [],
            path: url.pathname,
            timestamp: '',
          },
          409,
        )
      }

      if (url.pathname.endsWith('/deactivate') && method === 'POST') {
        const id = Number(url.pathname.split('/')[4])
        groupsData = groupsData.map((g) => (g.id === id ? { ...g, active: false } : g))
        return json({ id, active: false }, 200)
      }

      if (url.pathname.endsWith('/activate') && method === 'POST') {
        const id = Number(url.pathname.split('/')[4])
        groupsData = groupsData.map((g) => (g.id === id ? { ...g, active: true } : g))
        return json({ id, active: true }, 200)
      }

      return json({}, 404)
    }),
  )
})

afterEach(() => vi.unstubAllGlobals())

function renderDrawer(props: { open?: boolean; onClose?: () => void } = {}) {
  return render(
    <Providers>
      <ServiceGroupDrawer open={props.open ?? true} onClose={props.onClose ?? vi.fn()} />
    </Providers>,
  )
}

describe('ServiceGroupDrawer: danh sách nhóm', () => {
  it('hiển thị tên, thứ tự và tag trạng thái của các nhóm', async () => {
    renderDrawer()

    expect(await screen.findByText('Gội đầu dưỡng sinh')).toBeInTheDocument()
    expect(screen.getByText('Thứ tự: 10')).toBeInTheDocument()
    expect(screen.getByText('Đang dùng')).toBeInTheDocument()

    expect(screen.getByText('Chăm sóc da')).toBeInTheDocument()
    expect(screen.getByText('Thứ tự: 20')).toBeInTheDocument()
    expect(screen.getByText('Ngừng dùng', { selector: '.ant-tag' })).toBeInTheDocument()
  })

  it('hiển thị trạng thái trống khi không có nhóm nào', async () => {
    groupsData = []
    renderDrawer()

    expect(await screen.findByText('Chưa có nhóm dịch vụ nào')).toBeInTheDocument()
  })

  it('lỗi tải danh sách hiện thông báo và nút thử lại', { timeout: 15000 }, async () => {
    failGet = true
    renderDrawer()

    const alert = await screen.findByRole('alert', {}, { timeout: 8000 })
    expect(within(alert).getByText('Không tải được danh sách nhóm')).toBeInTheDocument()
    expect(screen.queryByText('Chưa có nhóm dịch vụ nào')).not.toBeInTheDocument()

    failGet = false
    fireEvent.click(within(alert).getByRole('button', { name: 'Thử lại' }))
    expect(await screen.findByText('Gội đầu dưỡng sinh')).toBeInTheDocument()
  })
})

describe('ServiceGroupDrawer: thêm và sửa nhóm', () => {
  it('thêm nhóm gọi POST đúng body và làm mới danh sách', async () => {
    renderDrawer()

    await screen.findByText('Gội đầu dưỡng sinh')

    const nameInput = screen.getByPlaceholderText('Ví dụ: Gội đầu dưỡng sinh')
    const sortInput = screen.getByRole('spinbutton', { name: /Thứ tự hiển thị/ })

    fireEvent.change(nameInput, { target: { value: 'Massage body' } })
    fireEvent.change(sortInput, { target: { value: '30' } })

    fireEvent.click(screen.getByRole('button', { name: 'Thêm nhóm' }))

    await waitFor(() => {
      const post = fetchCalls.find(
        (c) => c.method === 'POST' && c.url === '/api/catalog/service-groups',
      )
      expect(post).toBeDefined()
      expect(post?.body).toEqual({ name: 'Massage body', sortOrder: 30 })
    })

    expect(await screen.findByText('Massage body')).toBeInTheDocument()
  })

  it('báo lỗi validation khi để trống tên hoặc thứ tự âm', async () => {
    renderDrawer()
    await screen.findByText('Gội đầu dưỡng sinh')

    const nameInput = screen.getByPlaceholderText('Ví dụ: Gội đầu dưỡng sinh')
    const sortInput = screen.getByRole('spinbutton', { name: /Thứ tự hiển thị/ })

    fireEvent.change(nameInput, { target: { value: '   ' } })
    fireEvent.change(sortInput, { target: { value: '-1' } })

    fireEvent.click(screen.getByRole('button', { name: 'Thêm nhóm' }))

    expect(await screen.findByText('Tên nhóm không được để trống')).toBeInTheDocument()
    expect(screen.getByText('Thứ tự hiển thị không được âm')).toBeInTheDocument()
  })

  it('sửa nhóm nạp giá trị cũ, gọi PUT và có nút hủy sửa', async () => {
    renderDrawer()

    await screen.findByText('Gội đầu dưỡng sinh')

    const editButtons = screen.getAllByRole('button', { name: /Sửa/ })
    fireEvent.click(editButtons[0]) // Sửa nhóm 1: Gội đầu dưỡng sinh

    expect(await screen.findByText('Sửa nhóm: Gội đầu dưỡng sinh')).toBeInTheDocument()
    const nameInput = screen.getByPlaceholderText('Ví dụ: Gội đầu dưỡng sinh')
    expect(nameInput).toHaveValue('Gội đầu dưỡng sinh')

    // Bấm nút Hủy sửa thì form trở về thêm mới
    fireEvent.click(screen.getByRole('button', { name: 'Hủy sửa' }))
    expect(await screen.findByText('Thêm nhóm mới')).toBeInTheDocument()
    expect(nameInput).toHaveValue('')

    // Chọn sửa lại và lưu
    fireEvent.click(editButtons[0])
    fireEvent.change(nameInput, { target: { value: 'Gội đầu & Dưỡng sinh VIP' } })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu' }))

    await waitFor(() => {
      const put = fetchCalls.find(
        (c) => c.method === 'PUT' && c.url === '/api/catalog/service-groups/1',
      )
      expect(put).toBeDefined()
      expect(put?.body).toEqual({ name: 'Gội đầu & Dưỡng sinh VIP', sortOrder: 10 })
    })
  })

  it('409 trùng tên hiển thị lỗi dưới ô tên và giữ nguyên dữ liệu', async () => {
    renderDrawer()
    await screen.findByText('Gội đầu dưỡng sinh')

    const nameInput = screen.getByPlaceholderText('Ví dụ: Gội đầu dưỡng sinh')
    fireEvent.change(nameInput, { target: { value: 'Trùng tên' } })
    fireEvent.click(screen.getByRole('button', { name: 'Thêm nhóm' }))

    expect(await screen.findByText('Tên nhóm dịch vụ đã tồn tại.')).toBeInTheDocument()
    expect(nameInput).toHaveValue('Trùng tên')
  })
})

describe('ServiceGroupDrawer: ngừng dùng và dùng lại', () => {
  it('hủy xác nhận ngừng dùng thì không gọi API', async () => {
    renderDrawer()
    await screen.findByText('Gội đầu dưỡng sinh')

    // Nhóm 1 đang active -> có nút Ngừng dùng
    const deactivateBtn = screen.getByRole('button', { name: 'Ngừng dùng' })
    fireEvent.click(deactivateBtn)

    // Popconfirm xuất hiện
    expect(
      await screen.findByText(/Nhóm sẽ không còn để chọn khi thêm hoặc sửa dịch vụ/),
    ).toBeInTheDocument()

    // Hủy xác nhận -> không gọi API
    fireEvent.click(screen.getByRole('button', { name: 'Hủy' }))
    expect(fetchCalls.some((c) => c.url.endsWith('/deactivate'))).toBe(false)
  })

  it('ngừng dùng nhóm còn dịch vụ đang bán: hiện thông báo của backend, nhóm vẫn đang dùng', async () => {
    renderDrawer()
    await screen.findByText('Gội đầu dưỡng sinh')

    fireEvent.click(screen.getByRole('button', { name: 'Ngừng dùng' }))
    const confirm = await screen.findByRole('tooltip')
    fireEvent.click(within(confirm).getByRole('button', { name: 'Ngừng dùng' }))

    expect(
      await screen.findByText(
        'Nhóm còn dịch vụ đang bán. Hãy ngừng bán hoặc chuyển các dịch vụ sang nhóm khác trước.',
      ),
    ).toBeInTheDocument()
    expect(screen.getByText('Đang dùng')).toBeInTheDocument()
  })

  it('ngừng dùng nhóm không còn dịch vụ đang bán: gọi deactivate, tag chuyển sang Ngừng dùng', async () => {
    groupsData = [{ id: 3, name: 'Massage cổ vai gáy', sortOrder: 30, active: true }]
    renderDrawer()
    await screen.findByText('Massage cổ vai gáy')

    fireEvent.click(screen.getByRole('button', { name: 'Ngừng dùng' }))
    const confirm = await screen.findByRole('tooltip')
    fireEvent.click(within(confirm).getByRole('button', { name: 'Ngừng dùng' }))

    await waitFor(() =>
      expect(fetchCalls.some((c) => c.url === '/api/catalog/service-groups/3/deactivate')).toBe(
        true,
      ),
    )
    expect(await screen.findByRole('button', { name: 'Dùng lại' })).toBeInTheDocument()
  })

  it('dùng lại nhóm: xác nhận rồi gọi POST activate', async () => {
    renderDrawer()
    await screen.findByText('Chăm sóc da')

    // Nhóm 2 đang inactive -> có nút Dùng lại
    const activateBtn = screen.getByRole('button', { name: 'Dùng lại' })
    fireEvent.click(activateBtn)

    const confirm = await screen.findByRole('tooltip')
    expect(within(confirm).getByText(/Nhóm sẽ xuất hiện lại trong ô chọn nhóm/)).toBeInTheDocument()
    fireEvent.click(within(confirm).getByRole('button', { name: 'Dùng lại' }))

    await waitFor(() => {
      expect(fetchCalls.some((c) => c.url === '/api/catalog/service-groups/2/activate')).toBe(true)
    })
    await waitFor(() => expect(screen.getAllByText('Đang dùng')).toHaveLength(2))
  })
})
