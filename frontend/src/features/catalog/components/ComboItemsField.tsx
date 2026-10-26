import { CloseOutlined } from '@ant-design/icons'
import { Button, InputNumber, Select, Spin, Tag, Typography } from 'antd'
import { useState } from 'react'
import { Controller, useFieldArray, type Control, type FieldErrors } from 'react-hook-form'
import { useDebounce } from '@/shared/hooks/useDebounce'
import { formatMoney } from '@/shared/lib/money'
import { useServices } from '../api'
import { MAX_COMBO_ITEMS, type ComboFormValues } from '../schemas'
import type { ComboQuote } from '../types'
import styles from './ComboItemsField.module.css'

interface ComboItemsFieldProps {
  control: Control<ComboFormValues>
  errors: FieldErrors<ComboFormValues>
  quote?: ComboQuote
}

/**
 * Khu chọn và điều chỉnh danh sách dịch vụ thành phần của combo (FR-CMB-02).
 * Hỗ trợ tìm kiếm dịch vụ ở server, khóa dịch vụ đã chọn, điều chỉnh số lượng (1-99),
 * và hiển thị thành tiền từng dòng theo kết quả tính thử.
 */
export function ComboItemsField({ control, errors, quote }: ComboItemsFieldProps) {
  const { fields, append, remove } = useFieldArray({
    control,
    name: 'items',
  })

  const [searchText, setSearchText] = useState('')
  const debouncedSearch = useDebounce(searchText, 300)

  const servicesQuery = useServices({
    q: debouncedSearch.trim() || undefined,
    active: true,
    page: 0,
    size: 20,
  })

  const isMaxReached = fields.length >= MAX_COMBO_ITEMS

  const selectOptions = (servicesQuery.data?.content ?? []).map((s) => ({
    value: s.id,
    label: `${s.name} · ${formatMoney(s.listPrice)}`,
    disabled: fields.some((f) => f.serviceId === s.id),
  }))

  const handleSelectService = (serviceId: number) => {
    const s = servicesQuery.data?.content.find((item) => item.id === serviceId)
    if (s) {
      append({
        serviceId: s.id,
        serviceName: s.name,
        serviceActive: s.active,
        quantity: 1,
      })
    }
    setSearchText('')
  }

  const itemsError =
    errors.items?.message ||
    (errors.items as { root?: { message?: string } } | undefined)?.root?.message

  return (
    <div className={styles.container}>
      <div className={styles.selectWrapper}>
        <Select<number>
          id="combo-select-service"
          aria-label="Thêm dịch vụ vào combo"
          placeholder="Chọn dịch vụ để thêm"
          showSearch
          filterOption={false}
          searchValue={searchText}
          onSearch={setSearchText}
          value={undefined}
          onSelect={handleSelectService}
          disabled={isMaxReached}
          loading={servicesQuery.isPending}
          options={selectOptions}
          className={styles.selectInput}
          notFoundContent={
            servicesQuery.isPending ? (
              <div className={styles.spinContainer}>
                <Spin size="small" />
              </div>
            ) : servicesQuery.isError ? (
              <div className={styles.selectError}>
                <span>Không tải được danh sách dịch vụ</span>
                <Button type="link" size="small" onClick={() => servicesQuery.refetch()}>
                  Thử lại
                </Button>
              </div>
            ) : (
              <span>Không có dịch vụ đang bán khớp tên này</span>
            )
          }
        />

        {isMaxReached && <div className={styles.hintText}>Combo tối đa 20 dịch vụ.</div>}
        {itemsError && <div className={styles.errorText}>{itemsError}</div>}
      </div>

      {fields.length > 0 && (
        <div className={styles.itemList}>
          {fields.map((field, index) => {
            const quoteItem = quote?.items?.find((q) => q.serviceId === field.serviceId)
            const rowQtyError = errors.items?.[index]?.quantity?.message
            const isInactive = field.serviceActive === false

            return (
              <div key={field.id} className={styles.itemCard}>
                <div className={styles.itemHeader}>
                  <div className={styles.itemTitleGroup}>
                    <Typography.Text className={styles.itemName}>
                      {field.serviceName || `Dịch vụ #${field.serviceId}`}
                    </Typography.Text>
                    {isInactive && <Tag color="warning">Ngừng bán</Tag>}
                  </div>
                  <Button
                    type="text"
                    danger
                    icon={<CloseOutlined />}
                    aria-label={`Bỏ ${field.serviceName || 'dịch vụ'}`}
                    className={styles.removeButton}
                    onClick={() => remove(index)}
                  />
                </div>

                {isInactive && (
                  <div className={styles.warningText}>
                    Dịch vụ đã ngừng bán. Hãy bỏ hoặc thay dịch vụ này để lưu combo.
                  </div>
                )}

                <div className={styles.itemControls}>
                  <div className={styles.qtyWrapper}>
                    <Controller
                      name={`items.${index}.quantity`}
                      control={control}
                      render={({ field: qtyField }) => (
                        <InputNumber<number>
                          value={qtyField.value}
                          onChange={(v) => qtyField.onChange(v ?? null)}
                          onBlur={qtyField.onBlur}
                          min={1}
                          max={99}
                          precision={0}
                          inputMode="numeric"
                          aria-label={`Số lượng ${field.serviceName || 'dịch vụ'}`}
                          className={styles.qtyInput}
                          status={rowQtyError ? 'error' : undefined}
                        />
                      )}
                    />
                    {rowQtyError && <div className={styles.errorText}>{rowQtyError}</div>}
                  </div>

                  <div className={styles.itemCalculation}>
                    {quoteItem ? (
                      <span>
                        {formatMoney(quoteItem.listPrice)} × {field.quantity} ={' '}
                        <strong>{formatMoney(quoteItem.lineTotal)}</strong>
                      </span>
                    ) : (
                      '—'
                    )}
                  </div>
                </div>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
