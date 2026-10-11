import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { MoneyInput } from './MoneyInput'

describe('MoneyInput', () => {
  it('hiển thị số tiền có dấu chấm phân cách', () => {
    render(<MoneyInput aria-label="Giá" value={1234000} />)
    expect(screen.getByLabelText('Giá')).toHaveValue('1.234.000')
  })

  it('chỉ nhận chữ số và trả về số nguyên', () => {
    const onChange = vi.fn()
    render(<MoneyInput aria-label="Giá" value={null} onChange={onChange} />)

    fireEvent.change(screen.getByLabelText('Giá'), { target: { value: '69.000đ abc' } })

    expect(onChange).toHaveBeenLastCalledWith(69000)
  })

  it('ô trống hiển thị rỗng', () => {
    render(<MoneyInput aria-label="Giá" value={null} />)
    expect(screen.getByLabelText('Giá')).toHaveValue('')
  })
})
