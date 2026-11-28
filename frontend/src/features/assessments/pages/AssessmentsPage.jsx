import { useEffect, useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import HomeHeader from '../../home/components/HomeHeader'
import { getLessons, getSubjects, getTopics } from '../../academic/services/academicApi'
import { getClassroomStudents } from '../../classrooms/services/classroomApi'
import {
  createAssessment, createQuestion, deleteQuestion, getAssessment, getAssessmentClassrooms, getAssessmentSession,
  getAssessments, getAttemptDraft, getAttemptHistory, getAttemptResult, getQuestionBank, getQuestionForEditing,
  publishAssessment, saveAnswer, startAttempt, submitAttempt, updateQuestion,
} from '../services/assessmentApi'
import '../styles/assessments.css'

const types = { DIAGNOSTIC: 'Khảo sát đầu vào', PERIODIC: 'Định kỳ', PRACTICE: 'Luyện tập' }
const emptyOption = () => ({ name: '', correct: false })
const emptyQuestionForm = () => ({ content: '', difficulty: 'EASY', score: '1', kind: 'MULTIPLE_CHOICE', allowMultiple: false, options: [emptyOption(), emptyOption()], blanks: [{ correctAnswer: '' }], correctAnswer: '', caseSensitive: false, explanation: '' })

export default function AssessmentsPage() {
  const [searchParams] = useSearchParams()
  const requestedClassroomId = searchParams.get('classroomId') || ''
  const requestedStudentId = searchParams.get('studentId') || ''
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
  const [questionForm, setQuestionForm] = useState(emptyQuestionForm)
  const [editingQuestionId, setEditingQuestionId] = useState('')
  const [assessmentForm, setAssessmentForm] = useState({ title: '', type: 'DIAGNOSTIC', durationMinutes: 30 })
  const [activeAttempt, setActiveAttempt] = useState(null)
  const [answerDrafts, setAnswerDrafts] = useState({})
  const [result, setResult] = useState(null)
  const [history, setHistory] = useState([])
  const [resultClassId, setResultClassId] = useState('')
  const [resultStudents, setResultStudents] = useState([])
  const [resultStudentId, setResultStudentId] = useState('')
  const [teacherHistory, setTeacherHistory] = useState([])
  const [resultStudentsLoading, setResultStudentsLoading] = useState(false)
  const [teacherHistoryLoading, setTeacherHistoryLoading] = useState(false)
  const [notice, setNotice] = useState('')
  const [busy, setBusy] = useState(false)
  const isTeacher = user?.role === 'TEACHER'
  const canManageQuestionBank = isTeacher || user?.role === 'ADMIN'
  const isStudent = user?.role === 'STUDENT'
  const questionSubjects = user?.role === 'ADMIN'
    ? subjects.filter(subject => subject.shared)
    : subjects.filter(subject => !subject.shared)
  const groupedTeacherHistory = useMemo(() => {
    const groups = teacherHistory.reduce((result, attempt) => {
      const key = attempt.assessmentId == null
        ? `learning-step-${attempt.learningStepId ?? 'unknown'}`
        : `assessment-${attempt.assessmentId}`
      if (!result[key]) result[key] = {
        id: key,
        title: attempt.assessmentTitle || 'Bài luyện tập theo lộ trình',
        attempts: [],
      }
      result[key].attempts.push(attempt)
      return result
    }, {})
    return Object.values(groups).map(group => ({
      ...group,
      attempts: group.attempts.sort((a, b) =>
        new Date(b.submittedAt || b.startedAt) - new Date(a.submittedAt || a.startedAt)),
    })).sort((a, b) => new Date(b.attempts[0]?.submittedAt || b.attempts[0]?.startedAt)
      - new Date(a.attempts[0]?.submittedAt || a.attempts[0]?.startedAt))
  }, [teacherHistory])
  const classAssessmentGroups = useMemo(() => {
    const groups = Object.fromEntries(classrooms.map(classroom => [String(classroom.id), {
      id: String(classroom.id), name: classroom.name, schoolYear: classroom.schoolYear, assessments: [],
    }]))
    assessments.forEach(test => {
      const ids = test.classroomIds || []
      if (!ids.length) {
        if (!groups.unassigned) groups.unassigned = { id: 'unassigned', name: 'Chưa gắn lớp', assessments: [] }
        groups.unassigned.assessments.push(test)
        return
      }
      ids.forEach((classroomId, index) => {
        const key = String(classroomId)
        if (!groups[key]) groups[key] = {
          id: key, name: test.classroomNames?.[index] || 'Lớp học', assessments: [],
        }
        if (!groups[key].assessments.some(item => item.id === test.id)) groups[key].assessments.push(test)
      })
    })
    return Object.values(groups).filter(group => group.assessments.length)
      .sort((a, b) => a.name.localeCompare(b.name, 'vi'))
  }, [classrooms, assessments])

  const changeResultClass = id => {
    setResultClassId(id)
    setResultStudents([]); setResultStudentId(''); setTeacherHistory([])
    setResultStudentsLoading(Boolean(id)); setTeacherHistoryLoading(false)
  }
  const changeResultStudent = id => { setResultStudentId(id); setTeacherHistory([]); setTeacherHistoryLoading(false) }
  const changeSubject = id => {
    setSubjectId(id); setTopics([]); setTopicId(''); setLessons([]); setLessonId(''); setBank([]); setSelectedQuestions([])
  }
  const changeTopic = id => { setTopicId(id); setLessons([]); setLessonId(''); setBank([]); setSelectedQuestions([]) }
  const changeLesson = id => { setLessonId(id); setBank([]); setSelectedQuestions([]) }
  const changeQuestionKind = kind => setQuestionForm(current => ({
    ...current,
    kind,
    options: kind === 'MULTIPLE_CHOICE' && current.options.length >= 2 ? current.options : [emptyOption(), emptyOption()],
    blanks: kind === 'FILL_BLANK' && current.blanks.length ? current.blanks : [{ correctAnswer: '' }],
    correctAnswer: kind === 'SHORT_ANSWER' ? current.correctAnswer : '',
  }))

  useEffect(() => {
    document.title = 'EduLearn'
    let active = true
    getAssessmentSession().then(async session => {
      if (!active) return
      setUser(session)
      document.title = session?.role === 'ADMIN' ? 'Ngân hàng câu hỏi · EduLearn' : 'Bài kiểm tra · EduLearn'
      if (!['ADMIN', 'TEACHER', 'STUDENT'].includes(session?.role)) { setAccess('forbidden'); return }
      setAccess('allowed')
      try {
        const [tests, subjectItems] = await Promise.all([
          session.role === 'ADMIN' ? Promise.resolve([]) : getAssessments(),
          session.role === 'STUDENT' ? Promise.resolve([]) : getSubjects(),
        ])
        if (!active) return
        setAssessments(tests); setSubjects(subjectItems)
        const availableQuestionSubjects = session.role === 'ADMIN'
          ? subjectItems.filter(subject => subject.shared)
          : subjectItems.filter(subject => !subject.shared)
        setSubjectId(current => availableQuestionSubjects.some(subject => String(subject.id) === current)
          ? current
          : String(availableQuestionSubjects[0]?.id ?? ''))
        if (session.role === 'TEACHER') {
          const classItems = await getAssessmentClassrooms()
          if (!active) return
          setClassrooms(classItems)
          setResultClassId(current => classItems.some(item => String(item.id) === current)
            ? current
            : String(classItems.find(item => String(item.id) === requestedClassroomId)?.id ?? classItems[0]?.id ?? ''))
        } else setHistory(await getAttemptHistory(session.id))
      } catch (error) { if (active) setNotice(error.message) }
    }).catch(error => { if (active) setAccess(error.status === 403 ? 'forbidden' : 'signed-out') })
    return () => { active = false }
  }, [requestedClassroomId])

  useEffect(() => {
    if (!isTeacher || !resultClassId) return
    let active = true
    setResultStudentsLoading(true)
    getClassroomStudents(resultClassId).then(items => {
      if (!active) return
      const options = items.map(item => ({ ...item, name: item.fullName }))
      setResultStudents(options)
      setResultStudentId(current => options.some(item => String(item.id) === current) ? current
        : String(options.find(item => String(item.id) === requestedStudentId)?.id ?? options[0]?.id ?? ''))
    }).catch(error => { if (active) setNotice(error.message) })
      .finally(() => { if (active) setResultStudentsLoading(false) })
    return () => { active = false }
  }, [isTeacher, resultClassId, requestedStudentId])
  useEffect(() => {
    if (!isTeacher || !resultStudentId) return
    let active = true
    setTeacherHistoryLoading(true)
    getAttemptHistory(resultStudentId, resultClassId).then(items => { if (active) setTeacherHistory(items) })
      .catch(error => { if (active) setNotice(error.message) })
      .finally(() => { if (active) setTeacherHistoryLoading(false) })
    return () => { active = false }
  }, [isTeacher, resultStudentId, resultClassId])

  useEffect(() => {
    if (!subjectId) return
    let active = true
    getTopics(subjectId).then(items => { if (active) { setTopics(items); setTopicId(items[0] ? String(items[0].id) : '') } }).catch(error => { if (active) setNotice(error.message) })
    return () => { active = false }
  }, [subjectId])
  useEffect(() => {
    if (!topicId) return
    let active = true
    getLessons(topicId).then(items => { if (active) { setLessons(items); setLessonId(items[0] ? String(items[0].id) : '') } }).catch(error => { if (active) setNotice(error.message) })
    return () => { active = false }
  }, [topicId])
  useEffect(() => {
    if (!lessonId || !canManageQuestionBank) return
    let active = true
    getQuestionBank(lessonId).then(items => { if (active) setBank(items) }).catch(error => { if (active) setNotice(error.message) })
    return () => { active = false }
  }, [lessonId, canManageQuestionBank])

  const safeAction = async action => {
    setBusy(true); setNotice('')
    try { await action() } catch (error) { setNotice(error.message) } finally { setBusy(false) }
  }
  const saveQuestion = event => {
    event.preventDefault()
    safeAction(async () => {
      const payload = { ...questionForm, lessonId: Number(lessonId), score: Number(questionForm.score),
        options: questionForm.kind === 'MULTIPLE_CHOICE' ? questionForm.options.map((item, displayOrder) => ({ ...item, displayOrder })) : [],
        blanks: questionForm.kind === 'FILL_BLANK' ? questionForm.blanks.map((item, position) => ({ ...item, position })) : [] }
      if (editingQuestionId) {
        const previousId = Number(editingQuestionId)
        await updateQuestion(previousId, payload)
        setSelectedQuestions(current => current.filter(id => id !== previousId))
        setEditingQuestionId('')
        setQuestionForm(emptyQuestionForm())
        setNotice('Đã cập nhật câu hỏi trong ngân hàng. Bài kiểm tra đã giao vẫn giữ nguyên phiên bản cũ.')
      } else {
        await createQuestion(payload)
        setQuestionForm(current => ({ ...current, content: '', explanation: '' }))
        setNotice('Đã thêm câu hỏi vào ngân hàng.')
      }
      setBank(await getQuestionBank(lessonId))
    })
  }
  const editQuestion = id => safeAction(async () => {
    const question = await getQuestionForEditing(id)
    setEditingQuestionId(String(question.id))
    setQuestionForm({
      content: question.content, difficulty: question.difficulty, score: String(question.score), kind: question.kind,
      allowMultiple: question.allowMultiple, options: question.options.map(option => ({ ...option })),
      blanks: question.blanks.length ? question.blanks.map(blank => ({ correctAnswer: blank.correctAnswer })) : [{ correctAnswer: '' }],
      correctAnswer: question.correctAnswer || '', caseSensitive: question.caseSensitive,
      explanation: question.explanation || '',
    })
    setNotice('Đang sửa câu hỏi đã chọn. Dạng câu hỏi được giữ nguyên.')
  })
  const removeQuestion = question => {
    if (!window.confirm('Ẩn câu hỏi này khỏi ngân hàng? Các bài kiểm tra đã giao vẫn giữ nguyên nội dung.')) return
    safeAction(async () => {
      await deleteQuestion(question.id)
      if (String(question.id) === editingQuestionId) {
        setEditingQuestionId('')
        setQuestionForm(emptyQuestionForm())
      }
      setBank(await getQuestionBank(lessonId))
      setSelectedQuestions(current => current.filter(id => id !== question.id))
      setNotice('Đã xóa câu hỏi khỏi ngân hàng. Nội dung trong bài kiểm tra đã giao vẫn được giữ nguyên.')
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

  return <main id="noi-dung" tabIndex={-1} className={`${isStudent ? 'assessment-shell assessment-student' : 'assessment-shell'}${user?.role === 'ADMIN' ? ' assessment-admin-bank' : ''}`}>
    <HomeHeader user={user} onLogout={() => setUser(null)} />
    <section className="assessment-heading">
      <div>
        {!isStudent && <p className="assessment-kicker">{user?.role === 'ADMIN' ? 'QUẢN LÝ NỘI DUNG' : 'KIỂM TRA VÀ ĐÁNH GIÁ'}</p>}
        <h1>{activeAttempt ? 'Bài làm của em' : result ? 'Kết quả bài kiểm tra' : isStudent ? 'Bài kiểm tra của em' : user?.role === 'ADMIN' ? 'Ngân hàng câu hỏi' : 'Kiểm tra và đánh giá'}</h1>
        {activeAttempt && <p>{activeAttempt.assessment.durationMinutes ? `Em có ${activeAttempt.assessment.durationMinutes} phút để làm bài.` : 'Bài này không giới hạn thời gian.'} Em có thể lưu bài để làm tiếp sau hoặc nộp khi đã xong.</p>}
        {!activeAttempt && !result && <p>{isStudent ? 'Làm bài thầy cô đã giao hoặc xem lại bài em đã nộp.' : user?.role === 'ADMIN' ? 'Quản lý câu hỏi trong chương trình học chung theo từng chủ đề và bài học.' : 'Tạo câu hỏi theo bài học, tổ chức bài kiểm tra và theo dõi tiến độ học tập.'}</p>}
      </div>
      <span className="assessment-heading-mark" aria-hidden="true">✎</span>
    </section>
    {notice && <div className="assessment-notice" role="status">{notice}<button type="button" onClick={() => setNotice('')} aria-label="Đóng">×</button></div>}

    {activeAttempt && <section className="assessment-panel assessment-take"><div className="assessment-panel-title"><div><p className="assessment-kicker">{types[activeAttempt.assessment.type]}</p><h2>{activeAttempt.assessment.title}</h2></div><span className="assessment-count">{currentQuestions.length} câu</span></div>
      {currentQuestions.map((question, index) => <QuestionCard key={question.id} question={question} index={index} draft={answerDrafts[question.id] || {}} onChange={update => setDraft(question.id, update)} />)}
      {!currentQuestions.length && <Empty text="Bài kiểm tra chưa có câu hỏi." />}
      <div className="assessment-actions"><button className="assessment-secondary" disabled={busy} onClick={saveDraft}>Lưu để làm sau</button><button className="assessment-primary" disabled={busy} onClick={finishAttempt}>Nộp bài</button></div>
    </section>}

    {result && <ResultCard result={result} onClose={() => setResult(null)} />}

    {!activeAttempt && !result && canManageQuestionBank && <div className="assessment-teacher-grid">
      <section className="assessment-panel"><PanelTitle eyebrow={user?.role === 'ADMIN' ? 'CHƯƠNG TRÌNH CHUNG' : 'BƯỚC 1 · NGÂN HÀNG'} title={editingQuestionId ? 'Sửa câu hỏi' : user?.role === 'ADMIN' ? 'Thêm câu hỏi' : 'Tạo câu hỏi'} detail="Mỗi câu hỏi gắn với một bài học để phân tích kết quả." />
        {!questionSubjects.length ? <div className="assessment-private-subject-empty">
          <strong>{user?.role === 'ADMIN' ? 'Chưa có chương trình chung để tạo câu hỏi.' : 'Chưa có chương trình riêng để tạo câu hỏi.'}</strong>
          <span>{user?.role === 'ADMIN' ? 'Hãy tạo chương trình chung trước khi thêm câu hỏi.' : 'Hãy sao chép chương trình chung hoặc tạo chương trình của riêng bạn trước.'}</span>
          <Link to="/academic">Mở Chương trình học <span aria-hidden="true">→</span></Link>
        </div> : <div className="assessment-selects">
          <Select label={user?.role === 'ADMIN' ? 'Chương trình chung' : 'Chương trình riêng'} placeholder="Chọn chương trình" value={subjectId} items={questionSubjects} onChange={changeSubject} disabled={Boolean(editingQuestionId)} />
          <Select label="Chủ đề" value={topicId} items={topics} onChange={changeTopic} disabled={Boolean(editingQuestionId)} />
          <Select label="Bài học / kiến thức" value={lessonId} items={lessons} onChange={changeLesson} disabled={Boolean(editingQuestionId)} />
        </div>}
        {lessonId ? <form className="assessment-form" onSubmit={saveQuestion}>
          <label>Nội dung câu hỏi<textarea required maxLength="4000" value={questionForm.content} onChange={e => setQuestionForm({ ...questionForm, content: e.target.value })} placeholder="Ví dụ: 8 + 7 bằng bao nhiêu?" /></label>
          <div className="assessment-form-row"><Select label="Dạng câu hỏi" value={questionForm.kind} items={[{ id: 'MULTIPLE_CHOICE', name: 'Trắc nghiệm' }, { id: 'FILL_BLANK', name: 'Điền khuyết' }, { id: 'SHORT_ANSWER', name: 'Trả lời ngắn' }]} onChange={changeQuestionKind} /><Select label="Độ khó" value={questionForm.difficulty} items={[{ id: 'EASY', name: 'Dễ' }, { id: 'MEDIUM', name: 'Vừa' }, { id: 'HARD', name: 'Khó' }]} onChange={difficulty => setQuestionForm({ ...questionForm, difficulty })} /><label>Điểm<input type="number" min="0.1" max="100" step="0.1" required value={questionForm.score} onChange={e => setQuestionForm({ ...questionForm, score: e.target.value })} /></label></div>
          {questionForm.kind === 'MULTIPLE_CHOICE' && <div className="assessment-choice-builder"><label className="assessment-check"><input type="checkbox" checked={questionForm.allowMultiple} onChange={e => setQuestionForm({ ...questionForm, allowMultiple: e.target.checked })} /> Cho phép chọn nhiều đáp án</label>{questionForm.options.map((option, index) => <div className="assessment-option-edit" key={index}><input required value={option.name} placeholder={`Lựa chọn ${index + 1}`} onChange={e => setQuestionForm({ ...questionForm, options: questionForm.options.map((item, i) => i === index ? { ...item, name: e.target.value } : item) })} /><label className="assessment-check"><input type="checkbox" checked={option.correct} onChange={e => setQuestionForm({ ...questionForm, options: questionForm.options.map((item, i) => i === index ? { ...item, correct: e.target.checked } : item) })} /> Đúng</label>{questionForm.options.length > 2 && <button type="button" className="assessment-remove" onClick={() => setQuestionForm({ ...questionForm, options: questionForm.options.filter((_, i) => i !== index) })}>×</button>}</div>)}<button type="button" className="assessment-text-button" onClick={() => setQuestionForm({ ...questionForm, options: [...questionForm.options, emptyOption()] })}>+ Thêm lựa chọn</button></div>}
          {questionForm.kind === 'FILL_BLANK' && <div className="assessment-choice-builder"><p className="assessment-field-label">Đáp án theo thứ tự ô trống</p>{questionForm.blanks.map((blank, index) => <div className="assessment-option-edit" key={blank.position ?? index}><input required value={blank.correctAnswer} placeholder={`Đáp án ô ${index + 1}`} onChange={e => setQuestionForm({ ...questionForm, blanks: questionForm.blanks.map((item, i) => i === index ? { correctAnswer: e.target.value } : item) })} />{questionForm.blanks.length > 1 && <button type="button" className="assessment-remove" onClick={() => setQuestionForm({ ...questionForm, blanks: questionForm.blanks.filter((_, i) => i !== index) })}>×</button>}</div>)}<button type="button" className="assessment-text-button" onClick={() => setQuestionForm({ ...questionForm, blanks: [...questionForm.blanks, { correctAnswer: '' }] })}>+ Thêm ô trống</button></div>}
          {questionForm.kind === 'SHORT_ANSWER' && <label>Đáp án chuẩn<input required value={questionForm.correctAnswer} onChange={e => setQuestionForm({ ...questionForm, correctAnswer: e.target.value })} /></label>}
          {questionForm.kind !== 'MULTIPLE_CHOICE' && <label className="assessment-check"><input type="checkbox" checked={questionForm.caseSensitive} onChange={e => setQuestionForm({ ...questionForm, caseSensitive: e.target.checked })} /> Phân biệt chữ hoa và chữ thường</label>}
          <label>Lời giải / gợi ý<textarea value={questionForm.explanation} onChange={e => setQuestionForm({ ...questionForm, explanation: e.target.value })} placeholder="Hiển thị sau khi học sinh nộp bài" /></label>
          <div className="assessment-question-form-actions"><button className="assessment-primary" disabled={busy}>{editingQuestionId ? 'Lưu thay đổi' : 'Thêm vào ngân hàng'}</button>{editingQuestionId && <button type="button" className="assessment-secondary" disabled={busy} onClick={() => { setEditingQuestionId(''); setQuestionForm(emptyQuestionForm()); setNotice('Đã hủy sửa câu hỏi.') }}>Hủy sửa</button>}</div>
        </form> : <Empty text="Tạo môn học, chủ đề và bài học trước khi thêm câu hỏi." />}
      </section>
      <section className="assessment-panel"><PanelTitle eyebrow="CÂU HỎI ĐÃ LƯU" title={user?.role === 'ADMIN' ? 'Câu hỏi trong bài học' : 'Ngân hàng theo bài học'} detail={user?.role === 'ADMIN' ? 'Danh sách câu hỏi đã lưu cho bài học đang chọn.' : 'Chọn câu hỏi để đưa vào bài kiểm tra mới.'} />
        {bank.length ? <div className="assessment-bank-list">{bank.map(question => {
          const details = <span><strong>{question.content}</strong><small>{question.kind === 'MULTIPLE_CHOICE' ? 'Trắc nghiệm' : question.kind === 'FILL_BLANK' ? 'Điền khuyết' : 'Trả lời ngắn'} · {question.difficulty === 'EASY' ? 'Dễ' : question.difficulty === 'MEDIUM' ? 'Vừa' : 'Khó'} · {question.score} điểm</small></span>
          return <article className="assessment-bank-item" key={question.id}>
            {isTeacher && <input type="checkbox" aria-label={`Chọn câu hỏi: ${question.content}`} checked={selectedQuestions.includes(question.id)} onChange={() => toggleId(setSelectedQuestions, question.id)} />}
            {details}
            <div className="assessment-bank-actions"><button type="button" className="assessment-secondary" disabled={busy} onClick={() => editQuestion(question.id)}>Sửa</button><button type="button" className="assessment-secondary" disabled={busy} onClick={() => removeQuestion(question)}>Xóa</button></div>
          </article>
        })}</div> : <Empty text="Bài học này chưa có câu hỏi trong ngân hàng." />}
        {isTeacher && <form className="assessment-form assessment-create-form" onSubmit={saveAssessment}><PanelTitle eyebrow="BƯỚC 2 · BÀI KIỂM TRA" title="Lưu bài kiểm tra nháp" detail="Chọn lớp được giao và câu hỏi cần sử dụng." />
          <label>Tên bài kiểm tra<input required maxLength="180" value={assessmentForm.title} onChange={e => setAssessmentForm({ ...assessmentForm, title: e.target.value })} placeholder="Ví dụ: Ôn tập phép cộng trong phạm vi 20" /></label>
          <div className="assessment-form-row"><Select label="Loại bài" value={assessmentForm.type} items={Object.entries(types).map(([id, name]) => ({ id, name }))} onChange={type => setAssessmentForm({ ...assessmentForm, type })} /><label>Thời gian (phút)<input type="number" min="1" max="240" value={assessmentForm.durationMinutes} onChange={e => setAssessmentForm({ ...assessmentForm, durationMinutes: e.target.value })} /></label></div>
          <div><p className="assessment-field-label">Giao cho lớp</p><div className="assessment-class-list">{classrooms.map(item => <label className="assessment-check" key={item.id}><input type="checkbox" checked={selectedClasses.includes(item.id)} onChange={() => toggleId(setSelectedClasses, item.id)} /> {item.name} · {item.schoolYear}</label>)}</div>{!classrooms.length && <small className="assessment-muted">Chưa có lớp học được phân công.</small>}</div>
          <div className="assessment-form-footer"><span>{selectedCount} câu · {selectedClasses.length} lớp</span><button className="assessment-primary" disabled={busy || !selectedCount || !selectedClasses.length}>Lưu bản nháp</button></div>
        </form>}
      </section>
      {isTeacher && <section className="assessment-panel assessment-tests"><PanelTitle eyebrow="ĐỀ CỦA BẠN" title="Bài kiểm tra" detail="Bài nháp chỉ hiển thị với giáo viên cho đến khi công bố." />
        {assessments.length ? <div className="assessment-class-assessment-groups">{classAssessmentGroups.map(group => <details className="assessment-class-assessment-group" key={group.id} open={classAssessmentGroups.length === 1}>
          <summary><span><strong>{group.name}</strong>{group.schoolYear && <small>{group.schoolYear}</small>}</span><b>{group.assessments.length} đề <i aria-hidden="true">⌄</i></b></summary>
          <div className="assessment-test-list">{group.assessments.map(test => <article className="assessment-test-item" key={test.id}>
            <div><strong>{test.title}</strong><small>{types[test.type]} · {test.questions.length} câu</small></div>
            <div className="assessment-test-actions"><span className={`assessment-status ${test.published ? 'is-published' : ''}`}>{test.published ? 'Đã giao' : 'Bản nháp'}</span>
              {!test.published && <button disabled={busy} className="assessment-secondary" onClick={() => safeAction(async () => { const updated = await publishAssessment(test.id); setAssessments(current => current.map(item => item.id === updated.id ? updated : item)); setNotice('Đã công bố bài kiểm tra cho các lớp đã chọn.') })}>Công bố</button>}
            </div>
          </article>)}</div>
        </details>)}</div> : <Empty text="Chưa có bài kiểm tra. Hãy tạo bản nháp đầu tiên." />}
        <div className="assessment-results-filter">
          <PanelTitle title="Kết quả học sinh" detail="Chọn lớp và học sinh để xem các lần làm bài, điểm số và câu trả lời." />
          <div className="assessment-form-row assessment-result-selects">
            <Select label="Lớp học" placeholder="Chọn lớp học" value={resultClassId} items={classrooms} onChange={changeResultClass} />
            <Select label="Học sinh" placeholder={resultStudentsLoading ? 'Đang tải học sinh…' : 'Chọn học sinh'}
              value={resultStudentId} items={resultStudents} disabled={!resultClassId || resultStudentsLoading || !resultStudents.length}
              onChange={changeResultStudent} />
          </div>
          {teacherHistoryLoading ? <p className="assessment-results-state" role="status">Đang tải lịch sử kiểm tra…</p>
            : groupedTeacherHistory.length ? <div className="assessment-result-groups">{groupedTeacherHistory.map(group => <details className="assessment-result-group" key={group.id}>
              <summary><strong>{group.title}</strong><span>{group.attempts.length} lượt làm <i aria-hidden="true">⌄</i></span></summary>
              <div className="assessment-result-attempts">{group.attempts.map(item => <article className="assessment-test-item" key={item.id}>
                <div><strong>{item.studentName} · {new Date(item.submittedAt || item.startedAt).toLocaleDateString('vi-VN')}</strong>
                  <small>{item.status === 'SUBMITTED' ? `${item.totalScore}/${item.maxScore} điểm` : 'Đang làm'}</small></div>
                {item.status === 'SUBMITTED' && <button className="assessment-secondary" onClick={() => openResult(item.id)}>Xem câu trả lời</button>}
              </article>)}</div>
            </details>)}</div>
              : !classrooms.length ? <Empty text="Tài khoản của bạn chưa được phân công lớp học." />
                : !resultClassId ? <Empty text="Chọn một lớp để tải danh sách học sinh." />
                  : resultStudentsLoading ? <p className="assessment-results-state" role="status">Đang tải học sinh trong lớp…</p>
                    : !resultStudents.length ? <Empty text="Lớp này chưa có học sinh." />
                      : !resultStudentId ? <Empty text="Chọn học sinh để xem lịch sử kiểm tra." />
                        : <Empty text="Học sinh này chưa có kết quả kiểm tra trong lớp đã chọn." />}
        </div>
      </section>}
    </div>}

    {!activeAttempt && !result && isStudent && <div className="assessment-student-grid">
      <section className="assessment-panel"><PanelTitle title="Bài thầy cô giao" />{assessments.length ? <div className="assessment-test-list">{assessments.map(test => <article className="assessment-test-item" key={test.id}><div><strong>{test.title}</strong><small>{test.questions.length} câu{test.durationMinutes ? ` · ${test.durationMinutes} phút` : ''}</small></div><button className="assessment-primary" disabled={busy} onClick={() => openAttempt(test)}>Bắt đầu làm bài</button></article>)}</div> : <Empty text="Khi thầy cô giao bài kiểm tra, em sẽ thấy bài ở đây." />}</section>
      <section className="assessment-panel"><PanelTitle title="Các lần làm bài" />{history.length ? <div className="assessment-test-list">{history.map(item => <article className="assessment-test-item" key={item.id}><div><strong>{item.assessmentTitle}</strong><small>{new Date(item.submittedAt || item.startedAt).toLocaleDateString('vi-VN')} · {item.status === 'SUBMITTED' ? `${item.totalScore}/${item.maxScore} điểm` : 'Đang làm'}</small></div>{item.status === 'SUBMITTED' ? <button className="assessment-secondary" disabled={busy} onClick={() => openResult(item.id)}>Xem kết quả</button> : <button className="assessment-primary" disabled={busy} onClick={() => openAttempt(item)}>Làm tiếp</button>}</article>)}</div> : <Empty text="Các bài em đã bắt đầu hoặc đã nộp sẽ hiện ở đây." />}</section>
    </div>}
    {user?.role !== 'ADMIN' && <footer className="assessment-footer">Câu trả lời được chấm theo đáp án và quy tắc của từng dạng câu hỏi.</footer>}
  </main>
}

function Select({ label, value, items, onChange, placeholder = 'Chọn', disabled = false }) { return <label>{label}<select value={value} disabled={disabled} onChange={event => onChange(event.target.value)}><option value="">{placeholder}</option>{items.map(item => <option key={item.id} value={item.id}>{item.name}</option>)}</select></label> }
function PanelTitle({ eyebrow, title, detail }) { return <header className="assessment-panel-title"><div>{eyebrow && <p className="assessment-kicker">{eyebrow}</p>}<h2>{title}</h2>{detail && <p>{detail}</p>}</div></header> }
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
