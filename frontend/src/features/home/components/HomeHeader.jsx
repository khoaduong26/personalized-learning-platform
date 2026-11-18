
import { Link } from 'react-router-dom'
import { useEffect, useState } from 'react'
import { getCurrentUser } from '../../auth'

export default function HomeHeader() {
  const [user, setUser] = useState(null)

  useEffect(() => {
    const controller = new AbortController()

    getCurrentUser(controller.signal)
      .then(setUser)
      .catch(() => {})

    return () => controller.abort()
  }, [])

  return (
    <>
      <a className="home-skip" href="#noi-dung">
        Đến nội dung chính
      </a>

      <header className="home-header">
        <div className="home-container home-header-inner">
          <Link
            className="home-brand"
            to="/"
            aria-label="EduLearn — Trang chủ"
          >
            <img
              className="brand-symbol"
              src="/math-bunny.svg"
              width="44"
              height="44"
              alt=""
            />
            <span>
              EduLearn<span className="brand-dot">.</span>
            </span>
          </Link>

          <nav className="home-nav" aria-label="Điều hướng chính">
            <a href="/#kham-pha">Khám phá</a>
            <a href="/#thu-suc">Thử sức cùng Toán</a>
            <a href="/#giao-vien">Dành cho giáo viên</a>

            <Link to="/academic">
              Quản lý chương trình
            </Link>
          </nav>

          <div className="home-header-actions home-account-actions">
            <Link
              className="home-classrooms-link"
              to="/classrooms"
            >
              Lớp học
            </Link>

            <Link
              className="home-text-link"
              to="/login"
            >
              {user
                ? `Tài khoản: ${user.fullName}`
                : 'Đăng nhập'}
            </Link>

            {!user && (
              <Link
                className="home-button home-button-small"
                to="/register"
              >
                Tạo tài khoản
                <span aria-hidden="true">↗</span>
              </Link>
            )}
          </div>
        </div>
      </header>
    </>
  )
}
