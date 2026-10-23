import { zodResolver } from '@hookform/resolvers/zod'
import { Alert, App, Button, Drawer, Form, Grid, Input, InputNumber, Select, Space } from 'antd'
import { useEffect } from 'react'
import { Controller, useForm, useWatch, type FieldPath } from 'react-hook-form'
import { MoneyInput } from '@/shared/components/MoneyInput'
import { ApiError } from '@/shared/api/errors'
import { useCreateService, useServiceGroups, useUpdateService } from '../api'
import { serviceFormSchema, type ServiceFormValues } from '../schemas'
import type { ServiceItem } from '../types'
import styles from './ServiceFormDrawer.module.css'

interface ServiceFormDrawerProps {
  open: boolean
  /** Dịch vụ đang sửa; không có nghĩa là tạo mới. */
  service?: ServiceItem | null
  onClose: () => void
}

const EMPTY: ServiceFormValues = {
  name: '',
  groupId: undefined as unknown as number,
  durationMinutes: null,
  listPrice: undefined as unknown as number,
  tourFee: null,
}

function toFormValues(service?: ServiceItem | null): ServiceFormValues {
  if (!service) return EMPTY
  return {
    name: service.name,
    groupId: service.groupId,
    durationMinutes: service.durationMinutes,
    listPrice: service.listPrice,
    tourFee: service.tourFee,
  }
}

const FIELDS: FieldPath<ServiceFormValues>[] = [
  'name',
  'groupId',
  'durationMinutes',
  'listPrice',
  'tourFee',
]

