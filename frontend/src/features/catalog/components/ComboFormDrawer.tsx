import { zodResolver } from '@hookform/resolvers/zod'
import { Alert, App, Button, Drawer, Form, Grid, Input, Space } from 'antd'
import { useEffect } from 'react'
import { Controller, useForm, useWatch, type FieldPath } from 'react-hook-form'
import { ApiError } from '@/shared/api/errors'
import { MoneyInput } from '@/shared/components/MoneyInput'
import { useDebounce } from '@/shared/hooks/useDebounce'
import { useComboQuote, useCreateCombo, useUpdateCombo } from '../api'
import { comboFormSchema, comboItemSchema, type ComboFormValues } from '../schemas'
import type { Combo, ComboQuoteRequest } from '../types'
import { ComboItemsField } from './ComboItemsField'
import { ComboPriceSummary } from './ComboPriceSummary'
import styles from './ComboFormDrawer.module.css'

interface ComboFormDrawerProps {
  open: boolean
  combo?: Combo | null
  onClose: () => void
}

const EMPTY: ComboFormValues = {
  name: '',
  price: undefined as unknown as number,
  tourFee: null,
  items: [],
}

function toFormValues(combo?: Combo | null): ComboFormValues {
  if (!combo) return EMPTY
  return {
    name: combo.name,
    price: combo.price,
    tourFee: combo.tourFee,
    items: combo.items.map((i) => ({
      serviceId: i.serviceId,
      serviceName: i.serviceName,
      serviceActive: i.serviceActive,
      quantity: i.quantity,
    })),
  }
}

