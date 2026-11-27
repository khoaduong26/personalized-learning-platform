import { useEffect, useRef, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import HomeHeader from '../../home/components/HomeHeader'
import { getAssessmentSession } from '../../assessments/services/assessmentApi'
import { getClassrooms } from '../../classrooms/services/classroomApi'
import {
  getExerciseResult, getPendingExercises, getStepExercises, reviewExercise, saveExerciseAnswer,
  startExerciseAttempt, submitExerciseAttempt, updateExercise,
} from '../services/exerciseApi'
import { createExerciseDraft, toExerciseUpdate, validateExerciseUpdate } from '../services/exerciseDraft'
import { toExerciseAnswerPayload } from '../services/exerciseAttempt'
import '../styles/exercises.css'

const kindName = { MULTIPLE_CHOICE: 'Trắc nghiệm', FILL_BLANK: 'Điền khuyết', SHORT_ANSWER: 'Trả lời ngắn' }
const blankAnswers = question => question.blankCount ? Array(question.blankCount).fill('') : ['']

export default function ExercisesPage() {
  const { stepId } = useParams()
  return <ExerciseWorkspace key={stepId ?? 'review'} stepId={stepId} />
}

function ExerciseWorkspace({ stepId }) {
  const [user, setUser] = useState(null)
  const [access, setAccess] = useState('loading')
  const [questions, setQuestions] = useState([])
  const [pending, setPending] = useState([])
  const [reviewClassrooms, setReviewClassrooms] = useState([])
  const [pendingStudentId, setPendingStudentId] = useState(null)
  const [pendingClassroomId, setPendingClassroomId] = useState(null)
  const [answers, setAnswers] = useState({})
  const [attempt, setAttempt] = useState(null)
  const [submittedId, setSubmittedId] = useState(null)
  const [result, setResult] = useState(null)
  const [notice, setNotice] = useState('')
  const [actionError, setActionError] = useState('')
  const [busy, setBusy] = useState(false)
  const [questionIndex, setQuestionIndex] = useState(0)
  const [loadError, setLoadError] = useState('')
  const [editingId, setEditingId] = useState(null)
  const [draft, setDraft] = useState(null)
  const questionHeading = useRef(null)
  const resultHeading = useRef(null)
  const question = questions[questionIndex]
  const isTeacher = user?.role === 'TEACHER' || user?.role === 'ADMIN'
  const reviewClassroomGroups = reviewClassrooms.reduce((groups, classroom) => {
    const id = String(classroom.id)
    groups[id] = { id, name: classroom.name, items: [] }
    return groups
  }, {})
  const pendingByClassroom = Object.values(pending.reduce((groups, item) => {
    const id = item.classroomId == null ? 'unassigned' : String(item.classroomId)
    if (!groups[id]) groups[id] = { id, name: item.classroomName || 'Chưa gắn lớp', items: [] }
    groups[id].items.push(item)
    return groups
  }, reviewClassroomGroups)).sort((a, b) => a.name.localeCompare(b.name, 'vi'))
  const activePendingClassroom = pendingByClassroom.find(group => group.id === pendingClassroomId) || pendingByClassroom[0]
  const classroomPending = activePendingClassroom?.items || []
  const pendingByStudent = Object.values(classroomPending.reduce((groups, item) => {
    const id = item.studentId == null ? 'unknown' : String(item.studentId)
    if (!groups[id]) groups[id] = { id, name: item.studentName || 'Học sinh chưa xác định', items: [] }
    groups[id].items.push(item)
    return groups
  }, {})).sort((a, b) => a.name.localeCompare(b.name, 'vi'))
  const activePendingStudent = pendingByStudent.find(group => group.id === pendingStudentId) || pendingByStudent[0]

  useEffect(() => {
    document.title = stepId ? 'Bài luyện tập · EduLearn' : 'Duyệt bài tập · EduLearn'
    let active = true
    getAssessmentSession().then(async session => {
      if (!active) return
      setUser(session)
      if (!['ADMIN', 'TEACHER', 'STUDENT'].includes(session?.role)) { setAccess('forbidden'); return }
      if (stepId && session.role !== 'STUDENT') { setAccess('forbidden'); return }
      if (!stepId && session.role === 'STUDENT') { setAccess('missing-step'); return }
      try {
        const items = await (stepId ? getStepExercises(stepId) : getPendingExercises())
        if (!active) return
        if (stepId) setQuestions(items)
        else {
          const classes = await getClassrooms().catch(() => [])
          if (!active) return
          setReviewClassrooms(classes)
          setPending(items)
          setPendingClassroomId(items[0]?.classroomId == null
            ? String(classes[0]?.id ?? 'unassigned')
            : String(items[0].classroomId))
          setPendingStudentId(items[0]?.studentId == null ? null : String(items[0].studentId))
        }
      } catch {
        if (active) setLoadError('Chưa tải được bài tập. Hãy tải lại trang để thử lại.')
      }
      if (active) setAccess('allowed')
    }).catch(error => { if (active) setAccess(error.status === 403 ? 'forbidden' : 'signed-out') })
    return () => { active = false }
  }, [stepId])

  useEffect(() => {
    if (attempt) questionHeading.current?.focus()
    if (result) resultHeading.current?.focus()
  }, [attempt, questionIndex, result])

  const run = async action => {
    setBusy(true); setNotice(''); setActionError('')
    try { await action() } catch (error) { setActionError(error.message) } finally { setBusy(false) }
  }

  const change = (id, update) => setAnswers(current => ({ ...current, [id]: { ...current[id], ...update } }))
  const start = () => run(async () => {
    const active = await startExerciseAttempt(stepId)
    setAttempt(active); setResult(null); setAnswers({}); setQuestionIndex(0)
  })
  const submit = () => run(async () => {
    for (const question of questions) {
      const answer = answers[question.id] || {}
      await saveExerciseAnswer(attempt.id, toExerciseAnswerPayload(question, answer))
    }
    const submitted = await submitExerciseAttempt(attempt.id)
    setSubmittedId(submitted.id)
    setAttempt(null)
    setResult(await getExerciseResult(submitted.id))
  })

  const beginEdit = question => {
    setEditingId(question.questionId)
    setDraft(createExerciseDraft(question))
    setNotice('')
  }

  const saveEdit = question => run(async () => {
    const payload = toExerciseUpdate(question, draft)
    const validationError = validateExerciseUpdate(question, payload)
    if (validationError) throw new Error(validationError)
    const updated = await updateExercise(question.questionId, payload)
    setPending(items => items.map(item => item.questionId === question.questionId
      ? { ...updated, studentId: item.studentId, studentName: item.studentName }
      : item))
    setEditingId(null)
    setDraft(null)
    setNotice('Đã lưu chỉnh sửa. Bài tập vẫn đang chờ duyệt.')
  })

  const review = (questionId, status) => run(async () => {
    const updated = await reviewExercise(questionId, status)
    setPending(items => items.filter(item => item.questionId !== questionId))
    if (editingId === questionId) { setEditingId(null); setDraft(null) }
    setNotice(updated.status === 'APPROVED' ? 'Đã duyệt bài tập.' : 'Đã từ chối bài tập.')
  })

  if (access === 'loading') return <main className="exercise-page"><p role="status">Đang mở bài tập…</p></main>
  if (access !== 'allowed') return <main className="exercise-page"><section className="exercise-gate">
    <h1>{access === 'signed-out' ? 'Đăng nhập để mở bài tập' : access === 'missing-step' ? 'Mở bài từ lộ trình của em' : 'Trang dành cho vai trò khác'}</h1>
    {access === 'signed-out' && <Link to={stepId ? '/login?returnTo=/my-learning-path' : '/login?returnTo=/exercises/review'}>Đăng nhập</Link>}
    {access === 'missing-step' ? <Link to="/my-learning-path">Về lộ trình của em</Link> : <Link to="/">Về trang chủ</Link>}
  </section></main>

  return <main id="noi-dung" tabIndex={-1} className={stepId ? 'exercise-page exercise-student' : 'exercise-page exercise-review-page'}>
    <HomeHeader user={user} onLogout={() => setUser(null)} />
    <section className="exercise-heading">
      {!stepId && <p className="exercise-kicker">KHO BÀI TẬP · DUYỆT NỘI DUNG</p>}
      <h1>{stepId ? 'Bài luyện tập của em' : 'Duyệt bài tập'}</h1>
      <p>{stepId ? 'Em làm từng câu rồi nộp bài để xem kết quả nhé.' : 'Xem bài tập AI tạo hoặc giáo viên tự soạn, chỉnh sửa rồi duyệt trước khi giao cho học sinh.'}</p>
    </section>
    {notice && <p className="exercise-notice" role="status">{notice}</p>}
    {actionError && <p className="exercise-error" role="alert">{actionError}</p>}
    {loadError && <p className="exercise-error" role="alert">{loadError}</p>}

    {isTeacher && !loadError && <section className="exercise-review-workspace" aria-label="Duyệt bài tập theo lớp và học sinh">
      {pending.length === 0 && reviewClassrooms.length === 0 ? <p className="exercise-empty">Không có bài tập nào đang chờ duyệt.</p> : <>
        {pendingByClassroom.length > 0 && <div className="exercise-student-tabs" role="tablist" aria-label="Lớp có bài tập chờ duyệt">
          {pendingByClassroom.map(group => <button key={group.id} type="button" role="tab"
            id={`exercise-class-tab-${group.id}`} aria-controls={`exercise-class-panel-${group.id}`}
            aria-selected={activePendingClassroom?.id === group.id}
            className={activePendingClassroom?.id === group.id ? 'is-active' : ''}
            onClick={() => { setPendingClassroomId(group.id); setPendingStudentId(group.items[0]?.studentId == null ? null : String(group.items[0].studentId)); setEditingId(null); setDraft(null) }}>
            <span>{group.name}</span><small>{group.items.length} bài tập</small>
          </button>)}
        </div>}
        {activePendingClassroom && <div id={`exercise-class-panel-${activePendingClassroom.id}`} role="tabpanel"
          aria-labelledby={`exercise-class-tab-${activePendingClassroom.id}`}>
        <div className="exercise-review-class-heading"><span>LỚP ĐANG DUYỆT</span><h2>{activePendingClassroom.name}</h2></div>
        {classroomPending.length === 0 ? <p className="exercise-empty">Lớp này hiện không có bài tập chờ duyệt.</p> : <>
        <div className="exercise-student-tabs" role="tablist" aria-label="Học sinh có bài tập chờ duyệt trong lớp">
          {pendingByStudent.map(group => <button key={group.id} type="button" role="tab"
            id={`exercise-student-tab-${group.id}`} aria-controls={`exercise-student-panel-${group.id}`}
            aria-selected={activePendingStudent?.id === group.id}
            className={activePendingStudent?.id === group.id ? 'is-active' : ''}
            onClick={() => { setPendingStudentId(group.id); setEditingId(null); setDraft(null) }}>
            <span>{group.name}</span><small>{group.items.length} chờ duyệt</small>
          </button>)}
        </div>
        {activePendingStudent && <section className="exercise-list" role="tabpanel"
          id={`exercise-student-panel-${activePendingStudent.id}`}
          aria-labelledby={`exercise-student-tab-${activePendingStudent.id}`}>
          <div className="exercise-student-queue-heading"><div><span>HỒ SƠ ĐANG DUYỆT</span><h2>{activePendingStudent.name}</h2></div><small>{activePendingStudent.items.length} bài tập đang chờ</small></div>
          {activePendingStudent.items.map(question => <article className="exercise-card" key={question.questionId}>
            <div className="exercise-card-meta"><span>{question.aiGenerated ? 'AI tạo' : 'Giáo viên tự soạn'} · {kindName[question.kind]}</span><span>{question.difficulty} · {question.score} điểm</span></div>
            {editingId === question.questionId && draft ? <div className="exercise-edit-form">
              <label>Nội dung câu hỏi<textarea rows="3" value={draft.content} onChange={event => setDraft(current => ({ ...current, content: event.target.value }))} /></label>
              <div className="exercise-edit-row"><label>Mức độ<select value={draft.difficulty} onChange={event => setDraft(current => ({ ...current, difficulty: event.target.value }))}><option value="EASY">Dễ</option><option value="MEDIUM">Vừa</option><option value="HARD">Khó</option></select></label><label>Điểm<input type="number" min="0.1" step="0.1" value={draft.score} onChange={event => setDraft(current => ({ ...current, score: event.target.value }))} /></label></div>
              {question.kind === 'MULTIPLE_CHOICE' ? <label>Các lựa chọn (mỗi dòng một lựa chọn, thêm * trước đáp án đúng)<textarea rows="4" value={draft.choices} onChange={event => setDraft(current => ({ ...current, choices: event.target.value }))} /></label> : <label>{question.kind === 'FILL_BLANK' ? 'Đáp án cho từng ô (mỗi dòng một đáp án)' : 'Đáp án chuẩn'}<textarea rows={question.kind === 'FILL_BLANK' ? 3 : 2} value={draft.expectedAnswers} onChange={event => setDraft(current => ({ ...current, expectedAnswers: event.target.value }))} /></label>}
              <label>Lời giải<textarea rows="2" value={draft.explanation} onChange={event => setDraft(current => ({ ...current, explanation: event.target.value }))} /></label>
              <div className="exercise-actions"><button disabled={busy} onClick={() => { setEditingId(null); setDraft(null) }} className="exercise-secondary">Hủy sửa</button><button disabled={busy || !draft.content.trim() || !Number(draft.score)} onClick={() => saveEdit(question)} className="exercise-primary">Lưu chỉnh sửa</button></div>
            </div> : <>
              <h2>{question.content}</h2><p className="exercise-answer">Đáp án: {question.kind === 'MULTIPLE_CHOICE' ? question.options.filter(option => option.correct).map(option => option.name).join(', ') : question.expectedAnswers.join(', ')}</p>
              {question.explanation && <p className="exercise-explanation">{question.explanation}</p>}
              <div className="exercise-actions"><button disabled={busy} onClick={() => beginEdit(question)} className="exercise-secondary">Sửa câu hỏi</button><button disabled={busy} onClick={() => review(question.questionId, 'REJECTED')} className="exercise-secondary">Từ chối</button><button disabled={busy} onClick={() => review(question.questionId, 'APPROVED')} className="exercise-primary">Duyệt và giao</button></div>
            </>}
          </article>)}
        </section>}
        </>}
        </div>}
      </>}
    </section>}

    {stepId && !loadError && !attempt && !submittedId && <section className="exercise-list">
      {questions.length === 0 ? <>
        <p className="exercise-empty">Bước học này chưa có bài tập. Em hãy quay lại sau khi thầy cô giao bài nhé.</p>
        <Link className="exercise-secondary" to="/my-learning-path">Về lộ trình của em</Link>
      </> : <>
        <p className="exercise-count">Bài này có {questions.length} câu hỏi.</p>
        <button type="button" className="exercise-primary" onClick={start} disabled={busy}>{busy ? 'Đang mở bài…' : 'Bắt đầu làm bài'}</button>
      </>}
    </section>}

    {submittedId && !result && <section className="exercise-list">
      {busy ? <p className="exercise-count" role="status">Đã nộp bài. Đang tải kết quả…</p> : <>
        <p className="exercise-count">Em đã nộp bài. Hãy mở lại kết quả để xem điểm nhé.</p>
        <button type="button" className="exercise-primary" onClick={() => run(async () => setResult(await getExerciseResult(submittedId)))}>Mở lại kết quả</button>
      </>}
    </section>}

    {stepId && attempt && <section className="exercise-list">
      <article className="exercise-card" key={question.id}>
        <h2 ref={questionHeading} tabIndex={-1}><span className="exercise-question-number">Câu {questionIndex + 1} / {questions.length}</span>{question.content}</h2>
        {question.kind === 'MULTIPLE_CHOICE' && <fieldset className="exercise-options" disabled={busy}><legend>{question.allowMultiple ? 'Chọn tất cả đáp án đúng' : 'Chọn một đáp án'}</legend>{question.options.map(option => <label key={option.id}><input type={question.allowMultiple ? 'checkbox' : 'radio'} name={`question-${question.id}`} checked={(answers[question.id]?.selectedOptionIds || []).includes(option.id)} onChange={event => {
          const existing = answers[question.id]?.selectedOptionIds || []
          change(question.id, { selectedOptionIds: question.allowMultiple ? (event.target.checked ? [...existing, option.id] : existing.filter(id => id !== option.id)) : [option.id] })
        }} />{option.name}</label>)}</fieldset>}
        {question.kind === 'FILL_BLANK' && <div className="exercise-answer-fields">{blankAnswers(question).map((_, position) => <label key={position}>Ô {position + 1}<input disabled={busy} value={(answers[question.id]?.blankAnswers || blankAnswers(question))[position]} onChange={event => {
          const values = [...(answers[question.id]?.blankAnswers || blankAnswers(question))]; values[position] = event.target.value; change(question.id, { blankAnswers: values })
        }} /></label>)}</div>}
        {question.kind === 'SHORT_ANSWER' && <label className="exercise-answer-fields">Câu trả lời<input disabled={busy} value={answers[question.id]?.content || ''} onChange={event => change(question.id, { content: event.target.value })} /></label>}
      </article>
      <div className="exercise-actions">
        {questionIndex > 0 && <button type="button" className="exercise-secondary" disabled={busy} onClick={() => setQuestionIndex(index => index - 1)}>Câu trước</button>}
        {questionIndex < questions.length - 1
          ? <button type="button" className="exercise-primary" onClick={() => setQuestionIndex(index => index + 1)}>Câu tiếp theo</button>
          : <button type="button" className="exercise-primary" disabled={busy} onClick={submit}>{busy ? 'Đang nộp…' : 'Nộp bài'}</button>}
      </div>
    </section>}

    {result && <section className="exercise-list">
      <article className="exercise-card exercise-result">
        <h2 ref={resultHeading} tabIndex={-1}>Kết quả của em · {result.attempt.totalScore} / {result.attempt.maxScore} điểm</h2>
        {result.answers.map((answer, index) => <div key={answer.questionId} className="exercise-result-item">
          <strong>Câu {index + 1} · {answer.correct ? 'Chính xác!' : 'Mình cùng ôn lại nhé.'}</strong>
          <p>{answer.questionContent}</p>
          {!answer.correct && <p>Đáp án đúng là {answer.expected.join(', ')}.</p>}
          {answer.explanation && <p>{answer.explanation}</p>}
        </div>)}
      </article>
      <Link className="exercise-primary" to="/my-learning-path">Về lộ trình của em</Link>
    </section>}
  </main>
}
