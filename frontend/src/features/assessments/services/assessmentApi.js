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

export const getAssessmentSession = () => request('/api/auth/registration-session')
export const getAssessments = () => request('/api/assessments')
export const getAssessment = id => request(`/api/assessments/${id}`)
export const getQuestionBank = lessonId => request(`/api/assessments/questions?lessonId=${lessonId}`)
export const createQuestion = body => request('/api/assessments/questions', { method: 'POST', body })
export const createAssessment = body => request('/api/assessments', { method: 'POST', body })
export const publishAssessment = id => request(`/api/assessments/${id}/publish`, { method: 'POST' })
export const getAssessmentClassrooms = () => request('/api/assessments/classrooms')
export const startAttempt = assessmentId => request(`/api/assessments/${assessmentId}/attempts`, { method: 'POST' })
export const saveAnswer = (attemptId, body) => request(`/api/attempts/${attemptId}/answers`, { method: 'PUT', body })
export const submitAttempt = attemptId => request(`/api/attempts/${attemptId}/submit`, { method: 'POST' })
export const getAttemptResult = attemptId => request(`/api/attempts/${attemptId}/result`)
export const getAttemptDraft = attemptId => request(`/api/attempts/${attemptId}/draft`)
export const getAttemptHistory = (studentId, classroomId) => request(
  `/api/attempts/student/${studentId}${classroomId ? `?classroomId=${classroomId}` : ''}`,
)
