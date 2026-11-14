import assert from 'node:assert/strict'
import test from 'node:test'
import { httpRequest } from '../src/shared/services/httpClient.js'

test('HTTP client sends JSON and preserves cancellation signal', async t => {
  const controller = new AbortController()
  const fetchMock = t.mock.method(globalThis, 'fetch', async () => Response.json({ accepted: true }))
  const result = await httpRequest('/api/auth/register', {
    method: 'POST', body: { email: 'student@example.com' }, signal: controller.signal,
  })
  assert.deepEqual(result, { accepted: true })
  const [url, options] = fetchMock.mock.calls[0].arguments
  assert.equal(url, '/api/auth/register')
  assert.equal(options.method, 'POST')
  assert.equal(options.headers['Content-Type'], 'application/json')
  assert.deepEqual(JSON.parse(options.body), { email: 'student@example.com' })
  assert.equal(options.signal, controller.signal)
})

test('validation errors preserve backend field messages for forms', async t => {
  t.mock.method(globalThis, 'fetch', async () => Response.json({
    code: 'VALIDATION_ERROR', message: 'Thông tin chưa hợp lệ.', fieldErrors: { email: 'Email đã tồn tại.' },
  }, { status: 400 }))
  await assert.rejects(httpRequest('/api/auth/register'), error => {
    assert.equal(error.status, 400)
    assert.equal(error.code, 'VALIDATION_ERROR')
    assert.equal(error.message, 'Thông tin chưa hợp lệ.')
    assert.deepEqual(error.fieldErrors, { email: 'Email đã tồn tại.' })
    return true
  })
})

test('non-JSON backend errors still produce a useful form error', async t => {
  t.mock.method(globalThis, 'fetch', async () => new Response('Bad Gateway', { status: 502 }))
  await assert.rejects(httpRequest('/api/auth/options'), error => {
    assert.equal(error.status, 502)
    assert.deepEqual(error.fieldErrors, {})
    assert.match(error.message, /Vui lòng thử lại/)
    return true
  })
})

test('network failures preserve their cause and show a connection message', async t => {
  const cause = new TypeError('Failed to fetch')
  t.mock.method(globalThis, 'fetch', async () => { throw cause })
  await assert.rejects(httpRequest('/api/auth/options'), error => {
    assert.equal(error.cause, cause)
    assert.match(error.message, /Không kết nối được máy chủ/)
    return true
  })
})

test('aborted requests keep AbortError so unmounted pages can ignore it', async t => {
  const aborted = new DOMException('Aborted', 'AbortError')
  t.mock.method(globalThis, 'fetch', async () => { throw aborted })
  await assert.rejects(httpRequest('/api/auth/options'), error => error === aborted)
})

test('empty successful responses are accepted', async t => {
  t.mock.method(globalThis, 'fetch', async () => new Response(null, { status: 204 }))
  assert.equal(await httpRequest('/api/example'), null)
})

test('CSRF headers are preserved alongside the JSON content type', async t => {
  const fetchMock = t.mock.method(globalThis, 'fetch', async () => Response.json({ id: 1 }))
  await httpRequest('/api/auth/login', {
    method: 'POST', body: { email: 'student@example.com' }, headers: { 'X-CSRF-TOKEN': 'csrf-token' },
  })
  const options = fetchMock.mock.calls[0].arguments[1]
  assert.equal(options.headers['X-CSRF-TOKEN'], 'csrf-token')
  assert.equal(options.headers['Content-Type'], 'application/json')
})
