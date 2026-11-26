import { httpRequest } from '../../../shared/services/httpClient'

async function request(path, options = {}) {
  const method = options.method || 'GET'
  const headers = { ...options.headers }
  if (!['GET', 'HEAD'].includes(method)) {
    const csrf = await httpRequest('/api/csrf')
    headers[csrf.headerName] = csrf.token
  }
  return httpRequest(path, { ...options, headers })
}

export const getStepExercises = stepId => request(`/api/exercises/steps/${stepId}`)
export const generateStepExercise = stepId => request(`/api/exercises/steps/${stepId}/generate`, { method: 'POST' })
export const getPendingExercises = () => request('/api/exercises/pending-review')
export const updateExercise = (questionId, body) => request(`/api/exercises/questions/${questionId}`, { method: 'PATCH', body })
export const reviewExercise = (questionId, status) => request(`/api/exercises/questions/${questionId}/review`, { method: 'PATCH', body: { status } })
export const startExerciseAttempt = stepId => request(`/api/exercises/steps/${stepId}/attempts`, { method: 'POST' })
export const saveExerciseAnswer = (attemptId, body) => request(`/api/attempts/${attemptId}/answers`, { method: 'PUT', body })
export const submitExerciseAttempt = attemptId => request(`/api/attempts/${attemptId}/submit`, { method: 'POST' })
export const getExerciseResult = attemptId => request(`/api/attempts/${attemptId}/result`)
