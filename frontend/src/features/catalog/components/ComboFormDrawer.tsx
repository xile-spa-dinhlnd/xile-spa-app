import { zodResolver } from '@hookform/resolvers/zod'
import { App, Button, Drawer, Form, Grid, Input, Space } from 'antd'
import { useEffect } from 'react'
import { Controller, useForm, useWatch, type FieldPath } from 'react-hook-form'
import { ApiError } from '@/shared/api/errors'
import { MoneyInput } from '@/shared/components/MoneyInput'
import { useDebounce } from '@/shared/hooks/useDebounce'
import { useComboQuote, useCreateCombo } from '../api'
import { comboFormSchema, comboItemSchema, type ComboFormValues } from '../schemas'
import type { ComboQuoteRequest } from '../types'
import { ComboItemsField } from './ComboItemsField'
import { ComboPriceSummary } from './ComboPriceSummary'
import styles from './ComboFormDrawer.module.css'

interface ComboFormDrawerProps {
  open: boolean
  onClose: () => void
}

const EMPTY: ComboFormValues = {
  name: '',
  price: undefined as unknown as number,
  tourFee: null,
  items: [],
}

/** Form thêm combo (FR-CMB-01). Hiển thị lỗi từng ô và khối tính thử tổng giá lẻ/tiết kiệm. */
export function ComboFormDrawer({ open, onClose }: ComboFormDrawerProps) {
  const screens = Grid.useBreakpoint()
  const { message } = App.useApp()
  const createCombo = useCreateCombo()
  const saving = createCombo.isPending

  const {
    control,
    handleSubmit,
    reset,
    setError,
    formState: { errors },
  } = useForm<ComboFormValues>({
    resolver: zodResolver(comboFormSchema),
    defaultValues: EMPTY,
  })

  useEffect(() => {
    if (open) reset(EMPTY)
  }, [open, reset])

  const watchedPrice = useWatch({ control, name: 'price' })
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
      await createCombo.mutateAsync(body)
      message.success('Đã thêm combo.')
      onClose()
    } catch (error) {
      if (!(error instanceof ApiError)) {
        message.error('Không thực hiện được thao tác.')
        return
      }

      if (error.code === 'COMBO_NAME_DUPLICATE') {
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
      title="Thêm combo"
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
            Tạo combo
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
          help={errors.price?.message}
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

        <button type="submit" hidden />
      </Form>
    </Drawer>
  )
}
