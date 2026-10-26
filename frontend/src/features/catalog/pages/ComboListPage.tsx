import { PlusOutlined } from '@ant-design/icons'
import { Button, Typography } from 'antd'
import { useState } from 'react'
import { ComboFormDrawer } from '../components/ComboFormDrawer'
import styles from './ComboListPage.module.css'

/**
 * Trang quản lý danh sách combo (S2-08).
 * Phần 5: Khung trang với tiêu đề và nút mở form thêm combo.
 * Danh sách, bảng, thẻ và bộ lọc sẽ hoàn tất ở Phần 6.
 */
export function ComboListPage() {
  const [drawerOpen, setDrawerOpen] = useState(false)

  return (
    <div className={styles.pageContainer}>
      <div className={styles.header}>
        <Typography.Title level={3} className={styles.title}>
          Combo
        </Typography.Title>
        <Button
          type="primary"
          size="large"
          icon={<PlusOutlined />}
          onClick={() => setDrawerOpen(true)}
          className={styles.addButton}
        >
          Thêm combo
        </Button>
      </div>

      <ComboFormDrawer open={drawerOpen} onClose={() => setDrawerOpen(false)} />
    </div>
  )
}
