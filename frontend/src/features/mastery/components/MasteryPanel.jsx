import { useEffect, useState } from 'react'
import {
  getClassroomMasteryDashboard, getKnowledgeGaps, getMasteryHistory,
  getMasteryProfile, getStudentErrors,
} from '../services/masteryApi'
import { gapStatusLabel, masteryLevelLabel, openGapCount, triggerSourceLabel } from '../utils/masteryPresentation'
import '../styles/mastery.css'

function MasteryHistory({ mastery }) {
  const [history, setHistory] = useState([])
  const [state, setState] = useState('idle')
  const [message, setMessage] = useState('')

  const loadHistory = () => {
    setState('loading')
    getMasteryHistory(mastery.id)
      .then(items => { setHistory(items); setState('ready') })
      .catch(error => { setMessage(error.message); setState('error') })
  }

  return <details className="mastery-history" onToggle={event => {
    if (event.currentTarget.open && state === 'idle') loadHistory()
  }}>
    <summary>Xem lịch sử</summary>
    {state === 'loading' && <p className="mastery-empty" role="status">Đang tải lịch sử…</p>}
    {state === 'error' && <><p className="mastery-error" role="alert">{message}</p><button type="button" className="mastery-retry" onClick={loadHistory}>Thử tải lại</button></>}
    {state === 'ready' && (history.length
      ? <ol className="mastery-history-list">{history.map(item => <li key={item.id}>
        <span>{item.oldScore == null ? 'Đánh giá lần đầu' : `${Math.round(item.oldScore)} → ${Math.round(item.newScore)} điểm`}</span>
        <time dateTime={item.recordedAt}>{new Date(item.recordedAt).toLocaleDateString('vi-VN')}</time>
        <small>{triggerSourceLabel(item.triggerSource)}</small>
      </li>)}</ol>
      : <p className="mastery-empty">Chưa có lần cập nhật trước đó.</p>)}
  </details>
}

function ClassroomDashboard({ classroomId }) {
  const [result, setResult] = useState({ classroomId: null, items: [], error: '' })

  useEffect(() => {
    if (!classroomId) return undefined
    let active = true
    getClassroomMasteryDashboard(classroomId)
      .then(items => { if (active) setResult({ classroomId, items, error: '' }) })
      .catch(error => { if (active) setResult({ classroomId, items: [], error: error.message }) })
    return () => { active = false }
  }, [classroomId])

  const state = !classroomId ? 'empty' : result.classroomId !== classroomId ? 'loading' : result.error ? 'error' : 'ready'
  const items = result.classroomId === classroomId ? result.items : []

  return <section className="mastery-dashboard" aria-labelledby="mastery-dashboard-title">
    <div className="mastery-section-heading">
      <div><h4 id="mastery-dashboard-title">Tổng quan năng lực lớp</h4><p>Điểm trung bình tính trên học sinh đã có dữ liệu cho từng bài học.</p></div>
      <span className="mastery-threshold-note">Cần xem lại: dưới 70/100</span>
    </div>
    {state === 'loading' && <p className="mastery-empty" role="status">Đang tải thống kê lớp…</p>}
    {state === 'error' && <p className="mastery-error" role="alert">{result.error}</p>}
    {(state === 'empty' || (state === 'ready' && !items.length)) && <p className="mastery-empty">Chưa có dữ liệu năng lực để tổng hợp cho lớp này.</p>}
    {state === 'ready' && items.length > 0 && <ul className="mastery-dashboard-list">
      {items.map(item => {
        const average = Math.round(Number(item.averageMastery) || 0)
        const reviewShare = item.studentsTracked ? Math.round(100 * item.studentsNeedingReview / item.studentsTracked) : 0
        return <li key={item.lessonId}>
          <div className="mastery-dashboard-copy">
            <strong>{item.lessonName || `Bài học ${item.lessonId}`}</strong>
            <span>{item.studentsNeedingReview}/{item.studentsTracked} học sinh cần xem lại</span>
          </div>
          <div className="mastery-dashboard-meter" role="img" aria-label={`Điểm trung bình ${average} trên 100; ${item.studentsNeedingReview} trên ${item.studentsTracked} học sinh dưới 70 điểm`}>
            <span style={{ width: `${Math.max(0, Math.min(100, average))}%` }} />
          </div>
          <b>{average}<small>/100</small></b>
          <div className="mastery-review-meter" aria-hidden="true"><span style={{ width: `${reviewShare}%` }} /></div>
        </li>
      })}
    </ul>}
    <p className="mastery-level-legend">Mức thành thạo: Bắt đầu dưới 50 · Đang phát triển 50–74 · Thành thạo 75–89 · Nắm vững từ 90.</p>
  </section>
}

