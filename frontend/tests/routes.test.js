import assert from 'node:assert/strict'
import { after, before, test } from 'node:test'
import { createElement } from 'react'
import { renderToString } from 'react-dom/server'
import { MemoryRouter } from 'react-router-dom'
import { createServer } from 'vite'

let server
let AppRoutes
let ProfileForm
let PasswordForm

before(async () => {
  server = await createServer({ server: { middlewareMode: true }, appType: 'custom' })
  AppRoutes = (await server.ssrLoadModule('/src/app/routes/AppRoutes.jsx')).default
  ProfileForm = (await server.ssrLoadModule('/src/features/profile/components/ProfileForm.jsx')).default
  PasswordForm = (await server.ssrLoadModule('/src/features/profile/components/PasswordForm.jsx')).default
})

after(async () => { await server?.close() })

const routes = [
  ['/', 'Cùng thử một bài toán', 'home-header'],
  ['/register', 'Bắt đầu hành trình mới', 'registration-roles'],
  ['/register?google=exists', 'Email Google này đã có tài khoản', 'role="alert"'],
  ['/register?google=error', 'Chưa đăng ký được bằng Google', 'role="alert"'],
  ['/register?google=success', 'Đang kiểm tra tài khoản', 'ĐĂNG KÝ VỚI GOOGLE'],
  ['/register/complete', 'Tài khoản đã sẵn sàng!', 'Đến trang đăng nhập'],
  ['/login', 'Đăng nhập', 'Đăng nhập với Google'],
  ['/profile', 'Tài khoản của bạn', 'Đang tải thông tin tài khoản'],
  ['/does-not-exist', 'Không tìm thấy trang', 'home-header'],
]

for (const [url, heading, content] of routes) {
  test(`route ${url} renders its intended page`, () => {
    const html = renderToString(createElement(MemoryRouter, { initialEntries: [url] }, createElement(AppRoutes)))
    assert.ok(html.includes(heading), `Expected ${heading}`)
    assert.ok(html.includes(content), `Expected ${content}`)
    if (url.startsWith('/register') || url === '/login') assert.ok(!html.includes('home-header'))
    if (url === '/does-not-exist') assert.ok(!html.includes('home-hero-copy'))
  })
}

test('only student profiles expose gender and date of birth controls', () => {
  for (const role of ['STUDENT', 'TEACHER', 'ADMIN']) {
    const html = renderToString(createElement(ProfileForm, { user: { fullName: 'Tên mẫu', email: 'sample@example.invalid', role }, busy: false }))
    assert.equal(html.includes('id="profile-sex"'), role === 'STUDENT')
    assert.equal(html.includes('id="profile-dateOfBirth"'), role === 'STUDENT')
    assert.ok(html.includes('id="profile-email"'))
    assert.ok(html.includes('readOnly'))
    assert.ok(!html.includes('>Khác</option>'))
    assert.ok(html.includes('pattern="[0-9]{10}"'))
    assert.ok(html.includes('maxLength="10"'))
  }
})

test('Google profiles show Google security settings instead of a local password form', () => {
  const html = renderToString(createElement(PasswordForm, { allowed: false, busy: false }))
  assert.ok(html.includes('https://myaccount.google.com/security'))
  assert.ok(!html.includes('id="profile-currentPassword"'))
})

test('profile address controls keep unmatched legacy text visible and disable wards until a province is selected', () => {
  const html = renderToString(createElement(ProfileForm, {
    user: { fullName: 'Tên mẫu', email: 'sample@example.invalid', role: 'TEACHER', city: 'Tỉnh cũ', address: 'Số 12, Xã cũ, Huyện cũ' },
    busy: false,
  }))
  assert.equal((html.match(/role="combobox"/g) || []).length, 2)
  assert.ok(html.includes('value="Tỉnh cũ"'))
  assert.ok(html.includes('value="Số 12, Xã cũ, Huyện cũ"'))
  assert.match(html, /id="profile-ward"[^>]*disabled/)
})
