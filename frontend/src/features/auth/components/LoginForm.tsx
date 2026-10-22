import { LockOutlined, MailOutlined } from '@ant-design/icons'
import { Alert, Button, Form, Input } from 'antd'
import type { LoginRequest } from '../types'
import styles from './LoginForm.module.css'

interface LoginFormProps {
  onSubmit: (values: LoginRequest) => void
  onForgotPassword?: () => void
  isLoading?: boolean
  errorMessage?: string | null
}

/**
 * Form đăng nhập dành cho quản trị viên và chủ tiệm Xile Spa (FR-AUTH-01).
 * Tuân thủ NFR-USA-01: Chiều cao ô nhập và nút bấm chuẩn cảm ứng (touch target >= 44px).
 * Tuân thủ NFR-USA-02: Báo lỗi tiếng Việt thân thiện, rõ ràng.
 */
export function LoginForm({
  onSubmit,
  onForgotPassword,
  isLoading = false,
  errorMessage,
}: LoginFormProps) {
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
          className={styles.errorAlert}
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
          prefix={<MailOutlined className={styles.inputIcon} />}
          placeholder="owner@xilespa.vn"
          autoComplete="email"
          inputMode="email"
          className={styles.input}
        />
      </Form.Item>

      <Form.Item
        name="password"
        label="Mật khẩu"
        rules={[{ required: true, message: 'Vui lòng nhập mật khẩu.' }]}
      >
        <Input.Password
          prefix={<LockOutlined className={styles.inputIcon} />}
          placeholder="Nhập mật khẩu"
          autoComplete="current-password"
          className={styles.input}
        />
      </Form.Item>

      <div className={styles.forgotPasswordWrapper}>
        <a
          href="/forgot-password"
          onClick={(e) => {
            e.preventDefault()
            onForgotPassword?.()
          }}
          className={styles.forgotPasswordLink}
        >
          Quên mật khẩu?
        </a>
      </div>

      <Form.Item className={styles.submitItem}>
        <Button
          type="primary"
          htmlType="submit"
          loading={isLoading}
          block
          className={styles.submitButton}
        >
          Đăng nhập
        </Button>
      </Form.Item>
    </Form>
  )
}
