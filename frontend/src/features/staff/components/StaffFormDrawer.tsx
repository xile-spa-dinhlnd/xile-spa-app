import { zodResolver } from '@hookform/resolvers/zod'
import { Alert, App, Button, Drawer, Form, Grid, Input, Select, Space } from 'antd'
import { useEffect } from 'react'
import { Controller, useForm, type FieldPath } from 'react-hook-form'
import { ApiError } from '@/shared/api/errors'
import { useCreateStaff, useUpdateStaff } from '../api'
import { staffFormSchema, type StaffFormValues } from '../schemas'
import { STAFF_ROLE_LABELS, type Staff } from '../types'

interface StaffFormDrawerProps {
  open: boolean
  /** Nhân viên đang sửa; không có nghĩa là tạo mới. */
  staff?: Staff | null
  onClose: () => void
}

function toFormValues(staff?: Staff | null): StaffFormValues {
  return { fullName: staff?.fullName ?? '', role: staff?.role ?? 'THERAPIST' }
}

const FIELDS: FieldPath<StaffFormValues>[] = ['fullName', 'role']

/** Form thêm và sửa nhân viên (FR-STF-01). Lỗi hiển thị tại từng ô, kể cả lỗi từ backend. */
export function StaffFormDrawer({ open, staff, onClose }: StaffFormDrawerProps) {
  const isEdit = Boolean(staff)
  const screens = Grid.useBreakpoint()
  const { message } = App.useApp()
  const createStaff = useCreateStaff()
  const updateStaff = useUpdateStaff()
  const saving = createStaff.isPending || updateStaff.isPending

  const {
    control,
    handleSubmit,
    reset,
    setError,
    formState: { errors },
  } = useForm<StaffFormValues>({
    resolver: zodResolver(staffFormSchema),
    defaultValues: toFormValues(staff),
  })

  useEffect(() => {
    if (open) reset(toFormValues(staff))
  }, [open, staff, reset])

  const onSubmit = handleSubmit(async (values) => {
    try {
      if (staff) {
        await updateStaff.mutateAsync({ id: staff.id, body: values })
        message.success('Đã lưu thay đổi nhân viên.')
      } else {
        await createStaff.mutateAsync(values)
        message.success('Đã thêm nhân viên.')
      }
      onClose()
    } catch (error) {
      if (!(error instanceof ApiError)) throw error
      if (error.code === 'STAFF_NAME_DUPLICATE') {
        setError('fullName', { message: error.message })
      } else if (error.fieldErrors.length > 0) {
        for (const fieldError of error.fieldErrors) {
          const field = fieldError.field as FieldPath<StaffFormValues>
          if (FIELDS.includes(field)) setError(field, { message: fieldError.message })
        }
      } else {
        message.error(error.message)
      }
    }
  })

  const statusOf = (field: FieldPath<StaffFormValues>) => (errors[field] ? 'error' : undefined)

  return (
    <Drawer
      title={isEdit ? 'Sửa nhân viên' : 'Thêm nhân viên'}
      open={open}
      onClose={onClose}
      size={screens.md ? 480 : '100%'}
      destroyOnHidden
      footer={
        <Space style={{ width: '100%', justifyContent: 'flex-end' }}>
          <Button onClick={onClose} size="large">
            Hủy
          </Button>
          <Button type="primary" size="large" loading={saving} onClick={() => onSubmit()}>
            {isEdit ? 'Lưu' : 'Thêm nhân viên'}
          </Button>
        </Space>
      }
    >
      <Form layout="vertical" size="large" onFinish={() => onSubmit()} component="form">
        <Form.Item
          label="Tên nhân viên"
          required
          validateStatus={statusOf('fullName')}
          help={
            errors.fullName?.message ??
            'Hai người đang làm không được trùng tên; trùng thì thêm chữ để phân biệt, ví dụ "Lan A".'
          }
        >
          <Controller
            name="fullName"
            control={control}
            render={({ field }) => (
              <Input {...field} id="staff-name" placeholder="Ví dụ: Lan" maxLength={100} />
            )}
          />
        </Form.Item>

        <Form.Item
          label="Vai trò"
          required
          validateStatus={statusOf('role')}
          help={errors.role?.message}
        >
          <Controller
            name="role"
            control={control}
            render={({ field }) => (
              <Select
                {...field}
                id="staff-role"
                options={Object.entries(STAFF_ROLE_LABELS).map(([value, label]) => ({
                  value,
                  label,
                }))}
              />
            )}
          />
        </Form.Item>

        {isEdit && !staff?.active && (
          <Alert type="warning" showIcon title="Nhân viên này đã nghỉ." />
        )}
        <button type="submit" hidden />
      </Form>
    </Drawer>
  )
}
