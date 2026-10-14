import { CheckCircleOutlined, LockOutlined, WarningOutlined } from '@ant-design/icons'
import { Alert, Button, Card, Form, Input, Typography } from 'antd'
import { useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router'
import { ApiError, CLIENT_ERROR_CODES } from '@/shared/api/errors'
import { useResetPassword } from '../api'

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
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: 'linear-gradient(145deg, #f0f5f1 0%, #e6efe8 100%)',
        padding: '24px 16px',
      }}
    >
      <Card
        variant="borderless"
        style={{
          width: '100%',
          maxWidth: 420,
          borderRadius: 16,
          boxShadow: '0 8px 30px rgba(0, 0, 0, 0.06)',
          padding: '12px 8px',
        }}
      >
        <div style={{ textAlign: 'center', marginBottom: 20 }}>
          <img
            src="/logo.png"
            alt="Xile Beauty & Spa"
            style={{
              maxHeight: 100,
              maxWidth: '80%',
              objectFit: 'contain',
              marginBottom: 8,
              display: 'inline-block',
            }}
          />
          <Typography.Title level={4} style={{ margin: 0, color: '#1f2d24', fontWeight: 600 }}>
            Đặt lại mật khẩu
          </Typography.Title>
          <Typography.Text type="secondary" style={{ fontSize: 13 }}>
            Xile Spa & Giãn cơ
          </Typography.Text>
        </div>

        {!token ? (
          <div>
            <Alert
              type="warning"
              showIcon
              icon={<WarningOutlined />}
              title="Thiếu mã xác thực"
              description="Liên kết đặt lại mật khẩu này không hợp lệ hoặc thiếu mã xác thực. Vui lòng kiểm tra lại liên kết trong email hoặc gửi lại yêu cầu mới."
              style={{ marginBottom: 20 }}
            />
            <Button
              type="primary"
              block
              onClick={() => navigate('/forgot-password')}
              style={{ height: 46, fontSize: 15, fontWeight: 600, marginBottom: 10 }}
            >
              Yêu cầu liên kết mới
            </Button>
            <Button block onClick={() => navigate('/login')} style={{ height: 42, fontSize: 14 }}>
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
              style={{ marginBottom: 24 }}
            />
            <Button
              type="primary"
              block
              onClick={() => navigate('/login')}
              style={{ height: 46, fontSize: 15, fontWeight: 600 }}
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
                style={{ marginBottom: 20 }}
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
                prefix={<LockOutlined style={{ color: '#8c8c8c' }} />}
                placeholder="Nhập mật khẩu mới"
                autoComplete="new-password"
                style={{ height: 44 }}
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
                prefix={<LockOutlined style={{ color: '#8c8c8c' }} />}
                placeholder="Nhập lại mật khẩu mới"
                autoComplete="new-password"
                style={{ height: 44 }}
              />
            </Form.Item>

            <Form.Item style={{ marginTop: 24, marginBottom: 12 }}>
              <Button
                type="primary"
                htmlType="submit"
                loading={resetPasswordMutation.isPending}
                block
                style={{
                  height: 46,
                  fontSize: 15,
                  fontWeight: 600,
                }}
              >
                Lưu mật khẩu mới
              </Button>
            </Form.Item>

            <div style={{ textAlign: 'center' }}>
              <Button
                type="link"
                onClick={() => navigate('/login')}
                style={{ color: '#595959', fontSize: 13 }}
              >
                Hủy và về trang đăng nhập
              </Button>
            </div>
          </Form>
        )}
      </Card>
    </div>
  )
}
