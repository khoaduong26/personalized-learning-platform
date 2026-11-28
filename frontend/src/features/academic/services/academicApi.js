import { httpRequest } from '../../../shared/services/httpClient'

async function academicRequest(path, options = {}) {
  const method = options.method || 'GET'
  const headers = { ...options.headers }
  if (method !== 'GET' && method !== 'HEAD') {
    const csrf = await httpRequest('/api/csrf')
    headers[csrf.headerName] = csrf.token
  }
  return httpRequest(path, { ...options, headers })
}

export const getAcademicSession = () => academicRequest('/api/auth/session')
export const getSubjects = () => academicRequest('/api/subjects')
export const createSubject = name => academicRequest('/api/subjects', { method: 'POST', body: { name } })
export const updateSubject = (id, name) => academicRequest(`/api/subjects/${id}`, { method: 'PUT', body: { name } })
export const deleteSubject = id => academicRequest(`/api/subjects/${id}`, { method: 'DELETE' })
export const copySubject = id => academicRequest(`/api/subjects/${id}/copy`, { method: 'POST', body: {} })
export const getTopics = subjectId => academicRequest(`/api/topics?subjectId=${subjectId}`)
export const createTopic = (name, subjectId) => academicRequest('/api/topics', { method: 'POST', body: { name, subjectId } })
export const updateTopic = (id, name, subjectId) => academicRequest(`/api/topics/${id}`, { method: 'PUT', body: { name, subjectId } })
export const deleteTopic = id => academicRequest(`/api/topics/${id}`, { method: 'DELETE' })
export const getLessons = topicId => academicRequest(`/api/lessons?topicId=${topicId}`)
const imagePayload = images => images?.map(({ url, altText, caption, placement, alignment, displaySize }) => ({ url, altText, caption, placement, alignment, displaySize }))
export const createLesson = (name, description, learningContent, images, topicId) => academicRequest('/api/lessons', { method: 'POST', body: { name, description, learningContent, images: imagePayload(images), topicId } })
export const updateLesson = (id, name, description, learningContent, images, topicId) => academicRequest(`/api/lessons/${id}`, { method: 'PUT', body: { name, description, learningContent, images: imagePayload(images), topicId } })
export const getLesson = lessonId => academicRequest(`/api/lessons/${lessonId}`)
export async function uploadLessonImage(lessonId, file) {
  const csrf = await httpRequest('/api/csrf')
  const body = new FormData()
  body.append('file', file)
  return httpRequest(`/api/lessons/${lessonId}/images`, {
    method: 'POST', body, headers: { [csrf.headerName]: csrf.token },
  })
}
export const deleteLesson = id => academicRequest(`/api/lessons/${id}`, { method: 'DELETE' })
