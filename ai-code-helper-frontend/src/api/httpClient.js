import axios from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081/api'
const ACCESS_TOKEN_KEY = 'ai_code_helper_access_token'
const REFRESH_TOKEN_KEY = 'ai_code_helper_refresh_token'

export class ApiBusinessError extends Error {
  constructor(message, code, payload) {
    super(message || 'Business API error')
    this.name = 'ApiBusinessError'
    this.code = code
    this.payload = payload
  }
}

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 8000
})

export function getStoredAccessToken() {
  return localStorage.getItem(ACCESS_TOKEN_KEY) || ''
}

export function getStoredRefreshToken() {
  return localStorage.getItem(REFRESH_TOKEN_KEY) || ''
}

export function setStoredAccessToken(token) {
  if (!token) return
  localStorage.setItem(ACCESS_TOKEN_KEY, token)
}

export function setStoredRefreshToken(token) {
  if (!token) return
  localStorage.setItem(REFRESH_TOKEN_KEY, token)
}

export function clearStoredAccessToken() {
  localStorage.removeItem(ACCESS_TOKEN_KEY)
}

export function clearStoredRefreshToken() {
  localStorage.removeItem(REFRESH_TOKEN_KEY)
}

export function clearStoredAuthTokens() {
  clearStoredAccessToken()
  clearStoredRefreshToken()
}

export function setAuthHeaderToken(token) {
  if (token) {
    apiClient.defaults.headers.common.Authorization = `Bearer ${token}`
  } else {
    delete apiClient.defaults.headers.common.Authorization
  }
}

export function persistAuthTokensFromPayload(data) {
  const accessToken = data?.accessToken || ''
  const refreshToken = data?.refreshToken || ''
  if (accessToken) {
    setStoredAccessToken(accessToken)
    setAuthHeaderToken(accessToken)
  }
  if (refreshToken) {
    setStoredRefreshToken(refreshToken)
  }
}

export function unwrapBusinessResponse(payload, fallbackMessage = 'API error') {
  if (payload && typeof payload === 'object' && Object.prototype.hasOwnProperty.call(payload, 'code')) {
    if (payload.code !== 0) {
      throw new ApiBusinessError(payload.message || fallbackMessage, payload.code, payload)
    }
    return payload.data
  }
  return payload
}

let refreshPromise = null

async function refreshAccessToken() {
  const refreshToken = getStoredRefreshToken()
  if (!refreshToken) {
    throw new Error('Missing refresh token')
  }
  const response = await apiClient.post(
    '/auth/refresh',
    { refreshToken },
    { timeout: 8000, _skipAuthRefresh: true }
  )
  const data = unwrapBusinessResponse(response.data, 'Refresh token failed')
  persistAuthTokensFromPayload(data)
  return data?.accessToken || ''
}

const bootstrapAccessToken = getStoredAccessToken()
if (bootstrapAccessToken) {
  setAuthHeaderToken(bootstrapAccessToken)
}

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const status = error?.response?.status
    const originalRequest = error?.config
    if (
      status === 401 &&
      originalRequest &&
      !originalRequest._retry &&
      !originalRequest._skipAuthRefresh
    ) {
      try {
        if (!refreshPromise) {
          refreshPromise = refreshAccessToken().finally(() => {
            refreshPromise = null
          })
        }
        const newAccessToken = await refreshPromise
        originalRequest._retry = true
        originalRequest.headers = originalRequest.headers || {}
        originalRequest.headers.Authorization = `Bearer ${newAccessToken}`
        return apiClient.request(originalRequest)
      } catch (refreshError) {
        clearStoredAuthTokens()
        setAuthHeaderToken('')
        return Promise.reject(refreshError)
      }
    }
    if (status === 401) {
      clearStoredAuthTokens()
      setAuthHeaderToken('')
    }
    return Promise.reject(error)
  }
)

export { API_BASE_URL, ACCESS_TOKEN_KEY, REFRESH_TOKEN_KEY }
