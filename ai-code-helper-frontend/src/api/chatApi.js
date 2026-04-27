import { apiClient, API_BASE_URL } from './httpClient'

export function chatWithSSE(
  memoryId,
  message,
  roleId,
  onMessage,
  onError,
  onClose,
  currentProblemSlug = null,
  solvingMode = 'guided'
) {
  const params = new URLSearchParams({
    memoryId: String(memoryId),
    message,
    roleId,
    solvingMode
  })
  if (currentProblemSlug && currentProblemSlug.trim() !== '') {
    params.set('currentProblemSlug', currentProblemSlug.trim())
  }

  const eventSource = new EventSource(`${API_BASE_URL}/ai/chat?${params.toString()}`)
  let hasReceivedMessage = false

  eventSource.onmessage = (event) => {
    try {
      const data = event.data
      if (data && data.trim() !== '') {
        hasReceivedMessage = true
        onMessage(data)
      }
    } catch (error) {
      if (onError) onError(error)
    }
  }

  eventSource.onerror = (error) => {
    // SSE 在服务端正常关闭时通常也会触发 onerror。
    // 若已经收到过内容，按“正常结束”处理，避免误报连接失败。
    if (hasReceivedMessage) {
      if (onClose) onClose()
    } else if (onError) {
      onError(error)
    }

    if (eventSource.readyState !== EventSource.CLOSED) {
      eventSource.close()
    }
  }

  return eventSource
}

export async function checkServiceHealth() {
  try {
    const response = await apiClient.get('/health', { timeout: 5000 })
    if (response.status !== 200) return false
    const payload = response.data
    if (payload && typeof payload === 'object' && 'code' in payload) {
      return payload.code === 0
    }
    return true
  } catch (error) {
    console.error('Health check failed:', error)
    return false
  }
}

export async function fetchRoles() {
  const response = await apiClient.get('/roles', { timeout: 5000 })
  const payload = response.data
  const roleList = Array.isArray(payload?.data)
    ? payload.data
    : Array.isArray(payload)
      ? payload
      : []
  return roleList
    .map((role) => ({
      id: role.roleId,
      name: role.name,
      category: role.category || 'professional',
      tagline: role.tagline || '',
      description: role.description || '',
      avatar: role.avatar || '/characters/sakurajima-mai.png',
      avatarFallback: role.avatarFallback || role.avatar || '/characters/sakurajima-mai.png',
      image: role.image || role.avatar || '/characters/sakurajima-mai.png',
      imageFallback: role.imageFallback || role.avatarFallback || role.avatar || '/characters/sakurajima-mai.png',
      sortOrder: Number.isFinite(role.sortOrder) ? role.sortOrder : 9999
    }))
    .sort((a, b) => a.sortOrder - b.sortOrder)
}
