import { LockOutlined, MailOutlined } from '@ant-design/icons'
import { Alert, Button, Form, Input } from 'antd'
import type { LoginRequest } from '../types'

interface LoginFormProps {
  onSubmit: (values: LoginRequest) => void
  isLoading?: boolean
  errorMessage?: string | null
}

/**
 * Form đăng nhập dành cho quản trị viên và chủ tiệm Xile Spa (FR-AUTH-01).
 * Tuân thủ NFR-USA-01: Chiều cao ô nhập và nút bấm chuẩn cảm ứng (touch target >= 44px).
 * Tuân thủ NFR-USA-02: Báo lỗi tiếng Việt thân thiện, rõ ràng.
 */
export function LoginForm({ onSubmit, isLoading = false, errorMessage }: LoginFormProps) {
  const [form] = Form.useForm<LoginRequest>()

  return (
    <Form
      form={form}
      name="login"
      layout="vertical"
      requiredMark={false}
      onFinish={onSubmit}
      autoComplete="on"
      size="large"
    >
      {errorMessage && (
        <Alert
          type="error"
          showIcon
          title={errorMessage}
          style={{ marginBottom: 20 }}
          role="alert"
        />
      )}

      <Form.Item
        name="email"
        label="Email"
        rules={[
          { required: true, message: 'Vui lòng nhập email.' },
          { type: 'email', message: 'Email không đúng định dạng.' },
        ]}
      >
        <Input
          prefix={<MailOutlined style={{ color: '#8c8c8c' }} />}
          placeholder="owner@xilespa.vn"
          autoComplete="email"
          inputMode="email"
          style={{ height: 44 }}
        />
      </Form.Item>

      <Form.Item
        name="password"
        label="Mật khẩu"
        rules={[{ required: true, message: 'Vui lòng nhập mật khẩu.' }]}
      >
        <Input.Password
          prefix={<LockOutlined style={{ color: '#8c8c8c' }} />}
          placeholder="Nhập mật khẩu"
          autoComplete="current-password"
          style={{ height: 44 }}
        />
      </Form.Item>

      <Form.Item style={{ marginTop: 28, marginBottom: 8 }}>
        <Button
          type="primary"
          htmlType="submit"
          loading={isLoading}
          block
          style={{
            height: 46,
            fontSize: 16,
            fontWeight: 600,
          }}
        >
          Đăng nhập
        </Button>
      </Form.Item>
    </Form>
  )
}
