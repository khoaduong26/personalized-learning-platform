const MODE_KEY = 'edulearn.mock.enabled'

export function isMockMode() {
  if (!import.meta.env?.DEV || typeof window === 'undefined') return false
  if (new URLSearchParams(window.location.search).get('mock') === '1') {
    window.sessionStorage.setItem(MODE_KEY, '1')
  }
  return window.sessionStorage.getItem(MODE_KEY) === '1'
}
