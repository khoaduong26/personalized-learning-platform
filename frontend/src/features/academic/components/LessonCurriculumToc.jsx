import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getLessons, getTopics } from '../services/academicApi'

export default function LessonCurriculumToc({ subjectId, subjectName, currentLessonId, lessonPathPrefix }) {
  const [topics, setTopics] = useState([])
  const [lessonsByTopic, setLessonsByTopic] = useState({})
  const [openTopicIds, setOpenTopicIds] = useState(() => new Set())
  const [state, setState] = useState('loading')

  useEffect(() => {
    let active = true
    setState('loading')
    getTopics(subjectId).then(async items => {
      const entries = await Promise.all(items.map(async topic => [topic.id, await getLessons(topic.id)]))
      if (!active) return
      setTopics(items)
      setLessonsByTopic(Object.fromEntries(entries))
      setOpenTopicIds(new Set())
      setState('ready')
    }).catch(() => { if (active) setState('error') })
    return () => { active = false }
  }, [subjectId])

  useEffect(() => {
    if (currentLessonId == null) return
    setOpenTopicIds(current => {
      const next = new Set(current)
      const currentTopic = topics.find(topic => (lessonsByTopic[topic.id] || []).some(lesson => String(lesson.id) === String(currentLessonId)))
      if (currentTopic) next.add(String(currentTopic.id))
      return next
    })
  }, [currentLessonId, topics, lessonsByTopic])

  function toggleTopic(topicId) {
    setOpenTopicIds(current => {
      const next = new Set(current)
      const key = String(topicId)
      if (next.has(key)) next.delete(key)
      else next.add(key)
      return next
    })
  }

  return <nav className="academic-lesson-toc" aria-label="Mục lục chương trình">
    <h2>Mục lục chương trình</h2>
    {subjectName && <p className="academic-lesson-toc-program">{subjectName}</p>}
    {state === 'loading' && <p className="academic-lesson-toc-state">Đang tải chủ đề và bài học…</p>}
    {state === 'error' && <p className="academic-lesson-toc-state" role="alert">Chưa tải được danh sách bài học.</p>}
    {state === 'ready' && (topics.length ? <ol className="academic-lesson-toc-topics">{topics.map(topic => {
      const lessons = lessonsByTopic[topic.id] || []
      const isOpen = openTopicIds.has(String(topic.id))
      return <li key={topic.id}>
        <button className="academic-lesson-toc-topic" type="button" aria-expanded={isOpen} aria-controls={`curriculum-topic-${topic.id}`} onClick={() => toggleTopic(topic.id)}>
          <span>{topic.name}</span><span className="academic-lesson-toc-topic-meta">{lessons.length} bài <span aria-hidden="true">{isOpen ? '−' : '+'}</span></span>
        </button>
        {isOpen && (lessons.length > 0
          ? <ol id={`curriculum-topic-${topic.id}`}>{lessons.map(lesson => <li key={lesson.id}>
            <Link className="academic-lesson-toc-lesson" to={`${lessonPathPrefix}/${lesson.id}`} aria-current={String(lesson.id) === String(currentLessonId) ? 'page' : undefined}>{lesson.name}</Link>
          </li>)}</ol>
          : <p id={`curriculum-topic-${topic.id}`} className="academic-lesson-toc-state">Chủ đề này chưa có bài học.</p>)}
      </li>
    })}</ol> : <p className="academic-lesson-toc-state">Chương trình này chưa có chủ đề.</p>)}
  </nav>
}
