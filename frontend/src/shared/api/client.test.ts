import { afterEach, describe, expect, it, vi } from 'vitest'
import { apiRequest, buildUrl } from './client'
import { ApiError, CLIENT_ERROR_CODES } from './errors'

function mockFetch(response: Response | Error) {
  const fn =
    response instanceof Error
      ? vi.fn().mockRejectedValue(response)
      : vi.fn().mockResolvedValue(response)
  vi.stubGlobal('fetch', fn)
  return fn
}

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('apiRequest', () => {
  it('gửi cookie phiên và trả dữ liệu JSON', async () => {
    const fetchMock = mockFetch(json({ status: 'UP' }))

    await expect(apiRequest('/actuator/health')).resolves.toEqual({ status: 'UP' })
    expect(fetchMock).toHaveBeenCalledWith(
      '/actuator/health',
      expect.objectContaining({ credentials: 'include', method: 'GET' }),
    )
  })

  it('gửi body dạng JSON', async () => {
    const fetchMock = mockFetch(json({ id: 1 }, 201))

    await apiRequest('/api/catalog/services', { method: 'POST', body: { name: 'Gội 30 phút' } })

    const init = fetchMock.mock.calls[0][1] as RequestInit
    expect(init.body).toBe('{"name":"Gội 30 phút"}')
    expect(init.headers).toMatchObject({ 'Content-Type': 'application/json' })
  })

  it('đọc lỗi theo định dạng chung của backend', async () => {
    mockFetch(
      json(
        {
          code: 'VALIDATION_FAILED',
          message: 'Dữ liệu không hợp lệ, vui lòng kiểm tra lại.',
          fieldErrors: [{ field: 'price', message: 'Giá không được âm' }],
          path: '/api/catalog/services',
          timestamp: '2026-10-06T10:00:00+07:00',
        },
        400,
      ),
    )

    const error = await apiRequest('/api/catalog/services').catch((e: unknown) => e)

    expect(error).toBeInstanceOf(ApiError)
    expect(error).toMatchObject({
      code: 'VALIDATION_FAILED',
      status: 400,
      fieldErrors: [{ field: 'price', message: 'Giá không được âm' }],
    })
    expect((error as ApiError).isClientError).toBe(true)
  })

  it('phản hồi lỗi không đúng định dạng thành lỗi chung', async () => {
    mockFetch(new Response('<html>502</html>', { status: 502 }))

    await expect(apiRequest('/api/x')).rejects.toMatchObject({
      code: CLIENT_ERROR_CODES.unexpected,
      status: 502,
    })
  })

  it('mất mạng thành lỗi NETWORK_ERROR có thông báo tiếng Việt', async () => {
    mockFetch(new TypeError('Failed to fetch'))

    await expect(apiRequest('/api/x')).rejects.toMatchObject({
      code: CLIENT_ERROR_CODES.network,
      message: expect.stringContaining('Không kết nối được máy chủ'),
    })
  })

  it('204 trả về undefined', async () => {
    mockFetch(new Response(null, { status: 204 }))

    await expect(apiRequest('/api/x', { method: 'DELETE' })).resolves.toBeUndefined()
  })

  it('gặp 401 trên API nghiệp vụ thì tự động làm mới phiên và thử lại thành công', async () => {
    const fetchMock = vi
      .fn()
      // Lần 1: API ban đầu bị 401
      .mockResolvedValueOnce(json({ code: 'UNAUTHORIZED', message: 'Token hết hạn' }, 401))
      // Lần 2: /api/auth/refresh thành công
      .mockResolvedValueOnce(json({ success: true }, 200))
      // Lần 3: Thử lại API ban đầu thành công
      .mockResolvedValueOnce(json({ id: 10, name: 'Gội đầu' }, 200))

    vi.stubGlobal('fetch', fetchMock)

    const result = await apiRequest<{ id: number; name: string }>('/api/catalog/services')

    expect(result).toEqual({ id: 10, name: 'Gội đầu' })
    expect(fetchMock).toHaveBeenCalledTimes(3)
    expect(fetchMock.mock.calls[1][0]).toBe('/api/auth/refresh')
    expect(fetchMock.mock.calls[2][0]).toBe('/api/catalog/services')
  })

  it('gặp 401 và làm mới phiên thất bại thì gọi callback hết phiên và ném lỗi 401', async () => {
    const onExpired = vi.fn()
    const { onSessionExpired } = await import('./client')
    const unsubscribe = onSessionExpired(onExpired)

    const fetchMock = vi
      .fn()
      // Lần 1: API ban đầu bị 401
      .mockResolvedValueOnce(json({ code: 'UNAUTHORIZED', message: 'Token hết hạn' }, 401))
      // Lần 2: /api/auth/refresh cũng bị 401
      .mockResolvedValueOnce(json({ code: 'UNAUTHORIZED', message: 'Refresh token hết hạn' }, 401))

    vi.stubGlobal('fetch', fetchMock)

    await expect(apiRequest('/api/catalog/services')).rejects.toMatchObject({
      status: 401,
    })

    expect(onExpired).toHaveBeenCalledTimes(1)
    unsubscribe()
  })

  it('không tự động làm mới phiên khi gọi /api/auth/login bị 401', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(json({ code: 'INVALID_CREDENTIALS', message: 'Sai mật khẩu' }, 401))
    vi.stubGlobal('fetch', fetchMock)

    await expect(
      apiRequest('/api/auth/login', {
        method: 'POST',
        body: { email: 'test@xile.vn', password: '123' },
      }),
    ).rejects.toMatchObject({
      status: 401,
    })

    expect(fetchMock).toHaveBeenCalledTimes(1)
  })
})

describe('buildUrl', () => {
  it('bỏ tham số rỗng', () => {
    expect(
      buildUrl('/api/catalog/services', { q: 'gội', active: true, page: undefined, tag: '' }),
    ).toBe('/api/catalog/services?q=g%E1%BB%99i&active=true')
  })
})
