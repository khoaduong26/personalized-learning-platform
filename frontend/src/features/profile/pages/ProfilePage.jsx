import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { logoutAccount } from '../../auth/services/authApi'
import { notifyAccountChanged } from '../../../shared/services/accountEvents'
import { changePassword, getProfile, updateProfile, uploadAvatar } from '../services/profileApi'
import { roleNames } from '../utils/profile'
import AvatarEditor from '../components/AvatarEditor'
import PasswordForm from '../components/PasswordForm'
import ProfileForm from '../components/ProfileForm'
import ProfileIcon from '../components/ProfileIcon'
import '../styles/profile.css'

export default function ProfilePage() {
  const navigate = useNavigate()
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)
  const [reload, setReload] = useState(0)
  const [section, setSection] = useState('personal')
  const [busy, setBusy] = useState('')
  const [notice, setNotice] = useState(null)

  useEffect(() => {
    document.title = 'Tài khoản của bạn | EduLearn'
    const controller = new AbortController()
    getProfile(controller.signal).then(data => { setUser(data); setNotice(null) }).catch(failure => {
      if (failure.name === 'AbortError') return
      if (failure.status === 401) navigate('/login?next=profile', { replace: true })
      else setNotice({ type: 'error', text: failure.message })
    }).finally(() => { if (!controller.signal.aborted) setLoading(false) })
    return () => controller.abort()
  }, [navigate, reload])

  function failure(error) {
    if (error.status === 401) { notifyAccountChanged(); navigate('/login?next=profile', { replace: true }); return }
    setNotice({ type: 'error', text: error.message })
  }

  async function saveProfile(body) {
    setBusy('profile'); setNotice(null)
    try {
      const saved = await updateProfile(body)
      setUser(saved); notifyAccountChanged()
      setNotice({ type: 'success', text: 'Đã lưu thông tin cá nhân của bạn.' })
      return saved
    } finally { setBusy('') }
  }

  async function saveAvatar(file) {
    setBusy('avatar'); setNotice(null)
    try {
      const saved = await uploadAvatar(file)
      setUser(current => ({ ...current, avatarUrl: saved.avatarUrl })); notifyAccountChanged()
      setNotice({ type: 'success', text: 'Ảnh đại diện mới đã được cập nhật.' })
    } finally { setBusy('') }
  }

  async function savePassword(body) {
    setBusy('password'); setNotice(null)
    try {
      await changePassword(body); notifyAccountChanged()
      navigate('/login?password=changed&next=profile', { replace: true })
    } finally { setBusy('') }
  }

  async function logout() {
    setBusy('logout'); setNotice(null)
    try { await logoutAccount(); notifyAccountChanged(); navigate('/login', { replace: true }) }
    catch (error) { failure(error) }
    finally { setBusy('') }
  }

  function switchSection(next) { if (busy) return; setSection(next); setNotice(null) }

  return <main id="noi-dung" className="profile-page home-container">
    <Link className="profile-back" to="/"><ProfileIcon name="arrow" />Về trang chủ</Link>
    <div className="profile-heading"><p className="home-eyebrow"><span className="eyebrow-dot" />GÓC NHỎ CỦA BẠN</p>
      <h1>Tài khoản của bạn<span>.</span></h1><p>Quản lý thông tin cá nhân và giữ tài khoản luôn an toàn.</p>
    </div>
    {notice && <div className={`profile-notice ${notice.type}`} role={notice.type === 'error' ? 'alert' : 'status'}>
      {notice.type === 'success' && <ProfileIcon name="check" />}<span>{notice.text}</span>
    </div>}
    {loading ? <div className="profile-loading" role="status"><span className="profile-loading-dot" />Đang tải thông tin tài khoản…</div> : !user ?
      <div className="profile-empty"><p>Chưa tải được hồ sơ của bạn.</p><button className="profile-button profile-button-primary" onClick={() => { setLoading(true); setReload(value => value + 1) }}>Thử lại</button></div> :
      <div className="profile-layout">
        <aside className="profile-sidebar" aria-label="Tài khoản cá nhân">
          <div className="profile-identity"><AvatarEditor user={user} busy={!!busy} onSave={saveAvatar} onFailure={failure} />
            <h2>{user.fullName}</h2><span className="profile-role">{roleNames[user.role]}</span>
          </div>
          <nav className="profile-settings-nav" aria-label="Cài đặt tài khoản">
            <button type="button" aria-current={section === 'personal' ? 'page' : undefined} disabled={!!busy} onClick={() => switchSection('personal')}><ProfileIcon name="user" />Thông tin cá nhân<span aria-hidden="true">›</span></button>
            <button type="button" aria-current={section === 'password' ? 'page' : undefined} disabled={!!busy} onClick={() => switchSection('password')}><ProfileIcon name="lock" />Đổi mật khẩu<span aria-hidden="true">›</span></button>
          </nav>
          <button className="profile-logout" type="button" disabled={!!busy} onClick={logout}><ProfileIcon name="logout" />{busy === 'logout' ? 'Đang đăng xuất…' : 'Đăng xuất'}</button>
          <div className="profile-sidebar-note"><img src="/math-bunny.svg" width="44" height="44" alt="" /><p>Mỗi ngày một chút,<br />cùng nhau tiến bộ.</p></div>
        </aside>
        <section className="profile-card" aria-labelledby="profile-section-title">
          <div className="profile-card-heading"><span className="profile-section-icon"><ProfileIcon name={section === 'personal' ? 'user' : 'lock'} /></span><div>
            <h2 id="profile-section-title">{section === 'personal' ? 'Thông tin cá nhân' : 'Đổi mật khẩu'}</h2>
            <p>{section === 'personal' ? 'Một vài thông tin để EduLearn hiểu bạn hơn.' : 'Bảo vệ tài khoản bằng một mật khẩu riêng của bạn.'}</p>
          </div></div>
          {section === 'personal' ? <ProfileForm user={user} busy={!!busy} onSave={saveProfile} onFailure={failure} /> :
            <PasswordForm busy={!!busy} allowed={user.passwordChangeAllowed} onSave={savePassword} onFailure={failure} />}
        </section>
      </div>}
  </main>
}
