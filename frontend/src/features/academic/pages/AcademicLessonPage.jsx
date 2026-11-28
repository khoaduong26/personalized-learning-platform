import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import HomeHeader from '../../home/components/HomeHeader'
import LessonSectionsEditor from '../components/LessonSectionsEditor'
import LessonCurriculumToc from '../components/LessonCurriculumToc'
import { getAcademicSession, getLesson, getSubjects, updateLesson, uploadLessonImage } from '../services/academicApi'
import { parseLearningSections, serializeLearningSections } from '../utils/lessonContent'
import '../styles/academic.css'
import '../styles/academic-lesson.css'

const imageDefaults = { placement: 'AFTER_CONTENT', alignment: 'CENTER', displaySize: 'MEDIUM', altText: '', caption: '' }

export default function AcademicLessonPage() {
  const { lessonId } = useParams()
  const [user, setUser] = useState(null)
  const [lesson, setLesson] = useState(null)
  const [canEdit, setCanEdit] = useState(false)
  const [form, setForm] = useState(null)
  const [state, setState] = useState('loading')
  const [editing, setEditing] = useState(false)
  const [busy, setBusy] = useState(false)
  const [notice, setNotice] = useState('')

  useEffect(() => {
    document.title = 'Bài học · EduLearn'
    let active = true
    Promise.all([getAcademicSession(), getLesson(lessonId), getSubjects()]).then(([session, item, subjects]) => {
      if (!active) return
      if (!['ADMIN', 'TEACHER'].includes(session.role)) { setState('forbidden'); return }
      const subject = subjects.find(value => value.id === item.subjectId)
      const allowed = session.role === 'ADMIN'
        ? Boolean(subject?.shared)
        : Boolean(subject && !subject.shared && subject.ownerId === session.id)
      setUser(session)
      setLesson(item)
      setCanEdit(allowed)
      setForm(toForm(item))
      document.title = `${item.name} · EduLearn`
      setState('ready')
    }).catch(error => { if (active) setState(error.status === 401 ? 'signed-out' : error.status === 403 ? 'forbidden' : 'error') })
    return () => { active = false }
  }, [lessonId])

  function beginEditing() {
    setForm(toForm(lesson))
    setNotice('')
    setEditing(true)
  }

  async function save(event) {
    event.preventDefault()
    if (!canEdit || busy) return
    setBusy(true)
    setNotice('')
    try {
      let saved = await updateLesson(lesson.id, form.name, form.description, serializeLearningSections(form.sections),
        form.images.filter(image => !image.file), lesson.topicId)
      setLesson(saved)
      const uploaded = new Map()
      for (const image of form.images.filter(item => item.file)) {
        const result = await uploadLessonImage(lesson.id, image.file)
        uploaded.set(image.key, { ...image, url: result.url, file: undefined })
      }
      if (uploaded.size) {
        const images = form.images.map(image => image.file ? uploaded.get(image.key) : image)
        saved = await updateLesson(lesson.id, form.name, form.description, serializeLearningSections(form.sections), images, lesson.topicId)
      }
      setLesson(saved)
      setForm(toForm(saved))
      setEditing(false)
      setNotice('Đã lưu nội dung bài học.')
    } catch (error) {
      setNotice(error.message || 'Chưa lưu được bài học. Vui lòng thử lại.')
    } finally {
      setBusy(false)
    }
  }

  if (state === 'loading') return <main className="academic-shell"><p className="academic-state">Đang mở bài học…</p></main>
  if (state !== 'ready') return <Gate state={state} />

  const beforeImages = editing ? form.images.filter(image => image.placement === 'BEFORE_CONTENT') : (lesson.images || []).filter(image => image.placement === 'BEFORE_CONTENT')
  const afterImages = editing ? form.images.filter(image => image.placement === 'AFTER_CONTENT') : (lesson.images || []).filter(image => image.placement === 'AFTER_CONTENT')
  const sections = parseLearningSections(lesson.learningContent)
  return <main id="noi-dung" tabIndex={-1} className="academic-shell academic-lesson-shell">
    <HomeHeader user={user} onLogout={() => setUser(null)} />
    <div className="academic-lesson-back"><Link to="/academic">← Quay lại chương trình học</Link><span>{lesson.subjectName} <b>›</b> {lesson.topicName}</span></div>
    {notice && <div className="academic-notice" role="status">{notice}<button type="button" onClick={() => setNotice('')} aria-label="Đóng">×</button></div>}
    <article className="academic-lesson-card">
      <div className={`academic-lesson-layout${editing ? ' is-editing' : ''}`}>
      {!editing && <LessonCurriculumToc subjectId={lesson.subjectId} subjectName={lesson.subjectName} currentLessonId={lesson.id} lessonPathPrefix="/academic/lessons" />}
      <div className="academic-lesson-main">
      <header className="academic-lesson-heading" id="lesson-overview">
        <div><p className="academic-kicker">{editing ? 'CHỈNH SỬA NỘI DUNG' : 'CHI TIẾT BÀI HỌC'}</p>{!editing && <><h1>{lesson.name}</h1>{lesson.description && <p>{lesson.description}</p>}</>}</div>
        {canEdit && !editing && <button className="academic-primary" type="button" onClick={beginEditing}>Chỉnh sửa bài học</button>}
      </header>
      {!canEdit && <p className="academic-lesson-readonly">Chương trình chung đang ở chế độ chỉ xem. Giáo viên có thể lưu một bản riêng để chỉnh sửa.</p>}
      {editing ? <form className="academic-lesson-editor" onSubmit={save}>
        <label>Tên bài học<input required maxLength={160} value={form.name} onChange={event => setForm(current => ({ ...current, name: event.target.value }))} /></label>
        <label>Mô tả / nguồn<textarea maxLength={5000} rows="2" value={form.description} onChange={event => setForm(current => ({ ...current, description: event.target.value }))} placeholder="Mục tiêu hoặc trang sách tham khảo" /></label>
        <LessonSectionsEditor sections={form.sections} onChange={sections => setForm(current => ({ ...current, sections }))} />
        <ImageEditor images={form.images} onChange={images => setForm(current => ({ ...current, images }))} />
        <div className="academic-lesson-editor-actions"><button className="academic-primary" disabled={busy || !form.name.trim()}>{busy ? 'Đang lưu…' : 'Lưu bài học'}</button><button type="button" className="academic-cancel" disabled={busy} onClick={() => { setForm(toForm(lesson)); setEditing(false) }}>Hủy</button></div>
      </form> : <div className="academic-lesson-reading">
        <LessonImages id="lesson-images-before" title="Hình minh họa đầu bài" images={beforeImages} />
        {lesson.learningContent?.trim() ? sections.map((section, index) => <section id={`lesson-section-${index + 1}`} className="academic-lesson-content-section" key={`lesson-section-${index}`}><h2>{section.title}</h2><div className="academic-lesson-content">{section.content}</div></section>) : <section id="lesson-content" className="academic-lesson-empty"><h2>Chưa có nội dung ôn tập</h2><p>{canEdit ? 'Chọn “Chỉnh sửa bài học” để thêm kiến thức và ví dụ cho học sinh.' : 'Nội dung bài học đang được cập nhật.'}</p></section>}
        <LessonImages id="lesson-images-after" title="Hình minh họa cuối bài" images={afterImages} />
      </div>}
      </div>
      </div>
    </article>
  </main>
}

