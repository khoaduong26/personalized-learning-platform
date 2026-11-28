import { useEffect, useState } from 'react'
import { Link, useParams, useSearchParams } from 'react-router-dom'
import HomeHeader from '../../home/components/HomeHeader'
import LessonCurriculumToc from '../components/LessonCurriculumToc'
import { getAcademicSession, getLesson } from '../services/academicApi'
import { getLearningPaths } from '../../learningpath/services/learningPathApi'
import { parseLearningSections } from '../utils/lessonContent'
import '../styles/academic-lesson.css'
import '../styles/student-curriculum.css'

export default function StudentLessonPage() {
  const { lessonId } = useParams()
  const [searchParams] = useSearchParams()
  const requestedStepId = searchParams.get('stepId')
  const [student, setStudent] = useState(null)
  const [lesson, setLesson] = useState(null)
  const [exerciseStepId, setExerciseStepId] = useState(null)
  const [state, setState] = useState('loading')

  useEffect(() => {
    document.title = 'Bài học · EduLearn'
    let active = true
    getAcademicSession().then(async session => {
      if (!active) return
      if (session.role !== 'STUDENT') { setState('forbidden'); return }
      setStudent(session)
      try {
        const item = await getLesson(lessonId)
        let activeStepId = null
        if (requestedStepId) {
          const paths = await getLearningPaths(session.id).catch(() => [])
          const step = paths.filter(path => path.status === 'ACTIVE').flatMap(path => path.steps)
            .find(candidate => String(candidate.id) === requestedStepId
              && String(candidate.lessonId) === String(item.id)
              && candidate.status === 'IN_PROGRESS')
          activeStepId = step?.id ?? null
        }
        if (active) { setLesson(item); setExerciseStepId(activeStepId); document.title = `${item.name} · EduLearn`; setState('ready') }
      } catch (error) {
        if (active) setState(error.status === 401 ? 'signed-out' : 'error')
      }
    }).catch(error => { if (active) setState(error.status === 401 ? 'signed-out' : 'error') })
    return () => { active = false }
  }, [lessonId, requestedStepId])

  if (state === 'loading') return <main className="student-curriculum-shell"><p className="student-curriculum-state">Đang mở bài học…</p></main>
  if (state === 'signed-out') return <LessonGate title="Đăng nhập để mở bài học" signedOut />
  if (state === 'forbidden') return <LessonGate title="Trang dành cho học sinh" />
  if (state === 'error') return <LessonGate title="Không tìm thấy bài học" description="Bài học không tồn tại hoặc em không có quyền xem bài này." />

  const sections = parseLearningSections(lesson.learningContent)
  return <main id="noi-dung" tabIndex={-1} className="student-curriculum-shell">
    <HomeHeader user={student} onLogout={() => setStudent(null)} />
    <div className="student-lesson-layout">
    <LessonCurriculumToc subjectId={lesson.subjectId} subjectName={lesson.subjectName} currentLessonId={lesson.id} lessonPathPrefix="/curriculum/lessons" />
    <article className="student-lesson-page">
      <nav className="student-lesson-breadcrumb" aria-label="Đường dẫn bài học"><Link to="/curriculum">Chương trình học</Link><span>›</span><span>{lesson.subjectName}</span><span>›</span><span>{lesson.topicName}</span></nav>
      <header className="student-lesson-heading"><p className="student-curriculum-kicker">BÀI HỌC</p><h1>{lesson.name}</h1>{lesson.description && <p>{lesson.description}</p>}</header>
      <LessonImages images={(lesson.images || []).filter(image => image.placement === 'BEFORE_CONTENT')} />
      {lesson.learningContent?.trim() ? <section className="student-lesson-content" aria-label="Nội dung ôn tập"><div className="student-lesson-sections">{sections.map((section, index) => <article className="student-lesson-content-section" key={`lesson-section-${index}`}><h2>{section.title}</h2><div>{section.content}</div></article>)}</div></section> : <section className="student-curriculum-empty student-lesson-missing"><h2>Nội dung ôn tập đang được cập nhật</h2><p>Bài học này chưa có phần kiến thức chi tiết. Em có thể xem các câu hỏi đã lưu hoặc quay lại chọn bài khác.</p></section>}
      <LessonImages images={(lesson.images || []).filter(image => image.placement === 'AFTER_CONTENT')} />
      <footer className="student-lesson-actions"><Link to="/curriculum">← Xem các bài học</Link>{exerciseStepId && <Link className="student-lesson-exercise-action" to={`/exercises/steps/${exerciseStepId}`}>Tiếp theo: làm bài tập →</Link>}<Link to="/my-learning-path">Lộ trình của em</Link></footer>
    </article>
    </div>
  </main>
}

function LessonImages({ images }) {
  if (!images.length) return null
  return <div className="student-lesson-images">{images.map((image, index) => <figure key={`${image.url}-${index}`} className={`is-${image.alignment.toLowerCase()} is-${image.displaySize.toLowerCase()}`}>
    <img src={image.url} alt={image.altText || image.caption || 'Hình minh họa bài học'} loading="lazy" />
    {image.caption && <figcaption>{image.caption}</figcaption>}
  </figure>)}</div>
}

function LessonGate({ title, description, signedOut }) {
  return <main className="student-curriculum-shell"><section className="student-curriculum-empty student-curriculum-gate"><h1>{title}</h1><p>{description || 'Đăng nhập bằng tài khoản học sinh để xem nội dung bài học.'}</p>{signedOut && <Link to="/login">Đăng nhập</Link>}<Link to="/curriculum">Về chương trình học</Link></section></main>
}
