import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import AuthLayout from '../components/AuthLayout'
import RegistrationField from '../components/RegistrationField'
import useOtpCountdown from '../hooks/useOtpCountdown'
import { requestPasswordReset, resetPassword, verifyPasswordResetOtp } from '../services/authApi'
import { notifyAccountChanged } from '../../../shared/services/accountEvents'

export default function ForgotPasswordPage() {
  const navigate = useNavigate()
  const [step, setStep] = useState('email')
  const [email, setEmail] = useState('')
  const [otp, setOtp] = useState('')
  const [grant, setGrant] = useState(null)
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [resendAt, setResendAt] = useState(0)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [fields, setFields] = useState({})
  const [busy, setBusy] = useState(false)
  const { remaining, expired } = useOtpCountdown(resendAt, grant ? Date.parse(grant.expiresAt) : 0)

  useEffect(() => { document.title = 'Quên mật khẩu | EduLearn' }, [])

  async function submit(event, action) {
    event?.preventDefault()
    setBusy(true)
    setError('')
    setFields({})
    try {
      await action()
    } catch (failure) {
      setError(failure.message)
      setFields(failure.fieldErrors || {})
      if (failure.code === 'RESET_TOKEN_INVALID') {
        clearPasswords()
        setStep('otp')
        setMessage('')
      }
    } finally {
      setBusy(false)
    }
  }

  async function sendCode() {
    const normalizedEmail = email.trim().toLowerCase()
    const response = await requestPasswordReset(normalizedEmail)
    setEmail(normalizedEmail)
    setOtp('')
    setResendAt(Date.now() + 60_000)
    setMessage(response.message)
    setStep('otp')
  }

  async function verifyCode() {
    setGrant(await verifyPasswordResetOtp({ email, otp }))
    setOtp('')
    setMessage('')
    setStep('password')
  }

  function clearPasswords() {
    setGrant(null)
    setNewPassword('')
    setConfirmPassword('')
  }

  function restart(nextStep) {
    clearPasswords()
    setOtp('')
    setError('')
    setFields({})
    setMessage('')
    setStep(nextStep)
  }

  async function savePassword() {
    if (newPassword !== confirmPassword) {
      setFields({ confirmPassword: 'Xác nhận mật khẩu mới không khớp.' })
      return
    }
    await resetPassword({ resetToken: grant.resetToken, newPassword, confirmPassword })
    clearPasswords()
    notifyAccountChanged()
    navigate('/login?password=changed', { replace: true })
  }

  const title = { email: 'Quên mật khẩu', otp: 'Nhập mã xác thực', password: 'Đặt mật khẩu mới' }[step]
  return <AuthLayout label={title}>
    <h2>{title}</h2>
    {message && <p className="registration-notice success" role="status">{message}</p>}
    {error && <p className="registration-notice error" role="alert">{error}</p>}
    {step === 'email' && <form onSubmit={event => submit(event, sendCode)}>
      <fieldset disabled={busy}>
        <RegistrationField label="Email đã đăng ký" name="email" type="email" autoComplete="email" maxLength={190}
          value={email} onChange={event => setEmail(event.target.value)} error={fields.email} />
        <button className="registration-primary" type="submit">{busy ? 'Đang gửi…' : 'Gửi mã xác thực'}</button>
      </fieldset>
    </form>}
    {step === 'otp' && <form onSubmit={event => submit(event, verifyCode)}>
      <p className="registration-email">{email}</p>
      <fieldset disabled={busy}>
        <RegistrationField label="Mã OTP (6 chữ số)" name="otp" className="registration-otp" inputMode="numeric"
          autoComplete="one-time-code" pattern="[0-9]{6}" minLength={6} maxLength={6}
          value={otp} onChange={event => setOtp(event.target.value.replace(/[^0-9]/g, '').slice(0, 6))} error={fields.otp} />
        <button className="registration-primary" type="submit" disabled={otp.length !== 6}>
          {busy ? 'Đang xử lý…' : 'Xác thực mã'}
        </button>
        <p className="registration-hint">Mã có hiệu lực 5 phút. Hãy kiểm tra cả thư mục thư rác.</p>
        <button className="registration-secondary" type="button" disabled={remaining > 0}
          onClick={() => submit(null, sendCode)}>{remaining ? `Gửi lại sau ${remaining} giây` : 'Gửi lại OTP'}</button>
        <button className="registration-text-link" type="button" onClick={() => restart('email')}>Dùng email khác</button>
      </fieldset>
    </form>}
    {step === 'password' && <form onSubmit={event => submit(event, savePassword)}>
      {expired && <p className="registration-notice error" role="alert">Quyền đặt lại mật khẩu đã hết hạn. Hãy yêu cầu mã mới.</p>}
      <fieldset disabled={busy}>
        <RegistrationField label="Mật khẩu mới" name="newPassword" type="password" autoComplete="new-password"
          minLength={8} maxLength={128} value={newPassword} onChange={event => setNewPassword(event.target.value)}
          error={fields.newPassword} />
        <RegistrationField label="Xác nhận mật khẩu mới" name="confirmPassword" type="password" autoComplete="new-password"
          minLength={8} maxLength={128} value={confirmPassword} onChange={event => setConfirmPassword(event.target.value)}
          error={fields.confirmPassword} />
        <button className="registration-primary" type="submit" disabled={expired}>
          {busy ? 'Đang lưu…' : 'Cập nhật mật khẩu'}
        </button>
        <button className="registration-text-link" type="button" onClick={() => restart('otp')}>Quay lại bước xác thực</button>
      </fieldset>
    </form>}
    <Link className="registration-text-link" to="/login">← Quay lại đăng nhập</Link>
  </AuthLayout>
}
