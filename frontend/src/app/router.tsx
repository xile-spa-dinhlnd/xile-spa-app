import type { RouteObject } from 'react-router'
import { dashboardRoutes } from '@/features/dashboard'
import { AppLayout } from './layout/AppLayout'
import { NotFoundPage } from './NotFoundPage'

/**
 * Gom route của các tính năng. Trang đăng nhập và việc bảo vệ trang khi chưa đăng nhập thêm ở
 * S1-03 (FR-AUTH-01, 02).
 */
export const routes: RouteObject[] = [
  {
    element: <AppLayout />,
    children: [...dashboardRoutes, { path: '*', element: <NotFoundPage /> }],
  },
]
