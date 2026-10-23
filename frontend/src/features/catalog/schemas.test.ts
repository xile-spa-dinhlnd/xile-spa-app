import { describe, expect, it } from 'vitest'
import { serviceFormSchema, serviceGroupSchema } from './schemas'

const valid = {
  name: 'Gội 30 phút',
  groupId: 1,
  durationMinutes: 30,
  listPrice: 69000,
  tourFee: null,
}

function messages(input: unknown) {
  const result = serviceFormSchema.safeParse(input)
  return result.success
    ? {}
    : Object.fromEntries(result.error.issues.map((i) => [i.path[0], i.message]))
}

function groupMessages(input: unknown) {
  const result = serviceGroupSchema.safeParse(input)
  return result.success
    ? {}
    : Object.fromEntries(result.error.issues.map((i) => [i.path[0], i.message]))
}

describe('serviceFormSchema', () => {
  it('chấp nhận dữ liệu hợp lệ, giá 0 và tiền tour trống', () => {
    expect(serviceFormSchema.safeParse(valid).success).toBe(true)
    expect(serviceFormSchema.safeParse({ ...valid, listPrice: 0 }).success).toBe(true)
  })

  it('báo lỗi tiếng Việt theo từng ô', () => {
    expect(messages({ ...valid, name: '   ' })).toMatchObject({
      name: 'Tên dịch vụ không được để trống',
    })
    expect(messages({ ...valid, listPrice: -1 })).toMatchObject({
      listPrice: 'Giá niêm yết không được âm',
    })
    expect(messages({ ...valid, groupId: undefined })).toMatchObject({
      groupId: 'Vui lòng chọn nhóm dịch vụ',
    })
    expect(messages({ ...valid, durationMinutes: 0 })).toMatchObject({
      durationMinutes: 'Thời lượng phải lớn hơn 0 phút',
    })
  })
})

describe('serviceGroupSchema', () => {
  const validGroup = { name: 'Gội đầu dưỡng sinh', sortOrder: 10 }

  it('chấp nhận dữ liệu hợp lệ và thứ tự 0', () => {
    const parsed = serviceGroupSchema.parse({ name: '  Chăm sóc da  ', sortOrder: 0 })
    expect(parsed.name).toBe('Chăm sóc da')
    expect(parsed.sortOrder).toBe(0)
  })

  it('báo lỗi khi tên trống, quá 100 ký tự, thứ tự âm hoặc trống', () => {
    expect(groupMessages({ ...validGroup, name: '   ' })).toMatchObject({
      name: 'Tên nhóm không được để trống',
    })
    expect(groupMessages({ ...validGroup, name: 'a'.repeat(101) })).toMatchObject({
      name: 'Tên nhóm tối đa 100 ký tự',
    })
    expect(groupMessages({ ...validGroup, sortOrder: -1 })).toMatchObject({
      sortOrder: 'Thứ tự hiển thị không được âm',
    })
    expect(groupMessages({ name: 'Gội đầu' })).toMatchObject({
      sortOrder: 'Vui lòng nhập thứ tự hiển thị',
    })
  })
})