export default function MasteryPanel({ students, classroomId }) {
  const [studentId, setStudentId] = useState('')
  const [profile, setProfile] = useState([])
  const [gaps, setGaps] = useState([])
  const [errors, setErrors] = useState([])
  const [state, setState] = useState('idle')
  const [message, setMessage] = useState('')
  const [loadedStudentId, setLoadedStudentId] = useState('')

  const selectedStudentId = students.some(student => String(student.id) === studentId)
    ? studentId
    : String(students[0]?.id ?? '')

  useEffect(() => {
    if (!selectedStudentId) return undefined
    const controller = new AbortController()
    Promise.all([
      getMasteryProfile(selectedStudentId),
      getKnowledgeGaps(selectedStudentId),
      getStudentErrors(selectedStudentId),
    ])
      .then(([masteries, knowledgeGaps, studentErrors]) => {
        if (controller.signal.aborted) return
        setProfile(masteries)
        setGaps(knowledgeGaps)
        setErrors(studentErrors)
        setMessage('')
        setState('ready')
        setLoadedStudentId(selectedStudentId)
      })
      .catch(error => {
        if (controller.signal.aborted) return
        setMessage(error.message)
        setState('error')
        setLoadedStudentId(selectedStudentId)
      })
    return () => controller.abort()
  }, [selectedStudentId])

  const selected = students.find(student => String(student.id) === selectedStudentId)
  const isLoading = Boolean(selectedStudentId) && (loadedStudentId !== selectedStudentId || state === 'loading')
  const average = profile.length
    ? Math.round(profile.reduce((sum, item) => sum + (Number(item.score) || 0), 0) / profile.length)
    : 0
  const activeGapCount = openGapCount(gaps)

  return <section className="mastery-panel" aria-labelledby="mastery-title">
    <div className="mastery-panel-heading">
      <div><span className="classrooms-kicker">THEO DÕI TIẾN BỘ</span><h3 id="mastery-title">Hồ sơ năng lực</h3><p>Mức độ thành thạo, lịch sử tiến bộ, kiến thức cần ôn và dạng lỗi đã ghi nhận.</p></div>
      {students.length > 0 && <label>Chọn học sinh<select aria-label="Chọn học sinh xem hồ sơ năng lực" value={selectedStudentId} onChange={event => setStudentId(event.target.value)}>
        {students.map(student => <option key={student.id} value={student.id}>{student.fullName}</option>)}
      </select></label>}
    </div>
    <ClassroomDashboard classroomId={classroomId} />
    {!students.length ? <p className="mastery-empty">Thêm học sinh vào lớp để bắt đầu theo dõi năng lực.</p>
      : isLoading ? <p className="mastery-empty" role="status">Đang tải hồ sơ của {selected?.fullName}…</p>
        : state === 'error' ? <p className="mastery-error" role="alert">{message}</p>
          : <>
            <div className="mastery-summary">
              <div><strong>{average}<small>/100</small></strong><span>Điểm trung bình các kiến thức</span></div>
              <div><strong>{profile.length}</strong><span>Kiến thức đã được đánh giá</span></div>
              <div><strong>{activeGapCount}</strong><span>Khoảng trống đang cần ôn</span></div>
            </div>
            <p className="mastery-level-legend">Mức thành thạo theo dữ liệu backend: Bắt đầu dưới 50 · Đang phát triển 50–74 · Thành thạo 75–89 · Nắm vững từ 90. Điểm dưới 70 được tính là cần xem lại.</p>
            <div className="mastery-content-grid">
              <section><h4>Mức độ thành thạo và lịch sử</h4>{profile.length ? <ul className="mastery-list">{profile.map(item => <li key={item.id} className="mastery-profile-row">
                <div><strong>{item.lessonName || `Bài học ${item.lessonId}`}</strong><span>{masteryLevelLabel(item.level)} · Tin cậy {Math.round((Number(item.confidence) || 0) * 100)}%</span><MasteryHistory mastery={item} /></div>
                <b>{Math.round(Number(item.score) || 0)}%</b>
              </li>)}</ul> : <p className="mastery-empty">Chưa có kết quả bài làm được phân tích.</p>}</section>
              <section><h4>Khoảng trống kiến thức</h4>{gaps.length ? <ul className="mastery-list">{gaps.map(gap => <li key={gap.id}>
                <div><strong>{gap.lessonName || `Bài học ${gap.lessonId}`}</strong><span>{gap.rootCause || 'Chưa có ghi chú nguyên nhân.'}</span><small className={`mastery-gap-status gap-${String(gap.status).toLowerCase()}`}>{gapStatusLabel(gap.status)}</small></div>
                <b className={`mastery-priority priority-${String(gap.priority).toLowerCase()}`}>{gap.priority}</b>
              </li>)}</ul> : <p className="mastery-empty">Chưa phát hiện kiến thức cần củng cố.</p>}</section>
            </div>
            <section className="mastery-errors"><h4>Dạng lỗi đã ghi nhận</h4>{errors.length ? <ul className="mastery-list">{errors.slice(0, 5).map(error => <li key={error.id}><div><strong>{error.errorType}</strong><span>{error.description || `Bài học ${error.lessonId}`}</span></div><time dateTime={error.createdAt}>{new Date(error.createdAt).toLocaleDateString('vi-VN')}</time></li>)}</ul> : <p className="mastery-empty">Chưa ghi nhận lỗi sai.</p>}</section>
          </>}
  </section>
}
