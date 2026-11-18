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
  if (!form.currentPassword.trim()) fields.currentPassword = 'Vui lòng nhập mật khẩu hiện tại.'
  else if (form.currentPassword.length > 128) fields.currentPassword = 'Mật khẩu hiện tại tối đa 128 ký tự.'
  if (!form.newPassword.trim() || form.newPassword.length < 8 || form.newPassword.length > 128) fields.newPassword = 'Mật khẩu mới cần từ 8 đến 128 ký tự và không được chỉ gồm khoảng trắng.'
  else if (form.newPassword === form.currentPassword) fields.newPassword = 'Mật khẩu mới phải khác mật khẩu hiện tại.'
  if (form.confirmPassword !== form.newPassword) fields.confirmPassword = 'Xác nhận mật khẩu mới không khớp.'
  return fields
}

export function todayDate() {
  return new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Ho_Chi_Minh', year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date())
}

export function validateProfile(form, role, originalSex = '', today = todayDate()) {
  const fields = {}
  const name = form.fullName.trim()
  if (name.length < 2 || name.length > 120) fields.fullName = 'Họ và tên cần từ 2 đến 120 ký tự.'
  if (form.phone.trim() && !/^[0-9]{10}$/.test(form.phone.trim())) {
    fields.phone = 'Số điện thoại phải gồm đúng 10 chữ số, không có dấu hoặc ký tự khác.'
  }
  if (form.address.trim().length > 255) fields.address = 'Địa chỉ chi tiết và tên phường/xã gộp lại tối đa 255 ký tự.'
  if (form.city.trim().length > 100) fields.city = 'Tỉnh / thành phố tối đa 100 ký tự.'
  if (role === 'STUDENT') {
    const retainedLegacySex = form.sex === 'OTHER' && originalSex === 'OTHER'
    if (form.sex && !['MALE', 'FEMALE'].includes(form.sex) && !retainedLegacySex) fields.sex = 'Vui lòng chọn giới tính Nam hoặc Nữ.'
    if (form.dateOfBirth) {
      const date = new Date(`${form.dateOfBirth}T00:00:00Z`)
      if (!/^\d{4}-\d{2}-\d{2}$/.test(form.dateOfBirth) || Number.isNaN(date.getTime()) || date.toISOString().slice(0, 10) !== form.dateOfBirth) {
        fields.dateOfBirth = 'Ngày sinh không hợp lệ.'
      } else if (form.dateOfBirth > today) fields.dateOfBirth = 'Ngày sinh không được ở tương lai.'
    }
  }
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
