import { httpRequest } from '../../../shared/services/httpClient'

async function learningPathRequest(path, options = {}) {
  const method = options.method || 'GET'
  const headers = { ...options.headers }
  if (!['GET', 'HEAD'].includes(method)) {
    const csrf = await httpRequest('/api/csrf')
    headers[csrf.headerName] = csrf.token
  }
  return httpRequest(path, { ...options, headers })
}

export const getMySession = () => httpRequest('/api/auth/me')
export const getLearningPaths = studentId => learningPathRequest(`/api/students/${studentId}/learning-paths`)
export const generateLearningPath = studentId => learningPathRequest(
  `/api/students/${studentId}/learning-paths/generate`, { method: 'POST' },
)
