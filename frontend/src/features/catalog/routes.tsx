import type { RouteObject } from 'react-router'
import { ComboListPage } from './pages/ComboListPage'
import { ServiceListPage } from './pages/ServiceListPage'

export const catalogRoutes: RouteObject[] = [
  { path: '/services', element: <ServiceListPage /> },
  { path: '/combos', element: <ComboListPage /> },
]
