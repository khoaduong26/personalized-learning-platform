import { useEffect, useRef, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import WorkspaceNavigation from '../../../shared/components/navigation/WorkspaceNavigation'
import { getAssessmentSession } from '../../assessments/services/assessmentApi'
import {
  getExerciseResult, getPendingExercises, getStepExercises, reviewExercise, saveExerciseAnswer,
  startExerciseAttempt, submitExerciseAttempt, updateExercise,
} from '../services/exerciseApi'
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
  const [answers, setAnswers] = useState({})
  const [attempt, setAttempt] = useState(null)
  const [submittedId, setSubmittedId] = useState(null)
  const [result, setResult] = useState(null)
  const [notice, setNotice] = useState('')
  const [actionError, setActionError] = useState('')
  const [busy, setBusy] = useState(false)
  const [questionIndex, setQuestionIndex] = useState(0)
  const [loadError, setLoadError] = useState('')
  const questionHeading = useRef(null)
  const resultHeading = useRef(null)
  const question = questions[questionIndex]
  const isTeacher = user?.role === 'TEACHER' || user?.role === 'ADMIN'

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
        else setPending(items)
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
      const payload = question.kind === 'MULTIPLE_CHOICE'
        ? { questionId: question.id, selectedOptionIds: answer.selectedOptionIds || [] }
        : question.kind === 'FILL_BLANK'
          ? { questionId: question.id, blankAnswers: answer.blankAnswers || blankAnswers(question) }
          : { questionId: question.id, content: answer.content || '' }
      await saveExerciseAnswer(attempt.id, payload)
    }
    const submitted = await submitExerciseAttempt(attempt.id)
    setSubmittedId(submitted.id)
    setAttempt(null)
    setResult(await getExerciseResult(submitted.id))
  })

  const saveEdit = question => run(async () => {
    const content = window.prompt('Sửa nội dung câu hỏi', question.content)
    if (content === null) return
    let options = question.options.map(option => ({ name: option.name, correct: option.correct }))
    let expectedAnswers = question.expectedAnswers
    if (question.kind === 'MULTIPLE_CHOICE') {
      const choices = window.prompt('Mỗi lựa chọn một dòng; thêm * trước đáp án đúng.', options.map(option => `${option.correct ? '*' : ''}${option.name}`).join('\n'))
      if (choices === null) return
      options = choices.split('\n').map(line => line.trim()).filter(Boolean).map(line => ({
        name: line.replace(/^\*/, '').trim(), correct: line.startsWith('*'),
      }))
    } else {
      const expected = window.prompt('Đáp án chuẩn', expectedAnswers.join('\n'))
      if (expected === null) return
      expectedAnswers = question.kind === 'FILL_BLANK' ? expected.split('\n').map(value => value.trim()) : [expected.trim()]
    }
    const explanation = window.prompt('Lời giải (có thể để trống)', question.explanation || '')
    if (explanation === null) return
    const body = {
      content, difficulty: question.difficulty, score: question.score, lessonId: question.lessonId,
      kind: question.kind, explanation, allowMultiple: question.allowMultiple,
      options: options.map((option, displayOrder) => ({ ...option, displayOrder })),
      blanks: expectedAnswers.map((correctAnswer, position) => ({ correctAnswer, position })),
      correctAnswer: expectedAnswers[0] || '',
    }
    const updated = await updateExercise(question.questionId, body)
    setPending(items => items.map(item => item.questionId === question.questionId ? updated : item))
  })

  const review = (questionId, status) => run(async () => {
    const updated = await reviewExercise(questionId, status)
    setPending(items => items.filter(item => item.questionId !== questionId))
    setNotice(updated.status === 'APPROVED' ? 'Đã duyệt bài tập.' : 'Đã từ chối bài tập.')
  })

  if (access === 'loading') return <main className="exercise-page"><p role="status">Đang mở bài tập…</p></main>
  if (access !== 'allowed') return <main className="exercise-page"><section className="exercise-gate">
    <h1>{access === 'signed-out' ? 'Đăng nhập để mở bài tập' : access === 'missing-step' ? 'Mở bài từ lộ trình của em' : 'Trang dành cho vai trò khác'}</h1>
    {access === 'signed-out' && <Link to={stepId ? '/login?returnTo=/my-learning-path' : '/login?returnTo=/exercises/review'}>Đăng nhập</Link>}
    {access === 'missing-step' ? <Link to="/my-learning-path">Về lộ trình của em</Link> : <Link to="/">Về trang chủ</Link>}
  </section></main>

  return <main className={stepId ? 'exercise-page exercise-student' : 'exercise-page'}>
    <header className="exercise-header"><Link to="/" className="exercise-brand">EduLearn</Link><WorkspaceNavigation role={user.role} /><div>{user.fullName}</div></header>
    <section className="exercise-heading">
      <h1>{stepId ? 'Bài luyện tập của em' : 'Duyệt bài tập AI'}</h1>
      <p>{stepId ? 'Em làm từng câu rồi nộp bài để xem kết quả nhé.' : 'Xem đáp án, chỉnh sửa nội dung rồi duyệt trước khi giao cho học sinh.'}</p>
    </section>
    {notice && <p className="exercise-notice" role="status">{notice}</p>}
    {actionError && <p className="exercise-error" role="alert">{actionError}</p>}
    {loadError && <p className="exercise-error" role="alert">{loadError}</p>}

    {isTeacher && !loadError && <section className="exercise-list" aria-label="Bài tập chờ duyệt">
      {pending.length === 0 ? <p className="exercise-empty">Không có bài tập nào đang chờ duyệt.</p> : pending.map(question => <article className="exercise-card" key={question.questionId}>
        <div className="exercise-card-meta"><span>{kindName[question.kind]}</span><span>{question.difficulty} · {question.score} điểm</span></div>
        <h2>{question.content}</h2><p className="exercise-answer">Đáp án: {question.kind === 'MULTIPLE_CHOICE' ? question.options.filter(option => option.correct).map(option => option.name).join(', ') : question.expectedAnswers.join(', ')}</p>
        {question.explanation && <p className="exercise-explanation">{question.explanation}</p>}
        <div className="exercise-actions"><button disabled={busy} onClick={() => saveEdit(question)} className="exercise-secondary">Sửa câu hỏi</button><button disabled={busy} onClick={() => review(question.questionId, 'REJECTED')} className="exercise-secondary">Từ chối</button><button disabled={busy} onClick={() => review(question.questionId, 'APPROVED')} className="exercise-primary">Duyệt và giao</button></div>
      </article>)}
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