/** Form thêm và sửa combo (FR-CMB-01). Hiển thị lỗi từng ô và khối tính thử tổng giá lẻ/tiết kiệm. */
export function ComboFormDrawer({ open, combo, onClose }: ComboFormDrawerProps) {
  const isEdit = Boolean(combo)
  const screens = Grid.useBreakpoint()
  const { message } = App.useApp()
  const createCombo = useCreateCombo()
  const updateCombo = useUpdateCombo()
  const saving = createCombo.isPending || updateCombo.isPending

  const {
    control,
    handleSubmit,
    reset,
    setError,
    formState: { errors },
  } = useForm<ComboFormValues>({
    resolver: zodResolver(comboFormSchema),
    defaultValues: toFormValues(combo),
  })

  useEffect(() => {
    if (open) reset(toFormValues(combo))
  }, [open, combo, reset])

  const watchedPrice = useWatch({ control, name: 'price' })
  const priceChanged = isEdit && watchedPrice !== combo?.price
  const watchedItems = useWatch({ control, name: 'items' })
  const itemsList = watchedItems ?? []

  const itemCount = itemsList.length
  const hasInvalidQuantity =
    itemCount > 0 &&
    itemsList.some((item) => !comboItemSchema.shape.quantity.safeParse(item.quantity).success)

  const rawQuotePayloadString =
    itemCount === 0 || hasInvalidQuantity
      ? null
      : JSON.stringify({
          price:
            watchedPrice !== undefined && watchedPrice !== null && !isNaN(watchedPrice)
              ? watchedPrice
              : null,
          items: itemsList.map((i) => ({
            serviceId: i.serviceId,
            quantity: i.quantity,
          })),
        })

  const debouncedQuotePayloadString = useDebounce(rawQuotePayloadString, 300)
  const debouncedQuotePayload: ComboQuoteRequest | null = debouncedQuotePayloadString
    ? (JSON.parse(debouncedQuotePayloadString) as ComboQuoteRequest)
    : null

  const quoteQuery = useComboQuote(debouncedQuotePayload)

  const onSubmit = handleSubmit(async (values) => {
    const body = {
      name: values.name.trim(),
      price: values.price,
      tourFee: values.tourFee ?? null,
      items: values.items.map((i) => ({
        serviceId: i.serviceId,
        quantity: i.quantity,
      })),
    }

    try {
      if (combo) {
        await updateCombo.mutateAsync({ id: combo.id, body })
        message.success('Đã lưu thay đổi combo.')
      } else {
        await createCombo.mutateAsync(body)
        message.success('Đã thêm combo.')
      }
      onClose()
    } catch (error) {
      if (!(error instanceof ApiError)) {
        message.error('Không thực hiện được thao tác.')
        return
      }

      if (error.code === 'NOT_FOUND') {
        message.error('Không tìm thấy combo.')
      } else if (error.code === 'COMBO_NAME_DUPLICATE') {
        setError('name', { message: error.message })
      } else if (
        error.code === 'COMBO_ITEM_DUPLICATE' ||
        error.code === 'COMBO_SERVICE_UNAVAILABLE'
      ) {
        setError('items', { message: error.message })
      } else if (error.fieldErrors && error.fieldErrors.length > 0) {
        const unmatchedErrors: string[] = []
        for (const fieldError of error.fieldErrors) {
          const field = fieldError.field
          if (field === 'name') {
            setError('name', { message: fieldError.message })
          } else if (field === 'price') {
            setError('price', { message: fieldError.message })
          } else if (field === 'tourFee') {
            setError('tourFee', { message: fieldError.message })
          } else if (field === 'items') {
            setError('items', { message: fieldError.message })
          } else {
            const quantityMatch = field.match(/^items\[(\d+)\]\.quantity$/)
            if (quantityMatch) {
              const index = quantityMatch[1]
              setError(`items.${index}.quantity` as FieldPath<ComboFormValues>, {
                message: fieldError.message,
              })
            } else if (field.match(/^items\[\d+\]$/) || field.match(/^items\[\d+\]\.serviceId$/)) {
              setError('items', { message: fieldError.message })
            } else {
              unmatchedErrors.push(fieldError.message)
            }
          }
        }
        if (unmatchedErrors.length > 0) {
          message.error(unmatchedErrors.join(' '))
        }
      } else {
        message.error(error.message)
      }
    }
  })

  const itemsError = errors.items?.message ?? errors.items?.root?.message

  return (
    <Drawer
      title={isEdit ? 'Sửa combo' : 'Thêm combo'}
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
            {isEdit ? 'Lưu' : 'Tạo combo'}
          </Button>
        </Space>
      }
    >
      <Form layout="vertical" size="large" onFinish={() => onSubmit()} component="form">
        <Form.Item
          label="Tên combo"
          required
          validateStatus={errors.name ? 'error' : undefined}
          help={errors.name?.message}
        >
          <Controller
            name="name"
            control={control}
            render={({ field }) => (
              <Input
                {...field}
                id="combo-name"
                placeholder="Ví dụ: Combo Thư Giãn"
                maxLength={150}
              />
            )}
          />
        </Form.Item>

        <Form.Item
          label="Dịch vụ trong combo"
          required
          validateStatus={itemsError ? 'error' : undefined}
          help={itemsError}
        >
          <ComboItemsField control={control} errors={errors} quote={quoteQuery.data} />
        </Form.Item>

        <Form.Item
          label="Giá combo"
          required
          validateStatus={errors.price ? 'error' : undefined}
          help={
            errors.price?.message ??
            (priceChanged
              ? 'Đổi giá combo không ảnh hưởng các giao dịch đã ghi trước đó.'
              : undefined)
          }
        >
          <Controller
            name="price"
            control={control}
            render={({ field }) => (
              <MoneyInput
                id="combo-price"
                aria-label="Giá combo"
                value={field.value}
                onChange={field.onChange}
                onBlur={field.onBlur}
                status={errors.price ? 'error' : undefined}
                placeholder="0"
              />
            )}
          />
        </Form.Item>

        <Form.Item>
          <ComboPriceSummary
            quote={quoteQuery.data}
            isFetching={quoteQuery.isFetching}
            isError={quoteQuery.isError}
            onRetry={() => quoteQuery.refetch()}
            price={watchedPrice}
            itemCount={itemCount}
            hasInvalidQuantity={hasInvalidQuantity}
          />
        </Form.Item>

        <Form.Item
          label="Tiền tour mặc định"
          validateStatus={errors.tourFee ? 'error' : undefined}
          help={
            errors.tourFee?.message ??
            'Để trống nghĩa là 0. Áp dụng cho cả combo, không chia theo dịch vụ.'
          }
        >
          <Controller
            name="tourFee"
            control={control}
            render={({ field }) => (
              <MoneyInput
                id="combo-tour-fee"
                aria-label="Tiền tour mặc định"
                value={field.value}
                onChange={field.onChange}
                onBlur={field.onBlur}
                status={errors.tourFee ? 'error' : undefined}
                placeholder="0"
              />
            )}
          />
        </Form.Item>

        {isEdit && !combo?.active && (
          <Alert type="warning" showIcon title="Combo này đang ngừng bán." />
        )}

        <button type="submit" hidden />
      </Form>
    </Drawer>
  )
}
