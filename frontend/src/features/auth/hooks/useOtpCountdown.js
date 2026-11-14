import { useEffect, useState } from 'react'

export default function useOtpCountdown(resendAt, expiresAt) {
  const [now, setNow] = useState(Date.now)

  useEffect(() => {
    const timer = window.setInterval(() => setNow(Date.now()), 1000)
    return () => window.clearInterval(timer)
  }, [])

  return {
    remaining: Math.max(0, Math.ceil((resendAt - now) / 1000)),
    expired: expiresAt > 0 && now >= expiresAt,
  }
}
