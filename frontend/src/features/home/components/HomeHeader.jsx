import { Link } from 'react-router-dom'

export default function HomeHeader() {
  return (
    <>
      <a className="home-skip" href="#noi-dung">Đến nội dung chính</a>
      <header className="home-header">
        <div className="home-container home-header-inner">
          <Link className="home-brand" to="/" aria-label="TLCN — Trang chủ"><span className="brand-symbol" aria-hidden="true">✳</span><span>TLCN<span className="brand-dot">.</span></span></Link>
          <nav className="home-nav" aria-label="Điều hướng chính"><a href="/#kham-pha">Khám phá</a><a href="/#thu-suc">Thử sức cùng Toán</a><a href="/#giao-vien">Dành cho giáo viên</a></nav>
          <Link className="home-button home-button-small" to="/register">Tạo tài khoản <span aria-hidden="true">↗</span></Link>
        </div>
      </header>
    </>
  )
}
