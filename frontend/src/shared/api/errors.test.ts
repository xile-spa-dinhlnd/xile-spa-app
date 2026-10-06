import { describe, expect, it } from 'vitest'
import { ApiError, toFormFieldErrors } from './errors'

describe('toFormFieldErrors', () => {
  it('gom lỗi theo ô cho Ant Design Form', () => {
    const error = new ApiError('VALIDATION_FAILED', 'Dữ liệu không hợp lệ', 400, [
      { field: 'name', message: 'Tên không được để trống' },
      { field: 'price', message: 'Giá không được âm' },
      { field: 'price', message: 'Giá quá lớn' },
    ])

    expect(toFormFieldErrors(error)).toEqual([
      { name: 'name', errors: ['Tên không được để trống'] },
      { name: 'price', errors: ['Giá không được âm', 'Giá quá lớn'] },
    ])
  })
})
