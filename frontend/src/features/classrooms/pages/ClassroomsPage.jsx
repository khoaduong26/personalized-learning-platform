import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  createClassroom, enrollStudent, findStudentCandidate, getClassroomSession, getClassroomStudents,
  getClassroomTeachers, getClassrooms, removeStudent, updateClassroom, updateStudent,
} from '../services/classroomApi'
import '../styles/classrooms.css'
import MasteryPanel from '../../mastery/components/MasteryPanel'

function currentSchoolYear() {
  const today = new Date()
  const start = today.getMonth() >= 7 ? today.getFullYear() : today.getFullYear() - 1
  return `${start}-${start + 1}`
}

export default function ClassroomsPage() {
  const [user, setUser] = useState(null)
  const [access, setAccess] = useState('loading')
  const [classes, setClasses] = useState([])
  const [teachers, setTeachers] = useState([])
  const [selectedId, setSelectedId] = useState(null)
  const [students, setStudents] = useState([])
  const [classForm, setClassForm] = useState({ name: '', schoolYear: currentSchoolYear(), teacherId: '' })
  const [editingClass, setEditingClass] = useState(false)
  const [studentSearch, setStudentSearch] = useState('')
  const [candidateEmail, setCandidateEmail] = useState('')
  const [candidates, setCandidates] = useState([])
  const [notice, setNotice] = useState('')
  const [busy, setBusy] = useState(false)

  const loadClasses = useCallback(async preferredId => {
    const items = await getClassrooms()
    setClasses(items)
    setSelectedId(current => items.some(item => item.id === (preferredId ?? current))
      ? (preferredId ?? current)
      : (items[0]?.id ?? null))
  }, [])

  useEffect(() => {
    document.title = 'Lớp học và học sinh · TLCN'
    let active = true
    getClassroomSession().then(async session => {
      if (!active) return
      setUser(session)
      if (!['ADMIN', 'TEACHER'].includes(session?.role)) { setAccess('forbidden'); return }
      setAccess('allowed')
      try {
        await loadClasses()
        if (session.role === 'ADMIN') setTeachers(await getClassroomTeachers())
      } catch (error) { if (active) setNotice(error.message) }
    }).catch(error => { if (active) setAccess(error.status === 403 ? 'forbidden' : 'signed-out') })
    return () => { active = false }
  }, [loadClasses])

  const refreshStudents = useCallback(async () => {
    if (!selectedId) { setStudents([]); return }
    setStudents(await getClassroomStudents(selectedId, studentSearch))
  }, [selectedId, studentSearch])

  useEffect(() => {
    if (access !== 'allowed') return
    refreshStudents().catch(error => setNotice(error.message))
  }, [access, refreshStudents])

  const mutate = async (action, message, preferredClassId) => {
    setBusy(true); setNotice('')
    try { const result = await action(); await loadClasses(preferredClassId ?? result?.id); setNotice(message) }
    catch (error) { setNotice(error.message) }
    finally { setBusy(false) }
  }

  const saveClass = event => {
    event.preventDefault()
    const body = { name: classForm.name, schoolYear: classForm.schoolYear,
      ...(user?.role === 'ADMIN' ? { teacherId: Number(classForm.teacherId) } : {}) }
    mutate(async () => {
      const result = editingClass && selectedId ? await updateClassroom(selectedId, body) : await createClassroom(body)
      setClassForm({ name: '', schoolYear: currentSchoolYear(), teacherId: '' })
      setEditingClass(false)
      return result
    }, editingClass ? 'Đã cập nhật thông tin lớp.' : 'Đã tạo lớp học.', editingClass ? selectedId : undefined)
  }

  const editClass = item => {
    setSelectedId(item.id)
    setClassForm({ name: item.name, schoolYear: item.schoolYear, teacherId: item.teacherId || '' })
    setEditingClass(true)
  }

  const searchCandidate = async event => {
    event.preventDefault(); setNotice(''); setCandidates([])
    try { setCandidates(await findStudentCandidate(candidateEmail.trim())) }
    catch (error) { setNotice(error.message) }
  }

  const addStudent = student => mutate(async () => {
    await enrollStudent(selectedId, student.id)
    setCandidates(current => current.filter(item => item.id !== student.id))
    setCandidateEmail('')
    await refreshStudents()
  }, `Đã thêm ${student.fullName} vào lớp.`, selectedId)

  const dropStudent = student => {
    if (!window.confirm(`Gỡ ${student.fullName} khỏi lớp?`)) return
    mutate(async () => { await removeStudent(selectedId, student.id); await refreshStudents() }, 'Đã gỡ học sinh khỏi lớp.', selectedId)
  }

  const saveStudent = async (studentId, body) => {
    setBusy(true)
    try {
      await updateStudent(studentId, body)
      await refreshStudents()
      setNotice('Đã cập nhật hồ sơ học sinh.')
    } catch (error) { setNotice(error.message); throw error }
    finally { setBusy(false) }
  }

  if (access === 'loading') return <main className="classrooms-shell"><p className="classrooms-state">Đang kiểm tra phiên đăng nhập…</p></main>
  if (access === 'signed-out') return <Gate title="Đăng nhập để quản lý lớp học" description="Trang này dành cho tài khoản giáo viên hoặc quản trị viên." />
  if (access === 'forbidden') return (
    <Gate
      title="Trang này dành cho giáo viên và quản trị viên"
      description={`${user?.fullName ? `Xin chào ${user.fullName}, ` : ''}tài khoản hiện tại không thể quản lý lớp học.`}
    />
  )

  
  const selected = classes.find(item => item.id === selectedId)

  return <main className="classrooms-shell">
    <header className="classrooms-topbar"><Link to="/" className="classrooms-brand"><span>✳</span> TLCN</Link><div className="classrooms-user"><span>{user?.fullName}</span><span className="classrooms-role">{user?.role === 'ADMIN' ? 'Quản trị viên' : 'Giáo viên'}</span><Link to="/">Trang chủ</Link></div></header>
    <section className="classrooms-heading"><div><p className="classrooms-kicker">GÓC DÀNH CHO THẦY CÔ</p><h1>Lớp học và học sinh</h1><p>Tổ chức lớp, thêm học sinh và cập nhật hồ sơ trong phạm vi được phân quyền.</p></div><span aria-hidden="true">♧</span></section>
    {notice && <div className="classrooms-notice" role="status">{notice}<button type="button" onClick={() => setNotice('')} aria-label="Đóng">×</button></div>}

    <div className="classrooms-grid">
      <aside className="classrooms-panel classrooms-list-panel">
        <div className="classrooms-panel-heading"><div><h2>{user?.role === 'ADMIN' ? 'Danh sách lớp' : 'Lớp của tôi'}</h2><p>{classes.length} lớp học</p></div><span>{String(classes.length).padStart(2, '0')}</span></div>
        <form className="classrooms-form" onSubmit={saveClass}>
          <div className="classrooms-form-title">{editingClass ? 'Chỉnh sửa lớp' : 'Tạo lớp mới'}</div>
          <label>Tên lớp<input required maxLength="100" value={classForm.name} onChange={event => setClassForm({ ...classForm, name: event.target.value })} placeholder="Ví dụ: Lớp 2A1" /></label>
          <label>Năm học<input required pattern="\d{4}-\d{4}" title="Nhập theo dạng 2026-2027" value={classForm.schoolYear} onChange={event => setClassForm({ ...classForm, schoolYear: event.target.value })} placeholder="2026-2027" /></label>
          {user?.role === 'ADMIN' && !editingClass && <label>Giáo viên phụ trách<select required value={classForm.teacherId} onChange={event => setClassForm({ ...classForm, teacherId: event.target.value })}><option value="">Chọn giáo viên</option>{teachers.map(teacher => <option key={teacher.id} value={teacher.id}>{teacher.fullName} · {teacher.email}</option>)}</select></label>}
          <div className="classrooms-form-actions"><button className="classrooms-primary" disabled={busy}>{editingClass ? 'Lưu thay đổi' : '+ Tạo lớp'}</button>{editingClass && <button type="button" className="classrooms-secondary" onClick={() => { setEditingClass(false); setClassForm({ name: '', schoolYear: currentSchoolYear(), teacherId: '' }) }}>Hủy</button>}</div>
        </form>
        <div className="classrooms-class-list">{classes.map(item => <button type="button" key={item.id} className={`classrooms-class-card${selectedId === item.id ? ' is-active' : ''}`} onClick={() => { setSelectedId(item.id); setEditingClass(false); setClassForm({ name: '', schoolYear: currentSchoolYear(), teacherId: '' }) }}><span className="classrooms-class-icon">▦</span><span className="classrooms-class-copy"><strong>{item.name}</strong><small>{item.schoolYear} · {item.studentCount} học sinh</small></span><span aria-hidden="true">›</span></button>)}</div>
        {!classes.length && <p className="classrooms-empty">Chưa có lớp học. Tạo lớp đầu tiên để bắt đầu.</p>}
      </aside>

      <section className="classrooms-panel classrooms-roster-panel">
        {selected ? <>
          <div className="classrooms-panel-heading roster-heading"><div><span className="classrooms-kicker">{selected.schoolYear}</span><h2>{selected.name}</h2><p>Giáo viên: {selected.teacherName} · {selected.studentCount} học sinh</p></div><button type="button" className="classrooms-edit-class" onClick={() => editClass(selected)}>✎ <span>Sửa lớp</span></button></div>
          <section className="classrooms-add-student"><div><h3>Thêm học sinh</h3><p>Tra cứu bằng email đã đăng ký tài khoản TLCN.</p></div><form onSubmit={searchCandidate}><label className="classrooms-sr-only" htmlFor="student-email">Email học sinh</label><input id="student-email" type="email" required value={candidateEmail} onChange={event => setCandidateEmail(event.target.value)} placeholder="Email học sinh" /><button className="classrooms-secondary">Tìm học sinh</button></form>
            {candidates.map(student => <div key={student.id} className="classrooms-candidate"><div><strong>{student.fullName}</strong><span>{student.email}</span></div><button type="button" disabled={busy} onClick={() => addStudent(student)}>+ Thêm vào lớp</button></div>)}
            {candidateEmail && !candidates.length && <span className="classrooms-search-hint">Tìm đúng email của học sinh chưa được xếp lớp.</span>}
          </section>
          <div className="classrooms-roster-toolbar"><div><h3>Danh sách học sinh</h3><span>{students.length} em</span></div><form onSubmit={event => { event.preventDefault(); refreshStudents().catch(error => setNotice(error.message)) }}><input value={studentSearch} onChange={event => setStudentSearch(event.target.value)} placeholder="Tìm theo tên hoặc email" aria-label="Tìm học sinh trong lớp" /><button className="classrooms-secondary">Tìm</button></form></div>
          {students.length ? <div className="classrooms-students">{students.map(student => <StudentRow key={student.id} student={student} busy={busy} onSave={saveStudent} onRemove={() => dropStudent(student)} />)}</div> : <p className="classrooms-empty classrooms-roster-empty">Lớp chưa có học sinh. Tra cứu email ở phía trên để thêm thành viên.</p>}
          <MasteryPanel students={students} />
        </> : <div className="classrooms-no-selection"><span>▦</span><h2>Chọn một lớp học</h2><p>Tạo lớp mới hoặc chọn lớp ở cột bên trái để quản lý danh sách học sinh.</p></div>}
      </section>
    </div>
    <footer className="classrooms-footer">Hồ sơ học sinh chỉ hiển thị cho giáo viên phụ trách lớp và quản trị viên có quyền.</footer>
  </main>
}

