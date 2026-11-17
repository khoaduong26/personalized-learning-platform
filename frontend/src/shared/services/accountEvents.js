export const ACCOUNT_CHANGED = 'edulearn:account-changed'
export const notifyAccountChanged = () => window.dispatchEvent(new Event(ACCOUNT_CHANGED))
