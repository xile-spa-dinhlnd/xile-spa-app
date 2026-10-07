import { ApiError, isApiErrorBody } from './errors'

/**
 * Lớp gọi API duy nhất của frontend (`frontend/AGENTS.md`). Không gọi `fetch` trực tiếp trong
 * component. Phiên đăng nhập nằm trong cookie httpOnly nên luôn gửi kèm `credentials: 'include'`;
 * không bao giờ đọc hay lưu token ở đây.
 */

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? ''

type QueryValue = string | number | boolean | null | undefined

export interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'
  body?: unknown
  query?: Record<string, QueryValue>
  signal?: AbortSignal
  /** Đánh dấu nội bộ: yêu cầu này là lần thử lại sau khi làm mới phiên thành công. */
  _isRetry?: boolean
}

type SessionExpiredListener = () => void
const sessionExpiredListeners = new Set<SessionExpiredListener>()

/**
 * Đăng ký lắng nghe sự kiện phiên đăng nhập hết hạn hoàn toàn (khi làm mới phiên thất bại).
 * Trả về hàm hủy đăng ký.
 */
export function onSessionExpired(listener: SessionExpiredListener): () => void {
  sessionExpiredListeners.add(listener)
  return () => {
    sessionExpiredListeners.delete(listener)
  }
}

export function notifySessionExpired(): void {
  sessionExpiredListeners.forEach((fn) => {
    try {
      fn()
    } catch {
      // Bỏ qua lỗi trong listener để không ảnh hưởng các listener khác
    }
  })
}

let refreshPromise: Promise<boolean> | null = null

/**
 * Gọi làm mới phiên qua endpoint /api/auth/refresh.
 * Sử dụng promise lock để gom các yêu cầu 401 cùng lúc, tránh gọi xoay vòng token trùng lặp (BR-AUTH, FR-AUTH-02).
 */
export async function tryRefreshSession(): Promise<boolean> {
  if (refreshPromise) {
    return refreshPromise
  }
  refreshPromise = (async () => {
    try {
      const response = await fetch(buildUrl('/api/auth/refresh'), {
        method: 'POST',
        headers: { Accept: 'application/json' },
        credentials: 'include',
      })
      return response.ok
    } catch {
      return false
    } finally {
      refreshPromise = null
    }
  })()
  return refreshPromise
}

export function _resetRefreshStateForTests(): void {
  refreshPromise = null
  sessionExpiredListeners.clear()
}

export function buildUrl(path: string, query?: Record<string, QueryValue>): string {
  const params = new URLSearchParams()
  for (const [key, value] of Object.entries(query ?? {})) {
    if (value !== undefined && value !== null && value !== '') params.append(key, String(value))
  }
  const search = params.toString()
  return `${BASE_URL}${path}${search ? `?${search}` : ''}`
}

function isAuthBypassPath(path: string): boolean {
  return path === '/api/auth/login' || path === '/api/auth/refresh' || path === '/api/auth/logout'
}

export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = 'GET', body, query, signal, _isRetry } = options
  const headers: Record<string, string> = { Accept: 'application/json' }
  if (body !== undefined) headers['Content-Type'] = 'application/json'

  let response: Response
  try {
    response = await fetch(buildUrl(path, query), {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      credentials: 'include',
      signal,
    })
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') throw error
    throw ApiError.network()
  }

  // Tự động làm mới phiên (Silent Refresh) khi gặp HTTP 401 trên các API nghiệp vụ
  if (response.status === 401 && !_isRetry && !isAuthBypassPath(path)) {
    const refreshed = await tryRefreshSession()
    if (refreshed) {
      return apiRequest<T>(path, { ...options, _isRetry: true })
    }
    notifySessionExpired()
  }

  const data: unknown = await readJson(response)

  if (!response.ok) {
    if (isApiErrorBody(data)) {
      throw new ApiError(data.code, data.message, response.status, data.fieldErrors ?? [])
    }
    throw ApiError.unexpected(response.status)
  }
  return data as T
}

async function readJson(response: Response): Promise<unknown> {
  if (response.status === 204) return undefined
  const text = await response.text()
  if (!text) return undefined
  try {
    return JSON.parse(text)
  } catch {
    return undefined
  }
}

export const api = {
  get: <T>(path: string, options?: Omit<RequestOptions, 'method' | 'body'>) =>
    apiRequest<T>(path, { ...options, method: 'GET' }),
  post: <T>(path: string, body?: unknown, options?: Omit<RequestOptions, 'method' | 'body'>) =>
    apiRequest<T>(path, { ...options, method: 'POST', body }),
  put: <T>(path: string, body?: unknown, options?: Omit<RequestOptions, 'method' | 'body'>) =>
    apiRequest<T>(path, { ...options, method: 'PUT', body }),
  patch: <T>(path: string, body?: unknown, options?: Omit<RequestOptions, 'method' | 'body'>) =>
    apiRequest<T>(path, { ...options, method: 'PATCH', body }),
  delete: <T>(path: string, options?: Omit<RequestOptions, 'method' | 'body'>) =>
    apiRequest<T>(path, { ...options, method: 'DELETE' }),
}
