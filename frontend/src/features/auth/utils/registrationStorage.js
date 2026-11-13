const pendingKey = 'tlcn.registration.email'

export function readPendingEmail() {
  try { return sessionStorage.getItem(pendingKey) || '' } catch { return '' }
}

export function rememberEmail(email) {
  try {
    if (email) sessionStorage.setItem(pendingKey, email)
    else sessionStorage.removeItem(pendingKey)
  } catch { /* Registration also works when browser storage is unavailable. */ }
}
