import { useState } from 'react'
import { validatePassword } from '../utils/profile'
import ProfileIcon from './ProfileIcon'

const empty = { currentPassword: '', newPassword: '', confirmPassword: '' }

export default function PasswordForm({ busy, allowed, onSave, onFailure }) {
  const [form, setForm] = useState(empty)
  const [fields, setFields] = useState({})
  const [visible, setVisible] = useState(false)

  async function submit(event) {
    event.preventDefault()
    const errors = validatePassword(form)
    setFields(errors)
    if (Object.keys(errors).length) return
    try { await onSave(form) }
    catch (failure) { setFields(failure.fieldErrors || {}); onFailure(failure) }
  }

  if (!allowed) return <div className="profile-google-info">
    <span className="profile-security-symbol"><ProfileIcon name="lock" /></span>
    <h3>Tài khoản được bảo vệ bởi Google</h3>
    <p>Bạn đang đăng nhập bằng Google. Để đổi mật khẩu, hãy vào phần bảo mật trong tài khoản Google của bạn.</p>
    <a className="profile-button profile-button-secondary" href="https://myaccount.google.com/security" target="_blank" rel="noreferrer">Mở bảo mật Google <span aria-hidden="true">↗</span></a>
  </div>

  return <form onSubmit={submit}>
    <div className="profile-security-note"><ProfileIcon name="lock" /><p>Sau khi đổi mật khẩu, bạn sẽ cần đăng nhập lại trên các thiết bị.</p></div>
    <fieldset disabled={busy}>
      <div className="profile-password-fields">
        {[['currentPassword', 'Mật khẩu hiện tại', 'current-password'], ['newPassword', 'Mật khẩu mới', 'new-password'], ['confirmPassword', 'Xác nhận mật khẩu mới', 'new-password']].map(([name, label, autoComplete]) =>
          <label className="profile-field" htmlFor={`profile-${name}`} key={name}>
            <span>{label} <span className="profile-required">*</span></span>
            <input id={`profile-${name}`} name={name} type={visible ? 'text' : 'password'} autoComplete={autoComplete} required
              minLength={name === 'currentPassword' ? undefined : 8} maxLength={128} value={form[name]}
              onChange={event => { setForm(current => ({ ...current, [name]: event.target.value })); setFields(current => ({ ...current, [name]: undefined })) }}
              aria-invalid={!!fields[name]} aria-describedby={fields[name] ? `${name}-error` : name === 'newPassword' ? 'password-hint' : undefined} />
            {name === 'newPassword' && <small id="password-hint" className="profile-hint">Từ 8 đến 128 ký tự, khác mật khẩu hiện tại.</small>}
            {fields[name] && <small id={`${name}-error`} className="profile-field-error">{fields[name]}</small>}
          </label>)}
      </div>
      <label className="profile-show-password"><input type="checkbox" checked={visible} onChange={event => setVisible(event.target.checked)} />Hiển thị mật khẩu</label>
      <div className="profile-form-actions">
        <button type="button" className="profile-button profile-button-secondary" onClick={() => { setForm(empty); setFields({}); setVisible(false) }}>Hủy thay đổi</button>
        <button type="submit" className="profile-button profile-button-primary">{busy ? 'Đang lưu…' : 'Lưu mật khẩu mới'}</button>
      </div>
    </fieldset>
  </form>
}
