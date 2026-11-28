import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  copySubject, createLesson, createSubject, createTopic, deleteLesson, deleteSubject, deleteTopic,
  getAcademicSession, getLessons, getSubjects, getTopics, updateLesson, updateSubject, updateTopic,
  uploadLessonImage,
} from '../services/academicApi'
import '../styles/academic.css'
import '../styles/academic-lesson.css'
import HomeHeader from '../../home/components/HomeHeader'
import LessonSectionsEditor from '../components/LessonSectionsEditor'
import { parseLearningSections, serializeLearningSections } from '../utils/lessonContent'

const emptyForm = { name: '', description: '', images: [], sections: [] }
const emptyLessonForm = { ...emptyForm, sections: [{ title: '', content: '' }] }

export default function AcademicPage() {
  const [user, setUser] = useState(null)
  const [access, setAccess] = useState('loading')
  const [subjects, setSubjects] = useState([])
  const [topics, setTopics] = useState([])
  const [lessons, setLessons] = useState([])
  const [subjectId, setSubjectId] = useState(null)
  const [collection, setCollection] = useState('shared')
  const [topicId, setTopicId] = useState(null)
  const [forms, setForms] = useState({ subject: emptyForm, topic: emptyForm, lesson: emptyLessonForm })
  const [editing, setEditing] = useState(null)
  const [catalogRevision, setCatalogRevision] = useState(0)
  const [notice, setNotice] = useState('')
  const [busy, setBusy] = useState(false)
  const isTeacher = user?.role === 'TEACHER'
  const visibleSubjects = isTeacher
    ? subjects.filter(item => collection === 'shared' ? item.shared : !item.shared)
    : subjects.filter(item => item.shared)
  const selectedSubject = subjects.find(item => item.id === subjectId)

  const loadSubjects = useCallback(async () => {
    const items = await getSubjects()
    setSubjects(items)
    setSubjectId(current => items.some(item => item.id === current)
      ? current
      : (items.find(item => item.shared)?.id ?? items[0]?.id ?? null))
  }, [])

  useEffect(() => {
    document.title = 'Chương trình học · TLCN'
    let active = true
    getAcademicSession().then(session => {
      if (!active) return
      setUser(session)
      if (!['ADMIN', 'TEACHER'].includes(session?.role)) { setAccess('forbidden'); return }
      setAccess('allowed')
      loadSubjects().catch(loadError => { if (active) setNotice(loadError.message) })
    }).catch(error => {
      if (active) setAccess(error.status === 403 ? 'forbidden' : 'signed-out')
    })
    return () => { active = false }
  }, [loadSubjects])

  useEffect(() => {
    if (!subjectId || access !== 'allowed') return
    let active = true
    getTopics(subjectId).then(items => {
      if (!active) return
      setTopics(items)
      setTopicId(current => items.some(item => item.id === current) ? current : (items[0]?.id ?? null))
    }).catch(error => { if (active) setNotice(error.message) })
    return () => { active = false }
  }, [subjectId, access, catalogRevision])

  useEffect(() => {
    if (!topicId || access !== 'allowed') return
    let active = true
    getLessons(topicId).then(items => { if (active) setLessons(items) }).catch(error => { if (active) setNotice(error.message) })
    return () => { active = false }
  }, [topicId, access, catalogRevision])

  const mutate = async (action, success) => {
    setBusy(true); setNotice('')
    try { await action(); await loadSubjects(); setCatalogRevision(current => current + 1); setNotice(success); setEditing(null) }
    catch (error) { setNotice(error.message) }
    finally { setBusy(false) }
  }

  const save = (kind, values) => {
    if (kind === 'subject') {
      let saved
      return mutate(async () => {
      if (editing?.kind === kind) saved = await updateSubject(editing.id, values.name)
      else saved = await createSubject(values.name)
      setForms(current => ({ ...current, subject: emptyForm }))
      if (saved && !editing) setSubjectId(saved.id)
    }, 'Đã lưu chương trình học.')
    }
    if (kind === 'topic') return mutate(async () => {
      if (editing?.kind === kind) await updateTopic(editing.id, values.name, subjectId)
      else await createTopic(values.name, subjectId)
      setForms(current => ({ ...current, topic: emptyForm }))
    }, 'Đã lưu chủ đề.')
    return mutate(async () => {
      let saved = editing?.kind === kind
        ? await updateLesson(editing.id, values.name, values.description, serializeLearningSections(values.sections), values.images.filter(image => !image.file), topicId)
        : await createLesson(values.name, values.description, serializeLearningSections(values.sections), [], topicId)
      if (!editing && saved?.id) setEditing({ kind: 'lesson', id: saved.id })
      const uploaded = new Map()
      for (const image of values.images.filter(item => item.file)) {
        const { url } = await uploadLessonImage(saved.id, image.file)
        uploaded.set(image.key, { ...image, url, file: undefined })
      }
      if (uploaded.size) {
        const finalImages = values.images.map(image => image.file ? uploaded.get(image.key) : image)
        saved = await updateLesson(saved.id, values.name, values.description, serializeLearningSections(values.sections), finalImages, topicId)
      }
      setForms(current => ({ ...current, lesson: emptyLessonForm }))
    }, 'Đã lưu bài học.')
  }

  const beginEdit = (kind, item) => {
    setEditing({ kind, id: item.id })
    setForms(current => ({ ...current, [kind]: {
      name: item.name,
      description: item.description || '',
      sections: kind === 'lesson' ? parseLearningSections(item.learningContent) : [],
      images: (item.images || []).map((image, index) => ({ ...image, key: `saved-${item.id}-${index}` })),
    } }))
  }

  const remove = (kind, item) => {
    const contents = kind === 'subject' && item.topicCount
      ? ` Toàn bộ ${item.topicCount} chủ đề và bài học bên trong cũng sẽ bị xóa.`
      : kind === 'topic' && item.lessonCount
        ? ` Toàn bộ ${item.lessonCount} bài học bên trong cũng sẽ bị xóa.`
        : ''
    const dependencyNotice = kind === 'lesson' || kind === 'topic' || kind === 'subject'
      ? ' Nếu nội dung đã được dùng trong câu hỏi, lộ trình hoặc dữ liệu tiến độ học sinh, hệ thống sẽ từ chối để giữ lịch sử học tập.'
      : ''
    if (!window.confirm(`Xóa “${item.name}”?${contents}${dependencyNotice}`)) return
    const action = kind === 'subject' ? deleteSubject(item.id) : kind === 'topic' ? deleteTopic(item.id) : deleteLesson(item.id)
    mutate(() => action.then(() => {
      if (kind === 'subject' && item.id === subjectId) {
        const remaining = subjects.filter(value => value.id !== item.id && (collection === 'shared' ? value.shared : !value.shared))
        setSubjectId(remaining[0]?.id ?? null); setTopics([]); setTopicId(null); setLessons([])
      }
      if (kind === 'topic' && item.id === topicId) { setTopicId(null); setLessons([]) }
      if (kind === 'lesson') setLessons(current => current.filter(lesson => lesson.id !== item.id))
    }), 'Đã xóa mục đã chọn.')
  }

  const copySharedSubject = async item => {
    let copy
    await mutate(async () => { copy = await copySubject(item.id) }, `Đã lưu “${item.name} - copy” vào chương trình của bạn.`)
    if (copy) {
      setCollection('owned')
      setTopics([]); setTopicId(null); setLessons([])
      setForms({ subject: emptyForm, topic: emptyForm, lesson: emptyLessonForm })
      setSubjectId(copy.id)
    }
  }

  const chooseCollection = next => {
    setCollection(next)
    const nextSubjects = subjects.filter(item => next === 'shared' ? item.shared : !item.shared)
    setSubjectId(nextSubjects[0]?.id ?? null)
    setTopics([]); setTopicId(null); setLessons([])
    setEditing(null)
  }

  if (access === 'loading') return <main className="academic-shell"><p className="academic-state">Đang kiểm tra phiên đăng nhập…</p></main>
  if (access === 'signed-out') return <main className="academic-shell"><div className="academic-gate"><span className="academic-kicker">KHU VỰC GIÁO VIÊN</span><h1>Đăng nhập để quản lý chương trình học</h1><p>Trang này dành cho tài khoản giáo viên hoặc quản trị viên.</p><Link className="academic-primary" to="/login?returnTo=/academic">Đi đến đăng nhập</Link><Link className="academic-back" to="/">Về trang chủ</Link></div></main>
  if (access === 'forbidden') return <main className="academic-shell"><div className="academic-gate"><span className="academic-kicker">CHƯA ĐƯỢC CẤP QUYỀN</span><h1>Trang này dành cho giáo viên và quản trị viên</h1><p>{user?.fullName ? `Xin chào ${user.fullName}, ` : ''}tài khoản hiện tại không thể quản lý chương trình học.</p><Link className="academic-back" to="/">Về trang chủ</Link></div></main>

  return <main id="noi-dung" tabIndex={-1} className="academic-shell">
    <HomeHeader user={user} onLogout={() => setUser(null)} />
    <section className="academic-heading"><div><p className="academic-kicker">QUẢN LÝ NỘI DUNG HỌC TẬP</p><h1>Chương trình học</h1><p>{isTeacher ? 'Chương trình chung để tham khảo; bản của tôi để chỉnh sửa và sử dụng.' : 'Tổ chức môn học, chủ đề và bài học theo cấu trúc rõ ràng.'}</p></div><span className="academic-heading-mark" aria-hidden="true">✎</span></section>
    {isTeacher && <div className="academic-collections" role="tablist" aria-label="Loại chương trình">
      <button type="button" role="tab" aria-selected={collection === 'shared'} className={collection === 'shared' ? 'is-active' : ''} onClick={() => chooseCollection('shared')}><span>Chương trình chung</span><small>{subjects.filter(item => item.shared).length}</small></button>
      <button type="button" role="tab" aria-selected={collection === 'owned'} className={collection === 'owned' ? 'is-active' : ''} onClick={() => chooseCollection('owned')}><span>Chương trình của tôi</span><small>{subjects.filter(item => !item.shared).length}</small></button>
    </div>}
    {isTeacher && collection === 'shared' && <div className="academic-collection-hint"><strong>Chỉ xem</strong><span>Lưu một bản riêng để chỉnh sửa mà không ảnh hưởng giáo viên khác.</span></div>}
    <div className="academic-breadcrumb"><span>Chương trình học</span><span>›</span><strong>{subjects.find(item => item.id === subjectId)?.name || 'Chọn môn học'}</strong>{topicId && <><span>›</span><strong>{topics.find(item => item.id === topicId)?.name}</strong></>}</div>
    {notice && <div className="academic-notice" role="status">{notice}<button type="button" onClick={() => setNotice('')} aria-label="Đóng">×</button></div>}
    <section className="academic-columns" aria-label="Cấu trúc chương trình học">
      <AcademicColumn title={collection === 'shared' ? 'Chương trình chung' : 'Chương trình của tôi'} count={visibleSubjects.length} hint={collection === 'shared' ? 'Danh mục dùng chung · chỉ xem' : 'Các bản thuộc tài khoản của bạn'}>
        {((isTeacher && collection === 'owned') || (!isTeacher && collection === 'shared'))
          ? <ItemForm kind="subject" label={isTeacher ? 'Tên chương trình riêng' : 'Tên môn học chung'} form={forms.subject} editing={editing?.kind === 'subject'} busy={busy} onChange={value => setForms(current => ({ ...current, subject: value }))} onSubmit={value => save('subject', value)} onCancel={() => { setEditing(null); setForms(current => ({ ...current, subject: emptyForm })) }} />
          : <div className="academic-own-prompt"><span aria-hidden="true">＋</span><p>Chọn <strong>Lưu bản riêng</strong> ở một chương trình để bắt đầu chỉnh sửa.</p></div>}
        <div className="academic-list">{visibleSubjects.map(item => <AcademicItem key={item.id} item={item} selected={subjectId === item.id} meta={`${item.topicCount} chủ đề`} badge={item.shared ? 'Chung' : item.ownerId === user?.id ? 'Của tôi' : 'Riêng'} onSelect={() => { setSubjectId(item.id); setTopics([]); setTopicId(null); setLessons([]); setEditing(null); setForms({ subject: emptyForm, topic: emptyForm, lesson: emptyLessonForm }) }} onEdit={(!isTeacher || !item.shared) ? () => beginEdit('subject', item) : null} onDelete={(!isTeacher || !item.shared) ? () => remove('subject', item) : null} onCopy={isTeacher && item.shared ? () => copySharedSubject(item) : null} />)}</div>
        {!visibleSubjects.length && <EmptyState text={collection === 'shared' ? 'Chưa có chương trình chung.' : 'Bạn chưa lưu chương trình riêng nào. Chọn một chương trình chung để lưu bản sao.'} />}
      </AcademicColumn>
      <AcademicColumn title="Chủ đề" count={topics.length} hint={subjectId ? 'Cấp 2 · nhóm kiến thức' : 'Chọn môn học trước'}>
        {subjectId && (!isTeacher || !selectedSubject?.shared) ? <ItemForm kind="topic" label="Tên chủ đề" form={forms.topic} editing={editing?.kind === 'topic'} busy={busy} onChange={value => setForms(current => ({ ...current, topic: value }))} onSubmit={value => save('topic', value)} onCancel={() => { setEditing(null); setForms(current => ({ ...current, topic: emptyForm })) }} /> : <p className="academic-inline-hint">{subjectId ? 'Lưu môn học chung về chương trình của bạn để chỉnh sửa chủ đề.' : 'Hãy chọn một môn học để quản lý chủ đề.'}</p>}
        <div className="academic-list">{topics.map(item => <AcademicItem key={item.id} item={item} selected={topicId === item.id} meta={`${item.lessonCount} bài học`} onSelect={() => { setTopicId(item.id); setLessons([]); setEditing(null); setForms(current => ({ ...current, topic: emptyForm, lesson: emptyLessonForm })) }} onEdit={(!isTeacher || !selectedSubject?.shared) ? () => beginEdit('topic', item) : null} onDelete={(!isTeacher || !selectedSubject?.shared) ? () => remove('topic', item) : null} />)}</div>
        {subjectId && !topics.length && <EmptyState text="Môn học này chưa có chủ đề." />}
      </AcademicColumn>
      <AcademicColumn title="Bài học" count={lessons.length} hint={topicId ? 'Cấp 3 · đơn vị kiến thức' : 'Chọn chủ đề trước'}>
        {topicId && (!isTeacher || !selectedSubject?.shared) ? <ItemForm kind="lesson" label="Tên bài học / đơn vị kiến thức" form={forms.lesson} editing={editing?.kind === 'lesson'} busy={busy} onChange={value => setForms(current => ({ ...current, lesson: value }))} onSubmit={value => save('lesson', value)} onCancel={() => { setEditing(null); setForms(current => ({ ...current, lesson: emptyLessonForm })) }} /> : <p className="academic-inline-hint">{topicId ? 'Lưu môn học chung về chương trình của bạn để chỉnh sửa bài học.' : 'Hãy chọn một chủ đề để quản lý bài học.'}</p>}
        <div className="academic-list">{lessons.map(item => <AcademicItem key={item.id} item={item} meta={item.description || 'Đơn vị kiến thức'} onEdit={(!isTeacher || !selectedSubject?.shared) ? () => beginEdit('lesson', item) : null} onDelete={(!isTeacher || !selectedSubject?.shared) ? () => remove('lesson', item) : null} onView={`/academic/lessons/${item.id}`} />)}</div>
        {topicId && !lessons.length && <EmptyState text="Chủ đề này chưa có bài học." />}
      </AcademicColumn>
    </section>
    <footer className="academic-footer">Mỗi bài học có mã định danh riêng để liên kết với câu hỏi, bài làm và phân tích năng lực.</footer>
  </main>
}

