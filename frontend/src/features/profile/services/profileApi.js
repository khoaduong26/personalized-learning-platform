import { httpRequest } from '../../../shared/services/httpClient.js'

const profilePath = '/api/users/me'
export const getProfile = signal => httpRequest(profilePath, { signal })

async function save(path, method, body) {
  const csrf = await httpRequest('/api/auth/csrf')
  return httpRequest(path, { method, body, headers: { [csrf.headerName]: csrf.token } })
}

export const updateProfile = body => save(profilePath, 'PUT', body)
export const changePassword = body => save(`${profilePath}/password`, 'PUT', body)
export function uploadAvatar(file) {
  const body = new FormData()
  body.append('file', file)
  return save(`${profilePath}/avatar`, 'POST', body)
}
