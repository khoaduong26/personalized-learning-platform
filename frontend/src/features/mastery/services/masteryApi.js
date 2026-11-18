import { httpRequest } from '../../../shared/services/httpClient'

export const getMasteryProfile = studentId => httpRequest(`/api/students/${studentId}/mastery`)
export const getStudentErrors = studentId => httpRequest(`/api/students/${studentId}/mastery/errors`)
export const getKnowledgeGaps = studentId => httpRequest(`/api/students/${studentId}/mastery/gaps`)
