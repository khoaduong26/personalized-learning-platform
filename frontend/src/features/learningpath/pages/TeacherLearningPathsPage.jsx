import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import HomeHeader from '../../home/components/HomeHeader'
import { getClassroomStudents, getClassrooms } from '../../classrooms/services/classroomApi'
import LearningPathPanel from '../components/LearningPathPanel'
import { getMySession } from '../services/learningPathApi'
import '../styles/learningpath.css'

export default function TeacherLearningPathsPage() {
  const [teacher, setTeacher] = useState(null)
  const [classrooms, setClassrooms] = useState([])
  const [classroomId, setClassroomId] = useState('')
  const [students, setStudents] = useState([])
  const [access, setAccess] = useState('loading')
  const [rosterState, setRosterState] = useState('idle')
  const [error, setError] = useState('')

  useEffect(() => {
    document.title = 'Lộ trình học sinh · EduLearn'
    let active = true
    getMySession().then(async session => {
      if (!active) return
      if (session.role !== 'TEACHER') { setAccess('forbidden'); return }
      setTeacher(session)
      try {
        const items = await getClassrooms()
        if (!active) return
        setClassrooms(items)
        setClassroomId(String(items[0]?.id ?? ''))
        setAccess('ready')
      } catch (loadError) {
        if (active) { setError(loadError.message); setAccess('error') }
      }
    }).catch(loadError => {
      if (active) setAccess(loadError.status === 401 ? 'signed-out' : 'error')
    })
    return () => { active = false }
  }, [])

  useEffect(() => {
    if (!classroomId || access !== 'ready') { setStudents([]); setRosterState('idle'); return }
    let active = true
    setStudents([])
    setRosterState('loading')
    setError('')
    getClassroomStudents(classroomId).then(items => {
      if (active) { setStudents(items); setRosterState('ready') }
    }).catch(loadError => {
      if (active) { setError(loadError.message); setRosterState('error') }
    })
    return () => { active = false }
  }, [classroomId, access])

  if (access === 'loading') return <main className="teacher-learning-paths-shell"><p role="status">Đang mở trang quản lý lộ trình…</p></main>
  if (access === 'signed-out') return <PathGate title="Đăng nhập để quản lý lộ trình" signedOut />
  if (access === 'forbidden') return <PathGate title="Trang dành cho giáo viên" description="Chỉ giáo viên mới có thể quản lý lộ trình học sinh." />
  if (access === 'error' && !classrooms.length) return <PathGate title="Chưa tải được danh sách lớp" description={error} />

  const selectedClassroom = classrooms.find(item => String(item.id) === classroomId)
  return <main className="teacher-learning-paths-shell">
    <HomeHeader user={teacher} onLogout={() => setTeacher(null)} />
    <section className="teacher-learning-paths-heading">
      <p className="teacher-learning-paths-kicker">QUẢN LÝ LỚP HỌC</p>
      <h1>Lộ trình học sinh</h1>
      <p>Chọn lớp và học sinh để xem lộ trình đang học, các phiên bản trước và quản lý việc xóa.</p>
    </section>
    {classrooms.length > 0 ? <>
      <section className="teacher-learning-paths-filter">
        <label>Lớp học
          <select aria-label="Chọn lớp xem lộ trình" value={classroomId} onChange={event => setClassroomId(event.target.value)}>
            {classrooms.map(classroom => <option key={classroom.id} value={classroom.id}>{classroom.name} · {classroom.schoolYear}</option>)}
          </select>
        </label>
        {selectedClassroom && <p>{students.length} học sinh trong lớp <strong>{selectedClassroom.name}</strong></p>}
      </section>
      {error && <p className="teacher-learning-paths-error" role="alert">{error}</p>}
      {rosterState === 'loading' ? <p className="teacher-learning-paths-state" role="status">Đang tải danh sách học sinh…</p>
        : rosterState === 'ready' ? <LearningPathPanel key={classroomId} students={students} classroomId={classroomId} classroomName={selectedClassroom?.name || ''} />
          : null}
    </> : <section className="teacher-learning-paths-empty"><h2>Chưa có lớp học</h2><p>Tạo lớp hoặc tham gia quản lý một lớp để xem lộ trình học sinh.</p><Link to="/classrooms">Mở trang lớp học</Link></section>}
  </main>
}

function PathGate({ title, description, signedOut }) {
  return <main className="teacher-learning-paths-shell"><section className="teacher-learning-paths-empty">
    <h1>{title}</h1><p>{description || 'Đăng nhập bằng tài khoản giáo viên để xem lộ trình theo từng lớp và học sinh.'}</p>
    {signedOut && <Link to="/login?returnTo=/learning-paths">Đăng nhập</Link>}
    <Link to="/classrooms">Mở trang lớp học</Link>
  </section></main>
}
