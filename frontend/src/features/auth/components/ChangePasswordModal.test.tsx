import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { Providers } from '@/app/providers'
import { ChangePasswordModal } from './ChangePasswordModal'

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

describe('ChangePasswordModal', () => {
  let fetchMock: ReturnType<typeof vi.fn>

  beforeEach(() => {
    fetchMock = vi.fn(async (url: string, init?: RequestInit) => {
      if (url.endsWith('/api/auth/change-password') && init?.method === 'POST') {
        const body = JSON.parse(init.body as string)
        if (body.oldPassword === 'WrongOldPassword') {
          return json(
            {
              code: 'BAD_REQUEST',
              message: 'Mật khẩu hiện tại không chính xác.',
              fieldErrors: [],
              path: '/api/auth/change-password',
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

  it('hiển thị modal với các trường nhập mật khẩu khi open = true', () => {
    render(
      <Providers>
        <ChangePasswordModal open={true} onClose={vi.fn()} />
      </Providers>,
    )

    expect(screen.getByRole('dialog')).toBeInTheDocument()
    expect(screen.getByText('Đổi mật khẩu', { selector: '.ant-modal-title' })).toBeInTheDocument()
    expect(screen.getByLabelText('Mật khẩu hiện tại')).toBeInTheDocument()
    expect(screen.getByLabelText('Mật khẩu mới')).toBeInTheDocument()
    expect(screen.getByLabelText('Xác nhận mật khẩu mới')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Đổi mật khẩu' })).toBeInTheDocument()
  })

  it('báo lỗi khi để trống các trường hoặc mật khẩu mới dưới 10 ký tự', async () => {
    const user = userEvent.setup()

    render(
      <Providers>
        <ChangePasswordModal open={true} onClose={vi.fn()} />
      </Providers>,
    )

    await user.click(screen.getByRole('button', { name: 'Đổi mật khẩu' }))

    expect(await screen.findByText('Vui lòng nhập mật khẩu hiện tại.')).toBeInTheDocument()
    expect(await screen.findByText('Vui lòng nhập mật khẩu mới.')).toBeInTheDocument()
    expect(await screen.findByText('Vui lòng xác nhận mật khẩu mới.')).toBeInTheDocument()

    // Nhập mật khẩu dưới 10 ký tự
    await user.type(screen.getByLabelText('Mật khẩu mới'), 'short123')
    expect(await screen.findByText('Mật khẩu mới phải có tối thiểu 10 ký tự.')).toBeInTheDocument()
  })

  it('báo lỗi khi mật khẩu xác nhận không khớp mật khẩu mới', async () => {
    const user = userEvent.setup()

    render(
      <Providers>
        <ChangePasswordModal open={true} onClose={vi.fn()} />
      </Providers>,
    )

    await user.type(screen.getByLabelText('Mật khẩu hiện tại'), 'CurrentPass123')
    await user.type(screen.getByLabelText('Mật khẩu mới'), 'NewSecurePass123')
    await user.type(screen.getByLabelText('Xác nhận mật khẩu mới'), 'DifferentPass123')

    await user.click(screen.getByRole('button', { name: 'Đổi mật khẩu' }))

    expect(await screen.findByText('Mật khẩu xác nhận không khớp.')).toBeInTheDocument()
  })

  it('gọi API đổi mật khẩu thành công và đóng modal', async () => {
    const user = userEvent.setup()
    const onClose = vi.fn()

    render(
      <Providers>
        <ChangePasswordModal open={true} onClose={onClose} />
      </Providers>,
    )

    await user.type(screen.getByLabelText('Mật khẩu hiện tại'), 'CurrentPass123')
    await user.type(screen.getByLabelText('Mật khẩu mới'), 'NewSecurePass123')
    await user.type(screen.getByLabelText('Xác nhận mật khẩu mới'), 'NewSecurePass123')

    await user.click(screen.getByRole('button', { name: 'Đổi mật khẩu' }))

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        '/api/auth/change-password',
        expect.objectContaining({
          method: 'POST',
          body: JSON.stringify({
            oldPassword: 'CurrentPass123',
            newPassword: 'NewSecurePass123',
          }),
        }),
      )
      expect(onClose).toHaveBeenCalled()
    })
  })

  it('hiển thị thông báo lỗi khi server trả về lỗi mật khẩu cũ sai', async () => {
    const user = userEvent.setup()

    render(
      <Providers>
        <ChangePasswordModal open={true} onClose={vi.fn()} />
      </Providers>,
    )

    await user.type(screen.getByLabelText('Mật khẩu hiện tại'), 'WrongOldPassword')
    await user.type(screen.getByLabelText('Mật khẩu mới'), 'NewSecurePass123')
    await user.type(screen.getByLabelText('Xác nhận mật khẩu mới'), 'NewSecurePass123')

    await user.click(screen.getByRole('button', { name: 'Đổi mật khẩu' }))

    expect(await screen.findByText('Mật khẩu hiện tại không chính xác.')).toBeInTheDocument()
  })
})
