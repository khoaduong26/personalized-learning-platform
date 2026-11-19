import { NavLink } from 'react-router-dom'
import './workspace-navigation.css'

const links = [
  { to: '/', label: 'Trang chủ', end: true },
  { to: '/classrooms', label: 'Lớp học', staffOnly: true },
  { to: '/academic', label: 'Chương trình', staffOnly: true },
  { to: '/assessments', label: 'Kiểm tra' },
]

export default function WorkspaceNavigation({ role }) {
  const isStaff = role === 'TEACHER' || role === 'ADMIN'

  return (
    <nav className="workspace-navigation" aria-label="Điều hướng khu vực học tập">
      {links.filter(link => !link.staffOnly || isStaff).map(link => (
        <NavLink key={link.to} to={link.to} end={link.end}>
          {link.label}
        </NavLink>
      ))}
    </nav>
  )
}
