import { DashboardOutlined, MenuOutlined } from '@ant-design/icons'
import { Button, Drawer, Grid, Layout, Menu, Typography, type MenuProps } from 'antd'
import { useState } from 'react'
import { Outlet, useLocation, useNavigate } from 'react-router'

const { Header, Sider, Content } = Layout

/** Mục menu. Mỗi tính năng mới thêm một mục ở đây cùng với route của nó. */
const menuItems: MenuProps['items'] = [
  { key: '/', icon: <DashboardOutlined />, label: 'Tổng quan' },
]

/**
 * Khung trang dùng chung. Máy tính và iPad ngang (từ mốc `lg`): menu cố định bên trái. Điện thoại
 * và iPad dọc: menu ẩn trong ngăn kéo, mở bằng nút ☰ trên thanh tiêu đề.
 */
export function AppLayout() {
  const screens = Grid.useBreakpoint()
  const isDesktop = screens.lg ?? true
  const [drawerOpen, setDrawerOpen] = useState(false)
  const navigate = useNavigate()
  const { pathname } = useLocation()

  const menu = (
    <Menu
      mode="inline"
      selectedKeys={[pathname]}
      items={menuItems}
      style={{ borderInlineEnd: 'none' }}
      onClick={({ key }) => {
        navigate(key)
        setDrawerOpen(false)
      }}
    />
  )

  return (
    <Layout style={{ minHeight: '100vh' }}>
      {isDesktop && (
        <Sider theme="light" width={220}>
          <Typography.Title level={4} style={{ margin: 0, padding: '16px 24px' }}>
            Xile Spa
          </Typography.Title>
          {menu}
        </Sider>
      )}
      <Layout>
        <Header
          style={{
            background: 'transparent',
            padding: isDesktop ? '0 24px' : '0 16px',
            display: 'flex',
            alignItems: 'center',
            gap: 12,
          }}
        >
          {!isDesktop && (
            <Button
              type="text"
              icon={<MenuOutlined />}
              aria-label="Mở menu"
              onClick={() => setDrawerOpen(true)}
            />
          )}
          <Typography.Text strong>{isDesktop ? 'Quản trị tiệm' : 'Xile Spa'}</Typography.Text>
        </Header>
        <Content style={{ padding: isDesktop ? 24 : 16 }}>
          <Outlet />
        </Content>
      </Layout>
      {!isDesktop && (
        <Drawer
          title="Xile Spa"
          placement="left"
          size={260}
          open={drawerOpen}
          onClose={() => setDrawerOpen(false)}
          styles={{ body: { padding: 0 } }}
        >
          {menu}
        </Drawer>
      )}
    </Layout>
  )
}
