import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { Providers } from '@/app/providers'
import { ResetPasswordPage } from './ResetPasswordPage'

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

describe('ResetPasswordPage', () => {
  let fetchMock: ReturnType<typeof vi.fn>

  beforeEach(() => {
    fetchMock = vi.fn(async (url: string, init?: RequestInit) => {
      if (url.endsWith('/api/auth/reset-password') && init?.method === 'POST') {
        const body = JSON.parse(init.body as string)
        if (body.token === 'expired-token') {
          return json(
            {
              code: 'BAD_REQUEST',
              message: 'Liên kết đặt lại mật khẩu đã hết hạn.',
              fieldErrors: [],
              path: '/api/auth/reset-password',
              timestamp: '',
            },
            400,
          )
        }
        return json({}, 200)
      }
      return json({}, 404)
    })
    vi.stubGlobal('fetch', fetchMock)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('hiển thị cảnh báo khi không có token trong URL', () => {
    render(
      <Providers>
        <MemoryRouter initialEntries={['/reset-password']}>
          <ResetPasswordPage />
        </MemoryRouter>
      </Providers>,
    )

    expect(screen.getByText('Thiếu mã xác thực')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Yêu cầu liên kết mới' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Về trang đăng nhập' })).toBeInTheDocument()
  })

  it('hiển thị form nhập mật khẩu khi có token hợp lệ trong query string', () => {
    render(
      <Providers>
        <MemoryRouter initialEntries={['/reset-password?token=test-token-123']}>
          <ResetPasswordPage />
        </MemoryRouter>
      </Providers>,
    )

    expect(screen.getByRole('heading', { name: 'Đặt lại mật khẩu' })).toBeInTheDocument()
    expect(screen.getByLabelText('Mật khẩu mới')).toBeInTheDocument()
    expect(screen.getByLabelText('Xác nhận mật khẩu mới')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Lưu mật khẩu mới' })).toBeInTheDocument()
  })

  it('báo lỗi khi để trống, mật khẩu dưới 10 ký tự, hoặc mật khẩu xác nhận không khớp', async () => {
    const user = userEvent.setup()

    render(
      <Providers>
        <MemoryRouter initialEntries={['/reset-password?token=test-token-123']}>
          <ResetPasswordPage />
        </MemoryRouter>
      </Providers>,
    )

    await user.click(screen.getByRole('button', { name: 'Lưu mật khẩu mới' }))
    expect(await screen.findByText('Vui lòng nhập mật khẩu mới.')).toBeInTheDocument()
    expect(await screen.findByText('Vui lòng xác nhận mật khẩu mới.')).toBeInTheDocument()

    await user.type(screen.getByLabelText('Mật khẩu mới'), 'short')
    expect(await screen.findByText('Mật khẩu mới phải có ít nhất 10 ký tự.')).toBeInTheDocument()

    await user.clear(screen.getByLabelText('Mật khẩu mới'))
    await user.type(screen.getByLabelText('Mật khẩu mới'), 'ValidPassword123')
    await user.type(screen.getByLabelText('Xác nhận mật khẩu mới'), 'MismatchedPassword123')
    expect(await screen.findByText('Mật khẩu xác nhận không khớp.')).toBeInTheDocument()
  })

  it('đặt lại mật khẩu thành công và hiển thị giao diện xác nhận', async () => {
    const user = userEvent.setup()

    render(
      <Providers>
        <MemoryRouter initialEntries={['/reset-password?token=test-token-123']}>
          <ResetPasswordPage />
        </MemoryRouter>
      </Providers>,
    )

    await user.type(screen.getByLabelText('Mật khẩu mới'), 'BrandNewSecurePass123')
    await user.type(screen.getByLabelText('Xác nhận mật khẩu mới'), 'BrandNewSecurePass123')
    await user.click(screen.getByRole('button', { name: 'Lưu mật khẩu mới' }))

    await waitFor(() => {
      expect(screen.getByText('Đặt lại mật khẩu thành công')).toBeInTheDocument()
    })
    expect(screen.getByRole('button', { name: 'Đăng nhập ngay' })).toBeInTheDocument()
  })

  it('hiển thị thông báo lỗi khi token hết hạn', async () => {
    const user = userEvent.setup()

    render(
      <Providers>
        <MemoryRouter initialEntries={['/reset-password?token=expired-token']}>
          <ResetPasswordPage />
        </MemoryRouter>
      </Providers>,
    )

    await user.type(screen.getByLabelText('Mật khẩu mới'), 'BrandNewSecurePass123')
    await user.type(screen.getByLabelText('Xác nhận mật khẩu mới'), 'BrandNewSecurePass123')
    await user.click(screen.getByRole('button', { name: 'Lưu mật khẩu mới' }))

    expect(await screen.findByText('Liên kết đặt lại mật khẩu đã hết hạn.')).toBeInTheDocument()
  })
})
