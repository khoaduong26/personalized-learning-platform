const profilePaths = {
  TEACHER: '/profile/teacher',
  STUDENT: '/profile/student',
}

export function profilePathFor(role) {
  return profilePaths[role] || '/profile'
}
