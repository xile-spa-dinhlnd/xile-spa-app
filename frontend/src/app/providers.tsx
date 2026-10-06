import { QueryClientProvider } from '@tanstack/react-query'
import { App as AntApp, ConfigProvider } from 'antd'
import viVN from 'antd/locale/vi_VN'
import dayjs from 'dayjs'
import 'dayjs/locale/vi'
import { useState, type ReactNode } from 'react'
import { createQueryClient } from './queryClient'

dayjs.locale('vi')

export function Providers({ children }: { children: ReactNode }) {
  const [queryClient] = useState(createQueryClient)
  return (
    <QueryClientProvider client={queryClient}>
      <ConfigProvider locale={viVN} theme={{ token: { colorPrimary: '#2f7d6d' } }}>
        <AntApp>{children}</AntApp>
      </ConfigProvider>
    </QueryClientProvider>
  )
}