/** Form tạo và sửa dịch vụ (FR-SVC-02, 03). Lỗi hiển thị tại từng ô, kể cả lỗi từ backend. */
export function ServiceFormDrawer({ open, service, onClose }: ServiceFormDrawerProps) {
  const isEdit = Boolean(service)
  const screens = Grid.useBreakpoint()
  const { message } = App.useApp()
  const groups = useServiceGroups()
  const createService = useCreateService()
  const updateService = useUpdateService()
  const saving = createService.isPending || updateService.isPending

  // Dịch vụ đã ngừng bán có thể thuộc nhóm đã ngừng dùng (OQ-21): hiện tên nhóm thay cho số id, khóa không cho chọn.
  const groupOptions: { value: number; label: string; disabled?: boolean }[] = (
    groups.data ?? []
  ).map((g) => ({ value: g.id, label: g.name }))
  if (service && groups.data && !groups.data.some((g) => g.id === service.groupId)) {
    groupOptions.push({
      value: service.groupId,
      label: `${service.groupName} (ngừng dùng)`,
      disabled: true,
    })
  }

  const {
    control,
    handleSubmit,
    reset,
    setError,
    formState: { errors },
  } = useForm<ServiceFormValues>({
    resolver: zodResolver(serviceFormSchema),
    defaultValues: toFormValues(service),
  })

  useEffect(() => {
    if (open) reset(toFormValues(service))
  }, [open, service, reset])

  const listPrice = useWatch({ control, name: 'listPrice' })
  const priceChanged = isEdit && listPrice !== service?.listPrice

  const onSubmit = handleSubmit(async (values) => {
    const body = { ...values, name: values.name.trim() }
    try {
      if (service) {
        await updateService.mutateAsync({ id: service.id, body })
        message.success('Đã lưu thay đổi dịch vụ.')
      } else {
        await createService.mutateAsync(body)
        message.success('Đã tạo dịch vụ.')
      }
      onClose()
    } catch (error) {
      if (!(error instanceof ApiError)) throw error
      if (error.code === 'SERVICE_NAME_DUPLICATE') {
        setError('name', { message: error.message })
      } else if (error.code === 'SERVICE_GROUP_NOT_FOUND') {
        setError('groupId', { message: error.message })
      } else if (error.fieldErrors.length > 0) {
        for (const fieldError of error.fieldErrors) {
          const field = fieldError.field as FieldPath<ServiceFormValues>
          if (FIELDS.includes(field)) setError(field, { message: fieldError.message })
        }
      } else {
        message.error(error.message)
      }
    }
  })

  const statusOf = (field: FieldPath<ServiceFormValues>) => (errors[field] ? 'error' : undefined)

  return (
    <Drawer
      title={isEdit ? 'Sửa dịch vụ' : 'Thêm dịch vụ'}
      open={open}
      onClose={onClose}
      size={screens.md ? 480 : '100%'}
      destroyOnHidden
      footer={
        <Space className={styles.footerActions}>
          <Button onClick={onClose} size="large">
            Hủy
          </Button>
          <Button type="primary" size="large" loading={saving} onClick={() => onSubmit()}>
            {isEdit ? 'Lưu' : 'Tạo dịch vụ'}
          </Button>
        </Space>
      }
    >
      <Form layout="vertical" size="large" onFinish={() => onSubmit()} component="form">
        <Form.Item
          label="Tên dịch vụ"
          required
          validateStatus={statusOf('name')}
          help={errors.name?.message}
        >
          <Controller
            name="name"
            control={control}
            render={({ field }) => (
              <Input
                {...field}
                id="service-name"
                placeholder="Ví dụ: Gội 30 phút"
                maxLength={150}
              />
            )}
          />
        </Form.Item>

        <Form.Item
          label="Nhóm dịch vụ"
          required
          validateStatus={statusOf('groupId')}
          help={errors.groupId?.message}
        >
          <Controller
            name="groupId"
            control={control}
            render={({ field }) => (
              <Select
                {...field}
                id="service-group"
                placeholder="Chọn nhóm"
                loading={groups.isPending}
                options={groupOptions}
              />
            )}
          />
        </Form.Item>

        <Form.Item
          label="Giá niêm yết"
          required
          validateStatus={statusOf('listPrice')}
          help={
            errors.listPrice?.message ??
            (priceChanged
              ? 'Đổi giá niêm yết không ảnh hưởng các giao dịch đã ghi trước đó.'
              : 'Có thể để 0 nếu chưa có giá.')
          }
        >
          <Controller
            name="listPrice"
            control={control}
            render={({ field }) => (
              <MoneyInput
                id="service-price"
                aria-label="Giá niêm yết"
                value={field.value}
                onChange={field.onChange}
                onBlur={field.onBlur}
                status={statusOf('listPrice')}
                placeholder="0"
              />
            )}
          />
        </Form.Item>

        <Form.Item
          label="Tiền tour mặc định"
          validateStatus={statusOf('tourFee')}
          help={errors.tourFee?.message ?? 'Để trống nghĩa là 0.'}
        >
          <Controller
            name="tourFee"
            control={control}
            render={({ field }) => (
              <MoneyInput
                id="service-tour-fee"
                aria-label="Tiền tour"
                value={field.value}
                onChange={field.onChange}
                onBlur={field.onBlur}
                status={statusOf('tourFee')}
                placeholder="0"
              />
            )}
          />
        </Form.Item>

        <Form.Item
          label="Thời lượng (phút)"
          validateStatus={statusOf('durationMinutes')}
          help={errors.durationMinutes?.message}
        >
          <Controller
            name="durationMinutes"
            control={control}
            render={({ field }) => (
              <InputNumber<number>
                id="service-duration"
                aria-label="Thời lượng"
                value={field.value}
                onChange={(v) => field.onChange(v ?? null)}
                onBlur={field.onBlur}
                min={1}
                max={1440}
                precision={0}
                inputMode="numeric"
                placeholder="Ví dụ: 30"
                className={styles.fullWidth}
              />
            )}
          />
        </Form.Item>

        {isEdit && !service?.active && (
          <Alert type="warning" showIcon title="Dịch vụ này đang ngừng bán." />
        )}
        <button type="submit" hidden />
      </Form>
    </Drawer>
  )
}
