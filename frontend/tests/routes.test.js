import assert from 'node:assert/strict'
import { after, before, test } from 'node:test'
import { createElement } from 'react'
import { renderToString } from 'react-dom/server'
import { MemoryRouter } from 'react-router-dom'
import { createServer } from 'vite'

let server
let AppRoutes

before(async () => {
  server = await createServer({ server: { middlewareMode: true }, appType: 'custom' })
  AppRoutes = (await server.ssrLoadModule('/src/app/routes/AppRoutes.jsx')).default
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
