import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getAssessmentSession } from '../../assessments/services/assessmentApi'
import {
  getExerciseResult, getPendingExercises, getStepExercises, reviewExercise, saveExerciseAnswer,
  startExerciseAttempt, submitExerciseAttempt, updateExercise,
} from '../services/exerciseApi'
import { createExerciseDraft, toExerciseUpdate, validateExerciseUpdate } from '../services/exerciseDraft'
import '../styles/exercises.css'

const kindName = { MULTIPLE_CHOICE: 'Trắc nghiệm', FILL_BLANK: 'Điền khuyết', SHORT_ANSWER: 'Trả lời ngắn' }
const blankAnswers = question => question.blankCount ? Array(question.blankCount).fill('') : ['']

export default function ExercisesPage() {
  const { stepId } = useParams()
  const [user, setUser] = useState(null)
  const [access, setAccess] = useState('loading')
  const [questions, setQuestions] = useState([])
  const [pending, setPending] = useState([])
  const [answers, setAnswers] = useState({})
  const [attempt, setAttempt] = useState(null)
  const [result, setResult] = useState(null)
  const [notice, setNotice] = useState('')
  const [busy, setBusy] = useState(false)
  const [editingId, setEditingId] = useState(null)
  const [draft, setDraft] = useState(null)
  const isTeacher = user?.role === 'TEACHER' || user?.role === 'ADMIN'

  useEffect(() => {
    document.title = 'Bài tập cá nhân hóa · TLCN'
    let active = true
    getAssessmentSession().then(async session => {
      if (!active) return
      setUser(session)
      if (!['ADMIN', 'TEACHER', 'STUDENT'].includes(session?.role)) { setAccess('forbidden'); return }
      if (stepId && session.role !== 'STUDENT') { setAccess('forbidden'); return }
      if (!stepId && session.role === 'STUDENT') { setAccess('missing-step'); return }
      setAccess('allowed')
      try {
        if (stepId) setQuestions(await getStepExercises(stepId))
        else setPending(await getPendingExercises())
      } catch (error) { if (active) setNotice(error.message) }
    }).catch(error => { if (active) setAccess(error.status === 403 ? 'forbidden' : 'signed-out') })
    return () => { active = false }
  }, [stepId])

  const run = async action => {
    setBusy(true); setNotice('')
    try { await action() } catch (error) { setNotice(error.message) } finally { setBusy(false) }
  }

  const change = (id, update) => setAnswers(current => ({ ...current, [id]: { ...current[id], ...update } }))
  const start = () => run(async () => {
    const active = await startExerciseAttempt(stepId)
    setAttempt(active); setResult(null); setAnswers({})
  })
  const submit = () => run(async () => {
    for (const question of questions) {
      const answer = answers[question.id] || {}
      const payload = question.kind === 'MULTIPLE_CHOICE'
        ? { questionId: question.id, selectedOptionIds: answer.selectedOptionIds || [] }
        : question.kind === 'FILL_BLANK'
          ? { questionId: question.id, blankAnswers: answer.blankAnswers || blankAnswers(question) }
          : { questionId: question.id, content: answer.content || '' }
      await saveExerciseAnswer(attempt.id, payload)
    }
    await submitExerciseAttempt(attempt.id)
    setResult(await getExerciseResult(attempt.id))
    setAttempt(null)
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
    setPending(items => items.map(item => item.questionId === question.questionId ? updated : item))
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

  if (access === 'loading') return <main className="exercise-page"><p>Đang tải…</p></main>
  if (access !== 'allowed') return <main className="exercise-page"><section className="exercise-gate"><h1>{access === 'signed-out' ? 'Hãy đăng nhập' : access === 'missing-step' ? 'Chưa có bước học' : 'Không có quyền truy cập'}</h1><p>{access === 'missing-step' ? 'Mở bài tập từ một bước trong lộ trình học tập của em.' : 'Đăng nhập đúng vai trò để xem nội dung này.'}</p><Link to="/login">Đến trang đăng nhập</Link></section></main>

  return <main className="exercise-page">
    <header className="exercise-header"><Link to="/" className="exercise-brand">✿ TLCN</Link><div>{user?.fullName}<span>{isTeacher ? 'Giáo viên' : 'Học sinh'}</span></div></header>
    <section className="exercise-heading"><p className="exercise-kicker">LUYỆN TẬP CÁ NHÂN HÓA</p><h1>{stepId ? 'Bài tập của em' : 'Duyệt bài tập AI'}</h1><p>{stepId ? 'Các câu hỏi đã được giáo viên kiểm tra và duyệt.' : 'Xem đáp án, chỉnh sửa nội dung rồi duyệt trước khi giao cho học sinh.'}</p></section>
    {notice && <p className="exercise-notice" role="status">{notice}</p>}

    {isTeacher && <section className="exercise-list" aria-label="Bài tập chờ duyệt">
      {pending.length === 0 ? <p className="exercise-empty">Không có bài tập nào đang chờ duyệt.</p> : pending.map(question => <article className="exercise-card" key={question.questionId}>
        <div className="exercise-card-meta"><span>{kindName[question.kind]}</span><span>{question.difficulty} · {question.score} điểm</span></div>
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

    {stepId && !attempt && !result && <section className="exercise-list">
      {questions.length === 0 ? <p className="exercise-empty">Bước học này chưa có bài tập được duyệt.</p> : <>
        <p className="exercise-count">Có {questions.length} câu hỏi trong bước học này.</p><button className="exercise-primary" onClick={start} disabled={busy}>Bắt đầu làm bài</button>
      </>}
    </section>}

    {stepId && attempt && <section className="exercise-list">
      {questions.map((question, index) => <article className="exercise-card" key={question.id}>
        <div className="exercise-card-meta"><span>Câu {index + 1} · {kindName[question.kind]}</span><span>{question.score} điểm</span></div><h2>{question.content}</h2>
        {question.kind === 'MULTIPLE_CHOICE' && <fieldset className="exercise-options"><legend>Chọn câu trả lời</legend>{question.options.map(option => <label key={option.id}><input type={question.allowMultiple ? 'checkbox' : 'radio'} name={`question-${question.id}`} checked={(answers[question.id]?.selectedOptionIds || []).includes(option.id)} onChange={event => {
          const existing = answers[question.id]?.selectedOptionIds || []
          change(question.id, { selectedOptionIds: question.allowMultiple ? (event.target.checked ? [...existing, option.id] : existing.filter(id => id !== option.id)) : [option.id] })
        }} />{option.name}</label>)}</fieldset>}
        {question.kind === 'FILL_BLANK' && <div className="exercise-answer-fields">{blankAnswers(question).map((_, position) => <label key={position}>Ô {position + 1}<input value={(answers[question.id]?.blankAnswers || blankAnswers(question))[position]} onChange={event => {
          const values = [...(answers[question.id]?.blankAnswers || blankAnswers(question))]; values[position] = event.target.value; change(question.id, { blankAnswers: values })
        }} /></label>)}</div>}
        {question.kind === 'SHORT_ANSWER' && <label className="exercise-answer-fields">Câu trả lời<input value={answers[question.id]?.content || ''} onChange={event => change(question.id, { content: event.target.value })} /></label>}
      </article>)}
      <button className="exercise-primary" disabled={busy} onClick={submit}>Nộp bài</button>
    </section>}

    {result && <section className="exercise-list"><article className="exercise-card exercise-result"><p className="exercise-kicker">KẾT QUẢ CỦA EM</p><h2>{result.attempt.totalScore} / {result.attempt.maxScore} điểm</h2>{result.answers.map((answer, index) => <div key={answer.questionId} className="exercise-result-item"><strong>{index + 1}. {answer.correct ? 'Chính xác!' : 'Mình cùng ôn lại nhé.'}</strong><p>{answer.questionContent}</p>{!answer.correct && <p>Đáp án: {answer.expected.join(', ')}</p>}</div>)}</article></section>}
  </main>
}
