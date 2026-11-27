import { Link, useNavigate } from 'react-router-dom'
import { useEffect, useRef, useState } from 'react'
import { logoutAccount } from '../../auth/services/authApi'
import { initials, roleNames } from '../../profile/utils/profile'
import ProfileIcon from '../../profile/components/ProfileIcon'
import { notifyAccountChanged } from '../../../shared/services/accountEvents'
import { profilePathFor } from '../../../shared/utils/profilePath'
import WorkspaceNavigation from '../../../shared/components/navigation/WorkspaceNavigation'
import '../styles/home.css'
import '../../profile/styles/profile.css'

function AccountAvatar({ user }) {
  return <span className="home-account-avatar"><span>{initials(user.fullName)}</span>
    {user.avatarUrl && <img key={user.avatarUrl} src={user.avatarUrl} alt="" onError={event => { event.currentTarget.style.display = 'none' }} />}
  </span>
}

export default function HomeHeader({ user, onLogout }) {
  const navigate = useNavigate()
  const menu = useRef(null)
  const [signingOut, setSigningOut] = useState(false)
  const [logoutError, setLogoutError] = useState('')
  useEffect(() => {
    function closeOutside(event) {
      if (!menu.current?.contains(event.target)) menu.current?.removeAttribute('open')
    }
    function escape(event) {
      if (event.key === 'Escape' && menu.current?.open) {
        menu.current.removeAttribute('open')
        menu.current.querySelector('summary')?.focus()
      }
    }
    document.addEventListener('pointerdown', closeOutside)
    document.addEventListener('keydown', escape)
    return () => {
      document.removeEventListener('pointerdown', closeOutside)
      document.removeEventListener('keydown', escape)
    }
  }, [])

  function closeMenu() {
    menu.current?.removeAttribute('open')
  }

  async function logout() {
    setSigningOut(true)
    setLogoutError('')
    try {
      await logoutAccount()
      onLogout()
      notifyAccountChanged()
      navigate('/login', { replace: true })
    } catch (error) {
      setLogoutError(error.message)
    } finally {
      setSigningOut(false)
    }
  }
  return (
    <>
      <a className="home-skip" href="#noi-dung">
        Đến nội dung chính
      </a>

      <header className="home-header">
        <div className="home-container home-header-inner">
          <Link className="home-brand" to="/" aria-label="EduLearn — Trang chủ">EduLearn</Link>
          {user ? <WorkspaceNavigation role={user.role} /> : (
            <nav className="home-nav" aria-label="Điều hướng chính">
              <Link to="/#thu-suc">Thử bài Toán</Link>
              <Link to="/#giao-vien">Dành cho thầy cô</Link>
            </nav>
          )}
          <div className="home-account-actions">
            {user ? <details ref={menu} className="home-account-menu">
              <summary aria-label={`Tài khoản của ${user.fullName}`}><AccountAvatar user={user} />
                <span className="home-account-label"><span className="home-account-name">{user.fullName}</span><small>{roleNames[user.role]}</small></span>
                <span className="home-account-chevron"><ProfileIcon name="chevron" /></span>
              </summary>
              <div className="home-account-dropdown">
                <div className="home-account-overview"><AccountAvatar user={user} /><div><strong>{user.fullName}</strong><span>{user.email}</span></div></div>
                <Link to={profilePathFor(user.role)} onClick={closeMenu}>
                  <ProfileIcon name="user" /><span>{user.role === 'TEACHER' ? 'Hồ sơ giáo viên' : 'Tài khoản của tôi'}</span><span className="home-account-link-arrow" aria-hidden="true">↗</span>
                </Link>
                <Link to="/" onClick={closeMenu}><ProfileIcon name="arrow" /><span>Về trang chủ</span></Link>
                <div className="home-account-menu-bottom"><button type="button" disabled={signingOut} onClick={logout}><ProfileIcon name="logout" />{signingOut ? 'Đang đăng xuất…' : 'Đăng xuất'}</button></div>
                {logoutError && <p className="home-account-error" role="alert">{logoutError}</p>}
              </div>
            </details> : <Link className="home-text-link" to="/login">Đăng nhập</Link>}
            {!user && <Link className="home-button home-button-small" to="/register">Tạo tài khoản <span aria-hidden="true">↗</span></Link>}
          </div>
        </div>
      </header>
    </>
  )
}
