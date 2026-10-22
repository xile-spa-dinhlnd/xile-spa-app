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
import styles from './AppLayout.module.css'

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
    <Layout className={styles.layout}>
      {isDesktop && (
        <Sider theme="light" width={220}>
          <div className={styles.siderBrand}>
            <img src="/logo.png" alt="Xile Logo" className={styles.siderLogo} />
            <Typography.Title level={4} className={styles.siderTitle}>
              Xile Spa
            </Typography.Title>
          </div>
          {menu}
        </Sider>
      )}
      <Layout>
        <Header className={styles.header}>
          <div className={styles.headerLeft}>
            {!isDesktop && (
              <Button
                type="text"
                icon={<MenuOutlined />}
                aria-label="Mở menu"
                className={styles.menuButton}
                onClick={() => setDrawerOpen(true)}
              />
            )}
            <div className={styles.headerBrand}>
              {!isDesktop && <img src="/logo.png" alt="Xile Logo" className={styles.headerLogo} />}
              <Typography.Text strong className={styles.headerTitle}>
                {isDesktop ? 'Quản trị tiệm' : 'Xile Spa'}
              </Typography.Text>
            </div>
          </div>

          {user && (
            <Dropdown menu={{ items: userMenuItems }} trigger={['click']} placement="bottomRight">
              <Button type="text" className={styles.userButton}>
                <Space size={8} align="center">
                  <Avatar size="default" icon={<UserOutlined />} className={styles.userAvatar} />
                  <Typography.Text strong>{user.displayName}</Typography.Text>
                  {isDesktop && <Tag color={roleInfo.color}>{roleInfo.label}</Tag>}
                  <DownOutlined className={styles.dropdownArrow} />
                </Space>
              </Button>
            </Dropdown>
          )}
        </Header>
        <Content className={styles.content}>
          <Outlet />
        </Content>
      </Layout>
      {!isDesktop && (
        <Drawer
          title={
            <div className={styles.drawerTitleBrand}>
              <img src="/logo.png" alt="Xile Logo" className={styles.drawerLogo} />
              <span>Xile Spa</span>
            </div>
          }
          placement="left"
          size={260}
          open={drawerOpen}
          onClose={() => setDrawerOpen(false)}
          rootClassName={styles.drawer}
          classNames={{
            body: styles.drawerBody,
          }}
        >
          <div>
            {user && (
              <div className={styles.drawerUserCard}>
                <Typography.Text strong className={styles.drawerUserName}>
                  {user.displayName}
                </Typography.Text>
                <div className={styles.drawerUserRole}>
                  <Tag color={roleInfo.color}>{roleInfo.label}</Tag>
                </div>
              </div>
            )}
            {menu}
          </div>
          <div className={styles.drawerFooter}>
            <Button
              type="text"
              icon={<KeyOutlined />}
              onClick={() => {
                setDrawerOpen(false)
                setChangePasswordOpen(true)
              }}
              className={styles.drawerActionButton}
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
              className={styles.drawerActionButton}
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
