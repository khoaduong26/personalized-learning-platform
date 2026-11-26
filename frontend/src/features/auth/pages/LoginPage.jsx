import { useEffect, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import AuthLayout from '../components/AuthLayout'
import RegistrationField from '../components/RegistrationField'
import { getCurrentUser, getRegistrationOptions, loginAccount, logoutAccount } from '../services/authApi'
import { notifyAccountChanged } from '../../../shared/services/accountEvents'
import { profilePathFor } from '../../../shared/utils/profilePath'
import { isMockMode } from '../../../shared/services/mockMode.js'

const demoAccounts = [
  { email: 'teacher.demo@example.test', label: 'Vào với vai trò giáo viên', destination: '/classrooms' },
  { email: 'student.binh@example.test', label: 'Vào với vai trò học sinh', destination: '/my-learning-path' },
  { email: 'admin.demo@example.test', label: 'Vào với vai trò quản trị viên', destination: '/admin/accounts' },
]
const destinations = ['/classrooms', '/academic', '/assessments', '/my-learning-path', '/admin/accounts', '/exercises/review']
const workspaceByRole = {
  STUDENT: { to: '/my-learning-path', label: 'Mở lộ trình của em' },
  TEACHER: { to: '/classrooms', label: 'Mở lớp học' },
  ADMIN: { to: '/admin/accounts', label: 'Quản lý tài khoản' },
}
const destinationRoles = {
  '/my-learning-path': ['STUDENT'],
  '/classrooms': ['TEACHER', 'ADMIN'],
  '/academic': ['TEACHER', 'ADMIN'],
  '/exercises/review': ['TEACHER', 'ADMIN'],
  '/admin/accounts': ['ADMIN'],
}

function canReturnTo(path, role) {
  return path && (!destinationRoles[path] || destinationRoles[path].includes(role))
}

const googleErrors = {
  'not-registered': 'Bạn chưa có tài khoản Google trên EduLearn. Hãy đăng ký trước.',
  locked: 'Tài khoản đã bị khóa. Vui lòng liên hệ quản trị viên.',
  error: 'Chưa đăng nhập được bằng Google. Vui lòng thử lại.',
}
const roleNames = { ADMIN: 'Quản trị viên', TEACHER: 'Giáo viên', STUDENT: 'Học sinh' }

export default function LoginPage() {
  const [params] = useSearchParams()
  const returnTo = destinations.includes(params.get('returnTo')) ? params.get('returnTo') : null
  const navigate = useNavigate()
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
    document.title = 'Đăng nhập | EduLearn'
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
      const signedIn = await loginAccount(form)
      setUser(signedIn)
      notifyAccountChanged()
      if (params.get('next') === 'profile') {
        navigate(profilePathFor(signedIn.role), { replace: true })
      }
      setForm(current => ({ ...current, password: '' }))
      if (params.get('next') !== 'profile' && canReturnTo(returnTo, signedIn.role)) navigate(returnTo, { replace: true })
    } catch (failure) {
      setError(failure.message); setCode(failure.code); setFields(failure.fieldErrors || {})
    } finally { setBusy(false) }
  }

  async function enterDemoAccount(account) {
    setBusy(true); setError('')
    try {
      const signedIn = await loginAccount({ email: account.email, password: 'mock', rememberMe: false })
      setUser(signedIn)
      notifyAccountChanged()
      navigate(canReturnTo(returnTo, signedIn.role) ? returnTo : account.destination, { replace: true })
    } catch (failure) {
      setError(failure.message)
    } finally { setBusy(false) }
  }

  async function logout() {
    setBusy(true); setError('')
    try { await logoutAccount(); setUser(null); setGoogleDismissed(true); notifyAccountChanged() }
    catch (failure) { setError(failure.message) }
    finally { setBusy(false) }
  }

  const displayedError = error || (!googleDismissed && googleErrors[params.get('google')])
  const workspace = workspaceByRole[user?.role]
  return <AuthLayout label="Đăng nhập tài khoản">
    <span className="registration-step">TÀI KHOẢN EduLearn</span>
    <h2>{user ? `Xin chào, ${user.fullName}!` : 'Đăng nhập'}</h2>
    {displayedError && <p className="registration-notice error" role="alert">{displayedError}</p>}
    {!user && params.get('password') === 'changed' && <p className="registration-notice success" role="status">Đổi mật khẩu thành công. Bạn hãy đăng nhập bằng mật khẩu mới.</p>}
    {user ? <>
      <p className="registration-notice success" role="status">Bạn đã đăng nhập với vai trò {roleNames[user.role]}.</p>
      <p className="registration-email">{user.email}</p>
      <Link className="registration-primary registration-link" to={canReturnTo(returnTo, user.role) ? returnTo : workspace?.to || '/'}>
        {canReturnTo(returnTo, user.role) && returnTo !== workspace?.to ? 'Tiếp tục' : workspace?.label || 'Về trang chủ'}
      </Link>
      <Link className="registration-text-link" to={profilePathFor(user.role)}>Tài khoản của tôi</Link>
      <Link className="registration-text-link" to="/">Về trang chủ</Link>
      <button className="registration-secondary login-logout" disabled={busy} onClick={logout}>{busy ? 'Đang đăng xuất…' : 'Đăng xuất'}</button>
    </> : <>
      <p className="registration-description">Chào mừng bạn quay lại! Đăng nhập để tiếp tục hành trình học tập.</p>
      {isMockMode() && <section className="mock-login-panel" aria-label="Tài khoản kiểm tra dữ liệu mẫu">
        <strong>Chế độ dữ liệu mẫu</strong>
        <p>Chọn vai trò để mở các màn hình demo. Thay đổi chỉ tồn tại trong phiên này.</p>
        {demoAccounts.map(account => <button key={account.email} type="button" disabled={busy || checking} onClick={() => enterDemoAccount(account)}>{account.label}</button>)}
      </section>}
      {checking && <p role="status" className="registration-hint">Đang kiểm tra phiên đăng nhập…</p>}
      <form onSubmit={submit}>
        <fieldset disabled={busy || checking}>
          <RegistrationField label="Email" name="email" type="email" autoComplete="username" maxLength={190} value={form.email} onChange={update} error={fields.email} />
          <RegistrationField label="Mật khẩu" name="password" type="password" autoComplete="current-password" maxLength={128} value={form.password} onChange={update} error={fields.password} />
          <label className="login-remember"><input type="checkbox" name="rememberMe" checked={form.rememberMe} onChange={update} /> Ghi nhớ đăng nhập trong 30 ngày</label>
          <button className="registration-primary" type="submit">{busy ? 'Đang đăng nhập…' : 'Đăng nhập'}</button>
        </fieldset>
      </form>
      <Link className="registration-text-link" to="/forgot-password">Quên mật khẩu?</Link>
      {code === 'EMAIL_NOT_VERIFIED' && <Link className="registration-text-link" to="/register">Xác minh email / gửi lại OTP</Link>}
      <div className="registration-divider"><span>hoặc</span></div>
      <button className="registration-google" disabled={busy || checking || !googleEnabled}
        onClick={() => window.location.assign(`/api/auth/google-login?rememberMe=${form.rememberMe}`)}>Đăng nhập với Google</button>
      {!googleEnabled && <p className="registration-hint">Đăng nhập Google chưa khả dụng.</p>}
      <p className="registration-signin">Chưa có tài khoản? <Link to="/register">Đăng ký tài khoản</Link></p>
    </>}
  </AuthLayout>
}
