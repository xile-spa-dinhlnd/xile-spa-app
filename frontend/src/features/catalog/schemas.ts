import { z } from 'zod'

const MAX_MONEY = 1_000_000_000

/** Kiểm tra form dịch vụ ở giao diện, khớp với kiểm tra của backend (ServiceItemRequest). */
export const serviceFormSchema = z.object({
  name: z
    .string()
    .trim()
    .min(1, 'Tên dịch vụ không được để trống')
    .max(150, 'Tên dịch vụ tối đa 150 ký tự'),
  groupId: z.number({ error: 'Vui lòng chọn nhóm dịch vụ' }),
  durationMinutes: z
    .number()
    .int()
    .positive('Thời lượng phải lớn hơn 0 phút')
    .max(1440, 'Thời lượng tối đa 1440 phút')
    .nullable(),
  listPrice: z
    .number({ error: 'Vui lòng nhập giá niêm yết' })
    .int()
    .min(0, 'Giá niêm yết không được âm')
    .max(MAX_MONEY, 'Giá niêm yết tối đa 1.000.000.000 đ'),
  tourFee: z
    .number()
    .int()
    .min(0, 'Tiền tour không được âm')
    .max(MAX_MONEY, 'Tiền tour tối đa 1.000.000.000 đ')
    .nullable(),
})

export type ServiceFormValues = z.infer<typeof serviceFormSchema>

/** Kiểm tra form nhóm dịch vụ ở giao diện, khớp với ServiceGroupRequest (S2-06). */
export const serviceGroupSchema = z.object({
  name: z
    .string()
    .trim()
    .min(1, 'Tên nhóm không được để trống')
    .max(100, 'Tên nhóm tối đa 100 ký tự'),
  sortOrder: z
    .number({ error: 'Vui lòng nhập thứ tự hiển thị' })
    .int('Thứ tự hiển thị phải là số nguyên')
    .min(0, 'Thứ tự hiển thị không được âm'),
})

export type ServiceGroupFormValues = z.infer<typeof serviceGroupSchema>

export const MAX_COMBO_ITEMS = 20
export const MAX_COMBO_QUANTITY = 99

export const comboItemSchema = z.object({
  serviceId: z.number({ error: 'Vui lòng chọn dịch vụ' }),
  serviceName: z.string().optional(),
  serviceActive: z.boolean().optional(),
  quantity: z
    .number({ error: 'Vui lòng nhập số lượng' })
    .int('Số lượng phải là số nguyên')
    .min(1, 'Số lượng tối thiểu là 1')
    .max(MAX_COMBO_QUANTITY, 'Số lượng tối đa là 99'),
})

export type ComboItemFormValues = z.infer<typeof comboItemSchema>

/** Kiểm tra form combo ở giao diện (S2-08), khớp với ComboRequest của backend. */
export const comboFormSchema = z.object({
  name: z
    .string()
    .trim()
    .min(1, 'Tên combo không được để trống')
    .max(150, 'Tên combo tối đa 150 ký tự'),
  price: z
    .number({ error: 'Vui lòng nhập giá combo' })
    .int('Giá combo phải là số nguyên')
    .min(0, 'Giá combo không được âm')
    .max(MAX_MONEY, 'Giá combo tối đa 1.000.000.000 đ'),
  tourFee: z
    .number()
    .int('Tiền tour phải là số nguyên')
    .min(0, 'Tiền tour không được âm')
    .max(MAX_MONEY, 'Tiền tour tối đa 1.000.000.000 đ')
    .nullable(),
  items: z
    .array(comboItemSchema)
    .min(1, 'Combo phải có ít nhất một dịch vụ')
    .max(MAX_COMBO_ITEMS, 'Combo tối đa 20 dịch vụ'),
})

export type ComboFormValues = z.infer<typeof comboFormSchema>
