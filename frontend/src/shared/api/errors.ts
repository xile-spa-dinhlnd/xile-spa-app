/** Lỗi từ một ô nhập, khớp `ApiError.FieldError` của backend. */
export interface FieldError {
  field: string
  message: string
}

/** Định dạng lỗi chung mà mọi API của backend trả về (S0-04, `ApiError.java`). */
export interface ApiErrorBody {
  code: string
  message: string
  fieldErrors: FieldError[]
  path: string
  timestamp: string
}

/** Mã lỗi phía giao diện, khi không nhận được phản hồi đúng định dạng từ backend. */
export const CLIENT_ERROR_CODES = {
  network: 'NETWORK_ERROR',
  unexpected: 'UNEXPECTED_RESPONSE',
} as const

const NETWORK_MESSAGE = 'Không kết nối được máy chủ. Vui lòng kiểm tra mạng và thử lại.'
const UNEXPECTED_MESSAGE = 'Hệ thống gặp lỗi, vui lòng thử lại sau.'

/** Lỗi khi gọi API. Giao diện hiển thị `message`, xử lý theo `code`, báo lỗi từng ô bằng `fieldErrors`. */
export class ApiError extends Error {
  readonly code: string
  readonly status: number
  readonly fieldErrors: FieldError[]

  constructor(code: string, message: string, status: number, fieldErrors: FieldError[] = []) {
    super(message)
    this.name = 'ApiError'
    this.code = code
    this.status = status
    this.fieldErrors = fieldErrors
  }

  static network(): ApiError {
    return new ApiError(CLIENT_ERROR_CODES.network, NETWORK_MESSAGE, 0)
  }

  static unexpected(status: number): ApiError {
    return new ApiError(CLIENT_ERROR_CODES.unexpected, UNEXPECTED_MESSAGE, status)
  }

  get isUnauthorized(): boolean {
    return this.status === 401
  }

  /** Lỗi do dữ liệu người dùng gửi lên (4xx): thử lại cũng vô ích. */
  get isClientError(): boolean {
    return this.status >= 400 && this.status < 500
  }
}

export function isApiErrorBody(value: unknown): value is ApiErrorBody {
  if (typeof value !== 'object' || value === null) return false
  const body = value as Record<string, unknown>
  return typeof body.code === 'string' && typeof body.message === 'string'
}

/**
 * Chuyển lỗi từng ô sang dạng `form.setFields` của Ant Design Form.
 * Ví dụ: `form.setFields(toFormFieldErrors(error))`.
 */
export function toFormFieldErrors(error: ApiError): { name: string; errors: string[] }[] {
  const byField = new Map<string, string[]>()
  for (const { field, message } of error.fieldErrors) {
    byField.set(field, [...(byField.get(field) ?? []), message])
  }
  return [...byField].map(([name, errors]) => ({ name, errors }))
}
