import { httpRequest } from '../../../shared/services/httpClient'

async function classroomRequest(path, options = {}) {
  const method = options.method || 'GET'
  const headers = { ...options.headers }
  if (!['GET', 'HEAD'].includes(method)) {
    const csrf = await httpRequest('/api/csrf')
    headers['X-XSRF-TOKEN'] = csrf.token
  }
  return httpRequest(path, { ...options, headers })
}

export const getClassroomSession = () => classroomRequest('/api/auth/registration-session')
export const getClassrooms = () => classroomRequest('/api/classrooms')
export const createClassroom = body => classroomRequest('/api/classrooms', { method: 'POST', body })
export const updateClassroom = (id, body) => classroomRequest(`/api/classrooms/${id}`, { method: 'PUT', body })
export const getClassroomTeachers = () => classroomRequest('/api/classrooms/teachers')
export const getClassroomStudents = (id, search = '') => classroomRequest(`/api/classrooms/${id}/students?search=${encodeURIComponent(search)}`)
export const findStudentCandidate = email => classroomRequest(`/api/students/candidates?email=${encodeURIComponent(email)}`)
export const enrollStudent = (classId, studentId) => classroomRequest(`/api/classrooms/${classId}/students`, { method: 'POST', body: { studentId } })
export const removeStudent = (classId, studentId) => classroomRequest(`/api/classrooms/${classId}/students/${studentId}`, { method: 'DELETE' })
export const updateStudent = (id, body) => classroomRequest(`/api/students/${id}`, { method: 'PATCH', body })
