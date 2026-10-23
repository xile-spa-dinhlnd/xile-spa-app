import { zodResolver } from '@hookform/resolvers/zod'
import { App, Button, Drawer, Form, Grid, Input, InputNumber, Typography } from 'antd'
import { useEffect, useState } from 'react'
import { Controller, useForm, type FieldPath } from 'react-hook-form'
import { ApiError } from '@/shared/api/errors'
import { useCreateServiceGroup, useUpdateServiceGroup } from '../api'
import { serviceGroupSchema, type ServiceGroupFormValues } from '../schemas'
import type { ServiceGroup } from '../types'
import { ServiceGroupList } from './ServiceGroupList'
import styles from './ServiceGroupDrawer.module.css'

interface ServiceGroupDrawerProps {
  open: boolean
  onClose: () => void
}

const EMPTY: ServiceGroupFormValues = {
  name: '',
  sortOrder: 0,
}

const FIELDS: FieldPath<ServiceGroupFormValues>[] = ['name', 'sortOrder']

/** Drawer quản lý nhóm dịch vụ (FR-SVC-05): form thêm, sửa ở trên, danh sách nhóm ở dưới. */
export function ServiceGroupDrawer({ open, onClose }: ServiceGroupDrawerProps) {
  const screens = Grid.useBreakpoint()
  const { message } = App.useApp()
  const [editingGroup, setEditingGroup] = useState<ServiceGroup | null>(null)

  const createGroup = useCreateServiceGroup()
  const updateGroup = useUpdateServiceGroup()
  const saving = createGroup.isPending || updateGroup.isPending

  const {
    control,
    handleSubmit,
    reset,
    setError,
    formState: { errors },
  } = useForm<ServiceGroupFormValues>({
    resolver: zodResolver(serviceGroupSchema),
    defaultValues: EMPTY,
  })

  useEffect(() => {
    if (editingGroup) {
      reset({ name: editingGroup.name, sortOrder: editingGroup.sortOrder })
    } else {
      reset(EMPTY)
    }
  }, [editingGroup, reset])

  const cancelEdit = () => {
    setEditingGroup(null)
    reset(EMPTY)
  }

  const handleClose = () => {
    cancelEdit()
    onClose()
  }

  const onSubmit = handleSubmit(async (values) => {
    const body = { name: values.name.trim(), sortOrder: values.sortOrder }
    try {
      if (editingGroup) {
        await updateGroup.mutateAsync({ id: editingGroup.id, body })
        message.success('Đã lưu thay đổi nhóm dịch vụ.')
        setEditingGroup(null)
        reset(EMPTY)
      } else {
        await createGroup.mutateAsync(body)
        message.success('Đã tạo nhóm dịch vụ.')
        reset(EMPTY)
      }
    } catch (error) {
      if (!(error instanceof ApiError)) throw error
      if (error.code === 'SERVICE_GROUP_NAME_DUPLICATE') {
        setError('name', { message: error.message })
      } else if (error.fieldErrors.length > 0) {
        for (const fe of error.fieldErrors) {
          const field = fe.field as FieldPath<ServiceGroupFormValues>
          if (FIELDS.includes(field)) setError(field, { message: fe.message })
        }
      } else {
        message.error(error.message)
      }
    }
  })

  const statusOf = (field: FieldPath<ServiceGroupFormValues>) =>
    errors[field] ? 'error' : undefined

  return (
    <Drawer
      title="Quản lý nhóm dịch vụ"
      open={open}
      onClose={handleClose}
      size={screens.md ? 480 : '100%'}
      destroyOnHidden
      footer={
        <div className={styles.footerActions}>
          <Button onClick={handleClose} size="large">
            Đóng
          </Button>
        </div>
      }
    >
      <div className={styles.formCard}>
        <Typography.Text className={styles.formTitle}>
          {editingGroup ? `Sửa nhóm: ${editingGroup.name}` : 'Thêm nhóm mới'}
        </Typography.Text>
        <Form layout="vertical" onFinish={() => onSubmit()} component="form">
          <Form.Item
            label="Tên nhóm"
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
                  id="group-name"
                  placeholder="Ví dụ: Gội đầu dưỡng sinh"
                  maxLength={100}
                />
              )}
            />
          </Form.Item>

          <Form.Item
            label="Thứ tự hiển thị"
            required
            validateStatus={statusOf('sortOrder')}
            help={errors.sortOrder?.message ?? 'Số nguyên không âm, số nhỏ đứng trước.'}
          >
            <Controller
              name="sortOrder"
              control={control}
              render={({ field }) => (
                <InputNumber<number>
                  id="group-sort-order"
                  aria-label="Thứ tự hiển thị"
                  className={styles.fullWidth}
                  value={field.value}
                  onChange={(v) => field.onChange(v)}
                  precision={0}
                  placeholder="0"
                />
              )}
            />
          </Form.Item>

          <div className={styles.formActions}>
            {editingGroup && (
              <Button onClick={cancelEdit} disabled={saving}>
                Hủy sửa
              </Button>
            )}
            <Button
              type="primary"
              id="group-submit-btn"
              loading={saving}
              onClick={() => onSubmit()}
            >
              {editingGroup ? 'Lưu' : 'Thêm nhóm'}
            </Button>
          </div>
          <button type="submit" hidden />
        </Form>
      </div>

      <Typography.Title level={5} className={styles.listHeader}>
        Danh sách nhóm
      </Typography.Title>
      <ServiceGroupList onEdit={setEditingGroup} />
    </Drawer>
  )
}
