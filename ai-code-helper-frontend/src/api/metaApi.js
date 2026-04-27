import { apiClient, unwrapBusinessResponse } from './httpClient'

export async function fetchErrorCodeDictionary() {
  const response = await apiClient.get('/meta/error-codes', { timeout: 8000 })
  const data = unwrapBusinessResponse(response.data, 'Fetch error code dictionary failed')
  return Array.isArray(data) ? data : []
}
