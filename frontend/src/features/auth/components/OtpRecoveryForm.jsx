import RegistrationField from './RegistrationField'

export default function OtpRecoveryForm({ email, setEmail, busy, remaining, resend, changeStep }) {
  return (
    <form onSubmit={resend}>
      <RegistrationField label="Email đã đăng ký" name="recoveryEmail" type="email" value={email} onChange={event => setEmail(event.target.value)} autoComplete="email" maxLength={190} disabled={busy} />
      <button className="registration-primary" disabled={busy || remaining > 0}>{busy ? 'Đang gửi…' : remaining ? `Gửi lại sau ${remaining} giây` : 'Gửi mã xác minh'}</button>
      <button className="registration-text-link" type="button" disabled={busy} onClick={() => changeStep('register')}>← Quay lại đăng ký</button>
    </form>
  )
}
