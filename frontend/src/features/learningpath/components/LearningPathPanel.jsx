import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { generateLearningPath, getLearningPaths } from '../services/learningPathApi'
import { createManualStepExercise, generateStepExercise } from '../../exercises/services/exerciseApi'
import ManualExerciseForm from './ManualExerciseForm'
import '../styles/learningpath.css'

export default function LearningPathPanel({ students }) {
  const [studentId, setStudentId] = useState('')
  const [paths, setPaths] = useState([])
  const [state, setState] = useState('idle')
  const [message, setMessage] = useState('')
  const [loadedStudentId, setLoadedStudentId] = useState('')
  const [generatingStepId, setGeneratingStepId] = useState(null)
  const [generatedExercise, setGeneratedExercise] = useState(null)
  const [generationError, setGenerationError] = useState('')
  const [failedStepId, setFailedStepId] = useState(null)
  const [manualStepId, setManualStepId] = useState(null)
  const [savingManualStepId, setSavingManualStepId] = useState(null)
  const [manualExerciseNotice, setManualExerciseNotice] = useState('')
  const [manualExerciseError, setManualExerciseError] = useState('')

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
      const unchanged = paths.some(path => path.status === 'ACTIVE' && String(path.id) === String(created.id))
      setPaths(current => current.some(path => String(path.id) === String(created.id))
        ? current.map(path => String(path.id) === String(created.id) ? created : path)
        : [created, ...current.map(path => path.status === 'ACTIVE' ? { ...path, status: 'ARCHIVED' } : path)])
      setMessage(unchanged
        ? 'Lộ trình không thay đổi; không tạo phiên bản mới.'
        : 'Đã tạo lộ trình mới từ các kiến thức cần củng cố.')
      setState('ready')
    } catch (error) { setMessage(error.message); setState('ready') }
  }

  const createExercise = async step => {
    setGeneratingStepId(step.id)
    setGeneratedExercise(null)
    setGenerationError('')
    setFailedStepId(null)
    try {
      const result = await generateStepExercise(step.id)
      setGeneratedExercise({ stepId: step.id, ...result })
    } catch (error) {
      setGenerationError(error.message)
      setFailedStepId(step.id)
      setManualStepId(step.id)
    } finally {
      setGeneratingStepId(null)
    }
  }

  const saveManualExercise = async (step, body) => {
    setSavingManualStepId(step.id)
    setManualExerciseError('')
    setManualExerciseNotice('')
    try {
      await createManualStepExercise(step.id, body)
      setManualExerciseNotice(`Đã lưu bài tập cho “${step.lessonName}” vào hàng chờ duyệt.`)
      setGenerationError('')
      setFailedStepId(null)
      setManualStepId(null)
    } catch (error) {
      setManualExerciseError(error.message)
    } finally {
      setSavingManualStepId(null)
    }
  }

  const selected = students.find(student => String(student.id) === selectedStudentId)
  const isLoading = Boolean(selectedStudentId) && loadedStudentId !== selectedStudentId
  const visiblePaths = loadedStudentId === selectedStudentId ? paths : []
  const activePath = visiblePaths.find(path => path.status === 'ACTIVE')

  return <section className="learning-path-panel" aria-labelledby="learning-path-title">
    <div className="learning-path-heading">
      <div><span className="classrooms-kicker">KẾ HOẠCH CỦNG CỐ</span><h3 id="learning-path-title">Lộ trình học tập</h3><p>Xếp bước theo mức độ ưu tiên của khoảng trống kiến thức.</p></div>
      <label>Chọn học sinh<select aria-label="Chọn học sinh xem lộ trình" value={selectedStudentId} onChange={event => { setStudentId(event.target.value); setPaths([]); setGeneratedExercise(null); setGenerationError(''); setFailedStepId(null); setManualStepId(null); setManualExerciseNotice(''); setManualExerciseError('') }}>
        {students.map(student => <option key={student.id} value={student.id}>{student.fullName}</option>)}
      </select></label>
    </div>
    {!students.length ? <p className="learning-path-empty">Thêm học sinh vào lớp để tạo lộ trình cá nhân.</p>
      : isLoading ? <p className="learning-path-empty" role="status">Đang tải lộ trình của {selected?.fullName}…</p>
        : <>
          {message && <p className={state === 'error' ? 'learning-path-error' : 'learning-path-message'} role={state === 'error' ? 'alert' : 'status'}>{message}</p>}
          <div className="learning-path-toolbar"><p>{activePath ? `Phiên bản ${activePath.version} · ${activePath.progress?.percentage ?? 0}% hoàn thành` : 'Chưa có lộ trình đang học.'}</p><button type="button" disabled={state === 'saving'} onClick={createPath}>{state === 'saving' ? 'Đang tạo lộ trình…' : 'Tạo lộ trình từ khoảng trống'}</button></div>
          {state === 'saving' && <p className="learning-path-message" role="status">Backend đang sắp xếp theo mức độ ưu tiên; AI chỉ đề xuất thứ tự khi khả dụng.</p>}
          {activePath ? <PathSummary path={activePath} onGenerate={createExercise} generatingStepId={generatingStepId}
            failedStepId={failedStepId} manualStepId={manualStepId} onToggleManual={stepId => { setManualExerciseError(''); setManualStepId(current => current === stepId ? null : stepId) }}
            onCreateManual={saveManualExercise} savingManualStepId={savingManualStepId} manualExerciseError={manualExerciseError} />
            : <p className="learning-path-empty">Tạo lộ trình để sắp xếp các kiến thức cần ôn theo thứ tự ưu tiên.</p>}
          {generatingStepId !== null && <p className="learning-path-message" role="status">AI đang soạn bài tập. Nội dung sẽ được lưu ở trạng thái chờ thầy cô duyệt.</p>}
          {generationError && <p className="learning-path-error" role="alert">AI chưa tạo được bài tập: {generationError} Thầy cô có thể soạn thủ công ngay tại bước học.</p>}
          {generatedExercise && <div className="learning-path-ai-result" role="status">
            <div><strong>{generatedExercise.demo ? 'Bài minh họa' : `Đã gọi ${generatedExercise.provider}`}</strong><span>{generatedExercise.notice}</span></div>
            <p>{generatedExercise.question.content}</p>
            <Link to="/exercises/review">Mở hàng chờ duyệt</Link>
          </div>}
          {manualExerciseNotice && <p className="learning-path-message" role="status">{manualExerciseNotice} <Link to="/exercises/review">Mở hàng chờ duyệt</Link></p>}
          {visiblePaths.filter(path => path.status !== 'ACTIVE').length > 0 && <details className="learning-path-history"><summary>Lộ trình trước đây</summary>{visiblePaths.filter(path => path.status !== 'ACTIVE').map(path => <PathSummary key={path.id} path={path} />)}</details>}
        </>}
  </section>
}

