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
