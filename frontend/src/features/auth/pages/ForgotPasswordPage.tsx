import { ArrowLeftOutlined, MailOutlined } from '@ant-design/icons'
import { Alert, Button, Form, Input, Typography } from 'antd'
import { useState } from 'react'
import { useNavigate } from 'react-router'
import { ApiError, CLIENT_ERROR_CODES } from '@/shared/api/errors'
import { useForgotPassword } from '../api'
import { AuthCard } from '../components/AuthCard'
import type { ForgotPasswordRequest } from '../types'
import styles from './ForgotPasswordPage.module.css'

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
    <AuthCard title="Quên mật khẩu" subtitle="Xile Spa & Giãn cơ">
      {submitted ? (
        <div>
          <Alert
            type="success"
            showIcon
            title="Đã gửi yêu cầu thành công"
            description="Nếu email tồn tại trong hệ thống, chúng tôi đã gửi liên kết đặt lại mật khẩu. Vui lòng kiểm tra hộp thư (kể cả hòm thư rác/spam) trong vòng 30 phút."
            className={styles.successAlert}
          />
          <Button
            type="primary"
            block
            onClick={() => navigate('/login')}
            className={styles.submitButton}
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
          className={styles.form}
        >
          <Typography.Paragraph type="secondary" className={styles.description}>
            Nhập email tài khoản của bạn. Chúng tôi sẽ gửi hướng dẫn và liên kết để đặt lại mật khẩu
            mới.
          </Typography.Paragraph>

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
            label="Email tài khoản"
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

          <Form.Item className={styles.submitItem}>
            <Button
              type="primary"
              htmlType="submit"
              loading={forgotPasswordMutation.isPending}
              block
              className={styles.submitButton}
            >
              Gửi liên kết đặt lại mật khẩu
            </Button>
          </Form.Item>

          <div className={styles.backButtonWrapper}>
            <Button
              type="link"
              icon={<ArrowLeftOutlined />}
              onClick={() => navigate('/login')}
              className={styles.backButton}
            >
              Quay lại đăng nhập
            </Button>
          </div>
        </Form>
      )}
    </AuthCard>
  )
}