function PathSummary({ path, onGenerate, generatingStepId, failedStepId, manualStepId, onToggleManual, onCreateManual, savingManualStepId, manualExerciseError }) {
  const progress = path.progress || { completed: 0, total: path.steps.length, percentage: 0 }
  return <article className="learning-path-card">
    <div className="learning-path-card-heading"><strong>Phiên bản {path.version}</strong><span>{path.status === 'COMPLETED' ? 'Đã hoàn thành' : path.status === 'ARCHIVED' ? 'Đã lưu trữ' : 'Đang học'}</span></div>
    <div className="learning-path-progress"><span style={{ width: `${progress.percentage}%` }} /></div>
    <p>{progress.completed}/{progress.total} bước · {progress.percentage}%</p>
    <ol className="learning-path-steps">{path.steps.map(step => <li key={step.id} className={`step-${step.status.toLowerCase()}`}>
      <span className="learning-path-step-icon">{step.status === 'COMPLETED' ? '✓' : step.order}</span>
      <div>
        <strong>{step.lessonName}</strong>
        <small>{step.lessonDescription || 'Ôn tập kiến thức này'}</small>
        <em>{step.status === 'COMPLETED' ? 'Đã hoàn thành' : step.status === 'IN_PROGRESS' ? 'Đang học' : 'Chưa mở'} · Mục tiêu {step.targetScore}%</em>
        {onGenerate && <div className="learning-path-step-actions">
          <button type="button" className="learning-path-ai-button" disabled={generatingStepId !== null || savingManualStepId !== null} onClick={() => onGenerate(step)}>{generatingStepId === step.id ? 'Đang gọi AI…' : 'Sinh bài tập AI'}</button>
          <button type="button" className="learning-path-manual-button" disabled={generatingStepId !== null || savingManualStepId !== null} onClick={() => onToggleManual(step.id)}>{manualStepId === step.id ? 'Đóng biểu mẫu' : failedStepId === step.id ? 'Soạn thủ công thay thế' : 'Tự soạn bài tập'}</button>
        </div>}
        {onCreateManual && manualStepId === step.id && <ManualExerciseForm step={step} saving={savingManualStepId === step.id}
          error={manualExerciseError} onCancel={() => onToggleManual(step.id)} onSubmit={body => onCreateManual(step, body)} />}
      </div>
    </li>)}</ol>
  </article>
}
