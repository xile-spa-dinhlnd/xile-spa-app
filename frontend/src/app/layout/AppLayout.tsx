import {
  AppstoreOutlined,
  DashboardOutlined,
  DownOutlined,
  KeyOutlined,
  LogoutOutlined,
  MenuOutlined,
  TeamOutlined,
  UserOutlined,
} from '@ant-design/icons'
import {
  Avatar,
  Button,
  Drawer,
  Dropdown,
  Grid,
  Layout,
  Menu,
  Modal,
  Space,
  Tag,
  Typography,
  type MenuProps,
} from 'antd'
import { useState } from 'react'
import { Outlet, useLocation, useNavigate } from 'react-router'
import { ChangePasswordModal, useCurrentUser, useLogout, type UserRole } from '@/features/auth'

const { Header, Sider, Content } = Layout

/** Mục menu. Mỗi tính năng mới thêm một mục ở đây cùng với route của nó. */
const menuItems: MenuProps['items'] = [
  { key: '/', icon: <DashboardOutlined />, label: 'Tổng quan' },
  { key: '/services', icon: <AppstoreOutlined />, label: 'Dịch vụ' },
  { key: '/staff', icon: <TeamOutlined />, label: 'Nhân viên' },
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
 * Hiển thị thông tin phiên và menu tài khoản gồm đổi mật khẩu và đăng xuất (S1-03, S2-01).
 */
export function AppLayout() {
  const screens = Grid.useBreakpoint()
  const isDesktop = screens.lg ?? true
  const [drawerOpen, setDrawerOpen] = useState(false)
  const [changePasswordOpen, setChangePasswordOpen] = useState(false)
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

  const confirmLogout = () => {
    Modal.confirm({
      title: 'Đăng xuất',
      content: 'Bạn có chắc chắn muốn đăng xuất khỏi hệ thống?',
      okText: 'Đăng xuất',
      cancelText: 'Hủy',
      okButtonProps: { danger: true },
      onOk: handleLogout,
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

  const userMenuItems: MenuProps['items'] = [
    {
      key: 'change-password',
      icon: <KeyOutlined />,
      label: 'Đổi mật khẩu',
      onClick: () => setChangePasswordOpen(true),
    },
    {
      type: 'divider',
    },
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: 'Đăng xuất',
      danger: true,
      onClick: confirmLogout,
    },
  ]

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
            <Dropdown menu={{ items: userMenuItems }} trigger={['click']} placement="bottomRight">
              <Button
                type="text"
                style={{
                  height: 'auto',
                  padding: '6px 10px',
                  display: 'flex',
                  alignItems: 'center',
                  borderRadius: 8,
                }}
              >
                <Space size={8} align="center">
                  <Avatar
                    size="default"
                    icon={<UserOutlined />}
                    style={{ backgroundColor: '#2b5a3e' }}
                  />
                  <Typography.Text strong>{user.displayName}</Typography.Text>
                  {isDesktop && <Tag color={roleInfo.color}>{roleInfo.label}</Tag>}
                  <DownOutlined style={{ fontSize: 11, color: '#888' }} />
                </Space>
              </Button>
            </Dropdown>
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
          <div
            style={{
              padding: 16,
              borderTop: '1px solid #f0f0f0',
              display: 'flex',
              flexDirection: 'column',
              gap: 8,
            }}
          >
            <Button
              type="text"
              icon={<KeyOutlined />}
              onClick={() => {
                setDrawerOpen(false)
                setChangePasswordOpen(true)
              }}
              style={{ height: 40, justifyContent: 'flex-start' }}
            >
              Đổi mật khẩu
            </Button>
            <Button
              danger
              type="text"
              icon={<LogoutOutlined />}
              onClick={() => {
                setDrawerOpen(false)
                confirmLogout()
              }}
              style={{ height: 40, justifyContent: 'flex-start' }}
            >
              Đăng xuất
            </Button>
          </div>
        </Drawer>
      )}
      <ChangePasswordModal open={changePasswordOpen} onClose={() => setChangePasswordOpen(false)} />
    </Layout>
  )
}
