import { useState } from 'react'
import { profileForm, profilePayload } from '../utils/profile'

export default function ProfileForm({ user, busy, onSave, onFailure }) {
  const [form, setForm] = useState(() => profileForm(user))
  const [fields, setFields] = useState({})
  const dirty = JSON.stringify(form) !== JSON.stringify(profileForm(user))

  function update(event) {
    const { name, value } = event.target
    setForm(current => ({ ...current, [name]: value }))
    setFields(current => ({ ...current, [name]: undefined }))
  }

  async function submit(event) {
    event.preventDefault()
    setFields({})
    try {
      const saved = await onSave(profilePayload(form, user.role))
      setForm(profileForm(saved))
    } catch (failure) {
      setFields(failure.fieldErrors || {})
      onFailure(failure)
    }
  }

  function field(name, label, props = {}) {
    return <label className="profile-field" htmlFor={`profile-${name}`}>
      <span>{label}{props.required && <span className="profile-required"> *</span>}</span>
      <input id={`profile-${name}`} name={name} value={form[name]} onChange={update}
        aria-invalid={!!fields[name]} aria-describedby={fields[name] ? `profile-${name}-error` : undefined} {...props} />
      {fields[name] && <small id={`profile-${name}-error`} className="profile-field-error">{fields[name]}</small>}
    </label>
  }

  return <form onSubmit={submit}>
    <fieldset disabled={busy}>
      <div className="profile-form-grid">
        {field('fullName', 'Họ và tên', { required: true, minLength: 2, maxLength: 120, autoComplete: 'name' })}
        {field('phone', 'Số điện thoại', { type: 'tel', maxLength: 16, autoComplete: 'tel', pattern: '\\+?[0-9]{9,15}', title: 'Nhập 9–15 chữ số, có thể bắt đầu bằng +.', placeholder: 'Thêm số điện thoại' })}
        <label className="profile-field profile-field-wide" htmlFor="profile-email"><span>Email đăng nhập</span>
          <input id="profile-email" value={user.email} readOnly aria-describedby="profile-email-hint" />
          <small id="profile-email-hint" className="profile-hint">Email được dùng để đăng nhập và không thể thay đổi tại đây.</small>
        </label>
        {user.role === 'STUDENT' && <>
          <label className="profile-field" htmlFor="profile-sex"><span>Giới tính</span>
            <select id="profile-sex" name="sex" value={form.sex} onChange={update} aria-invalid={!!fields.sex} aria-describedby={fields.sex ? 'profile-sex-error' : undefined}>
              <option value="">Chưa chọn</option><option value="MALE">Nam</option><option value="FEMALE">Nữ</option><option value="OTHER">Khác</option>
            </select>
            {fields.sex && <small id="profile-sex-error" className="profile-field-error">{fields.sex}</small>}
          </label>
          {field('dateOfBirth', 'Ngày sinh', { type: 'date', autoComplete: 'bday', max: new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Ho_Chi_Minh', year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date()) })}
        </>}
        <div className="profile-field-wide">{field('address', 'Địa chỉ', { maxLength: 255, autoComplete: 'street-address', placeholder: 'Số nhà, tên đường, phường / xã' })}</div>
        <div className="profile-field-wide">{field('city', 'Tỉnh / Thành phố', { maxLength: 100, autoComplete: 'address-level1', placeholder: 'Nơi bạn đang sinh sống' })}</div>
      </div>
      <div className="profile-form-actions">
        <span className="profile-save-hint">{dirty ? 'Bạn có thay đổi chưa lưu.' : 'Thông tin của bạn đã được cập nhật.'}</span>
        <button className="profile-button profile-button-secondary" type="button" disabled={!dirty} onClick={() => { setForm(profileForm(user)); setFields({}) }}>Hủy thay đổi</button>
        <button className="profile-button profile-button-primary" type="submit" disabled={!dirty}>{busy ? 'Đang lưu…' : 'Lưu thay đổi'}</button>
      </div>
    </fieldset>
  </form>
}
