import { NavLink } from 'react-router-dom'
import NotificationCenter from '../notifications/NotificationCenter'
import './workspace-navigation.css'

const links = [
  { to: '/', label: 'Trang chủ', end: true },
  { to: '/my-learning-path', label: 'Lộ trình của em', studentOnly: true },
  { to: '/curriculum', label: 'Học lại chương trình', studentOnly: true },
  { to: '/classrooms', label: 'Lớp học', staffOnly: true },
  { to: '/learning-paths', label: 'Lộ trình học sinh', teacherOnly: true },
  { to: '/academic', label: 'Chương trình', staffOnly: true },
  { to: '/assessments', label: 'Kiểm tra' },
  { to: '/exercises/review', label: 'Duyệt bài tập', teacherOnly: true },
  { to: '/admin', label: 'Tổng quan', adminOnly: true, end: true },
  { to: '/admin/accounts', label: 'Tài khoản', adminOnly: true },
]

export default function WorkspaceNavigation({ role }) {
  const isStaff = role === 'TEACHER' || role === 'ADMIN'

  return (
    <nav className="workspace-navigation" data-role={role} aria-label="Điều hướng khu vực học tập">
      {links.filter(link => (
        (!link.staffOnly || isStaff) &&
        (!link.teacherOnly || role === 'TEACHER') &&
        (!link.adminOnly || role === 'ADMIN') &&
        (!link.studentOnly || role === 'STUDENT')
      )).map(link => (
        <NavLink key={link.to} to={link.to} end={link.end}>
          {link.to === '/assessments' && role === 'ADMIN' ? 'Ngân hàng câu hỏi' : role === 'STUDENT' && link.to === '/assessments' ? 'Bài kiểm tra' : link.label}
        </NavLink>
      ))}
      <NotificationCenter />
    </nav>
  )
}
