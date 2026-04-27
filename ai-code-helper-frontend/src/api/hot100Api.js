import { apiClient, unwrapBusinessResponse } from './httpClient'

function asArray(data) {
  return Array.isArray(data) ? data : []
}

async function pollHot100Task(taskId, { timeoutMs = 8000, intervalMs = 250, maxWaitMs = 15000 } = {}) {
  const start = Date.now()
  while (Date.now() - start < maxWaitMs) {
    const detailResponse = await apiClient.get(`/hot100/tasks/${encodeURIComponent(taskId)}`, {
      timeout: timeoutMs
    })
    const detail = unwrapBusinessResponse(detailResponse.data, 'Hot100 task query failed')
    const status = detail?.status
    if (status === 'SUCCESS') {
      return detail?.result
    }
    if (status === 'FAILED') {
      throw new Error(detail?.errorMessage || 'Hot100 async task failed')
    }
    await new Promise((resolve) => setTimeout(resolve, intervalMs))
  }
  throw new Error('Hot100 async task timeout')
}

export async function fetchHot100Problems(params = {}) {
  const response = await apiClient.get('/hot100/problems', {
    params,
    timeout: 8000
  })
  return asArray(unwrapBusinessResponse(response.data, 'Hot100 list failed'))
}

export async function fetchHot100ProblemDetail(slug) {
  const response = await apiClient.get(`/hot100/problems/${encodeURIComponent(slug)}`, {
    timeout: 8000
  })
  return unwrapBusinessResponse(response.data, 'Hot100 detail failed')
}

export async function fetchHot100Tags() {
  const response = await apiClient.get('/hot100/tags', {
    timeout: 8000
  })
  return asArray(unwrapBusinessResponse(response.data, 'Hot100 tags failed'))
}

export async function upsertHot100Progress(payload) {
  const response = await apiClient.post('/hot100/progress', payload, {
    timeout: 8000
  })
  return unwrapBusinessResponse(response.data, 'Save progress failed')
}

export async function fetchHot100ProgressList() {
  const response = await apiClient.get('/hot100/progress', {
    timeout: 8000
  })
  return asArray(unwrapBusinessResponse(response.data, 'Progress list failed'))
}

export async function fetchHot100WeakTags() {
  const response = await apiClient.get('/hot100/weak-tags', {
    timeout: 8000
  })
  return asArray(unwrapBusinessResponse(response.data, 'Weak tag list failed'))
}

export async function fetchHot100Recommendations(limit = 5) {
  try {
    const submitResponse = await apiClient.post(
      '/hot100/tasks/recommendations',
      null,
      {
        params: { limit },
        timeout: 8000
      }
    )
    const submitData = unwrapBusinessResponse(submitResponse.data, 'Submit recommendation task failed')
    const result = await pollHot100Task(submitData?.taskId)
    return asArray(result)
  } catch (error) {
    const response = await apiClient.get('/hot100/recommendations', {
      params: { limit },
      timeout: 8000
    })
    return asArray(unwrapBusinessResponse(response.data, 'Recommendation query failed'))
  }
}

export async function fetchHot100StudyPlan(days = 7) {
  try {
    const submitResponse = await apiClient.post(
      '/hot100/tasks/study-plan',
      null,
      {
        params: { days },
        timeout: 8000
      }
    )
    const submitData = unwrapBusinessResponse(submitResponse.data, 'Submit study plan task failed')
    const result = await pollHot100Task(submitData?.taskId)
    return asArray(result)
  } catch (error) {
    const response = await apiClient.get('/hot100/study-plan', {
      params: { days },
      timeout: 8000
    })
    return asArray(unwrapBusinessResponse(response.data, 'Study plan query failed'))
  }
}

export async function fetchHot100TagMastery() {
  const response = await apiClient.get('/hot100/tag-mastery', {
    timeout: 8000
  })
  return asArray(unwrapBusinessResponse(response.data, 'Tag mastery failed'))
}

export async function fetchHot100DatasetStats() {
  const response = await apiClient.get('/hot100/dataset-stats', {
    timeout: 8000
  })
  return unwrapBusinessResponse(response.data, 'Dataset stats failed')
}
