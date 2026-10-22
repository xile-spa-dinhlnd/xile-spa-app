import { describe, expect, it } from 'vitest'
import { staffFormSchema } from './schemas'

function messages(input: unknown) {
  const result = staffFormSchema.safeParse(input)
  return result.success
    ? {}
    : Object.fromEntries(result.error.issues.map((i) => [i.path[0], i.message]))
}

describe('staffFormSchema', () => {
  it('chấp nhận dữ liệu hợp lệ', () => {
    expect(staffFormSchema.safeParse({ fullName: 'Lan', role: 'THERAPIST' }).success).toBe(true)
    expect(staffFormSchema.safeParse({ fullName: 'Chị chủ', role: 'OWNER' }).success).toBe(true)
  })

  it('báo lỗi tiếng Việt theo từng ô', () => {
    expect(messages({ fullName: '   ', role: 'THERAPIST' })).toMatchObject({
      fullName: 'Tên nhân viên không được để trống',
    })
    expect(messages({ fullName: 'a'.repeat(101), role: 'THERAPIST' })).toMatchObject({
      fullName: 'Tên nhân viên tối đa 100 ký tự',
    })
    expect(messages({ fullName: 'Lan', role: undefined })).toMatchObject({
      role: 'Vui lòng chọn vai trò',
    })
  })
})
