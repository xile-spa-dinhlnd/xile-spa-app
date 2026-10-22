import { z } from 'zod'

/** Kiểm tra form nhân viên ở giao diện, khớp với kiểm tra của backend (StaffRequest). */
export const staffFormSchema = z.object({
  fullName: z
    .string()
    .trim()
    .min(1, 'Tên nhân viên không được để trống')
    .max(100, 'Tên nhân viên tối đa 100 ký tự'),
  role: z.enum(['OWNER', 'THERAPIST'], { error: 'Vui lòng chọn vai trò' }),
})

export type StaffFormValues = z.infer<typeof staffFormSchema>
