import type { ThemeConfig } from 'antd'

/**
 * Theme Ant Design. Giá trị màu phải khớp src/styles/tokens.css (Ant Design cần mã màu thật, không đọc var()).
 * Mục menu đang chọn dùng nền xanh đậm, chữ trắng để dễ nhìn (tương phản > 4.5:1).
 */
export const BRAND_COLOR = '#2f7d6d'

export const theme: ThemeConfig = {
  token: { colorPrimary: BRAND_COLOR },
  components: {
    Menu: {
      itemSelectedBg: BRAND_COLOR,
      itemSelectedColor: '#ffffff',
      itemHoverBg: 'rgba(47, 125, 109, 0.10)',
      itemActiveBg: 'rgba(47, 125, 109, 0.18)',
      itemHeight: 44,
      // Bỏ đường kẻ bên phải menu; class CSS không ghi đè được vì selector của Ant Design mạnh hơn (0,3,0).
      activeBarBorderWidth: 0,
    },
  },
}
