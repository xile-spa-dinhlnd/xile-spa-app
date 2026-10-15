import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { Providers } from '@/app/providers'
import { ForgotPasswordPage } from './ForgotPasswordPage'

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

describe('ForgotPasswordPage', () => {
  let fetchMock: ReturnType<typeof vi.fn>

  beforeEach(() => {
    fetchMock = vi.fn(async (url: string, init?: RequestInit) => {
      if (url.endsWith('/api/auth/forgot-password') && init?.method === 'POST') {
        const body = JSON.parse(init.body as string)
        if (body.email === 'error@xilespa.vn') {
          return json(
            {
              code: 'INTERNAL_ERROR',
              message: 'Lỗi hệ thống khi gửi email.',
              fieldErrors: [],
              path: '/api/auth/forgot-password',
              timestamp: '',
            },
            500,
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

  it('hiển thị đầy đủ form yêu cầu đặt lại mật khẩu', () => {
    render(
      <Providers>
        <MemoryRouter>
          <ForgotPasswordPage />
        </MemoryRouter>
      </Providers>,
    )

    expect(screen.getByRole('heading', { name: 'Quên mật khẩu' })).toBeInTheDocument()
    expect(screen.getByLabelText('Email tài khoản')).toBeInTheDocument()
    expect(
      screen.getByRole('button', { name: 'Gửi liên kết đặt lại mật khẩu' }),
    ).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Quay lại đăng nhập/i })).toBeInTheDocument()
  })

  it('báo lỗi khi để trống email hoặc nhập sai định dạng', async () => {
    const user = userEvent.setup()

    render(
      <Providers>
        <MemoryRouter>
          <ForgotPasswordPage />
        </MemoryRouter>
      </Providers>,
    )

    await user.click(screen.getByRole('button', { name: 'Gửi liên kết đặt lại mật khẩu' }))
    expect(await screen.findByText('Vui lòng nhập email.')).toBeInTheDocument()

    await user.type(screen.getByLabelText('Email tài khoản'), 'sai-dinh-dang')
    expect(await screen.findByText('Email không đúng định dạng.')).toBeInTheDocument()
  })

  it('gửi yêu cầu thành công và hiển thị thông báo chống user enumeration', async () => {
    const user = userEvent.setup()

    render(
      <Providers>
        <MemoryRouter>
          <ForgotPasswordPage />
        </MemoryRouter>
      </Providers>,
    )

    await user.type(screen.getByLabelText('Email tài khoản'), 'owner@xilespa.vn')
    await user.click(screen.getByRole('button', { name: 'Gửi liên kết đặt lại mật khẩu' }))

    await waitFor(() => {
      expect(screen.getByText('Đã gửi yêu cầu thành công')).toBeInTheDocument()
    })
    expect(
      screen.getByText(
        /Nếu email tồn tại trong hệ thống, chúng tôi đã gửi liên kết đặt lại mật khẩu/i,
      ),
    ).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Quay lại trang đăng nhập' })).toBeInTheDocument()
  })

  it('hiển thị thông báo lỗi khi API thất bại', async () => {
    const user = userEvent.setup()

    render(
      <Providers>
        <MemoryRouter>
          <ForgotPasswordPage />
        </MemoryRouter>
      </Providers>,
    )

    await user.type(screen.getByLabelText('Email tài khoản'), 'error@xilespa.vn')
    await user.click(screen.getByRole('button', { name: 'Gửi liên kết đặt lại mật khẩu' }))

    expect(await screen.findByText('Lỗi hệ thống khi gửi email.')).toBeInTheDocument()
  })
})
