import { useEffect, useState } from 'react'
import { generateLearningPath, getLearningPaths } from '../services/learningPathApi'
import '../styles/learningpath.css'

export default function LearningPathPanel({ students }) {
  const [studentId, setStudentId] = useState('')
  const [paths, setPaths] = useState([])
  const [state, setState] = useState('idle')
  const [message, setMessage] = useState('')
  const [loadedStudentId, setLoadedStudentId] = useState('')

  const selectedStudentId = students.some(student => String(student.id) === studentId)
    ? studentId
    : String(students[0]?.id ?? '')

  useEffect(() => {
    if (!selectedStudentId) return
    let active = true
    getLearningPaths(selectedStudentId)
      .then(result => { if (active) { setPaths(result); setMessage(''); setState('ready'); setLoadedStudentId(selectedStudentId) } })
      .catch(error => { if (active) { setMessage(error.message); setState('error'); setLoadedStudentId(selectedStudentId) } })
    return () => { active = false }
  }, [selectedStudentId])

  const createPath = async () => {
    setState('saving'); setMessage('')
    try {
      const created = await generateLearningPath(selectedStudentId)
      setPaths(current => [created, ...current.map(path => path.status === 'ACTIVE' ? { ...path, status: 'ARCHIVED' } : path)])
      setMessage('Đã tạo lộ trình mới từ các kiến thức cần củng cố.')
      setState('ready')
    } catch (error) { setMessage(error.message); setState('ready') }
  }

  const selected = students.find(student => String(student.id) === selectedStudentId)
  const isLoading = Boolean(selectedStudentId) && loadedStudentId !== selectedStudentId
  const visiblePaths = loadedStudentId === selectedStudentId ? paths : []
  const activePath = visiblePaths.find(path => path.status === 'ACTIVE')

  return <section className="learning-path-panel" aria-labelledby="learning-path-title">
    <div className="learning-path-heading">
      <div><span className="classrooms-kicker">KẾ HOẠCH CỦNG CỐ</span><h3 id="learning-path-title">Lộ trình học tập</h3><p>Xếp bước theo mức độ ưu tiên của khoảng trống kiến thức.</p></div>
      <label>Chọn học sinh<select aria-label="Chọn học sinh xem lộ trình" value={selectedStudentId} onChange={event => { setStudentId(event.target.value); setPaths([]) }}>
        {students.map(student => <option key={student.id} value={student.id}>{student.fullName}</option>)}
      </select></label>
    </div>
    {!students.length ? <p className="learning-path-empty">Thêm học sinh vào lớp để tạo lộ trình cá nhân.</p>
      : isLoading ? <p className="learning-path-empty" role="status">Đang tải lộ trình của {selected?.fullName}…</p>
        : <>
          {message && <p className={state === 'error' ? 'learning-path-error' : 'learning-path-message'} role={state === 'error' ? 'alert' : 'status'}>{message}</p>}
          <div className="learning-path-toolbar"><p>{activePath ? `Phiên bản ${activePath.version} · ${activePath.progress?.percentage ?? 0}% hoàn thành` : 'Chưa có lộ trình đang học.'}</p><button type="button" disabled={state === 'saving'} onClick={createPath}>{state === 'saving' ? 'Đang tạo…' : 'Tạo lộ trình từ khoảng trống'}</button></div>
          {activePath ? <PathSummary path={activePath} /> : <p className="learning-path-empty">Tạo lộ trình để sắp xếp các kiến thức cần ôn theo thứ tự ưu tiên.</p>}
          {visiblePaths.filter(path => path.status !== 'ACTIVE').length > 0 && <details className="learning-path-history"><summary>Lộ trình trước đây</summary>{visiblePaths.filter(path => path.status !== 'ACTIVE').map(path => <PathSummary key={path.id} path={path} />)}</details>}
        </>}
  </section>
}

function PathSummary({ path }) {
  const progress = path.progress || { completed: 0, total: path.steps.length, percentage: 0 }
  return <article className="learning-path-card"><div className="learning-path-card-heading"><strong>Phiên bản {path.version}</strong><span>{path.status === 'COMPLETED' ? 'Đã hoàn thành' : path.status === 'ARCHIVED' ? 'Đã lưu trữ' : 'Đang học'}</span></div><div className="learning-path-progress"><span style={{ width: `${progress.percentage}%` }} /></div><p>{progress.completed}/{progress.total} bước · {progress.percentage}%</p><ol className="learning-path-steps">{path.steps.map(step => <li key={step.id} className={`step-${step.status.toLowerCase()}`}><span className="learning-path-step-icon">{step.status === 'COMPLETED' ? '✓' : step.order}</span><div><strong>{step.lessonName}</strong><small>{step.lessonDescription || 'Ôn tập kiến thức này'}</small><em>{step.status === 'COMPLETED' ? 'Đã hoàn thành' : step.status === 'IN_PROGRESS' ? 'Đang học' : 'Chưa mở'} · Mục tiêu {step.targetScore}%</em></div></li>)}</ol></article>
}
