import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getLearningPaths, getMySession, updateLearningStep } from '../services/learningPathApi'
import '../styles/learningpath.css'

export default function StudentLearningPathPage() {
  const [student, setStudent] = useState(null)
  const [paths, setPaths] = useState([])
  const [state, setState] = useState('loading')
  const [message, setMessage] = useState('')
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    let active = true
    getMySession().then(async session => {
      if (!active) return
      if (session.role !== 'STUDENT') { setState('forbidden'); return }
      setStudent(session)
      try { const result = await getLearningPaths(session.id); if (active) { setPaths(result); setState('ready') } }
      catch (error) { if (active) { setMessage(error.message); setState('error') } }
    }).catch(error => { if (active) { setMessage(error.message); setState('signed-out') } })
    return () => { active = false }
  }, [])

  const completeStep = async (path, step) => {
    setBusy(true); setMessage('')
    try {
      const updated = await updateLearningStep(student.id, path.id, step.id, 'COMPLETED')
      setPaths(current => current.map(item => item.id === updated.id ? updated : item))
      setMessage(updated.status === 'COMPLETED' ? 'Tuyệt vời! Con đã hoàn thành lộ trình.' : 'Con đã hoàn thành bước học. Bước tiếp theo đã được mở!')
    } catch (error) { setMessage(error.message) }
    finally { setBusy(false) }
  }

  if (state === 'loading') return <main className="student-learning-shell"><p role="status">Đang mở lộ trình học tập…</p></main>
  if (state === 'signed-out') return <Gate title="Đăng nhập để xem lộ trình" description={message || 'Lộ trình học tập được lưu riêng cho từng học sinh.'} />
  if (state === 'forbidden') return <Gate title="Trang dành cho học sinh" description="Hãy đăng nhập bằng tài khoản học sinh để xem lộ trình của mình." />

  const currentPath = paths.find(path => path.status === 'ACTIVE')
  return <main className="student-learning-shell">
    <header className="student-learning-topbar"><Link to="/" className="student-learning-brand">✳ TLCN</Link><div><span>{student?.fullName}</span><Link to="/">Trang chủ</Link></div></header>
    <section className="student-learning-hero"><span className="student-learning-sparkle" aria-hidden="true">✦</span><p className="student-learning-kicker">HÀNH TRÌNH CỦA CON</p><h1>Lộ trình học tập</h1><p>Mỗi bước nhỏ giúp con tiến gần hơn đến mục tiêu. Cố lên nhé!</p></section>
    {message && <p className="student-learning-message" role="status">{message}</p>}
    {state === 'error' && <p className="student-learning-error" role="alert">{message}</p>}
    {currentPath ? <section className="student-learning-card"><div className="student-learning-progress-heading"><div><span>PHIÊN BẢN {currentPath.version}</span><h2>Con đã đi được {currentPath.progress.percentage}%</h2></div><strong>{currentPath.progress.completed}<small>/{currentPath.progress.total}</small></strong></div><div className="learning-path-progress student-progress"><span style={{ width: `${currentPath.progress.percentage}%` }} /></div><ol className="student-learning-steps">{currentPath.steps.map(step => <li key={step.id} className={`step-${step.status.toLowerCase()}`}><span className="student-step-marker">{step.status === 'COMPLETED' ? '✓' : step.order}</span><article><div className="student-step-heading"><div><span>{step.status === 'COMPLETED' ? 'HOÀN THÀNH' : step.status === 'IN_PROGRESS' ? 'ĐANG HỌC' : 'SẼ MỞ SAU'}</span><h3>{step.lessonName}</h3></div>{step.status === 'IN_PROGRESS' && <span className="student-step-star">★</span>}</div><p>{step.lessonDescription || 'Cùng ôn tập kiến thức này nhé.'}</p><small>Mục tiêu: đạt {step.targetScore}%</small>{step.status === 'IN_PROGRESS' && <><Link className="student-exercise-link" to={`/exercises/steps/${step.id}`}>Mở bài luyện tập</Link><button disabled={busy} onClick={() => completeStep(currentPath, step)}>{busy ? 'Đang lưu…' : 'Con đã học xong bước này'}</button></>}</article></li>)}</ol></section> : paths.length ? <section className="student-learning-card student-learning-complete"><span>🌟</span><h2>Con đã hoàn thành lộ trình!</h2><p>Thầy cô sẽ tạo hành trình mới nếu con còn kiến thức cần luyện thêm.</p></section> : <section className="student-learning-card student-learning-complete"><span>🌱</span><h2>Hành trình sắp bắt đầu</h2><p>Khi thầy cô tạo lộ trình riêng cho con, các bước học sẽ xuất hiện ở đây.</p></section>}
    <footer className="student-learning-footer">Con có thể nhờ thầy cô giúp đỡ bất cứ lúc nào nhé.</footer>
  </main>
}

function Gate({ title, description }) {
  return <main className="student-learning-shell"><section className="student-learning-card student-learning-complete"><h1>{title}</h1><p>{description}</p><Link to="/login">Đi đến đăng nhập</Link></section></main>
}
