import { Link } from 'react-router-dom'
import AuthLayout from '../components/AuthLayout'

export default function RegistrationCompletePage() {
  return (
    <AuthLayout label="Hoàn tất đăng ký">
      <div className="registration-status-icon" aria-hidden="true">✓</div>
      <span className="registration-step">HOÀN TẤT ĐĂNG KÝ</span>
      <h2>Tài khoản đã sẵn sàng!</h2>
      <p className="registration-notice success" role="status">Đăng ký và xác minh tài khoản thành công.</p>
      <Link className="registration-primary registration-link" to="/login">Đến trang đăng nhập</Link>
      <Link className="registration-text-link" to="/">Về trang chủ</Link>
    </AuthLayout>
  )
}
