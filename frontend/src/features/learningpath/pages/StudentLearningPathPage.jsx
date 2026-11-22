import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import WorkspaceNavigation from '../../../shared/components/navigation/WorkspaceNavigation'
import { getLearningPaths, getMySession, updateLearningStep } from '../services/learningPathApi'
import '../styles/learningpath.css'

export default function StudentLearningPathPage() {
  const [student, setStudent] = useState(null)
  const [paths, setPaths] = useState([])
  const [state, setState] = useState('loading')
  const [message, setMessage] = useState('')
  const [busy, setBusy] = useState(false)
  const [updateError, setUpdateError] = useState('')

  useEffect(() => {
    document.title = 'Lộ trình của em · EduLearn'
    let active = true
    getMySession().then(async session => {
      if (!active) return
      if (session.role !== 'STUDENT') { setState('forbidden'); return }
      setStudent(session)
      try { const result = await getLearningPaths(session.id); if (active) { setPaths(result); setState('ready') } }
      catch { if (active) setState('error') }
    }).catch(error => {
      if (active) setState(error.status === 401 ? 'signed-out' : 'error')
    })
    return () => { active = false }
  }, [])

  const completeStep = async (path, step) => {
    setBusy(true)
    setMessage('')
    setUpdateError('')
    try {
      const updated = await updateLearningStep(student.id, path.id, step.id, 'COMPLETED')
      setPaths(current => current.map(item => item.id === updated.id ? updated : item))
      setMessage(updated.status === 'COMPLETED' ? 'Tuyệt vời! Em đã hoàn thành lộ trình.' : 'Em đã học xong bước này. Bước tiếp theo đã mở!')
    } catch (error) { setUpdateError(error.message) }
    finally { setBusy(false) }
  }

  if (state === 'loading') return <main className="student-learning-shell"><p role="status">Đang mở lộ trình học tập…</p></main>
  if (state === 'signed-out') return <Gate title="Đăng nhập để xem lộ trình" description="Đăng nhập bằng tài khoản học sinh để xem em cần học gì." signedOut />
  if (state === 'forbidden') return <Gate title="Trang dành cho học sinh" description="Hãy đăng nhập bằng tài khoản học sinh để xem lộ trình của mình." />

  const currentPath = paths.find(path => path.status === 'ACTIVE')
  const completedPath = paths.some(path => path.status === 'COMPLETED')
  return <main className="student-learning-shell">
    <header className="student-learning-topbar">
      <Link to="/" className="student-learning-brand">EduLearn</Link>
      {student && <WorkspaceNavigation role={student.role} />}
      <span className="student-learning-name">{student?.fullName}</span>
    </header>
    <section className="student-learning-hero"><h1>Lộ trình của em</h1><p>Xem bước đang học và mở bài luyện tập của em.</p></section>
    {state === 'error' && <p className="student-learning-error" role="alert">Chưa tải được lộ trình. Em hãy tải lại trang để thử lại.</p>}
    {state === 'ready' && message && <p className="student-learning-message" role="status">{message}</p>}
    {updateError && <p className="student-learning-error" role="alert">{updateError}</p>}
    {currentPath && <section className="student-learning-card" aria-labelledby="student-progress-title">
      <h2 id="student-progress-title">Em đã học xong {currentPath.progress.completed} trên {currentPath.progress.total} bước</h2>
      <div className="learning-path-progress student-progress" role="progressbar" aria-labelledby="student-progress-title" aria-valuenow={currentPath.progress.percentage}>
        <span style={{ width: `${currentPath.progress.percentage}%` }} />
      </div>
      <ol className="student-learning-steps">
        {currentPath.steps.map(step => (
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
              {step.status === 'LOCKED' && <p>Học xong bước trước để mở bài này.</p>}
              {step.status === 'IN_PROGRESS' && <>
                <p>Mục tiêu của bài này là đạt {step.targetScore}%.</p>
                <div className="student-step-actions">
                  <Link className="student-learning-action" to={`/exercises/steps/${step.id}`}>Mở bài luyện tập <span aria-hidden="true">→</span></Link>
                  <button type="button" disabled={busy} onClick={() => completeStep(currentPath, step)}>{busy ? 'Đang lưu…' : 'Đánh dấu đã học xong'}</button>
                </div>
              </>}
            </article>
          </li>
        ))}
      </ol>
    </section>}
    {state === 'ready' && !currentPath && <section className="student-learning-card student-learning-complete">
      <h2>{completedPath ? 'Em đã hoàn thành lộ trình!' : 'Em chưa có lộ trình học'}</h2>
      <p>{completedPath ? 'Em có thể xem bài kiểm tra thầy cô đã giao.' : 'Khi thầy cô tạo lộ trình cho em, các bước học sẽ hiện ở đây.'}</p>
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
