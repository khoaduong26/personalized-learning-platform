import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import AuthLayout from '../components/AuthLayout'
import RegistrationField from '../components/RegistrationField'
import { getCurrentUser, getRegistrationOptions, loginAccount, logoutAccount } from '../services/authApi'

const googleErrors = {
  'not-registered': 'Bạn chưa có tài khoản Google trên TLCN. Hãy đăng ký trước.',
  locked: 'Tài khoản đã bị khóa. Vui lòng liên hệ quản trị viên.',
  error: 'Chưa đăng nhập được bằng Google. Vui lòng thử lại.',
}
const roleNames = { ADMIN: 'Quản trị viên', TEACHER: 'Giáo viên', STUDENT: 'Học sinh' }

export default function LoginPage() {
  const [params] = useSearchParams()
  const [user, setUser] = useState(null)
  const [checking, setChecking] = useState(true)
  const [form, setForm] = useState({ email: '', password: '', rememberMe: false })
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [code, setCode] = useState('')
  const [fields, setFields] = useState({})
  const [googleEnabled, setGoogleEnabled] = useState(false)
  const [googleDismissed, setGoogleDismissed] = useState(false)

  useEffect(() => {
    document.title = 'Đăng nhập | TLCN'
    const controller = new AbortController()
    getCurrentUser(controller.signal).then(setUser).catch(failure => {
      if (failure.name !== 'AbortError' && failure.status !== 401) setError(failure.message)
    }).finally(() => { if (!controller.signal.aborted) setChecking(false) })
    getRegistrationOptions(controller.signal).then(data => setGoogleEnabled(data.googleEnabled)).catch(() => {})
    return () => controller.abort()
  }, [])

  function update(event) {
    const { name, value, type, checked } = event.target
    setForm(current => ({ ...current, [name]: type === 'checkbox' ? checked : value }))
    setFields(current => ({ ...current, [name]: undefined }))
  }

  async function submit(event) {
    event.preventDefault()
    setBusy(true); setError(''); setCode(''); setFields({}); setGoogleDismissed(true)
    try {
      setUser(await loginAccount(form))
      setForm(current => ({ ...current, password: '' }))
    } catch (failure) {
      setError(failure.message); setCode(failure.code); setFields(failure.fieldErrors || {})
    } finally { setBusy(false) }
  }

  async function logout() {
    setBusy(true); setError('')
    try { await logoutAccount(); setUser(null); setGoogleDismissed(true) }
    catch (failure) { setError(failure.message) }
    finally { setBusy(false) }
  }

  const displayedError = error || (!googleDismissed && googleErrors[params.get('google')])
  return <AuthLayout label="Đăng nhập tài khoản">
    <span className="registration-step">TÀI KHOẢN TLCN</span>
    <h2>{user ? `Xin chào, ${user.fullName}!` : 'Đăng nhập'}</h2>
    {displayedError && <p className="registration-notice error" role="alert">{displayedError}</p>}
    {user ? <>
      <p className="registration-notice success" role="status">Bạn đã đăng nhập với vai trò {roleNames[user.role]}.</p>
      <p className="registration-email">{user.email}</p>
      <Link className="registration-primary registration-link" to="/">Về trang chủ</Link>
      <button className="registration-secondary login-logout" disabled={busy} onClick={logout}>{busy ? 'Đang đăng xuất…' : 'Đăng xuất'}</button>
    </> : <>
      <p className="registration-description">Chào mừng bạn quay lại! Đăng nhập để tiếp tục hành trình học tập.</p>
      {checking && <p role="status" className="registration-hint">Đang kiểm tra phiên đăng nhập…</p>}
      <form onSubmit={submit}>
        <fieldset disabled={busy || checking}>
          <RegistrationField label="Email" name="email" type="email" autoComplete="username" maxLength={190} value={form.email} onChange={update} error={fields.email} />
          <RegistrationField label="Mật khẩu" name="password" type="password" autoComplete="current-password" maxLength={128} value={form.password} onChange={update} error={fields.password} />
          <label className="login-remember"><input type="checkbox" name="rememberMe" checked={form.rememberMe} onChange={update} /> Ghi nhớ đăng nhập trong 30 ngày</label>
          <button className="registration-primary" type="submit">{busy ? 'Đang đăng nhập…' : 'Đăng nhập'}</button>
        </fieldset>
      </form>
      {code === 'EMAIL_NOT_VERIFIED' && <Link className="registration-text-link" to="/register">Xác minh email / gửi lại OTP</Link>}
      <div className="registration-divider"><span>hoặc</span></div>
      <button className="registration-google" disabled={busy || checking || !googleEnabled}
        onClick={() => window.location.assign(`/api/auth/google-login?rememberMe=${form.rememberMe}`)}>Đăng nhập với Google</button>
      {!googleEnabled && <p className="registration-hint">Đăng nhập Google chưa khả dụng.</p>}
      <p className="registration-signin">Chưa có tài khoản? <Link to="/register">Đăng ký tài khoản</Link></p>
    </>}
  </AuthLayout>
}
