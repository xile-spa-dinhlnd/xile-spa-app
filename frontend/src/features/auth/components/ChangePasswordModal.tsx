import { LockOutlined } from '@ant-design/icons'
import { Alert, App, Button, Form, Input, Modal, Space } from 'antd'
import { useState } from 'react'
import { ApiError, toFormFieldErrors } from '@/shared/api/errors'
import { useChangePassword } from '../api'

interface ChangePasswordFormValues {
  oldPassword: string
  newPassword: string
  confirmPassword: string
}

interface ChangePasswordModalProps {
  open: boolean
  onClose: () => void
}

/**
 * Hộp thoại đổi mật khẩu cho người dùng đang đăng nhập (FR-AUTH-04).
 * Kiểm tra phía client và hiển thị lỗi từ server theo NFR-USA-02, NFR-SEC-03.
 */
export function ChangePasswordModal({ open, onClose }: ChangePasswordModalProps) {
  const [form] = Form.useForm<ChangePasswordFormValues>()
  const [serverError, setServerError] = useState<string | null>(null)
  const { message } = App.useApp()
  const changePasswordMutation = useChangePassword()

  const handleClose = () => {
    form.resetFields()
    setServerError(null)
    onClose()
  }

  const handleSubmit = async (values: ChangePasswordFormValues) => {
    setServerError(null)
    changePasswordMutation.mutate(
      {
        oldPassword: values.oldPassword,
        newPassword: values.newPassword,
      },
      {
        onSuccess: () => {
          message.success('Đổi mật khẩu thành công!')
          handleClose()
        },
        onError: (error) => {
          if (error instanceof ApiError) {
            if (error.fieldErrors.length > 0) {
              form.setFields(toFormFieldErrors(error) as Parameters<typeof form.setFields>[0])
            } else {
              setServerError(error.message)
            }
          } else {
            setServerError('Đổi mật khẩu thất bại. Vui lòng thử lại sau.')
          }
        },
      },
    )
  }

  return (
    <Modal
      title="Đổi mật khẩu"
      open={open}
      onCancel={handleClose}
      footer={null}
      destroyOnHidden
      centered
      width={460}
    >
      <Form
        form={form}
        layout="vertical"
        name="change-password"
        requiredMark={false}
        onFinish={handleSubmit}
        style={{ marginTop: 16 }}
      >
        {serverError && (
          <Alert
            type="error"
            showIcon
            title={serverError}
            style={{ marginBottom: 16 }}
            role="alert"
          />
        )}

        <Form.Item
          name="oldPassword"
          label="Mật khẩu hiện tại"
          rules={[{ required: true, message: 'Vui lòng nhập mật khẩu hiện tại.' }]}
        >
          <Input.Password
            prefix={<LockOutlined style={{ color: '#8c8c8c' }} />}
            placeholder="Nhập mật khẩu hiện tại"
            autoComplete="current-password"
            style={{ height: 44 }}
          />
        </Form.Item>

        <Form.Item
          name="newPassword"
          label="Mật khẩu mới"
          rules={[
            { required: true, message: 'Vui lòng nhập mật khẩu mới.' },
            { min: 10, message: 'Mật khẩu mới phải có tối thiểu 10 ký tự.' },
            {
              validator(_, value) {
                if (value && new TextEncoder().encode(value).length > 72) {
                  return Promise.reject(new Error('Mật khẩu không được dài quá 72 bytes.'))
                }
                return Promise.resolve()
              },
            },
          ]}
          extra="Tối thiểu 10 ký tự (tối đa 72 bytes). Không dùng mật khẩu quá phổ biến hoặc trùng mật khẩu cũ."
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

        <Form.Item style={{ marginBottom: 0, marginTop: 24 }}>
          <Space style={{ display: 'flex', justifyContent: 'flex-end', width: '100%' }}>
            <Button
              onClick={handleClose}
              disabled={changePasswordMutation.isPending}
              style={{ height: 40 }}
            >
              Hủy
            </Button>
            <Button
              type="primary"
              htmlType="submit"
              loading={changePasswordMutation.isPending}
              style={{ height: 40, minWidth: 120 }}
            >
              Đổi mật khẩu
            </Button>
          </Space>
        </Form.Item>
      </Form>
    </Modal>
  )
}
