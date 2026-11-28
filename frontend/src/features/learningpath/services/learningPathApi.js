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
export const getLearningPaths = (studentId, classroomId) => learningPathRequest(
  `/api/students/${studentId}/learning-paths${classroomId ? `?classroomId=${classroomId}` : ''}`,
)
export const getStudentClassrooms = studentId => learningPathRequest(`/api/students/${studentId}/learning-paths/classrooms`)
export const generateLearningPath = (studentId, classroomId) => learningPathRequest(
  `/api/students/${studentId}/learning-paths/generate${classroomId ? `?classroomId=${classroomId}` : ''}`, { method: 'POST' },
)
export const updateLearningStep = (studentId, pathId, stepId, status) => learningPathRequest(
  `/api/students/${studentId}/learning-paths/${pathId}/steps/${stepId}`, { method: 'PATCH', body: { status } },
)
export const deleteLearningPath = (studentId, pathId) => learningPathRequest(
  `/api/students/${studentId}/learning-paths/${pathId}`, { method: 'DELETE' },
)
