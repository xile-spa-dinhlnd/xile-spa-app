import { ArrowLeftOutlined, MailOutlined } from '@ant-design/icons'
import { Alert, Button, Card, Form, Input, Typography } from 'antd'
import { useState } from 'react'
import { useNavigate } from 'react-router'
import { ApiError, CLIENT_ERROR_CODES } from '@/shared/api/errors'
import { useForgotPassword } from '../api'
import type { ForgotPasswordRequest } from '../types'

/**
 * Trang yêu cầu đặt lại mật khẩu qua email (FR-AUTH-03, OQ-10).
 * Tuân thủ nguyên tắc chống User Enumeration: luôn phản hồi thành công
 * và hướng dẫn kiểm tra email cho người dùng.
 */
export function ForgotPasswordPage() {
  const navigate = useNavigate()
  const forgotPasswordMutation = useForgotPassword()
  const [submitted, setSubmitted] = useState(false)
  const [form] = Form.useForm<ForgotPasswordRequest>()

  const handleSubmit = (values: ForgotPasswordRequest) => {
    forgotPasswordMutation.mutate(values, {
      onSuccess: () => {
        setSubmitted(true)
      },
    })
  }

  // Chuyển đổi lỗi API thành thông báo tiếng Việt
  let errorMessage: string | null = null
  if (forgotPasswordMutation.isError) {
    const error = forgotPasswordMutation.error
    if (error instanceof ApiError) {
      if (error.code === CLIENT_ERROR_CODES.network) {
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
            Quên mật khẩu
          </Typography.Title>
          <Typography.Text type="secondary" style={{ fontSize: 13 }}>
            Xile Spa & Giãn cơ
          </Typography.Text>
        </div>

        {submitted ? (
          <div>
            <Alert
              type="success"
              showIcon
              title="Đã gửi yêu cầu thành công"
              description="Nếu email tồn tại trong hệ thống, chúng tôi đã gửi liên kết đặt lại mật khẩu. Vui lòng kiểm tra hộp thư (kể cả hòm thư rác/spam) trong vòng 30 phút."
              style={{ marginBottom: 24 }}
            />
            <Button
              type="primary"
              block
              onClick={() => navigate('/login')}
              style={{ height: 46, fontSize: 15, fontWeight: 600 }}
            >
              Quay lại trang đăng nhập
            </Button>
          </div>
        ) : (
          <Form
            form={form}
            name="forgot-password"
            layout="vertical"
            requiredMark={false}
            onFinish={handleSubmit}
            size="large"
          >
            <Typography.Paragraph type="secondary" style={{ fontSize: 14, marginBottom: 20 }}>
              Nhập email tài khoản của bạn. Chúng tôi sẽ gửi hướng dẫn và liên kết để đặt lại mật
              khẩu mới.
            </Typography.Paragraph>

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
              label="Email tài khoản"
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

            <Form.Item style={{ marginTop: 24, marginBottom: 12 }}>
              <Button
                type="primary"
                htmlType="submit"
                loading={forgotPasswordMutation.isPending}
                block
                style={{
                  height: 46,
                  fontSize: 15,
                  fontWeight: 600,
                }}
              >
                Gửi liên kết đặt lại mật khẩu
              </Button>
            </Form.Item>

            <div style={{ textAlign: 'center' }}>
              <Button
                type="link"
                icon={<ArrowLeftOutlined />}
                onClick={() => navigate('/login')}
                style={{ color: '#595959', fontSize: 13 }}
              >
                Quay lại đăng nhập
              </Button>
            </div>
          </Form>
        )}
      </Card>
    </div>
  )
}
