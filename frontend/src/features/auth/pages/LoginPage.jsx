import { Link } from 'react-router-dom'
import AuthLayout from '../components/AuthLayout'

export default function LoginPage() {
  return (
    <AuthLayout>
      <span className="registration-step">TÀI KHOẢN TLCN</span>
      <h2>Đăng nhập</h2>
      <p className="registration-description">Chức năng đăng nhập bằng email sẽ sớm có mặt. Bạn có thể sử dụng tài khoản đã xác minh khi chức năng này sẵn sàng.</p>
      <Link className="registration-primary registration-link" to="/">Về trang chủ</Link>
      <Link className="registration-text-link" to="/register">Đăng ký tài khoản</Link>
    </AuthLayout>
  )
}
