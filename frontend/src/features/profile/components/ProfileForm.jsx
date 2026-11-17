import { useState } from 'react'
import { profileForm, profilePayload } from '../utils/profile'
import { addressPayload, provinces, readAddress, writeAddress } from '../utils/address'
import AddressSelect from './AddressSelect'

export default function ProfileForm({ user, busy, onSave, onFailure }) {
  const [form, setForm] = useState(() => profileForm(user))
  const [fields, setFields] = useState({})
  const [location, setLocation] = useState(() => readAddress(user))
  const savedLocation = readAddress(user)
  const locationDirty = JSON.stringify(location) !== JSON.stringify(savedLocation)
  const dirty = locationDirty || JSON.stringify(form) !== JSON.stringify(profileForm(user))
  const province = provinces.find(item => item.code === location.provinceCode)
  const provinceOptions = location.legacyCity
    ? [{ code: 'legacy', name: location.legacyCity }, ...provinces]
    : provinces

  function updateLocation(changes) {
    setLocation(current => ({ ...current, ...changes }))
    setFields(current => ({ ...current, city: undefined, address: undefined }))
  }

  function update(event) {
    const { name, value } = event.target
    setForm(current => ({ ...current, [name]: value }))
    setFields(current => ({ ...current, [name]: undefined }))
  }

  async function submit(event) {
    event.preventDefault()
    setFields({})
    const address = addressPayload(form, location)
    if ((address.address || '').trim().length > 255) {
      setFields({ address: 'Địa chỉ chi tiết và tên phường/xã gộp lại tối đa 255 ký tự.' })
      return
    }
    try {
      const saved = await onSave(profilePayload({ ...form, ...address }, user.role))
      setForm(profileForm(saved))
      setLocation(readAddress(saved))
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
        <div className="profile-field-wide profile-address-section">
          <div className="profile-address-grid">
            <AddressSelect id="profile-city" label="Tỉnh / Thành phố" options={provinceOptions}
              value={location.legacyCity ? 'legacy' : location.provinceCode} placeholder="Tìm tỉnh / thành phố"
              onChange={code => {
                if (code === 'legacy' || (!location.legacyCity && code === location.provinceCode)) return
                updateLocation({ provinceCode: code, wardCode: '', legacyCity: '' })
              }} />
            <AddressSelect id="profile-ward" label="Phường / Xã / Đặc khu" options={province?.wards || []}
              value={location.wardCode} disabled={!province} placeholder={province ? 'Tìm phường / xã' : 'Chọn tỉnh / thành phố trước'}
              onChange={code => updateLocation({ wardCode: code })} />
          </div>
          {location.legacyCity && <p className="profile-hint">Tỉnh/thành phố đã lưu là “{location.legacyCity}”. Bạn có thể giữ nguyên hoặc chọn lại trong danh sách.</p>}
          {fields.city && <p className="profile-field-error" role="alert">{fields.city}</p>}
          <label className="profile-field" htmlFor="profile-address"><span>Địa chỉ chi tiết</span>
            <input id="profile-address" name="address" maxLength={255} autoComplete="street-address"
              placeholder="Số nhà, tên đường, thôn / ấp" value={location.detail}
              onChange={event => updateLocation({ detail: event.target.value })}
              aria-invalid={!!fields.address} aria-describedby={fields.address ? 'profile-address-error' : 'profile-address-hint'} />
            <small id="profile-address-hint" className="profile-hint">Địa chỉ đã lưu chưa khớp danh sách sẽ được giữ nguyên trong ô này.</small>
            {fields.address && <small id="profile-address-error" className="profile-field-error" role="alert">{fields.address}</small>}
          </label>
          {locationDirty && <p className="profile-address-preview"><span>Địa chỉ sẽ lưu</span>{[writeAddress(location).address, writeAddress(location).city].filter(Boolean).join(', ') || 'Chưa có địa chỉ'}</p>}
        </div>
      </div>
      <div className="profile-form-actions">
        <span className="profile-save-hint">{dirty ? 'Bạn có thay đổi chưa lưu.' : 'Thông tin của bạn đã được cập nhật.'}</span>
        <button className="profile-button profile-button-secondary" type="button" disabled={!dirty} onClick={() => { setForm(profileForm(user)); setLocation(readAddress(user)); setFields({}) }}>Hủy thay đổi</button>
        <button className="profile-button profile-button-primary" type="submit" disabled={!dirty}>{busy ? 'Đang lưu…' : 'Lưu thay đổi'}</button>
      </div>
    </fieldset>
  </form>
}
