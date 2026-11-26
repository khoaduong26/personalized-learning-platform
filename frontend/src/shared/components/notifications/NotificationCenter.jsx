import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useNotifications } from './NotificationContext'
import './notification-center.css'

export default function NotificationCenter() {
  const [open, setOpen] = useState(false)
  const { notifications, unreadCount, connected, toast, setPanelOpen, dismissToast } = useNotifications()
  useEffect(() => () => setPanelOpen(false), [setPanelOpen])
  const toggle = () => {
    const next = !open
    setOpen(next)
    setPanelOpen(next)
  }

  return <>
    <div className="notification-center">
      <button className="notification-bell" type="button" onClick={toggle} aria-label={`Thông báo${unreadCount ? `, ${unreadCount} chưa đọc` : ''}`} aria-expanded={open}>
        <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M10 21h4" /></svg>
        {unreadCount > 0 && <span className="notification-count">{unreadCount > 9 ? '9+' : unreadCount}</span>}
      </button>
      <span className={`notification-connection ${connected ? 'is-connected' : ''}`} title={connected ? 'Thông báo realtime đã kết nối' : 'Đang kết nối thông báo'} aria-hidden="true" />
      {open && <section className="notification-popover" aria-label="Thông báo">
        <header><strong>Thông báo</strong><button type="button" onClick={toggle} aria-label="Đóng thông báo">×</button></header>
        {notifications.length === 0 ? <p className="notification-empty">Chưa có thông báo mới.</p> : <ul>
          {notifications.map(item => <li key={item.id}>
            <Link to={item.href || '/'} onClick={toggle}>
              <strong>{item.title}</strong><span>{item.message}</span>
              <time>{formatTime(item.createdAt)}</time>
            </Link>
          </li>)}
        </ul>}
      </section>}
    </div>
    {toast && <div className="notification-toast" role="status">
      <Link to={toast.href || '/'} onClick={dismissToast}><strong>{toast.title}</strong><span>{toast.message}</span></Link>
      <button type="button" onClick={dismissToast} aria-label="Đóng thông báo">×</button>
    </div>}
  </>
}

function formatTime(value) {
  if (!value) return ''
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '' : new Intl.DateTimeFormat('vi-VN', { hour: '2-digit', minute: '2-digit' }).format(date)
}
