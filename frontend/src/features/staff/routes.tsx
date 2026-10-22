import type { RouteObject } from 'react-router'
import { StaffListPage } from './pages/StaffListPage'

export const staffRoutes: RouteObject[] = [{ path: '/staff', element: <StaffListPage /> }]
