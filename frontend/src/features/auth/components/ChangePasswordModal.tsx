import { LockOutlined } from '@ant-design/icons'
import { Alert, App, Button, Form, Input, Modal, Space } from 'antd'
import { useState } from 'react'
import { ApiError, toFormFieldErrors } from '@/shared/api/errors'
import { useChangePassword } from '../api'
import styles from './ChangePasswordModal.module.css'

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
            if (error.code === 'PASSWORD_INCORRECT') {
              form.setFields([{ name: 'oldPassword', errors: [error.message] }])
            } else if (error.code === 'PASSWORD_POLICY_VIOLATION') {
              form.setFields([{ name: 'newPassword', errors: [error.message] }])
            } else if (error.fieldErrors.length > 0) {
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
        className={styles.form}
      >
        {serverError && (
          <Alert
            type="error"
            showIcon
            title={serverError}
            className={styles.errorAlert}
            role="alert"
          />
        )}

        <Form.Item
          name="oldPassword"
          label="Mật khẩu hiện tại"
          rules={[{ required: true, message: 'Vui lòng nhập mật khẩu hiện tại.' }]}
        >
          <Input.Password
            prefix={<LockOutlined className={styles.inputIcon} />}
            placeholder="Nhập mật khẩu hiện tại"
            autoComplete="current-password"
            className={styles.input}
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

        <Form.Item className={styles.footerItem}>
          <Space className={styles.footerActions}>
            <Button
              onClick={handleClose}
              disabled={changePasswordMutation.isPending}
              className={styles.cancelButton}
            >
              Hủy
            </Button>
            <Button
              type="primary"
              htmlType="submit"
              loading={changePasswordMutation.isPending}
              className={styles.submitButton}
            >
              Đổi mật khẩu
            </Button>
          </Space>
        </Form.Item>
      </Form>
    </Modal>
  )
}
