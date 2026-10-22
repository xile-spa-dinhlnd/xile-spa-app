import { CheckCircleOutlined, LockOutlined, WarningOutlined } from '@ant-design/icons'
import { Alert, Button, Form, Input } from 'antd'
import { useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router'
import { ApiError, CLIENT_ERROR_CODES } from '@/shared/api/errors'
import { useResetPassword } from '../api'
import { AuthCard } from '../components/AuthCard'
import styles from './ResetPasswordPage.module.css'

interface ResetPasswordFormValues {
  newPassword: string
  confirmPassword: string
}

/**
 * Trang đặt lại mật khẩu bằng token xác thực (FR-AUTH-03, NFR-SEC-03).
 * Xác thực token từ query parameter, kiểm tra độ dài mật khẩu >= 10 ký tự,
 * và điều hướng người dùng sau khi hoàn tất.
 */
export function ResetPasswordPage() {
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token')
  const navigate = useNavigate()
  const resetPasswordMutation = useResetPassword()
  const [success, setSuccess] = useState(false)
  const [form] = Form.useForm<ResetPasswordFormValues>()

  const handleSubmit = (values: ResetPasswordFormValues) => {
    if (!token) return

    resetPasswordMutation.mutate(
      {
        token,
        newPassword: values.newPassword,
      },
      {
        onSuccess: () => {
          setSuccess(true)
        },
      },
    )
  }

  // Chuyển đổi lỗi API thành thông báo tiếng Việt
  let errorMessage: string | null = null
  if (resetPasswordMutation.isError) {
    const error = resetPasswordMutation.error
    if (error instanceof ApiError) {
      if (error.status === 400) {
        errorMessage =
          error.message ||
          'Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn (sau 30 phút). Vui lòng yêu cầu liên kết mới.'
      } else if (error.code === CLIENT_ERROR_CODES.network) {
        errorMessage = 'Không kết nối được máy chủ. Vui lòng kiểm tra lại mạng.'
      } else {
        errorMessage = error.message
      }
    } else {
      errorMessage = 'Đã xảy ra lỗi không xác định. Vui lòng thử lại sau.'
    }
  }

  return (
    <AuthCard title="Đặt lại mật khẩu" subtitle="Xile Spa & Giãn cơ">
      {!token ? (
        <div>
          <Alert
            type="warning"
            showIcon
            icon={<WarningOutlined />}
            title="Thiếu mã xác thực"
            description="Liên kết đặt lại mật khẩu này không hợp lệ hoặc thiếu mã xác thực. Vui lòng kiểm tra lại liên kết trong email hoặc gửi lại yêu cầu mới."
            className={styles.warningAlert}
          />
          <Button
            type="primary"
            block
            onClick={() => navigate('/forgot-password')}
            className={styles.actionButtonPrimary}
          >
            Yêu cầu liên kết mới
          </Button>
          <Button block onClick={() => navigate('/login')} className={styles.secondaryButton}>
            Về trang đăng nhập
          </Button>
        </div>
      ) : success ? (
        <div>
          <Alert
            type="success"
            showIcon
            icon={<CheckCircleOutlined />}
            title="Đặt lại mật khẩu thành công"
            description="Mật khẩu của bạn đã được cập nhật và tài khoản đã được mở khóa. Vui lòng đăng nhập bằng mật khẩu mới."
            className={styles.successAlert}
          />
          <Button
            type="primary"
            block
            onClick={() => navigate('/login')}
            className={styles.submitButton}
          >
            Đăng nhập ngay
          </Button>
        </div>
      ) : (
        <Form
          form={form}
          name="reset-password"
          layout="vertical"
          requiredMark={false}
          onFinish={handleSubmit}
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
            name="newPassword"
            label="Mật khẩu mới"
            extra="Tối thiểu 10 ký tự (tối đa 72 bytes)"
            rules={[
              { required: true, message: 'Vui lòng nhập mật khẩu mới.' },
              { min: 10, message: 'Mật khẩu mới phải có ít nhất 10 ký tự.' },
              {
                validator(_, value) {
                  if (value && new TextEncoder().encode(value).length > 72) {
                    return Promise.reject(new Error('Mật khẩu không được dài quá 72 bytes.'))
                  }
                  return Promise.resolve()
                },
              },
            ]}
          >
            <Input.Password
              prefix={<LockOutlined className={styles.inputIcon} />}
              placeholder="Nhập mật khẩu mới"
              autoComplete="new-password"
              className={styles.input}
            />
          </Form.Item>

          <Form.Item
            name="confirmPassword"
            label="Xác nhận mật khẩu mới"
            dependencies={['newPassword']}
            rules={[
              { required: true, message: 'Vui lòng xác nhận mật khẩu mới.' },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  if (!value || getFieldValue('newPassword') === value) {
                    return Promise.resolve()
                  }
                  return Promise.reject(new Error('Mật khẩu xác nhận không khớp.'))
                },
              }),
            ]}
          >
            <Input.Password
              prefix={<LockOutlined className={styles.inputIcon} />}
              placeholder="Nhập lại mật khẩu mới"
              autoComplete="new-password"
              className={styles.input}
            />
          </Form.Item>

          <Form.Item className={styles.submitItem}>
            <Button
              type="primary"
              htmlType="submit"
              loading={resetPasswordMutation.isPending}
              block
              className={styles.submitButton}
            >
              Lưu mật khẩu mới
            </Button>
          </Form.Item>

          <div className={styles.backButtonWrapper}>
            <Button type="link" onClick={() => navigate('/login')} className={styles.backButton}>
              Hủy và về trang đăng nhập
            </Button>
          </div>
        </Form>
      )}
    </AuthCard>
  )
}
