import '../styles/registration.css'
import { Link } from 'react-router-dom'

export default function AuthLayout({ children }) {
  return (
    <div className="registration">
      <header className="registration-header">
        <Link to="/" className="registration-brand">TLCN<span>.</span></Link>
        <span>Học Toán lớp 2 · Tiến bộ từng ngày</span>
      </header>
      <main className="registration-main">
        <aside className="registration-intro">
          <span className="registration-eyebrow">CÙNG CON KHÁM PHÁ TOÁN HỌC</span>
          <h1>Mỗi bạn nhỏ,<br />một hành trình riêng.</h1>
          <p>Kết nối giáo viên và học sinh để từng bước xây dựng nền tảng Toán học vững vàng.</p>
          <div className="registration-math" aria-hidden="true"><span>2</span><span>+</span><span>3</span><span>=</span><span>5</span></div>
          <ul><li>Bắt đầu với tài khoản của bạn</li><li>Xác minh email để bảo vệ tài khoản</li><li>Đồng hành trên hành trình học tập</li></ul>
        </aside>
        <section className="registration-card" aria-label="Đăng ký tài khoản">{children}</section>
      </main>
      <footer className="registration-footer">TLCN · Nền tảng học tập cá nhân hóa cho học sinh tiểu học</footer>
    </div>
  )
}
