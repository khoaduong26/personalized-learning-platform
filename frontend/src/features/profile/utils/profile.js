export const roleNames = { ADMIN: 'Quản trị viên', TEACHER: 'Giáo viên', STUDENT: 'Học sinh' }

export function profileForm(user) {
  return Object.fromEntries(['fullName', 'phone', 'address', 'city', 'sex', 'dateOfBirth'].map(key => [key, user[key] ?? '']))
}

export function profilePayload(form, role) {
  const payload = Object.fromEntries(['fullName', 'phone', 'address', 'city'].map(key => [key, form[key].trim() || null]))
  if (role === 'STUDENT') {
    payload.sex = form.sex || null
    payload.dateOfBirth = form.dateOfBirth || null
  }
  return payload
}

export function validatePassword(form) {
  const fields = {}
  if (!form.currentPassword) fields.currentPassword = 'Vui lòng nhập mật khẩu hiện tại.'
  if (form.newPassword.length < 8 || form.newPassword.length > 128) fields.newPassword = 'Mật khẩu mới cần từ 8 đến 128 ký tự.'
  else if (form.newPassword === form.currentPassword) fields.newPassword = 'Mật khẩu mới phải khác mật khẩu hiện tại.'
  if (form.confirmPassword !== form.newPassword) fields.confirmPassword = 'Xác nhận mật khẩu mới không khớp.'
  return fields
}

export function avatarError(file) {
  if (!['image/jpeg', 'image/png'].includes(file.type)) return 'Bạn hãy chọn ảnh JPG hoặc PNG.'
  if (file.size > 2 * 1024 * 1024) return 'Ảnh cần có dung lượng tối đa 2 MB.'
  if (file.size === 0) return 'File ảnh trống. Bạn hãy chọn ảnh khác.'
  return ''
}

export function initials(name) {
  return name.trim().split(/\s+/).slice(-2).map(word => word[0]).join('').toLocaleUpperCase('vi-VN')
}
