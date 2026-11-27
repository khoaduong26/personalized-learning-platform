import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useLocation } from 'react-router-dom'
import { httpRequest } from '../../services/httpClient'
import { isMockMode } from '../../services/mockMode'
import { NotificationContext } from './context'

export function NotificationProvider({ children }) {
  const location = useLocation()
  const [user, setUser] = useState(null)
  const [notifications, setNotifications] = useState([])
  const [unreadIds, setUnreadIds] = useState(() => new Set())
  const [connected, setConnected] = useState(false)
  const [toast, setToast] = useState(null)
  const panelOpen = useRef(false)
  const toastTimer = useRef(null)
  const notificationUserId = useRef(null)

  useEffect(() => {
    let active = true
    if (isMockMode()) {
      return () => { active = false }
    }
    httpRequest('/api/auth/session')
      .then(session => { if (active) setUser(session) })
      .catch(() => { if (active) setUser(null) })
    return () => { active = false }
  }, [location.pathname])

  useEffect(() => {
    if (notificationUserId.current !== (user?.id ?? null)) {
      notificationUserId.current = user?.id ?? null
      panelOpen.current = false
      setNotifications([])
      setUnreadIds(new Set())
      setToast(null)
    }
  }, [user?.id])

  useEffect(() => {
    if (!user?.id || isMockMode()) {
      return undefined
    }
    let active = true
    let socket
    let retryTimer
    let retryDelay = 1000
    const connect = () => {
      if (!active) return
      const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
      socket = new WebSocket(`${protocol}//${window.location.host}/ws/notifications`)
      socket.onopen = () => { retryDelay = 1000; setConnected(true) }
      socket.onmessage = event => {
        try {
          const notification = JSON.parse(event.data)
          if (!notification?.id || !notification?.title) return
          setNotifications(current => [notification, ...current.filter(item => item.id !== notification.id)].slice(0, 30))
          if (!panelOpen.current) setUnreadIds(current => new Set([...current, notification.id]))
          setToast(notification)
          window.clearTimeout(toastTimer.current)
          toastTimer.current = window.setTimeout(() => setToast(null), 6500)
        } catch {
          // Ignore malformed server events and keep the connection alive.
        }
      }
      socket.onclose = () => {
        if (!active) return
        setConnected(false)
        retryTimer = window.setTimeout(connect, retryDelay)
        retryDelay = Math.min(retryDelay * 2, 15000)
      }
      socket.onerror = () => socket.close()
    }
    connect()
    return () => {
      active = false
      window.clearTimeout(retryTimer)
      window.clearTimeout(toastTimer.current)
      socket?.close()
      setConnected(false)
    }
  }, [user?.id])

  const setPanelOpen = useCallback(open => {
    panelOpen.current = open
    if (open) setUnreadIds(new Set())
  }, [])
  const dismissToast = useCallback(() => setToast(null), [])
  const value = useMemo(() => ({
    notifications, unreadCount: unreadIds.size, connected, toast, setPanelOpen, dismissToast,
  }), [notifications, unreadIds.size, connected, toast, setPanelOpen, dismissToast])
  return <NotificationContext.Provider value={value}>{children}</NotificationContext.Provider>
}
