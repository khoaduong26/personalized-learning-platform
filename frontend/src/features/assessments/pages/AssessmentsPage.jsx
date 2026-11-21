import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { getLessons, getSubjects, getTopics } from '../../academic/services/academicApi'
import { getClassroomStudents } from '../../classrooms/services/classroomApi'
import {
  createAssessment, createQuestion, getAssessment, getAssessmentClassrooms, getAssessmentSession, getAssessments,
  getAttemptDraft, getAttemptHistory, getAttemptResult, getQuestionBank, publishAssessment, saveAnswer, startAttempt,
  submitAttempt,
} from '../services/assessmentApi'
import '../styles/assessments.css'
import WorkspaceNavigation from '../../../shared/components/navigation/WorkspaceNavigation'

const types = { DIAGNOSTIC: 'Khảo sát đầu vào', PERIODIC: 'Định kỳ', PRACTICE: 'Luyện tập' }
const roles = { ADMIN: 'Quản trị viên', TEACHER: 'Giáo viên', STUDENT: 'Học sinh' }
const emptyOption = () => ({ name: '', correct: false })

export default function AssessmentsPage() {
  const [user, setUser] = useState(null)
  const [access, setAccess] = useState('loading')
  const [assessments, setAssessments] = useState([])
  const [classrooms, setClassrooms] = useState([])
  const [subjects, setSubjects] = useState([])
  const [topics, setTopics] = useState([])
  const [lessons, setLessons] = useState([])
  const [subjectId, setSubjectId] = useState('')
  const [topicId, setTopicId] = useState('')
  const [lessonId, setLessonId] = useState('')
  const [bank, setBank] = useState([])
  const [selectedQuestions, setSelectedQuestions] = useState([])
  const [selectedClasses, setSelectedClasses] = useState([])
  const [questionForm, setQuestionForm] = useState({ content: '', difficulty: 'EASY', score: '1', kind: 'MULTIPLE_CHOICE', allowMultiple: false, options: [emptyOption(), emptyOption()], blanks: [{ correctAnswer: '' }], correctAnswer: '', caseSensitive: false, explanation: '' })
  const [assessmentForm, setAssessmentForm] = useState({ title: '', type: 'DIAGNOSTIC', durationMinutes: 30 })
  const [activeAttempt, setActiveAttempt] = useState(null)
  const [answerDrafts, setAnswerDrafts] = useState({})
  const [result, setResult] = useState(null)
  const [history, setHistory] = useState([])
  const [resultClassId, setResultClassId] = useState('')
  const [resultStudents, setResultStudents] = useState([])
  const [resultStudentId, setResultStudentId] = useState('')
  const [teacherHistory, setTeacherHistory] = useState([])
  const [notice, setNotice] = useState('')
  const [busy, setBusy] = useState(false)
  const isTeacher = user?.role === 'TEACHER' || user?.role === 'ADMIN'

  useEffect(() => {
    document.title = 'Kiểm tra và đánh giá · TLCN'
    let active = true
    getAssessmentSession().then(async session => {
      if (!active) return
      setUser(session)
      if (!['ADMIN', 'TEACHER', 'STUDENT'].includes(session?.role)) { setAccess('forbidden'); return }
      setAccess('allowed')
      try {
        const [tests, subjectItems] = await Promise.all([getAssessments(), session.role === 'STUDENT' ? Promise.resolve([]) : getSubjects()])
        if (!active) return
        setAssessments(tests); setSubjects(subjectItems)
        if (subjectItems[0]) setSubjectId(String(subjectItems[0].id))
        if (session.role !== 'STUDENT') setClassrooms(await getAssessmentClassrooms())
        else setHistory(await getAttemptHistory(session.id))
      } catch (error) { if (active) setNotice(error.message) }
    }).catch(error => { if (active) setAccess(error.status === 403 ? 'forbidden' : 'signed-out') })
    return () => { active = false }
  }, [])

  useEffect(() => {
    if (!isTeacher || !resultClassId) { setResultStudents([]); setResultStudentId(''); return }
    let active = true
    getClassroomStudents(resultClassId).then(items => {
      if (!active) return
      const options = items.map(item => ({ ...item, name: item.fullName }))
      setResultStudents(options)
      setResultStudentId(current => options.some(item => String(item.id) === current) ? current : (options[0] ? String(options[0].id) : ''))
    }).catch(error => { if (active) setNotice(error.message) })
    return () => { active = false }
  }, [isTeacher, resultClassId])
  useEffect(() => {
    if (!isTeacher || !resultStudentId) { setTeacherHistory([]); return }
    let active = true
    getAttemptHistory(resultStudentId).then(items => { if (active) setTeacherHistory(items) }).catch(error => { if (active) setNotice(error.message) })
    return () => { active = false }
  }, [isTeacher, resultStudentId])

  useEffect(() => {
    if (!subjectId) { setTopics([]); setTopicId(''); return }
    let active = true
    getTopics(subjectId).then(items => { if (active) { setTopics(items); setTopicId(items[0] ? String(items[0].id) : '') } }).catch(error => { if (active) setNotice(error.message) })
    return () => { active = false }
  }, [subjectId])
  useEffect(() => {
    if (!topicId) { setLessons([]); setLessonId(''); return }
    let active = true
    getLessons(topicId).then(items => { if (active) { setLessons(items); setLessonId(items[0] ? String(items[0].id) : '') } }).catch(error => { if (active) setNotice(error.message) })
    return () => { active = false }
  }, [topicId])
  useEffect(() => {
    if (!lessonId || !isTeacher) { setBank([]); return }
    let active = true
    getQuestionBank(lessonId).then(items => { if (active) setBank(items) }).catch(error => { if (active) setNotice(error.message) })
    return () => { active = false }
  }, [lessonId, isTeacher])

  const safeAction = async action => {
    setBusy(true); setNotice('')
    try { await action() } catch (error) { setNotice(error.message) } finally { setBusy(false) }
  }
  const saveQuestion = event => {
    event.preventDefault()
    safeAction(async () => {
      await createQuestion({ ...questionForm, lessonId: Number(lessonId), score: Number(questionForm.score),
        options: questionForm.kind === 'MULTIPLE_CHOICE' ? questionForm.options.map((item, displayOrder) => ({ ...item, displayOrder })) : [],
        blanks: questionForm.kind === 'FILL_BLANK' ? questionForm.blanks.map((item, position) => ({ ...item, position })) : [] })
      setQuestionForm(current => ({ ...current, content: '', explanation: '' }))
      setNotice('Đã thêm câu hỏi vào ngân hàng.')
      setBank(await getQuestionBank(lessonId))
    })
  }
  const saveAssessment = event => {
    event.preventDefault()
    safeAction(async () => {
      const created = await createAssessment({ ...assessmentForm, durationMinutes: Number(assessmentForm.durationMinutes),
        questionIds: selectedQuestions, classroomIds: selectedClasses })
      setAssessments(current => [created, ...current]); setAssessmentForm(current => ({ ...current, title: '' }))
      setSelectedQuestions([]); setNotice('Đã lưu bài kiểm tra ở trạng thái nháp.')
    })
  }
  const toggleId = (setter, id) => setter(current => current.includes(id) ? current.filter(value => value !== id) : [...current, id])
  const openAttempt = test => safeAction(async () => {
    const attempt = test.status === 'IN_PROGRESS' ? test : await startAttempt(test.id)
    const [assessment, saved] = await Promise.all([
      test.status === 'IN_PROGRESS' ? getAssessment(test.assessmentId) : Promise.resolve(test),
      test.status === 'IN_PROGRESS' ? getAttemptDraft(test.id) : Promise.resolve([]),
    ])
    const drafts = Object.fromEntries(saved.map(item => [item.questionId, {
      selectedOptionIds: item.selectedOptionIds, blankAnswers: item.blankAnswers, content: item.content,
    }]))
    setActiveAttempt({ ...attempt, assessment }); setAnswerDrafts(drafts); setResult(null)
  })
  const currentQuestions = activeAttempt?.assessment?.questions || []
  const setDraft = (questionId, update) => setAnswerDrafts(current => ({ ...current, [questionId]: { ...current[questionId], ...update } }))
  const questionPayload = question => {
    const draft = answerDrafts[question.id] || {}
    if (question.kind === 'MULTIPLE_CHOICE') return { questionId: question.id, selectedOptionIds: draft.selectedOptionIds || [] }
    if (question.kind === 'FILL_BLANK') return { questionId: question.id, blankAnswers: draft.blankAnswers || Array(question.blankCount).fill('') }
    return { questionId: question.id, content: draft.content || '' }
  }
  const saveDraft = () => safeAction(async () => {
    for (const question of currentQuestions) await saveAnswer(activeAttempt.id, questionPayload(question))
    setNotice('Đã lưu tạm các câu trả lời.')
  })
  const finishAttempt = () => safeAction(async () => {
    for (const question of currentQuestions) await saveAnswer(activeAttempt.id, questionPayload(question))
    const submitted = await submitAttempt(activeAttempt.id)
    const details = await getAttemptResult(submitted.id)
    setResult(details); setActiveAttempt(null); setHistory(await getAttemptHistory(user.id))
  })
  const openResult = id => safeAction(async () => { setResult(await getAttemptResult(id)); setActiveAttempt(null) })
  const selectedCount = useMemo(() => selectedQuestions.length, [selectedQuestions])

  if (access === 'loading') return <main className="assessment-shell"><p className="assessment-state">Đang kiểm tra phiên đăng nhập…</p></main>
  if (access !== 'allowed') return <main className="assessment-shell"><div className="assessment-gate"><span className="assessment-kicker">KIỂM TRA VÀ ĐÁNH GIÁ</span><h1>{access === 'signed-out' ? 'Đăng nhập để tiếp tục' : 'Khu vực dành cho thành viên'}</h1><p>{access === 'signed-out' ? 'Đăng nhập để xem bài kiểm tra, lưu câu trả lời và theo dõi kết quả.' : 'Tài khoản này chưa có quyền truy cập nội dung đánh giá.'}</p>{access === 'signed-out' && <Link className="assessment-primary" to="/login?returnTo=/assessments">Đi đến đăng nhập</Link>}<Link className="assessment-back" to="/">Về trang chủ</Link></div></main>

  return <main className="assessment-shell">
    <header className="assessment-topbar"><Link to="/" className="assessment-brand"><span>✳</span> TLCN</Link><WorkspaceNavigation role={user?.role} /><div className="assessment-user"><span>{user?.fullName}</span><span className="assessment-role">{roles[user?.role]}</span>{isTeacher && <Link to="/exercises/review">Duyệt bài tập</Link>}</div></header>
    <section className="assessment-heading"><div><p className="assessment-kicker">NGÂN HÀNG CÂU HỎI · KIỂM TRA · KẾT QUẢ</p><h1>{activeAttempt ? 'Bài làm của em' : result ? 'Kết quả bài kiểm tra' : 'Kiểm tra và đánh giá'}</h1><p>{activeAttempt ? `Làm từng câu rồi lưu tạm hoặc nộp bài. Thời gian: ${activeAttempt.assessment.durationMinutes || 'không giới hạn'} phút.` : 'Tạo câu hỏi theo bài học, tổ chức bài kiểm tra và theo dõi tiến độ học tập.'}</p></div><span className="assessment-heading-mark" aria-hidden="true">✎</span></section>
    {notice && <div className="assessment-notice" role="status">{notice}<button type="button" onClick={() => setNotice('')} aria-label="Đóng">×</button></div>}

    {activeAttempt && <section className="assessment-panel assessment-take"><div className="assessment-panel-title"><div><p className="assessment-kicker">{types[activeAttempt.assessment.type]}</p><h2>{activeAttempt.assessment.title}</h2></div><span className="assessment-count">{currentQuestions.length} câu</span></div>
      {currentQuestions.map((question, index) => <QuestionCard key={question.id} question={question} index={index} draft={answerDrafts[question.id] || {}} onChange={update => setDraft(question.id, update)} />)}
      {!currentQuestions.length && <Empty text="Bài kiểm tra chưa có câu hỏi." />}
      <div className="assessment-actions"><button className="assessment-secondary" disabled={busy} onClick={saveDraft}>Lưu tạm</button><button className="assessment-primary" disabled={busy} onClick={finishAttempt}>Nộp bài</button></div>
    </section>}

    {result && <ResultCard result={result} onClose={() => setResult(null)} />}

    {!activeAttempt && !result && isTeacher && <div className="assessment-teacher-grid">
      <section className="assessment-panel"><PanelTitle eyebrow="BƯỚC 1 · NGÂN HÀNG" title="Tạo câu hỏi" detail="Mỗi câu hỏi gắn với một bài học để phân tích kết quả." />
        <div className="assessment-selects"><Select label="Môn học" value={subjectId} items={subjects} onChange={setSubjectId} /><Select label="Chủ đề" value={topicId} items={topics} onChange={setTopicId} /><Select label="Bài học / kiến thức" value={lessonId} items={lessons} onChange={setLessonId} /></div>
        {lessonId ? <form className="assessment-form" onSubmit={saveQuestion}>
          <label>Nội dung câu hỏi<textarea required maxLength="4000" value={questionForm.content} onChange={e => setQuestionForm({ ...questionForm, content: e.target.value })} placeholder="Ví dụ: 8 + 7 bằng bao nhiêu?" /></label>
          <div className="assessment-form-row"><Select label="Dạng câu hỏi" value={questionForm.kind} items={[{ id: 'MULTIPLE_CHOICE', name: 'Trắc nghiệm' }, { id: 'FILL_BLANK', name: 'Điền khuyết' }, { id: 'SHORT_ANSWER', name: 'Trả lời ngắn' }]} onChange={kind => setQuestionForm({ ...questionForm, kind })} /><Select label="Độ khó" value={questionForm.difficulty} items={[{ id: 'EASY', name: 'Dễ' }, { id: 'MEDIUM', name: 'Vừa' }, { id: 'HARD', name: 'Khó' }]} onChange={difficulty => setQuestionForm({ ...questionForm, difficulty })} /><label>Điểm<input type="number" min="0.1" max="100" step="0.1" required value={questionForm.score} onChange={e => setQuestionForm({ ...questionForm, score: e.target.value })} /></label></div>
          {questionForm.kind === 'MULTIPLE_CHOICE' && <div className="assessment-choice-builder"><label className="assessment-check"><input type="checkbox" checked={questionForm.allowMultiple} onChange={e => setQuestionForm({ ...questionForm, allowMultiple: e.target.checked })} /> Cho phép chọn nhiều đáp án</label>{questionForm.options.map((option, index) => <div className="assessment-option-edit" key={index}><input required value={option.name} placeholder={`Lựa chọn ${index + 1}`} onChange={e => setQuestionForm({ ...questionForm, options: questionForm.options.map((item, i) => i === index ? { ...item, name: e.target.value } : item) })} /><label className="assessment-check"><input type="checkbox" checked={option.correct} onChange={e => setQuestionForm({ ...questionForm, options: questionForm.options.map((item, i) => i === index ? { ...item, correct: e.target.checked } : item) })} /> Đúng</label>{questionForm.options.length > 2 && <button type="button" className="assessment-remove" onClick={() => setQuestionForm({ ...questionForm, options: questionForm.options.filter((_, i) => i !== index) })}>×</button>}</div>)}<button type="button" className="assessment-text-button" onClick={() => setQuestionForm({ ...questionForm, options: [...questionForm.options, emptyOption()] })}>+ Thêm lựa chọn</button></div>}
          {questionForm.kind === 'FILL_BLANK' && <div className="assessment-choice-builder"><p className="assessment-field-label">Đáp án theo thứ tự ô trống</p>{questionForm.blanks.map((blank, index) => <div className="assessment-option-edit" key={blank.position ?? index}><input required value={blank.correctAnswer} placeholder={`Đáp án ô ${index + 1}`} onChange={e => setQuestionForm({ ...questionForm, blanks: questionForm.blanks.map((item, i) => i === index ? { correctAnswer: e.target.value } : item) })} />{questionForm.blanks.length > 1 && <button type="button" className="assessment-remove" onClick={() => setQuestionForm({ ...questionForm, blanks: questionForm.blanks.filter((_, i) => i !== index) })}>×</button>}</div>)}<button type="button" className="assessment-text-button" onClick={() => setQuestionForm({ ...questionForm, blanks: [...questionForm.blanks, { correctAnswer: '' }] })}>+ Thêm ô trống</button></div>}
          {questionForm.kind === 'SHORT_ANSWER' && <label>Đáp án chuẩn<input required value={questionForm.correctAnswer} onChange={e => setQuestionForm({ ...questionForm, correctAnswer: e.target.value })} /></label>}
          {questionForm.kind !== 'MULTIPLE_CHOICE' && <label className="assessment-check"><input type="checkbox" checked={questionForm.caseSensitive} onChange={e => setQuestionForm({ ...questionForm, caseSensitive: e.target.checked })} /> Phân biệt chữ hoa và chữ thường</label>}
          <label>Lời giải / gợi ý<textarea value={questionForm.explanation} onChange={e => setQuestionForm({ ...questionForm, explanation: e.target.value })} placeholder="Hiển thị sau khi học sinh nộp bài" /></label>
          <button className="assessment-primary" disabled={busy}>Thêm vào ngân hàng</button>
        </form> : <Empty text="Tạo môn học, chủ đề và bài học trước khi thêm câu hỏi." />}
      </section>
      <section className="assessment-panel"><PanelTitle eyebrow="CÂU HỎI ĐÃ LƯU" title="Ngân hàng theo bài học" detail="Chọn câu hỏi để đưa vào bài kiểm tra mới." />
        {bank.length ? <div className="assessment-bank-list">{bank.map(question => <label className="assessment-bank-item" key={question.id}><input type="checkbox" checked={selectedQuestions.includes(question.id)} onChange={() => toggleId(setSelectedQuestions, question.id)} /><span><strong>{question.content}</strong><small>{question.kind === 'MULTIPLE_CHOICE' ? 'Trắc nghiệm' : question.kind === 'FILL_BLANK' ? 'Điền khuyết' : 'Trả lời ngắn'} · {question.difficulty === 'EASY' ? 'Dễ' : question.difficulty === 'MEDIUM' ? 'Vừa' : 'Khó'} · {question.score} điểm</small></span></label>)}</div> : <Empty text="Bài học này chưa có câu hỏi trong ngân hàng." />}
        <form className="assessment-form assessment-create-form" onSubmit={saveAssessment}><PanelTitle eyebrow="BƯỚC 2 · BÀI KIỂM TRA" title="Lưu bài kiểm tra nháp" detail="Chọn lớp được giao và câu hỏi cần sử dụng." />
          <label>Tên bài kiểm tra<input required maxLength="180" value={assessmentForm.title} onChange={e => setAssessmentForm({ ...assessmentForm, title: e.target.value })} placeholder="Ví dụ: Ôn tập phép cộng trong phạm vi 20" /></label>
          <div className="assessment-form-row"><Select label="Loại bài" value={assessmentForm.type} items={Object.entries(types).map(([id, name]) => ({ id, name }))} onChange={type => setAssessmentForm({ ...assessmentForm, type })} /><label>Thời gian (phút)<input type="number" min="1" max="240" value={assessmentForm.durationMinutes} onChange={e => setAssessmentForm({ ...assessmentForm, durationMinutes: e.target.value })} /></label></div>
          <div><p className="assessment-field-label">Giao cho lớp</p><div className="assessment-class-list">{classrooms.map(item => <label className="assessment-check" key={item.id}><input type="checkbox" checked={selectedClasses.includes(item.id)} onChange={() => toggleId(setSelectedClasses, item.id)} /> {item.name} · {item.schoolYear}</label>)}</div>{!classrooms.length && <small className="assessment-muted">Chưa có lớp học được phân công.</small>}</div>
          <div className="assessment-form-footer"><span>{selectedCount} câu · {selectedClasses.length} lớp</span><button className="assessment-primary" disabled={busy || !selectedCount || !selectedClasses.length}>Lưu bản nháp</button></div>
        </form>
      </section>
      <section className="assessment-panel assessment-tests"><PanelTitle eyebrow="ĐỀ CỦA BẠN" title="Bài kiểm tra" detail="Bài nháp chỉ hiển thị với giáo viên cho đến khi công bố." />
        {assessments.length ? <div className="assessment-test-list">{assessments.map(test => <article className="assessment-test-item" key={test.id}><div><strong>{test.title}</strong><small>{types[test.type]} · {test.questions.length} câu · {test.classroomNames?.join(', ')}</small></div><div className="assessment-test-actions"><span className={`assessment-status ${test.published ? 'is-published' : ''}`}>{test.published ? 'Đã giao' : 'Bản nháp'}</span>{!test.published && <button disabled={busy} className="assessment-secondary" onClick={() => safeAction(async () => { const updated = await publishAssessment(test.id); setAssessments(current => current.map(item => item.id === updated.id ? updated : item)); setNotice('Đã công bố bài kiểm tra cho các lớp đã chọn.') })}>Công bố</button>}</div></article>)}</div> : <Empty text="Chưa có bài kiểm tra. Hãy tạo bản nháp đầu tiên." />}
        <div className="assessment-results-filter"><PanelTitle eyebrow="UC11 · UC12" title="Kết quả học sinh" detail="Tra cứu điểm và câu trả lời trong các lớp của bạn." /><div className="assessment-form-row assessment-result-selects"><Select label="Lớp học" value={resultClassId} items={classrooms} onChange={setResultClassId} /><Select label="Học sinh" value={resultStudentId} items={resultStudents} onChange={setResultStudentId} /></div>{teacherHistory.length ? <div className="assessment-test-list">{teacherHistory.map(item => <article className="assessment-test-item" key={item.id}><div><strong>{item.assessmentTitle}</strong><small>{item.studentName} · {new Date(item.submittedAt || item.startedAt).toLocaleDateString('vi-VN')} · {item.status === 'SUBMITTED' ? `${item.totalScore}/${item.maxScore} điểm` : 'Đang làm'}</small></div>{item.status === 'SUBMITTED' && <button className="assessment-secondary" onClick={() => openResult(item.id)}>Xem kết quả</button>}</article>)}</div> : <Empty text={resultStudentId ? 'Học sinh này chưa có kết quả cho bài kiểm tra của bạn.' : 'Chọn lớp và học sinh để xem lịch sử.'} />}</div>
      </section>
    </div>}

    {!activeAttempt && !result && !isTeacher && <div className="assessment-student-grid">
      <section className="assessment-panel"><PanelTitle eyebrow="BÀI ĐƯỢC GIAO" title="Bài kiểm tra của em" detail="Chọn bài được giao cho lớp của em để bắt đầu." />{assessments.length ? <div className="assessment-test-list">{assessments.map(test => <article className="assessment-test-item" key={test.id}><div><strong>{test.title}</strong><small>{types[test.type]} · {test.questions.length} câu · {test.durationMinutes} phút</small></div><button className="assessment-primary" disabled={busy} onClick={() => openAttempt(test)}>Bắt đầu</button></article>)}</div> : <Empty text="Chưa có bài kiểm tra được giao. Em hãy hỏi giáo viên khi nào có bài mới nhé!" />}</section>
      <section className="assessment-panel"><PanelTitle eyebrow="LỊCH SỬ HỌC TẬP" title="Các lần làm bài" detail="Xem lại điểm và đáp án sau khi nộp bài." />{history.length ? <div className="assessment-test-list">{history.map(item => <article className="assessment-test-item" key={item.id}><div><strong>{item.assessmentTitle}</strong><small>{new Date(item.submittedAt || item.startedAt).toLocaleDateString('vi-VN')} · {item.status === 'SUBMITTED' ? `${item.totalScore}/${item.maxScore} điểm` : 'Đang làm'}</small></div>{item.status === 'SUBMITTED' ? <button className="assessment-secondary" onClick={() => openResult(item.id)}>Xem kết quả</button> : <button className="assessment-primary" onClick={() => openAttempt(item)}>Tiếp tục</button>}</article>)}</div> : <Empty text="Bài đã nộp sẽ xuất hiện ở đây." />}</section>
    </div>}
    <footer className="assessment-footer">Câu trả lời được chấm theo đáp án và quy tắc của từng dạng câu hỏi.</footer>
  </main>
}

