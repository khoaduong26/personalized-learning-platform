import RegistrationField from './RegistrationField'
import { ROLES } from '../../../shared/constants/roles'

export default function RegistrationForm({ form, busy, fieldErrors, googleEnabled, update, register, changeStep }) {
  return (
    <form onSubmit={register}>
      <fieldset disabled={busy}>
        <legend>Bạn tham gia với vai trò</legend>
        <div className="registration-roles">
          {[[ROLES.TEACHER, 'Giáo viên', 'Đồng hành cùng học sinh'], [ROLES.STUDENT, 'Học sinh', 'Khám phá và luyện tập']].map(([value, label, description]) => (
            <label key={value} className={`registration-role ${form.role === value ? 'selected' : ''}`}>
              <input type="radio" name="role" value={value} checked={form.role === value} onChange={update} />
              <span><strong>{label}</strong><small>{description}</small></span>
            </label>
          ))}
        </div>
        <RegistrationField label="Họ và tên" name="fullName" value={form.fullName} onChange={update} autoComplete="name" minLength={2} maxLength={120} error={fieldErrors.fullName} />
        <RegistrationField label="Email" name="email" type="email" value={form.email} onChange={update} autoComplete="email" maxLength={190} error={fieldErrors.email} />
        <RegistrationField label="Số điện thoại" name="phone" type="tel" value={form.phone} onChange={update} autoComplete="tel" pattern="\+?[0-9]{9,15}" title="9–15 chữ số, có thể bắt đầu bằng +" error={fieldErrors.phone} />
        <div className="registration-passwords">
          <RegistrationField label="Mật khẩu" name="password" type="password" value={form.password} onChange={update} autoComplete="new-password" minLength={8} maxLength={128} error={fieldErrors.password} />
          <RegistrationField label="Nhập lại mật khẩu" name="confirmPassword" type="password" value={form.confirmPassword} onChange={update} autoComplete="new-password" minLength={8} maxLength={128} error={fieldErrors.confirmPassword} />
        </div>
        <p className="registration-hint">Mật khẩu từ 8 đến 128 ký tự. Với học sinh nhỏ tuổi, hãy nhờ phụ huynh hỗ trợ đăng ký.</p>
        <button className="registration-primary" type="submit">{busy ? 'Đang gửi mã xác minh…' : 'Đăng ký bằng email →'}</button>
      </fieldset>
      <div className="registration-divider"><span>hoặc</span></div>
      <button className="registration-google" type="button" disabled={busy || !googleEnabled}
        onClick={() => window.location.assign(`/api/auth/google?role=${form.role}`)}>
        Tiếp tục với Google
      </button>
      {!googleEnabled && <p className="registration-hint">Đăng ký Google chưa khả dụng. Bạn có thể đăng ký bằng email.</p>}
      <button className="registration-text-link" type="button" disabled={busy} onClick={() => changeStep('recover')}>Đã đăng ký nhưng chưa xác minh email?</button>
    </form>
  )
}
