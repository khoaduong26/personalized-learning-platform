import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getRegistrationSession } from '../services/authApi'
import AuthLayout from '../components/AuthLayout'
import { rememberEmail } from '../utils/registrationStorage'

export default function GoogleRegistrationCompletePage() {
  const [user, setUser] = useState(null)
  const [error, setError] = useState('')
  useEffect(() => {
    const controller = new AbortController()
    getRegistrationSession(controller.signal)
      .then(data => { rememberEmail(''); setUser(data) })
      .catch(failure => { if (failure.name !== 'AbortError') setError(failure.message) })
    return () => controller.abort()
  }, [])
  return <AuthLayout>
    <span className="registration-step">ĐĂNG KÝ VỚI GOOGLE</span>
    <h2>{user ? `Xin chào, ${user.fullName}!` : 'Đang kiểm tra tài khoản…'}</h2>
    {user && <><p className="registration-notice success" role="status">Tài khoản {user.role === 'TEACHER' ? 'giáo viên' : 'học sinh'} đã được tạo và đăng nhập bằng Google.</p>
      <p className="registration-email">{user.email}</p><Link to="/" className="registration-primary registration-link">Về trang chủ</Link></>}
    {error && <><p className="registration-notice error" role="alert">{error}</p><Link to="/register" className="registration-text-link">Quay lại đăng ký</Link></>}
  </AuthLayout>
}
