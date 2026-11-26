import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import WorkspaceNavigation from '../../../shared/components/navigation/WorkspaceNavigation'
import { getCurrentUser } from '../../auth/services/authApi'
import { listAccounts } from '../../accounts/services/accountApi'
import { getClassrooms } from '../../classrooms/services/classroomApi'
import { getPendingExercises } from '../../exercises/services/exerciseApi'
import './admin-dashboard.css'

const initialState = { status: 'loading' }

export default function AdminDashboardPage() {
  const [dashboard, setDashboard] = useState(initialState)
  const [revision, setRevision] = useState(0)

  useEffect(() => {
    let active = true
    const controller = new AbortController()
    document.title = 'Tổng quan quản trị · EduLearn'

    async function load() {
      try {
        const admin = await getCurrentUser(controller.signal)
        if (!active) return
        if (admin?.role !== 'ADMIN') {
          setDashboard({ status: admin ? 'forbidden' : 'signed-out' })
          return
        }
        setDashboard({ status: 'loading', admin })
        const [admins, teachers, students, pendingAccounts, classrooms, pendingExercises] = await Promise.all([
          listAccounts({ page: 0, role: 'ADMIN' }, controller.signal),
          listAccounts({ page: 0, role: 'TEACHER' }, controller.signal),
          listAccounts({ page: 0, role: 'STUDENT' }, controller.signal),
          listAccounts({ page: 0, status: 'PENDING_OTP' }, controller.signal),
          getClassrooms(),
          getPendingExercises(),
        ])
        if (active) setDashboard({
          status: 'ready',
          admin,
          stats: {
            admins: admins.totalElements,
            teachers: teachers.totalElements,
            students: students.totalElements,
            classrooms: classrooms.length,
            pendingAccounts: pendingAccounts.totalElements,
            pendingExercises: pendingExercises.length,
            enrolledStudents: classrooms.reduce((sum, classroom) => sum + classroom.studentCount, 0),
          },
        })
      } catch (error) {
        if (active && error.name !== 'AbortError') {
          setDashboard({ status: error.status === 401 ? 'signed-out' : error.status === 403 ? 'forbidden' : 'error', message: error.message })
        }
      }
    }

    load()
    return () => { active = false; controller.abort() }
  }, [revision])

  if (dashboard.status !== 'ready') {
    return <main className="admin-dashboard-shell">
      <section className="admin-dashboard-gate" aria-live="polite">
        {dashboard.status === 'loading' ? <><span className="admin-dashboard-spinner" aria-hidden="true" /><p>Đang tải tổng quan quản trị…</p></>
          : <>
            <h1>{dashboard.status === 'signed-out' ? 'Đăng nhập để mở dashboard' : dashboard.status === 'forbidden' ? 'Dashboard dành cho quản trị viên' : 'Chưa tải được dữ liệu'}</h1>
            <p>{dashboard.message || (dashboard.status === 'signed-out' ? 'Vui lòng đăng nhập bằng tài khoản quản trị viên.' : 'Tài khoản hiện tại không có quyền xem tổng quan hệ thống.')}</p>
            <div className="admin-dashboard-gate-actions">
              {dashboard.status === 'signed-out' && <Link to="/login?returnTo=/admin">Đăng nhập</Link>}
              {dashboard.status === 'error' && <button type="button" onClick={() => setRevision(value => value + 1)}>Thử tải lại</button>}
              <Link to="/">Về trang chủ</Link>
            </div>
          </>}
      </section>
    </main>
  }

  const { admin, stats } = dashboard
  return <div className="admin-dashboard-shell">
    <header className="admin-dashboard-topbar">
      <Link to="/" className="admin-dashboard-brand"><span aria-hidden="true">✳</span> EduLearn</Link>
      <WorkspaceNavigation role="ADMIN" />
      <Link to="/profile" className="admin-dashboard-account"><strong>{admin.fullName}</strong><span>Quản trị viên</span></Link>
    </header>

    <main className="admin-dashboard-content">
      <section className="admin-dashboard-welcome">
        <div><span className="admin-dashboard-kicker">BẢNG ĐIỀU KHIỂN QUẢN TRỊ</span><h1>Xin chào, {admin.fullName}</h1><p>Theo dõi quy mô và các việc cần xử lý trên EduLearn.</p></div>
        <Link className="admin-dashboard-primary" to="/admin/accounts">Quản lý tài khoản <span aria-hidden="true">↗</span></Link>
      </section>

      <section className="admin-dashboard-metrics" aria-label="Số liệu hệ thống">
        <Metric icon="♙" label="Quản trị viên" value={stats.admins} tone="purple" href="/admin/accounts" />
        <Metric icon="♧" label="Giáo viên" value={stats.teachers} tone="blue" href="/admin/accounts" />
        <Metric icon="♧" label="Học sinh" value={stats.students} detail={`${stats.enrolledStudents} lượt trong các lớp`} tone="green" href="/classrooms" />
        <Metric icon="▦" label="Lớp học" value={stats.classrooms} tone="orange" href="/classrooms" />
      </section>

      <section className="admin-dashboard-work" aria-label="Công việc cần chú ý">
        <div className="admin-dashboard-section-heading"><div><span className="admin-dashboard-kicker">CẦN THEO DÕI</span><h2>Hàng chờ và tài khoản</h2></div><button type="button" onClick={() => setRevision(value => value + 1)}>Làm mới số liệu</button></div>
        <div className="admin-dashboard-queue-grid">
          <Link className="admin-dashboard-queue-card" to="/exercises/review"><span className="admin-dashboard-queue-icon queue-exercise">✎</span><span><strong>Bài tập chờ duyệt</strong><small>Kiểm tra nội dung AI và bài giáo viên soạn</small></span><b>{stats.pendingExercises}</b></Link>
          <Link className="admin-dashboard-queue-card" to="/admin/accounts"><span className="admin-dashboard-queue-icon queue-account">◎</span><span><strong>Tài khoản chờ xác minh</strong><small>Danh sách đang chờ xác thực email</small></span><b>{stats.pendingAccounts}</b></Link>
        </div>
      </section>

      <section className="admin-dashboard-shortcuts" aria-labelledby="admin-shortcuts-title">
        <div className="admin-dashboard-section-heading"><div><span className="admin-dashboard-kicker">TRUY CẬP NHANH</span><h2 id="admin-shortcuts-title">Quản lý hệ thống</h2></div></div>
        <div className="admin-dashboard-shortcut-grid">
          <Shortcut to="/admin/accounts" title="Tài khoản" description="Tạo tài khoản, cập nhật vai trò và khóa hoặc mở khóa người dùng." icon="♙" />
          <Shortcut to="/classrooms" title="Lớp học" description="Xem lớp, giáo viên phụ trách và số học sinh đang tham gia." icon="▦" />
          <Shortcut to="/academic" title="Chương trình học" description="Quản lý môn học, chủ đề và bài học trong chương trình." icon="▤" />
          <Shortcut to="/exercises/review" title="Duyệt bài tập" description="Rà soát bài tập AI và nội dung giáo viên gửi duyệt." icon="✎" />
        </div>
      </section>
    </main>
  </div>
}

function Metric({ icon, label, value, detail, tone, href }) {
  return <Link className="admin-dashboard-metric" to={href}>
    <span className={`admin-dashboard-metric-icon metric-${tone}`} aria-hidden="true">{icon}</span>
    <span className="admin-dashboard-metric-label">{label}</span>
    <strong>{Number(value).toLocaleString('vi-VN')}</strong>
    <small>{detail || 'Tổng số hiện tại'}</small>
  </Link>
}

function Shortcut({ to, title, description, icon }) {
  return <Link className="admin-dashboard-shortcut" to={to}><span aria-hidden="true">{icon}</span><div><h3>{title}</h3><p>{description}</p></div><b aria-hidden="true">↗</b></Link>
}
