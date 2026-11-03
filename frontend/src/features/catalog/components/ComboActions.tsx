import { EditOutlined } from '@ant-design/icons'
import { Button, Popconfirm, Space, Tooltip, Typography } from 'antd'
import type { Combo } from '../types'
import styles from './ComboActions.module.css'

export interface ComboActionsProps {
  combo: Combo
  onEdit: (combo: Combo) => void
  onToggleActive: (combo: Combo) => void
}

/**
 * Các nút hành động trên một dòng combo: Sửa, Ngừng bán, Bán lại (FR-CMB-04, FR-CMB-05).
 * Khi combo ngừng bán chứa dịch vụ đã ngừng bán: khóa nút Bán lại, có Tooltip và dòng cảnh báo bên dưới.
 */
export function ComboActions({ combo, onEdit, onToggleActive }: ComboActionsProps) {
  const inactiveServices = combo.items.filter((it) => !it.serviceActive)
  const hasInactiveServices = !combo.active && inactiveServices.length > 0
  const inactiveNames = inactiveServices.map((it) => it.serviceName).join(', ')
  const warningMessage = `Có dịch vụ đã ngừng bán: ${inactiveNames}. Hãy sửa combo để bỏ hoặc thay dịch vụ đó, hoặc bán lại dịch vụ trước.`

  const renderActiveButton = () => {
    if (combo.active) {
      return (
        <Popconfirm
          title="Ngừng bán combo?"
          description="Combo sẽ không còn để chọn khi nhập giao dịch. Giao dịch cũ không bị ảnh hưởng."
          okText="Ngừng bán"
          cancelText="Hủy"
          okButtonProps={{ danger: true }}
          onConfirm={() => onToggleActive(combo)}
        >
          <Button danger>Ngừng bán</Button>
        </Popconfirm>
      )
    }

    if (hasInactiveServices) {
      return (
        <Tooltip title={warningMessage}>
          <span>
            <Button disabled>Bán lại</Button>
          </span>
        </Tooltip>
      )
    }

    return (
      <Popconfirm
        title="Bán lại combo?"
        description="Combo sẽ xuất hiện lại khi nhập giao dịch."
        okText="Bán lại"
        cancelText="Hủy"
        onConfirm={() => onToggleActive(combo)}
      >
        <Button>Bán lại</Button>
      </Popconfirm>
    )
  }

  return (
    <div>
      <Space wrap>
        <Button icon={<EditOutlined />} onClick={() => onEdit(combo)}>
          Sửa
        </Button>
        {renderActiveButton()}
      </Space>
      {hasInactiveServices && (
        <div className={styles.warningLine}>
          <Typography.Text type="warning">{warningMessage}</Typography.Text>
        </div>
      )}
    </div>
  )
}
