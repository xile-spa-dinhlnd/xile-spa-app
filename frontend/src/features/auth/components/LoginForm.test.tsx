import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { LoginForm } from './LoginForm'

describe('LoginForm', () => {
  it('hiển thị đầy đủ ô nhập email, mật khẩu và nút đăng nhập', () => {
    render(<LoginForm onSubmit={vi.fn()} />)

    expect(screen.getByLabelText('Email')).toBeInTheDocument()
    expect(screen.getByLabelText('Mật khẩu')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Đăng nhập' })).toBeInTheDocument()
  })

  it('báo lỗi tiếng Việt khi để trống trường bắt buộc', async () => {
    const user = userEvent.setup()
    const onSubmit = vi.fn()

    render(<LoginForm onSubmit={onSubmit} />)

    await user.click(screen.getByRole('button', { name: 'Đăng nhập' }))

    expect(await screen.findByText('Vui lòng nhập email.')).toBeInTheDocument()
    expect(await screen.findByText('Vui lòng nhập mật khẩu.')).toBeInTheDocument()
    expect(onSubmit).not.toHaveBeenCalled()
  })

  it('báo lỗi khi email sai định dạng', async () => {
    const user = userEvent.setup()
    const onSubmit = vi.fn()

    render(<LoginForm onSubmit={onSubmit} />)

    await user.type(screen.getByLabelText('Email'), 'email-sai-dinh-dang')
    await user.type(screen.getByLabelText('Mật khẩu'), 'password123')
    await user.click(screen.getByRole('button', { name: 'Đăng nhập' }))

    expect(await screen.findByText('Email không đúng định dạng.')).toBeInTheDocument()
    expect(onSubmit).not.toHaveBeenCalled()
  })

  it('gửi thông tin thành công khi nhập đúng', async () => {
    const user = userEvent.setup()
    const onSubmit = vi.fn()

    render(<LoginForm onSubmit={onSubmit} />)

    await user.type(screen.getByLabelText('Email'), 'owner@xilespa.vn')
    await user.type(screen.getByLabelText('Mật khẩu'), 'MatKhauDung123')
    await user.click(screen.getByRole('button', { name: 'Đăng nhập' }))

    await waitFor(() => {
      expect(onSubmit).toHaveBeenCalledWith({
        email: 'owner@xilespa.vn',
        password: 'MatKhauDung123',
      })
    })
  })

  it('hiển thị thông báo lỗi khi có errorMessage', () => {
    render(<LoginForm onSubmit={vi.fn()} errorMessage="Email hoặc mật khẩu không đúng." />)

    expect(screen.getByRole('alert')).toBeInTheDocument()
    expect(screen.getByText('Email hoặc mật khẩu không đúng.')).toBeInTheDocument()
  })
})
