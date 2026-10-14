import type { RouteObject } from 'react-router'
import { PublicOnlyRoute } from './components/PublicOnlyRoute'
import { ForgotPasswordPage } from './pages/ForgotPasswordPage'
import { LoginPage } from './pages/LoginPage'
import { ResetPasswordPage } from './pages/ResetPasswordPage'

export const authRoutes: RouteObject[] = [
  {
    element: <PublicOnlyRoute />,
    children: [
      {
        path: '/login',
        element: <LoginPage />,
      },
      {
        path: '/forgot-password',
        element: <ForgotPasswordPage />,
      },
    ],
  },
  {
    path: '/reset-password',
    element: <ResetPasswordPage />,
  },
]
