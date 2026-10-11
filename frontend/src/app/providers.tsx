import { QueryClientProvider } from '@tanstack/react-query'
import { App as AntApp, ConfigProvider, type ThemeConfig } from 'antd'
import viVN from 'antd/locale/vi_VN'
import dayjs from 'dayjs'
import 'dayjs/locale/vi'
import { useState, type ReactNode } from 'react'
import { createQueryClient } from './queryClient'

dayjs.locale('vi')

/** Màu thương hiệu. Mục menu đang chọn dùng nền xanh đậm, chữ trắng để dễ nhìn (tương phản > 4.5:1). */
const BRAND = '#2f7d6d'
const theme: ThemeConfig = {
  token: { colorPrimary: BRAND },
  components: {
    Menu: {
      itemSelectedBg: BRAND,
      itemSelectedColor: '#ffffff',
      itemHoverBg: 'rgba(47, 125, 109, 0.10)',
      itemActiveBg: 'rgba(47, 125, 109, 0.18)',
      itemHeight: 44,
    },
  },
}

export function Providers({ children }: { children: ReactNode }) {
  const [queryClient] = useState(createQueryClient)
  return (
    <QueryClientProvider client={queryClient}>
      <ConfigProvider locale={viVN} theme={theme}>
        <AntApp>{children}</AntApp>
      </ConfigProvider>
    </QueryClientProvider>
  )
}
