import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import HomeHeader from '../../home/components/HomeHeader'
import { getLearningPaths, getMySession, getStudentClassrooms } from '../services/learningPathApi'
import { groupStepsByTopic } from '../utils/groupStepsByTopic'
import '../styles/learningpath.css'

export default function StudentLearningPathPage() {
  const [student, setStudent] = useState(null)
  const [paths, setPaths] = useState([])
  const [classrooms, setClassrooms] = useState([])
  const [selectedClassroomId, setSelectedClassroomId] = useState('')
  const [state, setState] = useState('loading')

  useEffect(() => {
    document.title = 'Lộ trình của em · EduLearn'
    let active = true
    getMySession().then(async session => {
      if (!active) return
      if (session.role !== 'STUDENT') { setState('forbidden'); return }
      setStudent(session)
      try {
        const [result, memberships] = await Promise.all([
          getLearningPaths(session.id), getStudentClassrooms(session.id),
        ])
        if (active) {
          setPaths(result)
          setClassrooms(memberships)
          const current = result.find(path => path.status === 'ACTIVE')
          setSelectedClassroomId(String(current?.classroomId ?? memberships[0]?.id ?? ''))
          setState('ready')
        }
      }
      catch { if (active) setState('error') }
    }).catch(error => {
      if (active) setState(error.status === 401 ? 'signed-out' : 'error')
    })
    return () => { active = false }
  }, [])

  if (state === 'loading') return <main className="student-learning-shell"><p role="status">Đang mở lộ trình học tập…</p></main>
  if (state === 'signed-out') return <Gate title="Đăng nhập để xem lộ trình" description="Đăng nhập bằng tài khoản học sinh để xem em cần học gì." signedOut />
  if (state === 'forbidden') return <Gate title="Trang dành cho học sinh" description="Hãy đăng nhập bằng tài khoản học sinh để xem lộ trình của mình." />

  const classPaths = selectedClassroomId ? paths.filter(path => String(path.classroomId) === selectedClassroomId) : paths
  const currentPath = classPaths.find(path => path.status === 'ACTIVE')
  const completedPath = classPaths.some(path => path.status === 'COMPLETED')
  const topicGroups = groupStepsByTopic(currentPath?.steps)
  const selectedClassroom = classrooms.find(item => String(item.id) === selectedClassroomId)
  return <main id="noi-dung" tabIndex={-1} className="student-learning-shell">
    <HomeHeader user={student} onLogout={() => setStudent(null)} />
    <section className="student-learning-hero"><h1>Lộ trình của em</h1><p>Xem bước đang học và mở bài luyện tập của em.</p></section>
    {classrooms.length > 0 && <label className="student-learning-class-picker">Lớp học
      <select aria-label="Chọn lớp xem lộ trình" value={selectedClassroomId} onChange={event => setSelectedClassroomId(event.target.value)}>
        {classrooms.map(classroom => <option key={classroom.id} value={classroom.id}>{classroom.name} · {classroom.schoolYear}</option>)}
      </select>
    </label>}
    {selectedClassroom && <p className="student-learning-class-context">Lộ trình của lớp <strong>{selectedClassroom.name}</strong></p>}
    {state === 'error' && <p className="student-learning-error" role="alert">Chưa tải được lộ trình. Em hãy tải lại trang để thử lại.</p>}
    {currentPath && <section className="student-learning-card" aria-labelledby="student-progress-title">
      <h2 id="student-progress-title">Em đã học xong {currentPath.progress.completed} trên {currentPath.progress.total} bước</h2>
      <div className="learning-path-progress student-progress" role="progressbar" aria-labelledby="student-progress-title" aria-valuenow={currentPath.progress.percentage}>
        <span style={{ width: `${currentPath.progress.percentage}%` }} />
      </div>
      <div className="student-learning-topics" aria-label="Các chủ đề cần học lại">
        {topicGroups.map((group, index) => <details className="student-learning-topic" key={group.id}
          open={group.steps.some(step => step.status === 'IN_PROGRESS') || (!currentPath.steps.some(step => step.status === 'IN_PROGRESS') && index === 0)}>
          <summary><span>{group.name}</span><small>{group.steps.length} bài cần ôn</small></summary>
          <ol className="student-learning-steps">
        {group.steps.map(step => (
          <li key={step.id} className={`step-${step.status.toLowerCase()}`} aria-current={step.status === 'IN_PROGRESS' ? 'step' : undefined}>
            <span className="student-step-marker" aria-hidden="true">{step.status === 'COMPLETED' ? '✓' : step.order}</span>
            <article>
              <div className="student-step-heading">
                <div>
                  <span>{step.status === 'COMPLETED' ? 'Đã học xong' : step.status === 'IN_PROGRESS' ? 'Đang học' : 'Chưa mở'}</span>
                  <h3>{step.lessonName}</h3>
                </div>
              </div>
              {step.lessonDescription && <p>{step.lessonDescription}</p>}
              {step.status === 'LOCKED' && <p>Bài luyện tập mở sau khi em học xong bước trước. Em vẫn có thể xem nội dung để ôn bài.</p>}
              {step.lessonId && <div className="student-step-actions">
                <Link className="student-learning-action student-learning-review-action" to={`/curriculum/lessons/${step.lessonId}?stepId=${step.id}`}>Xem bài học chi tiết <span aria-hidden="true">→</span></Link>
              </div>}
              {step.status === 'IN_PROGRESS' && <>
                <p>Mục tiêu của bài này là đạt {step.targetScore}%.</p>
                <p>Hoàn thành bước sau khi nộp bài luyện tập và đạt mục tiêu.</p>
                <div className="student-step-actions">
                  <Link className="student-learning-action" to={`/exercises/steps/${step.id}`}>Mở bài luyện tập <span aria-hidden="true">→</span></Link>
                </div>
              </>}
            </article>
          </li>
        ))}
          </ol>
        </details>)}
      </div>
    </section>}
    {state === 'ready' && !currentPath && <section className="student-learning-card student-learning-complete">
      <h2>{completedPath ? 'Em đã hoàn thành lộ trình!' : 'Em chưa có lộ trình học'}</h2>
      <p>{completedPath ? 'Em có thể xem bài kiểm tra thầy cô đã giao.' : `Khi thầy cô tạo lộ trình cho em ở lớp ${selectedClassroom?.name || 'này'}, các bước học sẽ hiện ở đây.`}</p>
      <Link className="student-learning-action" to="/assessments">Xem bài kiểm tra</Link>
    </section>}
  </main>
}

function Gate({ title, description, signedOut }) {
  return <main className="student-learning-shell"><section className="student-learning-card student-learning-complete student-learning-gate">
    <h1>{title}</h1><p>{description}</p>
    {signedOut && <Link className="student-learning-action" to="/login?returnTo=/my-learning-path">Đăng nhập</Link>}
    <Link className="student-learning-back" to="/">Về trang chủ</Link>
  </section></main>
}
