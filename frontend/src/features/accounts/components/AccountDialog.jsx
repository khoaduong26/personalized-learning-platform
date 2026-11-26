import { useEffect, useRef, useState } from 'react'
import { initials, roleNames } from '../../profile/utils/profile.js'

export default function AccountDialog({ dialog, adminId, pending, onClose, onSubmit }) {
  const modal = useRef(null)
  const { kind, account } = dialog
  const editing = kind === 'edit'
  const confirming = kind === 'status'
  const locking = confirming && account.status !== 'LOCKED'
  const title = confirming ? (locking ? 'Khóa tài khoản' : 'Mở khóa tài khoản')
    : editing ? 'Chỉnh sửa tài khoản' : 'Thêm tài khoản'
  const [form, setForm] = useState(() => ({
    fullName: account?.fullName ?? '',
    email: account?.email ?? '',
    phone: account?.phone ?? '',
    role: account?.role ?? 'STUDENT',
    password: '',
  }))
  const [error, setError] = useState(null)

  useEffect(() => {
    const element = modal.current
    element.showModal()
    element.querySelector(confirming ? '[data-cancel]' : 'input')?.focus()
    return () => element.close()
  }, [confirming])

  function change(event) {
    const { name, value } = event.target
    setForm(previous => ({ ...previous, [name]: value }))
    if (error?.fieldErrors?.[name]) {
      setError(previous => ({ ...previous, fieldErrors: { ...previous.fieldErrors, [name]: undefined } }))
    }
  }

  async function submit(event) {
    event.preventDefault()
    setError(null)
    try {
      if (confirming) {
        await onSubmit(locking)
        return
      }
      const values = { fullName: form.fullName.trim(), email: form.email.trim(), phone: form.phone.trim() || null, role: form.role }
      if (!editing) values.password = form.password
      const fieldErrors = {}
      if (values.fullName.length < 2) fieldErrors.fullName = 'Họ và tên cần ít nhất 2 ký tự.'
      if (values.phone && !/^\+?[0-9]{9,15}$/.test(values.phone)) fieldErrors.phone = 'Nhập 9–15 chữ số, có thể bắt đầu bằng dấu +.'
      if (!editing && !form.password.trim()) fieldErrors.password = 'Mật khẩu cần ít nhất 8 ký tự và không chỉ gồm khoảng trắng.'
      if (Object.keys(fieldErrors).length) {
        setError({ message: 'Vui lòng kiểm tra các trường được đánh dấu.', fieldErrors })
        return
      }
      await onSubmit(values)
    } catch (failure) {
      setError({ message: failure.message, fieldErrors: failure.fieldErrors })
    }
  }

  const field = name => {
    const message = error?.fieldErrors?.[name]
    return {
      id: `account-${name}`,
      name, value: form[name], onChange: change,
      'aria-invalid': message ? true : undefined,
      'aria-describedby': message ? `account-error-${name}` : undefined,
    }
  }
  const errorFor = name => error?.fieldErrors?.[name] && (
    <span className="accounts-field-error" id={`account-error-${name}`}>{error.fieldErrors[name]}</span>
  )

  return (
    <dialog ref={modal} className="accounts-dialog" aria-labelledby="account-dialog-title"
      onCancel={event => { event.preventDefault(); if (!pending) onClose() }}>
      <div className="accounts-dialog-header">
        <h2 id="account-dialog-title">{title}</h2>
        <button type="button" className="accounts-close" onClick={onClose} disabled={pending} aria-label="Đóng">×</button>
      </div>
      <form onSubmit={submit}>
        {confirming ? (
          <div className="accounts-dialog-body">
            <div className="accounts-identity">
              <span className="accounts-avatar" aria-hidden="true">{initials(account.fullName)}</span>
              <span><strong>{account.fullName}</strong><small>{account.email}</small></span>
            </div>
            <p>{locking ? 'Tài khoản sẽ bị đăng xuất và không thể đăng nhập cho đến khi được mở khóa.'
              : 'Tài khoản sẽ trở về trạng thái trước khi khóa. Nếu còn chờ xác minh email, người dùng cần hoàn tất xác minh để đăng nhập.'}</p>
          </div>
        ) : (
          <fieldset className="accounts-dialog-body accounts-form" disabled={pending}>
            <label htmlFor="account-fullName">Họ và tên
              <input {...field('fullName')} required minLength={2} maxLength={120} autoComplete="off" />
              {errorFor('fullName')}
            </label>
            <label htmlFor="account-email">Email
              <input {...field('email')} type="email" required maxLength={190} autoComplete="off" />
              {errorFor('email')}
            </label>
            <label htmlFor="account-phone">Số điện thoại
              <input {...field('phone')} type="tel" maxLength={16} placeholder="Không bắt buộc" autoComplete="off" />
              {errorFor('phone')}
            </label>
            <label htmlFor="account-role">Vai trò
              <select {...field('role')} disabled={editing && account.id === adminId}>
                {Object.entries(roleNames).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
              </select>
              {errorFor('role')}
            </label>
            {!editing && <label htmlFor="account-password">Mật khẩu ban đầu
              <input {...field('password')} type="password" required minLength={8} maxLength={128} autoComplete="new-password" />
              <span className="accounts-field-hint">Từ 8 đến 128 ký tự.</span>
              {errorFor('password')}
            </label>}
          </fieldset>
        )}
        {error && <p className="accounts-dialog-error" role="alert">{error.message}</p>}
        <div className="accounts-dialog-footer">
          <button type="button" className="accounts-button accounts-secondary" data-cancel disabled={pending} onClick={onClose}>Hủy</button>
          <button type="submit" className={`accounts-button ${locking ? 'accounts-danger' : 'accounts-primary'}`} disabled={pending}>
            {pending ? 'Đang lưu…' : confirming ? title : editing ? 'Lưu thay đổi' : 'Tạo tài khoản'}
          </button>
        </div>
      </form>
    </dialog>
  )
}
