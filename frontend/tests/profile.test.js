import assert from 'node:assert/strict'
import test from 'node:test'
import { httpRequest } from '../src/shared/services/httpClient.js'
import { uploadAvatar, changePassword } from '../src/features/profile/services/profileApi.js'
import { avatarError, profilePayload, validatePassword } from '../src/features/profile/utils/profile.js'

test('avatar upload sends FormData with a fresh CSRF token and browser-managed content type', async t => {
  const requests = []
  t.mock.method(globalThis, 'fetch', async (path, options) => {
    requests.push({ path, options })
    return Response.json(path.endsWith('/csrf') ? { headerName: 'X-CSRF-TOKEN', token: 'fresh-token' } : { avatarUrl: 'https://example.invalid/avatar.png' })
  })
  const file = new File(['image'], 'avatar.png', { type: 'image/png' })
  await uploadAvatar(file)
  assert.equal(requests[0].path, '/api/auth/csrf')
  assert.equal(requests[1].path, '/api/users/me/avatar')
  assert.equal(requests[1].options.body.get('file').name, 'avatar.png')
  assert.equal(requests[1].options.headers['Content-Type'], undefined)
  assert.equal(requests[1].options.headers['X-CSRF-TOKEN'], 'fresh-token')
  assert.equal(requests[1].options.credentials, 'include')
})

test('password update handles an empty success response and sends PUT with CSRF', async t => {
  const requests = []
  t.mock.method(globalThis, 'fetch', async (path, options) => {
    requests.push({ path, options })
    return path.endsWith('/csrf') ? Response.json({ headerName: 'X-CSRF-TOKEN', token: 'token' }) : new Response(null, { status: 204 })
  })
  const payload = { currentPassword: 'old password', newPassword: 'new password', confirmPassword: 'new password' }
  assert.equal(await changePassword(payload), null)
  assert.equal(requests[1].options.method, 'PUT')
  assert.deepEqual(JSON.parse(requests[1].options.body), payload)
})

test('profile payload excludes identity/security fields and limits student data to students', () => {
  const form = { fullName: '  Nguyễn An ', phone: '', address: '', city: ' Huế ', sex: 'FEMALE', dateOfBirth: '2018-05-20', role: 'ADMIN', email: 'other@example.invalid' }
  const common = { fullName: 'Nguyễn An', phone: null, address: null, city: 'Huế' }
  assert.deepEqual(profilePayload(form, 'ADMIN'), common)
  assert.deepEqual(profilePayload(form, 'TEACHER'), common)
  assert.deepEqual(profilePayload(form, 'STUDENT'), { ...common, sex: 'FEMALE', dateOfBirth: '2018-05-20' })
})

test('password confirmation and avatar limits give actionable errors', () => {
  assert.ok(validatePassword({ currentPassword: 'password123', newPassword: 'password123', confirmPassword: 'different' }).newPassword)
  assert.ok(validatePassword({ currentPassword: 'password123', newPassword: 'newPassword123', confirmPassword: 'different' }).confirmPassword)
  assert.deepEqual(validatePassword({ currentPassword: 'password123', newPassword: 'newPassword123', confirmPassword: 'newPassword123' }), {})
  assert.ok(avatarError({ type: 'image/svg+xml', size: 50 }))
  assert.ok(avatarError({ type: 'image/png', size: 2 * 1024 * 1024 + 1 }))
  assert.equal(avatarError({ type: 'image/png', size: 100 }), '')
})

test('expired profile sessions preserve HTTP 401 for the sign-in redirect', async t => {
  t.mock.method(globalThis, 'fetch', async () => Response.json({ code: 'UNAUTHORIZED', message: 'Bạn cần đăng nhập.' }, { status: 401 }))
  await assert.rejects(httpRequest('/api/users/me'), error => error.status === 401 && error.code === 'UNAUTHORIZED')
})
