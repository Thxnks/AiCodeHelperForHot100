import {
  apiClient,
  clearStoredAuthTokens,
  getStoredRefreshToken,
  persistAuthTokensFromPayload,
  unwrapBusinessResponse
} from './httpClient'

export async function registerAuth(payload) {
  const response = await apiClient.post('/auth/register', payload, { timeout: 8000 })
  const data = unwrapBusinessResponse(response.data, 'Register failed')
  persistAuthTokensFromPayload(data)
  return data
}

export async function loginAuth(payload) {
  const response = await apiClient.post('/auth/login', payload, { timeout: 8000 })
  const data = unwrapBusinessResponse(response.data, 'Login failed')
  persistAuthTokensFromPayload(data)
  return data
}

export async function fetchMe() {
  const response = await apiClient.get('/auth/me', { timeout: 8000 })
  return unwrapBusinessResponse(response.data, 'Fetch user info failed')
}

export async function logoutAuth() {
  const refreshToken = getStoredRefreshToken()
  try {
    const response = await apiClient.post('/auth/logout', { refreshToken }, { timeout: 8000 })
    return unwrapBusinessResponse(response.data, 'Logout failed')
  } finally {
    clearStoredAuthTokens()
  }
}
