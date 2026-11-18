import { useEffect, useRef, useState } from 'react'
import { avatarError, initials } from '../utils/profile'
import ProfileIcon from './ProfileIcon'

export default function AvatarEditor({ user, busy, onSave, onFailure }) {
  const input = useRef(null)
  const [selection, setSelection] = useState(null)
  const [error, setError] = useState('')
  const [brokenAvatar, setBrokenAvatar] = useState('')

  useEffect(() => {
    return () => { if (selection) URL.revokeObjectURL(selection.preview) }
  }, [selection])

  function cancel() { setSelection(null); setError(''); if (input.current) input.current.value = '' }
  function select(event) {
    const selected = event.target.files?.[0]
    if (!selected) return
    const message = avatarError(selected)
    if (message) { setError(message); event.target.value = ''; return }
    setError(''); setSelection({ file: selected, preview: URL.createObjectURL(selected) })
  }
  async function save() {
    try { await onSave(selection.file); cancel() }
    catch (failure) { setError(failure.message); onFailure(failure) }
  }
  const src = selection?.preview || (brokenAvatar !== user.avatarUrl ? user.avatarUrl : '')

  return <div className="profile-avatar-editor">
    <div className="profile-avatar-wrap">
      <div className="profile-avatar">{src ? <img src={src} alt={`Ảnh đại diện của ${user.fullName}`} onError={() => {
        if (selection) { setError('Không đọc được ảnh. Bạn hãy chọn ảnh khác.'); setSelection(null); if (input.current) input.current.value = '' }
        else setBrokenAvatar(user.avatarUrl)
      }} /> : <span aria-label="Ảnh đại diện mặc định">{initials(user.fullName)}</span>}</div>
      <button className="profile-camera" type="button" disabled={busy} onClick={() => input.current?.click()} aria-label="Chọn ảnh đại diện mới"><ProfileIcon name="camera" /></button>
    </div>
    <input className="profile-file-input" ref={input} type="file" accept="image/png,image/jpeg" aria-label="File ảnh đại diện" onChange={select} disabled={busy} tabIndex={-1} />
    <button className="profile-avatar-link" type="button" disabled={busy} onClick={() => input.current?.click()}>Thay ảnh đại diện</button>
    <p className="profile-avatar-hint">JPG hoặc PNG · Tối đa 2 MB<br />Kích thước tối đa 4096 × 4096 px</p>
    {selection && <div className="profile-avatar-confirm"><p>Ảnh mới chưa được lưu.</p>
      <div><button className="profile-button profile-button-secondary" type="button" disabled={busy} onClick={cancel}>Hủy</button><button className="profile-button profile-button-primary" type="button" disabled={busy} onClick={save}>{busy ? 'Đang tải…' : 'Lưu ảnh'}</button></div>
    </div>}
    {error && <p className="profile-field-error" role="alert">{error}</p>}
  </div>
}
