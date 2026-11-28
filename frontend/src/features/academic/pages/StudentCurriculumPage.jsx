import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import HomeHeader from '../../home/components/HomeHeader'
import { getAcademicSession, getLessons, getSubjects, getTopics } from '../services/academicApi'
import '../styles/student-curriculum.css'

export default function StudentCurriculumPage() {
  const [student, setStudent] = useState(null)
  const [subjects, setSubjects] = useState([])
  const [subjectId, setSubjectId] = useState('')
  const [topics, setTopics] = useState([])
  const [lessonsByTopic, setLessonsByTopic] = useState({})
  const [state, setState] = useState('loading')
  const [error, setError] = useState('')
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    document.title = 'Học lại chương trình · EduLearn'
    let active = true
    getAcademicSession().then(async session => {
      if (!active) return
      if (session.role !== 'STUDENT') { setState('forbidden'); return }
      setStudent(session)
      try {
        const items = await getSubjects()
        if (!active) return
        const sharedSubjects = items.filter(item => item.shared)
        setSubjects(sharedSubjects)
        setSubjectId(String(sharedSubjects[0]?.id ?? ''))
        setState('ready')
      } catch (loadError) {
        if (active) { setError(loadError.message); setState('error') }
      }
    }).catch(loadError => {
      if (active) setState(loadError.status === 401 ? 'signed-out' : 'error')
    })
    return () => { active = false }
  }, [])

  useEffect(() => {
    if (!subjectId || state !== 'ready') { setTopics([]); setLessonsByTopic({}); return }
    let active = true
    setState('loading-content')
    setError('')
    getTopics(subjectId).then(async items => {
      const lessonEntries = await Promise.all(items.map(async topic => [topic.id, await getLessons(topic.id)]))
      if (!active) return
      setTopics(items)
      setLessonsByTopic(Object.fromEntries(lessonEntries))
      setState('ready')
    }).catch(loadError => {
      if (active) { setError(loadError.message); setState('content-error') }
    })
    return () => { active = false }
  }, [subjectId, reloadKey])

  if (state === 'loading') return <main className="student-curriculum-shell"><p className="student-curriculum-state">Đang mở chương trình học…</p></main>
  if (state === 'signed-out') return <Gate title="Đăng nhập để học lại" description="Đăng nhập bằng tài khoản học sinh để mở chương trình học." signedOut />
  if (state === 'forbidden') return <Gate title="Trang dành cho học sinh" description="Tài khoản hiện tại không thể mở khu vực học lại." />

  const selectedSubject = subjects.find(item => String(item.id) === subjectId)
  const lessonCount = Object.values(lessonsByTopic).reduce((sum, items) => sum + items.length, 0)
  return <main id="noi-dung" tabIndex={-1} className="student-curriculum-shell">
    <HomeHeader user={student} onLogout={() => setStudent(null)} />
    <section className="student-curriculum-hero">
      <p className="student-curriculum-kicker">GÓC HỌC TẬP</p>
      <h1>Học lại chương trình</h1>
      <p>Chọn chương trình, mở từng chủ đề và ôn lại kiến thức theo bài học.</p>
    </section>
    {subjects.length > 0 ? <>
      <section className="student-curriculum-toolbar">
        <label>Chương trình học
          <select value={subjectId} onChange={event => setSubjectId(event.target.value)}>
            {subjects.map(subject => <option key={subject.id} value={subject.id}>{subject.name}</option>)}
          </select>
        </label>
        <div className="student-curriculum-summary"><strong>{topics.length}</strong> chủ đề <span>·</span> <strong>{lessonCount}</strong> bài học</div>
      </section>
      {error && <p className="student-curriculum-error" role="alert">{error}</p>}
      {state === 'loading-content' && <p className="student-curriculum-state" role="status">Đang tải các bài học…</p>}
      {state === 'content-error' && <button className="student-curriculum-retry" type="button" onClick={() => setReloadKey(current => current + 1)}>Thử tải lại</button>}
      {state === 'ready' && <>
        {!topics.length ? <section className="student-curriculum-empty"><h2>Chương trình chưa có chủ đề</h2><p>Thầy cô sẽ bổ sung nội dung học tập tại đây.</p></section> : <div className="student-curriculum-topics">
          {topics.map((topic, index) => {
            const lessons = lessonsByTopic[topic.id] || []
            return <section className="student-curriculum-topic" key={topic.id}>
              <header><span className="student-curriculum-topic-number">{String(index + 1).padStart(2, '0')}</span><div><h2>{topic.name}</h2><p>{lessons.length} bài học</p></div></header>
              {lessons.length ? <ol>{lessons.map((lesson, lessonIndex) => <li key={lesson.id}>
                <span className="student-curriculum-lesson-number">{lessonIndex + 1}</span>
                <div className="student-curriculum-lesson-copy"><h3>{lesson.name}</h3><p>{lesson.learningContent ? 'Có nội dung ôn tập' : 'Nội dung ôn tập đang được cập nhật'}</p></div>
                <Link to={`/curriculum/lessons/${lesson.id}`} aria-label={`Mở bài ${lesson.name}`}>Học bài <span aria-hidden="true">→</span></Link>
              </li>)}</ol> : <p className="student-curriculum-empty-topic">Chủ đề này chưa có bài học.</p>}
            </section>
          })}
        </div>}
        <p className="student-curriculum-subject-note">Đang xem: <strong>{selectedSubject?.name}</strong></p>
      </>}
    </> : state === 'error' ? <section className="student-curriculum-empty" role="alert"><h2>Chưa tải được chương trình</h2><p>{error || 'Em hãy thử tải lại trang.'}</p><button className="student-curriculum-retry" type="button" onClick={() => window.location.reload()}>Tải lại</button></section> : <section className="student-curriculum-empty"><h2>Chưa có chương trình học</h2><p>Chương trình chung sẽ xuất hiện tại đây khi được nhà trường thêm vào.</p><Link to="/my-learning-path">Quay lại lộ trình của em</Link></section>}
  </main>
}

function Gate({ title, description, signedOut }) {
  return <main className="student-curriculum-shell"><section className="student-curriculum-empty student-curriculum-gate"><h1>{title}</h1><p>{description}</p>{signedOut && <Link to="/login?returnTo=/curriculum">Đăng nhập</Link>}<Link to="/">Về trang chủ</Link></section></main>
}