function toForm(lesson) {
  return {
    name: lesson.name || '', description: lesson.description || '', sections: parseLearningSections(lesson.learningContent),
    images: (lesson.images || []).map((image, index) => ({ ...image, key: `saved-${lesson.id}-${index}` })),
  }
}

function ImageEditor({ images, onChange }) {
  function addFiles(event) {
    const files = [...event.target.files].slice(0, Math.max(0, 8 - images.length))
    onChange([...images, ...files.map((file, index) => ({
      ...imageDefaults, key: `new-${Date.now()}-${index}`, file, previewUrl: URL.createObjectURL(file),
    }))])
    event.target.value = ''
  }
  function update(index, key, value) {
    onChange(images.map((image, position) => position === index ? { ...image, [key]: value } : image))
  }
  function move(index, delta) {
    const next = [...images]
    const target = index + delta
    if (target < 0 || target >= next.length) return
    ;[next[index], next[target]] = [next[target], next[index]]
    onChange(next)
  }
  return <section className="academic-lesson-image-editor"><div><h2>Hình minh họa</h2><p>Chọn vị trí, cách căn và thứ tự ảnh trong bài.</p></div>
    <label className="academic-lesson-file-input">Thêm ảnh<input type="file" accept="image/jpeg,image/png" multiple disabled={images.length >= 8} onChange={addFiles} /><small>JPEG/PNG, tối đa 2 MB mỗi ảnh; tối đa 8 ảnh.</small></label>
    {images.map((image, index) => <div className="academic-lesson-image-row" key={image.key || image.url}>
      <img src={image.previewUrl || image.url} alt={image.altText || ''} />
      <div className="academic-lesson-image-fields">
        <label>Mô tả ảnh<input maxLength={200} value={image.altText || ''} onChange={event => update(index, 'altText', event.target.value)} /></label>
        <label>Chú thích<input maxLength={300} value={image.caption || ''} onChange={event => update(index, 'caption', event.target.value)} /></label>
        <div className="academic-lesson-image-selects">
          <label>Vị trí<select value={image.placement} onChange={event => update(index, 'placement', event.target.value)}><option value="BEFORE_CONTENT">Trước nội dung</option><option value="AFTER_CONTENT">Sau nội dung</option></select></label>
          <label>Căn ảnh<select value={image.alignment} onChange={event => update(index, 'alignment', event.target.value)}><option value="LEFT">Trái</option><option value="CENTER">Giữa</option><option value="RIGHT">Phải</option></select></label>
          <label>Kích thước<select value={image.displaySize} onChange={event => update(index, 'displaySize', event.target.value)}><option value="SMALL">Nhỏ</option><option value="MEDIUM">Vừa</option><option value="LARGE">Lớn</option></select></label>
        </div>
      </div>
      <div className="academic-lesson-image-actions"><button type="button" disabled={index === 0} onClick={() => move(index, -1)} aria-label="Đưa ảnh lên">↑</button><button type="button" disabled={index === images.length - 1} onClick={() => move(index, 1)} aria-label="Đưa ảnh xuống">↓</button><button type="button" onClick={() => onChange(images.filter((_, position) => position !== index))} aria-label="Gỡ ảnh">×</button></div>
    </div>)}
  </section>
}

