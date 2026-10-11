import type { RouteObject } from 'react-router'
import { ServiceListPage } from './pages/ServiceListPage'

export const catalogRoutes: RouteObject[] = [{ path: '/services', element: <ServiceListPage /> }]
