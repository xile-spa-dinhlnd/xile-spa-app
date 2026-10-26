import { describe, expect, it } from 'vitest'
import { findViolations } from './check-css-tokens.mjs'

describe('findViolations', () => {
  it('cho qua khai báo dùng biến, số đo và từ khóa màu không cố định', () => {
    const css = `
.header {
  margin-bottom: var(--xl-space-lg);
  border: 1px solid var(--xl-color-border);
  color: inherit;
  background: transparent;
  outline-color: currentColor;
}
.header .title:hover {
  margin: 0;
}
@media (min-width: 768px) {
  .header { padding: var(--xl-space-xl); }
}
`

    expect(findViolations(css)).toEqual([])
  })

  it.each([
    ['mã màu 6 ký tự', '.a { color: #ff4d4f; }'],
    ['mã màu 3 ký tự', '.a { background: #fff; }'],
    ['mã màu trong viền', '.a { border: 1px solid #d46b08; }'],
    ['rgba trong đổ bóng', '.a { box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1); }'],
    ['hsl', '.a { color: hsl(0 100% 50%); }'],
    ['tên màu', '.a { color: red; }'],
    ['tên màu viết hoa trong viền', '.a { border-top: 1px solid White; }'],
    ['!important', '.title { margin: 0 !important; }'],
  ])('báo lỗi %s', (_name, css) => {
    expect(findViolations(css)).toHaveLength(1)
  })

  it('báo đúng số dòng của khai báo, kể cả khi trước đó có comment nhiều dòng', () => {
    const css = `/*
 * Ghi chú
 */
.a {
  margin: 0;
  color: #ff4d4f;
}`

    expect(findViolations(css)).toEqual([
      {
        line: 6,
        text: 'color: #ff4d4f',
        problem: 'không viết mã màu; dùng biến trong tokens.css hoặc token Ant Design',
      },
    ])
  })

  it('bỏ qua mã màu, !important nằm trong comment', () => {
    const css =
      '/* trước đây: color: #ff4d4f !important; */\n.a { color: var(--xl-color-heading); }'

    expect(findViolations(css)).toEqual([])
  })

  it('không coi tên lớp, tên biến có chữ màu là tên màu', () => {
    const css = '.redBox { color: var(--xl-color-text-red); background: var(--gray-1); }'

    expect(findViolations(css)).toEqual([])
  })

  it('báo khai báo cuối khối không có dấu chấm phẩy', () => {
    const css = '.a { margin: 0; color: #000 }'

    expect(findViolations(css)).toHaveLength(1)
  })
})
