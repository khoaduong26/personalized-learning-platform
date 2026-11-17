import { Link } from 'react-router-dom'
import { useEffect, useState } from 'react'
import { getProfile } from '../../profile/services/profileApi'
import { initials } from '../../profile/utils/profile'
import { ACCOUNT_CHANGED } from '../../../shared/services/accountEvents'
import { profilePathFor } from '../../../shared/utils/profilePath'
import '../../profile/styles/profile.css'

export default function HomeHeader() {
  const [user, setUser] = useState(null)
  useEffect(() => {
    const controller = new AbortController()
    function refresh() {
      getProfile(controller.signal).then(setUser).catch(error => { if (error.status === 401) setUser(null) })
    }
    refresh()
    window.addEventListener(ACCOUNT_CHANGED, refresh)
    return () => { controller.abort(); window.removeEventListener(ACCOUNT_CHANGED, refresh) }
  }, [])
  return (
    <>
      <a className="home-skip" href="#noi-dung">Đến nội dung chính</a>
      <header className="home-header">
        <div className="home-container home-header-inner">
          <Link className="home-brand" to="/" aria-label="EduLearn — Trang chủ"><img className="brand-symbol" src="/math-bunny.svg" width="44" height="44" alt="" /><span>EduLearn<span className="brand-dot">.</span></span></Link>
          <nav className="home-nav" aria-label="Điều hướng chính"><a href="/#kham-pha">Khám phá</a><a href="/#thu-suc">Thử sức cùng Toán</a><a href="/#giao-vien">Dành cho giáo viên</a></nav>
          <div className="home-account-actions">
            {user ? <details className="home-account-menu">
              <summary aria-label={`Tài khoản của ${user.fullName}`}><span className="home-account-avatar"><span>{initials(user.fullName)}</span>{user.avatarUrl && <img key={user.avatarUrl} src={user.avatarUrl} alt="" onError={event => { event.currentTarget.style.display = 'none' }} />}</span><span className="home-account-name">{user.fullName}</span><span aria-hidden="true">⌄</span></summary>
              <div className="home-account-dropdown">
                <Link to={profilePathFor(user.role)} onClick={event => event.currentTarget.closest('details').removeAttribute('open')}>
                  {user.role === 'TEACHER' ? 'Hồ sơ giáo viên' : 'Tài khoản của tôi'}
                </Link>
                <Link to="/" onClick={event => event.currentTarget.closest('details').removeAttribute('open')}>Về trang chủ</Link>
              </div>
            </details> : <Link className="home-text-link" to="/login">Đăng nhập</Link>}
            {!user && <Link className="home-button home-button-small" to="/register">Tạo tài khoản <span aria-hidden="true">↗</span></Link>}
          </div>
        </div>
      </header>
    </>
  )
}