function Select({ label, value, items, onChange }) { return <label>{label}<select value={value} onChange={event => onChange(event.target.value)}><option value="">Chọn</option>{items.map(item => <option key={item.id} value={item.id}>{item.name}</option>)}</select></label> }
function PanelTitle({ eyebrow, title, detail }) { return <header className="assessment-panel-title"><div><p className="assessment-kicker">{eyebrow}</p><h2>{title}</h2><p>{detail}</p></div></header> }
function Empty({ text }) { return <p className="assessment-empty">{text}</p> }
function QuestionCard({ question, index, draft, onChange }) {
  const selected = draft.selectedOptionIds || []
  const blanks = draft.blankAnswers || Array(question.blankCount).fill('')
  return <article className="assessment-question"><div className="assessment-question-heading"><span className="assessment-question-number">{index + 1}</span><div><h3>{question.content}</h3><small>{question.score} điểm {question.allowMultiple ? '· Chọn tất cả đáp án đúng' : ''}</small></div></div>
    {question.kind === 'MULTIPLE_CHOICE' && <div className="assessment-option-list">{question.options.map(option => <label className={`assessment-option ${selected.includes(option.id) ? 'is-selected' : ''}`} key={option.id}><input type={question.allowMultiple ? 'checkbox' : 'radio'} name={`question-${question.id}`} checked={selected.includes(option.id)} onChange={() => onChange({ selectedOptionIds: question.allowMultiple ? (selected.includes(option.id) ? selected.filter(id => id !== option.id) : [...selected, option.id]) : [option.id] })} /><span>{option.name}</span></label>)}</div>}
    {question.kind === 'FILL_BLANK' && <div className="assessment-input-list">{blanks.map((value, i) => <label key={i}>Ô {i + 1}<input value={value} onChange={event => onChange({ blankAnswers: blanks.map((item, n) => n === i ? event.target.value : item) })} /></label>)}</div>}
    {question.kind === 'SHORT_ANSWER' && <label className="assessment-answer-input">Câu trả lời<input value={draft.content || ''} onChange={event => onChange({ content: event.target.value })} /></label>}
  </article>
}
function ResultCard({ result, onClose }) {
  const { attempt, answers } = result
  const percentage = attempt.maxScore ? Math.round(attempt.totalScore / attempt.maxScore * 100) : 0
  return <section className="assessment-panel assessment-result"><div className="assessment-result-head"><div><p className="assessment-kicker">ĐÃ HOÀN THÀNH</p><h2>{attempt.assessmentTitle}</h2><p>Điểm của em</p></div><div className="assessment-score">{attempt.totalScore}<small> / {attempt.maxScore} điểm</small><span>{percentage}%</span></div></div><div className="assessment-result-answers">{answers.map((answer, index) => <article className={`assessment-result-item ${answer.correct ? 'is-correct' : 'is-wrong'}`} key={answer.questionId}><strong>{index + 1}. {answer.questionContent}</strong><p>{answer.correct ? 'Đúng' : 'Chưa đúng'} · {answer.awardedScore}/{answer.score} điểm</p><div><span>Câu trả lời của em: {answer.submitted.length ? answer.submitted.join(' · ') : 'Chưa trả lời'}</span><span>Đáp án: {answer.expected.join(' · ')}</span></div>{answer.explanation && <small>Gợi ý: {answer.explanation}</small>}</article>)}</div><button className="assessment-secondary" onClick={onClose}>Đóng kết quả</button></section>
}
