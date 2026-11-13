import RegistrationField from './RegistrationField'

export default function OtpVerificationForm({ email, otp, setOtp, busy, fieldErrors, expiresAt, expired, remaining, verify, resend, changeStep, restart }) {
  return (
    <form onSubmit={verify}>
      <p className="registration-email">{email}</p>
      <RegistrationField label="Mã xác minh OTP" name="otp" className="registration-otp" value={otp} onChange={event => setOtp(event.target.value.replace(/[^0-9]/g, '').slice(0, 6))}
        inputMode="numeric" autoComplete="one-time-code" pattern="[0-9]{6}" minLength={6} maxLength={6} disabled={busy} error={fieldErrors.otp} />
      {expiresAt > 0 && <p className="registration-hint">{expired ? 'Mã đã hết hạn. Hãy yêu cầu mã mới.' : `Mã hết hạn lúc ${new Date(expiresAt).toLocaleTimeString('vi-VN')}.`}</p>}
      <button className="registration-primary" disabled={busy || otp.length !== 6}>{busy ? 'Đang xử lý…' : 'Xác minh tài khoản →'}</button>
      <p className="registration-hint">Chưa nhận được email? Kiểm tra cả thư mục Spam.</p>
      <button className="registration-secondary" type="button" disabled={busy || remaining > 0} onClick={resend}>{remaining ? `Gửi lại sau ${remaining} giây` : 'Gửi lại OTP'}</button>
      <button className="registration-text-link" type="button" disabled={busy} onClick={() => changeStep('recover')}>Dùng email đăng ký khác</button>
      <button className="registration-text-link" type="button" disabled={busy} onClick={restart}>← Quay lại đăng ký</button>
    </form>
  )
}
