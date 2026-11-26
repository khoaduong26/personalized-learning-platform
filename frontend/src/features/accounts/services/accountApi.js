import { httpRequest } from '../../../shared/services/httpClient.js'

const path = '/api/admin/accounts'

export function listAccounts({ keyword, role, status, page }, signal) {
  const query = new URLSearchParams({ page, size: 20 })
  if (keyword) query.set('keyword', keyword)
  if (role) query.set('role', role)
  if (status) query.set('status', status)
  return httpRequest(`${path}?${query}`, { signal })
}

async function save(url, method, body) {
  const csrf = await httpRequest('/api/auth/csrf')
  return httpRequest(url, { method, body, headers: { [csrf.headerName]: csrf.token } })
}

export const createAccount = body => save(path, 'POST', body)
export const updateAccount = (id, body) => save(`${path}/${id}`, 'PUT', body)
export const setAccountLocked = (id, locked) => save(`${path}/${id}/status`, 'PATCH', { locked })