function LessonImages({ id, title, images }) {
  if (!images.length) return null
  return <section id={id} className="academic-lesson-image-section"><h2>{title}</h2><div className="academic-lesson-images">{images.map((image, index) => <figure key={`${image.url}-${index}`} className={`is-${image.alignment.toLowerCase()} is-${image.displaySize.toLowerCase()}`}>
    <img src={image.url} alt={image.altText || image.caption || 'Hình minh họa bài học'} loading="lazy" />{image.caption && <figcaption>{image.caption}</figcaption>}
  </figure>)}</div></section>
}

function Gate({ state }) {
  const signedOut = state === 'signed-out'
  const title = signedOut ? 'Đăng nhập để xem bài học' : state === 'forbidden' ? 'Trang dành cho giáo viên và quản trị viên' : 'Không tìm thấy bài học'
  return <main className="academic-shell"><section className="academic-gate"><h1>{title}</h1><p>{state === 'error' ? 'Bài học không tồn tại hoặc tài khoản không có quyền xem.' : 'Đăng nhập bằng tài khoản giáo viên hoặc quản trị viên để tiếp tục.'}</p>{signedOut && <Link className="academic-primary" to="/login?returnTo=/academic">Đăng nhập</Link>}<Link className="academic-back" to="/academic">Quay lại chương trình học</Link></section></main>
}
