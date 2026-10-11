import { DashboardOutlined, LogoutOutlined, MenuOutlined, UserOutlined } from '@ant-design/icons'
import {
  Avatar,
  Button,
  Drawer,
  Grid,
  Layout,
  Menu,
  Popconfirm,
  Space,
  Tag,
  Typography,
  type MenuProps,
} from 'antd'
import { useState } from 'react'
import { Outlet, useLocation, useNavigate } from 'react-router'
import { useCurrentUser, useLogout, type UserRole } from '@/features/auth'

const { Header, Sider, Content } = Layout

/** Mục menu. Mỗi tính năng mới thêm một mục ở đây cùng với route của nó. */
const menuItems: MenuProps['items'] = [
  { key: '/', icon: <DashboardOutlined />, label: 'Tổng quan' },
]

function getRoleLabel(role?: UserRole): { label: string; color: string } {
  switch (role) {
    case 'OWNER':
      return { label: 'Chủ tiệm', color: 'success' }
    case 'MANAGER':
      return { label: 'Quản lý', color: 'processing' }
    case 'STAFF':
      return { label: 'Nhân viên', color: 'default' }
    default:
      return { label: 'Người dùng', color: 'default' }
  }
}

/**
 * Khung trang dùng chung. Máy tính và iPad ngang (từ mốc `lg`): menu cố định bên trái. Điện thoại
 * và iPad dọc: menu ẩn trong ngăn kéo, mở bằng nút ☰ trên thanh tiêu đề.
 * Hiển thị thông tin phiên và nút đăng xuất có xác nhận (S1-03, FR-AUTH-02).
 */
export function AppLayout() {
  const screens = Grid.useBreakpoint()
  const isDesktop = screens.lg ?? true
  const [drawerOpen, setDrawerOpen] = useState(false)
  const navigate = useNavigate()
  const { pathname } = useLocation()

  const { data: user } = useCurrentUser()
  const logoutMutation = useLogout()

  const handleLogout = () => {
    logoutMutation.mutate(undefined, {
      onSuccess: () => {
        navigate('/login', { replace: true })
      },
    })
  }

  const roleInfo = getRoleLabel(user?.role)

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

  const logoutButton = (
    <Popconfirm
      title="Đăng xuất"
      description="Bạn có chắc chắn muốn đăng xuất khỏi hệ thống?"
      okText="Đăng xuất"
      cancelText="Hủy"
      okButtonProps={{ danger: true, loading: logoutMutation.isPending }}
      onConfirm={handleLogout}
    >
      <Button
        danger
        type="text"
        icon={<LogoutOutlined />}
        loading={logoutMutation.isPending}
        style={{ height: 40 }}
      >
        Đăng xuất
      </Button>
    </Popconfirm>
  )

  return (
    <Layout style={{ minHeight: '100vh' }}>
      {isDesktop && (
        <Sider theme="light" width={220}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '16px 20px' }}>
            <img
              src="/logo.png"
              alt="Xile Logo"
              style={{ height: 32, width: 'auto', objectFit: 'contain' }}
            />
            <Typography.Title level={4} style={{ margin: 0 }}>
              Xile Spa
            </Typography.Title>
          </div>
          {menu}
        </Sider>
      )}
      <Layout>
        <Header
          style={{
            background: '#ffffff',
            padding: isDesktop ? '0 24px' : '0 16px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            borderBottom: '1px solid #f0f0f0',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            {!isDesktop && (
              <Button
                type="text"
                icon={<MenuOutlined />}
                aria-label="Mở menu"
                style={{ height: 44, width: 44 }}
                onClick={() => setDrawerOpen(true)}
              />
            )}
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              {!isDesktop && (
                <img
                  src="/logo.png"
                  alt="Xile Logo"
                  style={{ height: 26, width: 'auto', objectFit: 'contain' }}
                />
              )}
              <Typography.Text strong style={{ fontSize: 16 }}>
                {isDesktop ? 'Quản trị tiệm' : 'Xile Spa'}
              </Typography.Text>
            </div>
          </div>

          {user && (
            <Space size={12} align="center">
              <Space size={8} align="center">
                <Avatar
                  size="default"
                  icon={<UserOutlined />}
                  style={{ backgroundColor: '#2b5a3e' }}
                />
                <Typography.Text strong>{user.displayName}</Typography.Text>
                {isDesktop && <Tag color={roleInfo.color}>{roleInfo.label}</Tag>}
              </Space>
              {logoutButton}
            </Space>
          )}
        </Header>
        <Content style={{ padding: isDesktop ? 24 : 16 }}>
          <Outlet />
        </Content>
      </Layout>
      {!isDesktop && (
        <Drawer
          title={
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <img
                src="/logo.png"
                alt="Xile Logo"
                style={{ height: 26, width: 'auto', objectFit: 'contain' }}
              />
              <span>Xile Spa</span>
            </div>
          }
          placement="left"
          size={260}
          open={drawerOpen}
          onClose={() => setDrawerOpen(false)}
          styles={{
            body: {
              padding: 0,
              display: 'flex',
              flexDirection: 'column',
              justifyContent: 'space-between',
            },
          }}
        >
          <div>
            {user && (
              <div
                style={{
                  padding: '16px 20px',
                  borderBottom: '1px solid #f0f0f0',
                  background: '#fafafa',
                }}
              >
                <Typography.Text strong style={{ display: 'block' }}>
                  {user.displayName}
                </Typography.Text>
                <div style={{ marginTop: 4 }}>
                  <Tag color={roleInfo.color}>{roleInfo.label}</Tag>
                </div>
              </div>
            )}
            {menu}
          </div>
          <div style={{ padding: 16, borderTop: '1px solid #f0f0f0' }}>{logoutButton}</div>
        </Drawer>
      )}
    </Layout>
  )
}
