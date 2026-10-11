import type { RouteObject } from 'react-router'
import { authRoutes, ProtectedRoute } from '@/features/auth'
import { dashboardRoutes } from '@/features/dashboard'
import { AppLayout } from './layout/AppLayout'
import { NotFoundPage } from './NotFoundPage'

/**
 * Gom route của các tính năng.
 * Trang đăng nhập (/login) là công khai, mọi trang quản trị còn lại được bảo vệ bằng ProtectedRoute (S1-03, FR-AUTH-01, 02).
 */
export const routes: RouteObject[] = [
  ...authRoutes,
  {
    element: <ProtectedRoute />,
    children: [
      {
        element: <AppLayout />,
        children: [...dashboardRoutes, { path: '*', element: <NotFoundPage /> }],
      },
    ],
  },
]
