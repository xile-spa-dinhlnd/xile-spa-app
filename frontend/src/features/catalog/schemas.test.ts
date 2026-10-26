import { describe, expect, it } from 'vitest'
import { comboFormSchema, serviceFormSchema, serviceGroupSchema } from './schemas'

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

function comboMessages(input: unknown) {
  const result = comboFormSchema.safeParse(input)
  return result.success
    ? {}
    : Object.fromEntries(result.error.issues.map((i) => [i.path.join('.'), i.message]))
}

describe('comboFormSchema', () => {
  const validCombo = {
    name: 'Combo Thư Giãn',
    price: 400000,
    tourFee: null,
    items: [
      { serviceId: 1, quantity: 1 },
      { serviceId: 2, quantity: 2 },
    ],
  }

  it('chấp nhận combo hợp lệ, tiền tour trống', () => {
    expect(comboFormSchema.safeParse(validCombo).success).toBe(true)
    expect(comboFormSchema.safeParse({ ...validCombo, tourFee: 50000 }).success).toBe(true)
  })

  it('tên chỉ có khoảng trắng bị báo trống', () => {
    expect(comboMessages({ ...validCombo, name: '   ' })).toMatchObject({
      name: 'Tên combo không được để trống',
    })
  })

  it('giá 0 và số lượng 1, 99, đúng 20 dòng là hợp lệ', () => {
    const items20 = Array.from({ length: 20 }, (_, idx) => ({
      serviceId: idx + 1,
      quantity: idx === 0 ? 1 : 99,
    }))
    const result = comboFormSchema.safeParse({
      ...validCombo,
      price: 0,
      items: items20,
    })
    expect(result.success).toBe(true)
  })

  it.each([
    ['tên trống', { ...validCombo, name: '' }, 'name', 'Tên combo không được để trống'],
    [
      'tên 151 ký tự',
      { ...validCombo, name: 'a'.repeat(151) },
      'name',
      'Tên combo tối đa 150 ký tự',
    ],
    ['giá trống', { ...validCombo, price: undefined }, 'price', 'Vui lòng nhập giá combo'],
    ['giá âm', { ...validCombo, price: -1000 }, 'price', 'Giá combo không được âm'],
    [
      'giá quá 1 tỷ',
      { ...validCombo, price: 1000000001 },
      'price',
      'Giá combo tối đa 1.000.000.000 đ',
    ],
    ['giá số lẻ 1.5', { ...validCombo, price: 1.5 }, 'price', 'Giá combo phải là số nguyên'],
    ['tiền tour âm', { ...validCombo, tourFee: -1 }, 'tourFee', 'Tiền tour không được âm'],
    [
      'tiền tour quá 1 tỷ',
      { ...validCombo, tourFee: 1000000001 },
      'tourFee',
      'Tiền tour tối đa 1.000.000.000 đ',
    ],
    [
      'tiền tour số lẻ 1.5',
      { ...validCombo, tourFee: 1.5 },
      'tourFee',
      'Tiền tour phải là số nguyên',
    ],
    ['items rỗng', { ...validCombo, items: [] }, 'items', 'Combo phải có ít nhất một dịch vụ'],
    [
      'items 21 dòng',
      {
        ...validCombo,
        items: Array.from({ length: 21 }, (_, idx) => ({ serviceId: idx + 1, quantity: 1 })),
      },
      'items',
      'Combo tối đa 20 dịch vụ',
    ],
    [
      'thiếu serviceId dòng đầu',
      { ...validCombo, items: [{ serviceId: undefined, quantity: 1 }] },
      'items.0.serviceId',
      'Vui lòng chọn dịch vụ',
    ],
    [
      'số lượng trống dòng đầu',
      { ...validCombo, items: [{ serviceId: 1, quantity: undefined }] },
      'items.0.quantity',
      'Vui lòng nhập số lượng',
    ],
    [
      'số lượng 0 dòng đầu',
      { ...validCombo, items: [{ serviceId: 1, quantity: 0 }] },
      'items.0.quantity',
      'Số lượng tối thiểu là 1',
    ],
    [
      'số lượng 100 dòng đầu',
      { ...validCombo, items: [{ serviceId: 1, quantity: 100 }] },
      'items.0.quantity',
      'Số lượng tối đa là 99',
    ],
    [
      'số lượng số lẻ 1.5 dòng đầu',
      { ...validCombo, items: [{ serviceId: 1, quantity: 1.5 }] },
      'items.0.quantity',
      'Số lượng phải là số nguyên',
    ],
  ])('báo lỗi đúng đường dẫn và thông báo khi %s', (_desc, input, path, expectedMessage) => {
    expect(comboMessages(input)).toMatchObject({
      [path]: expectedMessage,
    })
  })
})