function StudentRow({ student, busy, onSave, onRemove }) {
  const [editing, setEditing] = useState(false)
  const [form, setForm] = useState({ fullName: student.fullName, sex: student.sex || '', dateOfBirth: student.dateOfBirth || '' })
  const submit = async event => {
    event.preventDefault()
    try { await onSave(student.id, { ...form, dateOfBirth: form.dateOfBirth || null }); setEditing(false) }
    catch { /* Parent displays the request error. */ }
  }
  return <article className="classrooms-student-row"><div className="classrooms-avatar" aria-hidden="true">{student.fullName.trim().charAt(0).toUpperCase()}</div>
    {editing ? <form className="classrooms-student-edit" onSubmit={submit}><input aria-label="Họ và tên" required maxLength="120" value={form.fullName} onChange={event => setForm({ ...form, fullName: event.target.value })} /><div><input aria-label="Giới tính" maxLength="30" value={form.sex} onChange={event => setForm({ ...form, sex: event.target.value })} placeholder="Giới tính" /><input aria-label="Ngày sinh" type="date" value={form.dateOfBirth} onChange={event => setForm({ ...form, dateOfBirth: event.target.value })} /></div><button className="classrooms-primary" disabled={busy}>Lưu hồ sơ</button><button type="button" className="classrooms-secondary" onClick={() => setEditing(false)}>Hủy</button></form>
      : <><div className="classrooms-student-copy"><strong>{student.fullName}</strong><span>{student.email}</span><small>{student.dateOfBirth || 'Chưa có ngày sinh'}{student.sex ? ` · ${student.sex}` : ''}</small></div><div className="classrooms-student-actions"><button type="button" onClick={() => { setForm({ fullName: student.fullName, sex: student.sex || '', dateOfBirth: student.dateOfBirth || '' }); setEditing(true) }}>Sửa hồ sơ</button><button type="button" className="classrooms-remove" onClick={onRemove}>Gỡ khỏi lớp</button></div></>}
  </article>
}

function Gate({ title, description }) {
  return <main className="classrooms-shell"><div className="classrooms-gate"><span className="classrooms-kicker">KHU VỰC GIÁO VIÊN</span><h1>{title}</h1><p>{description}</p><Link className="classrooms-primary" to="/login">Đi đến đăng nhập</Link><Link className="classrooms-home-link" to="/">Về trang chủ</Link></div></main>
}
