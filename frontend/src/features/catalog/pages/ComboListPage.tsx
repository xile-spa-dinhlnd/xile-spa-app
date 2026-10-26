import { PlusOutlined } from '@ant-design/icons'
import { Button, Flex, Typography } from 'antd'
import { useState } from 'react'
import { ComboFormDrawer } from '../components/ComboFormDrawer'
import styles from './ComboListPage.module.css'

/**
 * Trang danh sách combo (FR-CMB-01).
 * Phần 5 chỉ cung cấp khung tiêu đề và nút Thêm combo để mở drawer.
 */
export function ComboListPage() {
  const [drawerOpen, setDrawerOpen] = useState(false)

  return (
    <div>
      <Flex
        justify="space-between"
        align="center"
        wrap="wrap"
        gap="middle"
        className={styles.header}
      >
        <Typography.Title level={3} className={styles.title}>
          Combo
        </Typography.Title>
        <Button
          type="primary"
          icon={<PlusOutlined />}
          size="large"
          onClick={() => setDrawerOpen(true)}
        >
          Thêm combo
        </Button>
      </Flex>

      <ComboFormDrawer open={drawerOpen} onClose={() => setDrawerOpen(false)} />
    </div>
  )
}
