import { useEffect, useState } from 'react'
import { getKnowledgeGaps, getMasteryProfile, getStudentErrors } from '../services/masteryApi'
import '../styles/mastery.css'

export default function MasteryPanel({ students }) {
  const [studentId, setStudentId] = useState('')
  const [profile, setProfile] = useState([])
  const [gaps, setGaps] = useState([])
  const [errors, setErrors] = useState([])
  const [state, setState] = useState('idle')
  const [message, setMessage] = useState('')

  useEffect(() => {
    if (!students.some(student => String(student.id) === studentId)) {
      setStudentId(students[0] ? String(students[0].id) : '')
    }
  }, [students, studentId])

  useEffect(() => {
    if (!studentId) { setProfile([]); setGaps([]); setErrors([]); return }
    const controller = new AbortController()
    setState('loading'); setMessage('')
    Promise.all([getMasteryProfile(studentId), getKnowledgeGaps(studentId), getStudentErrors(studentId)])
      .then(([masteries, knowledgeGaps, studentErrors]) => {
        if (controller.signal.aborted) return
        setProfile(masteries); setGaps(knowledgeGaps); setErrors(studentErrors); setState('ready')
      })
      .catch(error => {
        if (controller.signal.aborted) return
        setMessage(error.message); setState('error')
      })
    return () => controller.abort()
  }, [studentId])

  const selected = students.find(student => String(student.id) === studentId)
  const average = profile.length
    ? Math.round(profile.reduce((sum, item) => sum + item.score, 0) / profile.length)
    : 0

  return <section className="mastery-panel" aria-labelledby="mastery-title">
    <div className="mastery-panel-heading">
      <div><span className="classrooms-kicker">THEO DÕI TIẾN BỘ</span><h3 id="mastery-title">Hồ sơ năng lực</h3><p>Mức độ thành thạo, kiến thức cần ôn và dạng lỗi đã ghi nhận.</p></div>
      <label>Chọn học sinh<select aria-label="Chọn học sinh xem hồ sơ năng lực" value={studentId} onChange={event => setStudentId(event.target.value)}>
        {students.map(student => <option key={student.id} value={student.id}>{student.fullName}</option>)}
      </select></label>
    </div>
    {!students.length ? <p className="mastery-empty">Thêm học sinh vào lớp để bắt đầu theo dõi năng lực.</p>
      : state === 'loading' ? <p className="mastery-empty" role="status">Đang tải hồ sơ của {selected?.fullName}…</p>
        : state === 'error' ? <p className="mastery-error" role="alert">{message}</p>
          : <>
            <div className="mastery-summary"><div><strong>{average}<small>/100</small></strong><span>Điểm trung bình các kiến thức</span></div><div><strong>{profile.length}</strong><span>Kiến thức đã được đánh giá</span></div><div><strong>{gaps.length}</strong><span>Khoảng trống cần ôn</span></div></div>
            <div className="mastery-content-grid">
              <section><h4>Mức độ thành thạo</h4>{profile.length ? <ul className="mastery-list">{profile.map(item => <li key={item.id}><div><strong>{item.lessonName || `Bài học ${item.lessonId}`}</strong><span>{item.level} · Tin cậy {Math.round(item.confidence * 100)}%</span></div><b>{Math.round(item.score)}%</b></li>)}</ul> : <p className="mastery-empty">Chưa có kết quả bài làm được phân tích.</p>}</section>
              <section><h4>Khoảng trống kiến thức</h4>{gaps.length ? <ul className="mastery-list">{gaps.map(gap => <li key={gap.id}><div><strong>{gap.lessonName || `Bài học ${gap.lessonId}`}</strong><span>{gap.rootCause}</span></div><b className={`mastery-priority priority-${gap.priority.toLowerCase()}`}>{gap.priority}</b></li>)}</ul> : <p className="mastery-empty">Chưa phát hiện kiến thức cần củng cố.</p>}</section>
            </div>
            <section className="mastery-errors"><h4>Dạng lỗi đã ghi nhận</h4>{errors.length ? <ul className="mastery-list">{errors.slice(0, 5).map(error => <li key={error.id}><div><strong>{error.errorType}</strong><span>{error.description || `Bài học ${error.lessonId}`}</span></div><time>{new Date(error.createdAt).toLocaleDateString('vi-VN')}</time></li>)}</ul> : <p className="mastery-empty">Chưa ghi nhận lỗi sai.</p>}</section>
          </>}
  </section>
}