function AcademicColumn({ title, count, hint, children }) {
  return <section className="academic-column"><header><div><h2>{title}</h2><p>{hint}</p></div><span className="academic-count">{count}</span></header>{children}</section>
}

function ItemForm({ kind, label, form, editing, busy, onChange, onSubmit, onCancel }) {
  const update = (key, value) => onChange({ ...form, [key]: value })
  const updateImage = (index, key, value) => update('images', form.images.map((image, itemIndex) => itemIndex === index ? { ...image, [key]: value } : image))
  const moveImage = (index, direction) => {
    const next = [...form.images]
    const target = index + direction
    if (target < 0 || target >= next.length) return
    ;[next[index], next[target]] = [next[target], next[index]]
    update('images', next)
  }
  const addImages = event => {
    const selected = [...event.target.files].slice(0, Math.max(0, 8 - form.images.length))
    update('images', [...form.images, ...selected.map((file, index) => ({
      key: `new-${Date.now()}-${index}`, file, previewUrl: URL.createObjectURL(file), altText: '', caption: '',
      placement: 'AFTER_CONTENT', alignment: 'CENTER', displaySize: 'MEDIUM',
    }))])
    event.target.value = ''
  }
  return <form className="academic-form" onSubmit={event => { event.preventDefault(); onSubmit(form) }}>
    <label>{label}<input required maxLength={kind === 'subject' ? 120 : 160} value={form.name} onChange={event => update('name', event.target.value)} placeholder={kind === 'subject' ? 'Ví dụ: Toán 2' : kind === 'topic' ? 'Ví dụ: Phép cộng và phép trừ' : 'Ví dụ: Cộng có nhớ trong phạm vi 20'} /></label>
    {kind === 'lesson' && <>
      <label>Mô tả / nguồn <textarea maxLength={5000} rows="2" value={form.description} onChange={event => update('description', event.target.value)} placeholder="Ví dụ: Mục tiêu hoặc trang sách tham khảo" /></label>
      <LessonSectionsEditor sections={form.sections} onChange={sections => update('sections', sections)} />
      <small className="academic-form-hint">Học sinh sẽ xem từng mục nhỏ trong lộ trình và trang học lại chương trình.</small>
      <label className="academic-image-upload">Hình minh họa<input type="file" accept="image/jpeg,image/png" multiple disabled={form.images.length >= 8} onChange={addImages} /><small>JPEG hoặc PNG, tối đa 2 MB mỗi ảnh; có thể thêm tối đa 8 ảnh.</small></label>
      {form.images.length > 0 && <div className="academic-image-editor" aria-label="Chỉnh sửa hình minh họa">
        {form.images.map((image, index) => <section className="academic-image-row" key={image.key || `${image.url}-${index}`}>
          <img src={image.previewUrl || image.url} alt={image.altText || ''} />
          <div className="academic-image-fields">
            <label>Mô tả ảnh<input maxLength={200} value={image.altText || ''} onChange={event => updateImage(index, 'altText', event.target.value)} placeholder="Ví dụ: Các khối lập phương" /></label>
            <label>Chú thích<input maxLength={300} value={image.caption || ''} onChange={event => updateImage(index, 'caption', event.target.value)} placeholder="Chú thích hiển thị bên dưới ảnh" /></label>
            <div className="academic-image-options">
              <label>Vị trí trong bài<select value={image.placement} onChange={event => updateImage(index, 'placement', event.target.value)}><option value="BEFORE_CONTENT">Trước phần ôn tập</option><option value="AFTER_CONTENT">Sau phần ôn tập</option></select></label>
              <label>Căn ảnh<select value={image.alignment} onChange={event => updateImage(index, 'alignment', event.target.value)}><option value="LEFT">Bên trái</option><option value="CENTER">Ở giữa</option><option value="RIGHT">Bên phải</option></select></label>
              <label>Kích thước<select value={image.displaySize} onChange={event => updateImage(index, 'displaySize', event.target.value)}><option value="SMALL">Nhỏ</option><option value="MEDIUM">Vừa</option><option value="LARGE">Lớn</option></select></label>
            </div>
          </div>
          <div className="academic-image-row-actions">
            <button type="button" disabled={index === 0} onClick={() => moveImage(index, -1)} aria-label="Đưa ảnh lên trước">↑</button>
            <button type="button" disabled={index === form.images.length - 1} onClick={() => moveImage(index, 1)} aria-label="Đưa ảnh xuống sau">↓</button>
            <button className="academic-image-remove" type="button" onClick={() => update('images', form.images.filter((_, itemIndex) => itemIndex !== index))} aria-label="Xóa ảnh khỏi bài học">×</button>
          </div>
        </section>)}
      </div>}
    </>}
    <div className="academic-form-actions"><button className="academic-add" disabled={busy || !form.name.trim()} type="submit">{editing ? 'Lưu thay đổi' : '+ Thêm mới'}</button>{editing && <button className="academic-cancel" type="button" onClick={onCancel}>Hủy</button>}</div>
  </form>
}

function AcademicItem({ item, selected = false, meta, badge, onSelect, onEdit, onDelete, onCopy, onView }) {
  return <article className={`academic-item${selected ? ' is-selected' : ''}`}>
    <button type="button" className="academic-item-main" onClick={onSelect} disabled={!onSelect}><span className="academic-item-name">{item.name}{badge && <small className="academic-item-badge">{badge}</small>}</span><span className="academic-item-meta">{meta}</span></button>
    <div className="academic-item-actions">{onView && <Link className="academic-view-button" to={onView}>Xem</Link>}{onCopy && <button type="button" className="academic-copy-button" onClick={onCopy} aria-label={`Lưu bản riêng ${item.name}`} title="Lưu bản riêng"><span aria-hidden="true">＋</span><span>Lưu bản riêng</span></button>}{onEdit && <button type="button" onClick={onEdit} aria-label={`Sửa ${item.name}`} title="Sửa">✎</button>}{onDelete && <button type="button" className="is-delete" onClick={onDelete} aria-label={`Xóa ${item.name}`} title="Xóa">×</button>}</div>
  </article>
}

function EmptyState({ text }) { return <p className="academic-empty">{text}</p> }
