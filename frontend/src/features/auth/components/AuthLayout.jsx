import '../styles/registration.css'
import { Link } from 'react-router-dom'
import MathGarden from '../../home/components/MathGarden'
import '../../home/styles/home.css'

export default function AuthLayout({ children, label = 'Đăng ký tài khoản' }) {
  return (
    <div className="registration">
      <a className="registration-skip" href="#auth-content">Đến nội dung chính</a>
      <header className="registration-header">
        <Link to="/" className="home-brand" aria-label="EduLearn — Trang chủ">
          <img className="brand-symbol" src="/math-bunny.svg" width="44" height="44" alt="" />
          <span>EduLearn<span className="brand-dot">.</span></span>
        </Link>
        <Link to="/" className="registration-back"><span aria-hidden="true">←</span> Về trang chủ</Link>
      </header>
      <main id="auth-content" className="registration-main">
        <aside className="registration-intro">
          <span className="registration-eyebrow"><span className="eyebrow-dot" /> CÙNG EM KHÁM PHÁ TOÁN LỚP 2</span>
          <h1>Mỗi bước nhỏ,<br />thêm một niềm <span>vui học.</span></h1>
          <p>Cùng những con số, bài toán nhỏ và thầy cô, em tự tin hơn trên hành trình của riêng mình.</p>
          <div className="registration-art"><MathGarden /></div>
          <p className="registration-encouragement"><span aria-hidden="true">♡</span> Không cần giỏi ngay. Chỉ cần vui học mỗi ngày.</p>
        </aside>
        <section className="registration-card" aria-label={label}>{children}</section>
      </main>
      <footer className="registration-footer"><span>EduLearn · Cùng thầy cô, hiểu từng em.</span><Link to="/#thu-suc">Thử một bài toán <span aria-hidden="true">↗</span></Link></footer>
    </div>
  )
}
