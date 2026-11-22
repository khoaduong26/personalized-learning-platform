import fixture from '../../../tests/fixtures/mock-data.json'

const MODE_KEY = 'edulearn.mock.enabled'
const ACTOR_KEY = 'edulearn.mock.actor'
const clone = value => value == null ? value : structuredClone(value)
const state = clone(fixture)
for (const exercise of state.generatedExercises) exercise.kind ??= 'MULTIPLE_CHOICE'
let nextId = 9900

function fail(status, message, code = 'MOCK_ERROR') {
  const error = new Error(message)
  error.status = status
  error.code = code
  error.fieldErrors = {}
  throw error
}

function actor() {
  const id = Number(window.sessionStorage.getItem(ACTOR_KEY))
  return state.actors.find(item => item.id === id) || null
}

function requireActor(roles = ['ADMIN', 'TEACHER', 'STUDENT']) {
  const current = actor()
  if (!current) fail(401, 'Hãy chọn một tài khoản demo để tiếp tục.', 'UNAUTHORIZED')
  if (!roles.includes(current.role)) fail(403, 'Tài khoản demo này không có quyền truy cập.', 'FORBIDDEN')
  return current
}

function withProfile(user) {
  return user && { ...user, avatarUrl: user.avatarUrl || '', phone: user.phone || '', address: user.address || '', city: user.city || '', passwordChangeAllowed: true }
}

function accountView({ id, fullName, email, phone = null, role, status, createdAt = null }) {
  return { id, fullName, email, phone, role, status, createdAt }
}

function manageAccounts(route, params, method, body) {
  const admin = requireActor(['ADMIN'])
  const [, , , , accountId, action] = route.split('/')
  if (!accountId && method === 'GET') {
    const keyword = (params.get('keyword') || '').toLowerCase()
    const role = params.get('role')
    const status = params.get('status')
    const page = Number(params.get('page') || 0)
    const size = Number(params.get('size') || 20)
    const matches = state.actors.filter(item => (
      (!role || item.role === role) && (!status || item.status === status)
      && [item.fullName, item.email].some(value => value.toLowerCase().includes(keyword))
    )).sort((first, second) => second.id - first.id)
    return { items: matches.slice(page * size, (page + 1) * size).map(accountView), page, size,
      totalElements: matches.length, totalPages: Math.ceil(matches.length / size) }
  }

  const account = accountId ? state.actors.find(item => item.id === Number(accountId)) : null
  if (accountId && !account) fail(404, 'Không tìm thấy tài khoản.', 'ACCOUNT_NOT_FOUND')
  if (account && !action && method === 'GET') return accountView(account)
  if (account && action === 'status' && method === 'PATCH') {
    if (account.id === admin.id && body.locked) fail(409, 'Bạn không thể tự khóa tài khoản.', 'SELF_LOCK')
    if (body.locked && account.status !== 'LOCKED') {
      account.statusBeforeLock = account.status
      account.status = 'LOCKED'
    } else if (!body.locked && account.status === 'LOCKED') {
      account.status = account.statusBeforeLock || 'ACTIVE'
      delete account.statusBeforeLock
    }
    return accountView(account)
  }
  if ((!accountId && method === 'POST') || (account && !action && method === 'PUT')) {
    const { fullName, phone, role } = body
    const email = body.email.trim().toLowerCase()
    if (state.actors.some(item => item.id !== account?.id && item.email === email)) {
      fail(409, 'Email đã được sử dụng.', 'EMAIL_EXISTS')
    }
    const fields = { fullName, email, phone, role }
    if (account) {
      if (account.role !== role) {
        if (account.id === admin.id) fail(409, 'Bạn không thể tự đổi vai trò quản trị.', 'SELF_ROLE_CHANGE')
        const assigned = state.classrooms.some(item => item.teacherId === account.id || item.studentIds.includes(account.id))
        if (assigned) fail(409, 'Không thể đổi vai trò khi tài khoản còn liên kết với lớp học.', 'ROLE_IN_USE')
      }
      Object.assign(account, fields)
      return accountView(account)
    }
    const created = { ...fields, id: nextId++, status: 'ACTIVE', createdAt: new Date().toISOString() }
    state.actors.push(created)
    return accountView(created)
  }
  fail(404, 'Không tìm thấy thao tác tài khoản.')
}

function lessonById(id) {
  return state.academic.lessons.find(item => item.id === Number(id))
}

function subjectView(subject) {
  return { ...subject, topicCount: state.academic.topics.filter(topic => topic.subjectId === subject.id).length }
}

function topicView(topic) {
  const subject = state.academic.subjects.find(item => item.id === topic.subjectId)
  return { ...topic, subjectName: subject?.name || '', lessonCount: state.academic.lessons.filter(item => item.topicId === topic.id).length }
}

function lessonView(lesson) {
  const topic = state.academic.topics.find(item => item.id === lesson.topicId)
  return { ...lesson, topicName: topic?.name || '' }
}

function questionView(question) {
  return {
    id: question.id,
    content: question.content,
    difficulty: question.difficulty,
    score: question.score,
    lessonId: question.lessonId,
    kind: question.kind,
    allowMultiple: Boolean(question.allowMultiple),
    options: (question.options || []).map(({ id, name, displayOrder }) => ({ id, name, displayOrder })),
    blankCount: question.blankCount || question.expectedAnswers?.length || 0,
  }
}

function assessmentView(assessment) {
  const classrooms = state.classrooms.filter(item => assessment.classroomIds.includes(item.id))
  const teacher = state.actors.find(item => item.id === assessment.teacherId)
  return {
    ...assessment,
    teacherName: teacher?.fullName || '',
    classroomNames: classrooms.map(item => item.name),
    questions: assessment.questions.map(questionView),
  }
}

function pathView(path) {
  const steps = path.steps.map(step => ({
    ...step,
    order: step.stepOrder,
    lessonDescription: lessonById(step.lessonId)?.description || '',
  }))
  const completed = steps.filter(step => step.status === 'COMPLETED').length
  return { ...path, steps, progress: { completed, total: steps.length, percentage: steps.length ? Math.round(completed * 100 / steps.length) : 0 } }
}

function teacherClassroomIds(user) {
  return user.role === 'ADMIN'
    ? state.classrooms.map(item => item.id)
    : state.classrooms.filter(item => item.teacherId === user.id).map(item => item.id)
}

function studentsFor(classroomId) {
  const classroom = state.classrooms.find(item => item.id === Number(classroomId))
  return state.students.filter(item => classroom?.studentIds.includes(item.id))
}

function attemptSummary(attempt) {
  const assessment = state.assessments.find(item => item.id === attempt.assessmentId)
  const exercise = state.generatedExercises.find(item => item.questionId === attempt.exerciseQuestionIds?.[0])
  const questions = attempt.assessmentId ? assessment?.questions || []
    : state.generatedExercises.filter(item => attempt.exerciseQuestionIds?.includes(item.questionId)).map(item => ({ ...item, id: item.questionId }))
  return {
    id: attempt.id,
    assessmentId: attempt.assessmentId ?? null,
    assessmentTitle: assessment?.title || (exercise ? 'Bài tập ôn luyện' : ''),
    learningStepId: attempt.learningStepId ?? null,
    studentId: attempt.studentId,
    studentName: state.actors.find(item => item.id === attempt.studentId)?.fullName || '',
    status: attempt.status,
    startedAt: attempt.startedAt,
    submittedAt: attempt.submittedAt || null,
    totalScore: attempt.status === 'SUBMITTED' ? attempt.totalScore : 0,
    maxScore: questions.reduce((sum, question) => sum + Number(question.score || 0), 0),
  }
}

function answerExpected(question) {
  if (question.kind === 'MULTIPLE_CHOICE') return question.options.filter(option => option.correct || question.correctOptionIds?.includes(option.id)).sort((a, b) => a.displayOrder - b.displayOrder).map(option => option.name)
  return question.expectedAnswers || (question.correctAnswer ? [question.correctAnswer] : [])
}

function assessmentQuestionById(assessment, questionId) {
  return assessment?.questions.find(item => item.id === Number(questionId))
}

function questionForAttempt(attempt, questionId) {
  if (attempt.assessmentId) return assessmentQuestionById(state.assessments.find(item => item.id === attempt.assessmentId), questionId)
  return state.generatedExercises.find(item => item.questionId === Number(questionId) && attempt.exerciseQuestionIds?.includes(item.questionId))
}

function answerDraftView(answer) {
  return { questionId: answer.questionId, selectedOptionIds: answer.selectedOptionIds || [], blankAnswers: answer.blankAnswers || [], content: answer.content ?? null }
}

function resultView(attempt) {
  const summary = attemptSummary(attempt)
  const questions = attempt.assessmentId
    ? state.assessments.find(item => item.id === attempt.assessmentId)?.questions || []
    : state.generatedExercises.filter(item => attempt.exerciseQuestionIds?.includes(item.questionId)).map(item => ({ ...item, id: item.questionId }))
  return {
    attempt: summary,
    answers: questions.map(question => {
      const answer = attempt.answers.find(item => item.questionId === question.id)
      const selected = (answer?.selectedOptionIds || []).map(id => question.options?.find(option => option.id === id)?.name).filter(Boolean)
      const submitted = question.kind === 'MULTIPLE_CHOICE' ? selected : question.kind === 'FILL_BLANK' ? answer?.blankAnswers || [] : answer?.content ? [answer.content] : []
      return {
        questionId: question.id,
        questionContent: question.content,
        kind: question.kind,
        correct: Boolean(answer?.correct),
        score: question.score,
        awardedScore: answer?.awardedScore || 0,
        submitted,
        expected: answerExpected(question),
        explanation: question.explanation || '',
      }
    }),
  }
}

function publishAttempt(attempt) {
  const questions = attempt.assessmentId
    ? state.assessments.find(item => item.id === attempt.assessmentId)?.questions || []
    : state.generatedExercises.filter(item => attempt.exerciseQuestionIds?.includes(item.questionId)).map(item => ({ ...item, id: item.questionId }))
  attempt.totalScore = 0
  for (const question of questions) {
    const answer = attempt.answers.find(item => item.questionId === question.id)
    const expected = question.kind === 'MULTIPLE_CHOICE'
      ? question.options.filter(option => option.correct || question.correctOptionIds?.includes(option.id)).map(option => option.id)
      : answerExpected(question).map(value => String(value).trim().toLocaleLowerCase())
    const actual = question.kind === 'MULTIPLE_CHOICE'
      ? answer?.selectedOptionIds || []
      : (question.kind === 'FILL_BLANK' ? answer?.blankAnswers || [] : answer?.content ? [answer.content] : []).map(value => String(value).trim().toLocaleLowerCase())
    const correct = expected.length > 0 && expected.length === actual.length && expected.every(value => actual.includes(value))
    if (answer) {
      answer.correct = correct
      answer.awardedScore = correct ? question.score : 0
    }
    attempt.totalScore += correct ? Number(question.score || 0) : 0
  }
  attempt.status = 'SUBMITTED'
  attempt.submittedAt = new Date().toISOString()
  return attempt
}

function mockQuestion(input) {
  const options = (input.options || []).map((option, index) => ({ ...option, id: nextId++, displayOrder: option.displayOrder ?? index }))
  return {
    ...input,
    id: nextId++,
    lessonId: Number(input.lessonId),
    options,
    blankCount: input.blanks?.length || 0,
    expectedAnswers: input.kind === 'FILL_BLANK' ? (input.blanks || []).map(item => item.correctAnswer) : input.correctAnswer ? [input.correctAnswer] : [],
    isAiGenerated: false,
    approvalStatus: null,
  }
}

function exerciseView(item) {
  return {
    id: item.id,
    questionId: item.questionId,
    learningStepId: item.learningStepId,
    lessonId: item.lessonId,
    content: item.content,
    difficulty: item.difficulty,
    score: item.score,
    kind: item.kind,
    allowMultiple: Boolean(item.allowMultiple),
    options: clone(item.options || []),
    blankCount: item.blankCount || 0,
    expectedAnswers: clone(item.expectedAnswers || []),
    explanation: item.explanation || '',
    isAiGenerated: true,
    approvalStatus: item.approvalStatus,
    status: item.approvalStatus,
  }
}

export async function mockRequest(path, { method = 'GET', body, signal } = {}) {
  if (signal?.aborted) throw new DOMException('Aborted', 'AbortError')
  const url = new URL(path, window.location.origin)
  const route = url.pathname
  const current = actor()

  if (route === '/api/auth/csrf' || route === '/api/csrf') return { headerName: 'X-CSRF-TOKEN', token: 'mock-csrf-token' }
  if (route === '/api/auth/options') return { googleEnabled: false, emailEnabled: true }
  if (route === '/api/auth/login' && method === 'POST') {
    const selected = state.actors.find(item => item.email.toLowerCase() === String(body?.email || '').trim().toLowerCase())
    if (!selected) fail(401, 'Không tìm thấy tài khoản demo.', 'INVALID_CREDENTIALS')
    if (selected.status === 'LOCKED') fail(403, 'Tài khoản đã bị khóa.', 'ACCOUNT_LOCKED')
    window.sessionStorage.setItem(MODE_KEY, '1')
    window.sessionStorage.setItem(ACTOR_KEY, String(selected.id))
    return withProfile(selected)
  }
  if (route === '/api/auth/logout' && method === 'POST') { window.sessionStorage.removeItem(ACTOR_KEY); return null }
  if (method === 'GET' && (route === '/api/auth/me' || route === '/api/auth/session' || route === '/api/auth/registration-session' || route === '/api/users/me')) {
    if (!current) fail(401, 'Hãy chọn một tài khoản demo để tiếp tục.', 'UNAUTHORIZED')
    return withProfile(current)
  }
  if (route.startsWith('/api/auth/register') || route.startsWith('/api/auth/verify-otp') || route.startsWith('/api/auth/resend-otp')) fail(400, 'Đăng ký/OTP chưa được mô phỏng trong chế độ demo.')
  if (!current) fail(401, 'Hãy chọn một tài khoản demo để tiếp tục.', 'UNAUTHORIZED')

  if (/^\/api\/admin\/accounts(?:\/\d+(?:\/status)?)?$/.test(route)) {
    return manageAccounts(route, url.searchParams, method, body)
  }

  if (route === '/api/users/me' && method === 'PUT') {
    Object.assign(current, body)
    return withProfile(current)
  }
  if (route === '/api/users/me/password' && method === 'PUT') return null
  if (route === '/api/users/me/avatar' && method === 'POST') {
    current.avatarUrl = '/math-bunny.svg'
    return { avatarUrl: current.avatarUrl }
  }

  if (route === '/api/classrooms/teachers') {
    requireActor(['ADMIN'])
    return state.actors.filter(item => item.role === 'TEACHER').map(({ id, fullName, email }) => ({ id, fullName, email }))
  }
  if (route === '/api/classrooms' && method === 'GET') {
    requireActor(['ADMIN', 'TEACHER'])
    return state.classrooms.filter(item => teacherClassroomIds(current).includes(item.id)).map(item => ({ ...item, studentCount: item.studentIds.length }))
  }
  if (route === '/api/classrooms' && method === 'POST') {
    const user = requireActor(['ADMIN', 'TEACHER'])
    const teacherId = user.role === 'ADMIN' ? Number(body.teacherId) : user.id
    const newClass = { id: nextId++, name: body.name, schoolYear: body.schoolYear, teacherId, teacherName: state.actors.find(item => item.id === teacherId)?.fullName || user.fullName, studentIds: [], studentCount: 0 }
    state.classrooms.push(newClass)
    return clone(newClass)
  }
  let match = route.match(/^\/api\/classrooms\/(\d+)$/)
  if (match && method === 'PUT') {
    const user = requireActor(['ADMIN', 'TEACHER'])
    const classroom = state.classrooms.find(item => item.id === Number(match[1]) && teacherClassroomIds(user).includes(item.id))
    if (!classroom) fail(404, 'Không tìm thấy lớp học.')
    Object.assign(classroom, body)
    return { ...classroom, studentCount: classroom.studentIds.length }
  }
  match = route.match(/^\/api\/classrooms\/(\d+)\/students$/)
  if (match && method === 'GET') {
    const user = requireActor(['ADMIN', 'TEACHER'])
    if (!teacherClassroomIds(user).includes(Number(match[1]))) fail(403, 'Bạn không có quyền xem lớp này.')
    const needle = (url.searchParams.get('search') || '').toLocaleLowerCase()
    return studentsFor(match[1]).filter(item => `${item.fullName} ${item.email}`.toLocaleLowerCase().includes(needle))
  }
  if (match && method === 'POST') {
    const user = requireActor(['ADMIN', 'TEACHER'])
    const classroom = state.classrooms.find(item => item.id === Number(match[1]) && teacherClassroomIds(user).includes(item.id))
    const student = state.students.find(item => item.id === Number(body.studentId))
    if (!classroom || !student) fail(404, 'Không tìm thấy lớp hoặc học sinh.')
    if (!classroom.studentIds.includes(student.id)) classroom.studentIds.push(student.id)
    student.classroomId = classroom.id
    return clone(student)
  }
  match = route.match(/^\/api\/classrooms\/(\d+)\/students\/(\d+)$/)
  if (match && method === 'DELETE') {
    requireActor(['ADMIN', 'TEACHER'])
    const classroom = state.classrooms.find(item => item.id === Number(match[1]))
    if (classroom) classroom.studentIds = classroom.studentIds.filter(id => id !== Number(match[2]))
    const student = state.students.find(item => item.id === Number(match[2]))
    if (student) student.classroomId = null
    return null
  }
  if (route === '/api/students/candidates') {
    requireActor(['ADMIN', 'TEACHER'])
    const email = (url.searchParams.get('email') || '').toLocaleLowerCase()
    return state.students.filter(item => item.email.toLocaleLowerCase() === email && item.classroomId == null)
  }
  match = route.match(/^\/api\/students\/(\d+)$/)
  if (match && method === 'PATCH') {
    requireActor(['ADMIN', 'TEACHER'])
    const student = state.students.find(item => item.id === Number(match[1]))
    if (!student) fail(404, 'Không tìm thấy học sinh.')
    Object.assign(student, body)
    return clone(student)
  }

  if (route === '/api/subjects') {
    requireActor(['ADMIN', 'TEACHER'])
    if (method === 'GET') return state.academic.subjects.map(subjectView)
    const item = { id: nextId++, name: body.name }
    state.academic.subjects.push(item)
    return subjectView(item)
  }
  match = route.match(/^\/api\/subjects\/(\d+)$/)
  if (match) {
    requireActor(['ADMIN', 'TEACHER'])
    const item = state.academic.subjects.find(value => value.id === Number(match[1]))
    if (!item) fail(404, 'Không tìm thấy môn học.')
    if (method === 'PUT') { item.name = body.name; return subjectView(item) }
    if (method === 'DELETE') { state.academic.subjects = state.academic.subjects.filter(value => value.id !== item.id); state.academic.topics = state.academic.topics.filter(value => value.subjectId !== item.id); return null }
  }
  if (route === '/api/topics') {
    requireActor(['ADMIN', 'TEACHER'])
    if (method === 'GET') return state.academic.topics.filter(item => item.subjectId === Number(url.searchParams.get('subjectId'))).map(topicView)
    const item = { id: nextId++, name: body.name, subjectId: Number(body.subjectId) }
    state.academic.topics.push(item)
    return topicView(item)
  }
  match = route.match(/^\/api\/topics\/(\d+)$/)
  if (match) {
    requireActor(['ADMIN', 'TEACHER'])
    const item = state.academic.topics.find(value => value.id === Number(match[1]))
    if (!item) fail(404, 'Không tìm thấy chủ đề.')
    if (method === 'PUT') { Object.assign(item, body, { subjectId: Number(body.subjectId) }); return topicView(item) }
    if (method === 'DELETE') { state.academic.topics = state.academic.topics.filter(value => value.id !== item.id); state.academic.lessons = state.academic.lessons.filter(value => value.topicId !== item.id); return null }
  }
  if (route === '/api/lessons') {
    requireActor(['ADMIN', 'TEACHER'])
    if (method === 'GET') return state.academic.lessons.filter(item => item.topicId === Number(url.searchParams.get('topicId'))).map(lessonView)
    const item = { id: nextId++, name: body.name, description: body.description || '', topicId: Number(body.topicId), subjectId: state.academic.topics.find(topic => topic.id === Number(body.topicId))?.subjectId }
    state.academic.lessons.push(item)
    return lessonView(item)
  }
  match = route.match(/^\/api\/lessons\/(\d+)$/)
  if (match) {
    requireActor(['ADMIN', 'TEACHER'])
    const item = state.academic.lessons.find(value => value.id === Number(match[1]))
    if (!item) fail(404, 'Không tìm thấy bài học.')
    if (method === 'PUT') { Object.assign(item, body, { topicId: Number(body.topicId) }); return lessonView(item) }
    if (method === 'DELETE') { state.academic.lessons = state.academic.lessons.filter(value => value.id !== item.id); return null }
  }

  if (route === '/api/assessments/classrooms') {
    const user = requireActor(['ADMIN', 'TEACHER'])
    return state.classrooms.filter(item => teacherClassroomIds(user).includes(item.id)).map(({ id, name, schoolYear }) => ({ id, name, schoolYear }))
  }
  if (route === '/api/assessments/questions' && method === 'GET') {
    requireActor(['ADMIN', 'TEACHER'])
    return state.assessments.flatMap(item => item.questions).filter(question => question.lessonId === Number(url.searchParams.get('lessonId'))).map(questionView)
  }
  if (route === '/api/assessments/questions' && method === 'POST') {
    requireActor(['ADMIN', 'TEACHER'])
    const item = mockQuestion(body)
    const target = state.assessments[0]
    if (target) target.questions.push(item)
    return questionView(item)
  }
  if (route === '/api/assessments' && method === 'GET') {
    const user = requireActor()
    const allowed = user.role === 'STUDENT'
      ? state.assessments.filter(item => item.published && item.classroomIds.includes(user.classroomId))
      : state.assessments.filter(item => user.role === 'ADMIN' || item.teacherId === user.id)
    return allowed.map(assessmentView)
  }
  if (route === '/api/assessments' && method === 'POST') {
    const user = requireActor(['ADMIN', 'TEACHER'])
    const item = { id: nextId++, title: body.title, type: body.type, durationMinutes: body.durationMinutes, published: false, teacherId: user.id, createdAt: new Date().toISOString(), classroomIds: body.classroomIds || [], questions: (body.questionIds || []).flatMap(id => state.assessments.flatMap(test => test.questions).filter(question => question.id === id)) }
    state.assessments.unshift(item)
    return assessmentView(item)
  }
  match = route.match(/^\/api\/assessments\/(\d+)(?:\/(publish))?$/)
  if (match) {
    const user = requireActor()
    const item = state.assessments.find(value => value.id === Number(match[1]))
    if (!item) fail(404, 'Không tìm thấy bài kiểm tra.')
    if (user.role === 'STUDENT' && (!item.published || !item.classroomIds.includes(user.classroomId))) fail(403, 'Bài kiểm tra này chưa được giao cho em.')
    if (user.role === 'TEACHER' && item.teacherId !== user.id) fail(403, 'Bạn không có quyền với bài kiểm tra này.')
    if (method === 'POST' && match[2] === 'publish') { item.published = true; return assessmentView(item) }
    if (method === 'GET') return assessmentView(item)
  }

  match = route.match(/^\/api\/students\/(\d+)\/mastery(?:\/(errors|gaps))?$/)
  if (match) {
    const studentId = Number(match[1])
    if (current.role === 'TEACHER' && !state.classrooms.some(item => item.teacherId === current.id && item.studentIds.includes(studentId))) fail(403, 'Bạn không có quyền xem hồ sơ này.')
    if (!['ADMIN', 'TEACHER'].includes(current.role) && current.id !== studentId) fail(403, 'Bạn không có quyền xem hồ sơ này.')
    if (match[2] === 'errors') return state.studentErrors.filter(item => item.studentId === studentId).map(item => ({ ...item, lessonName: lessonById(item.lessonId)?.name || '' }))
    if (match[2] === 'gaps') return state.knowledgeGaps.filter(item => item.studentId === studentId).map(item => ({ ...item, lessonName: lessonById(item.lessonId)?.name || '' }))
    return state.masteryProfiles.find(item => item.studentId === studentId)?.records || []
  }
  match = route.match(/^\/api\/mastery\/classrooms\/(\d+)\/dashboard$/)
  if (match) return state.masteryProfiles.filter(profile => studentsFor(match[1]).some(student => student.id === profile.studentId)).flatMap(profile => profile.records)
  match = route.match(/^\/api\/mastery\/(\d+)\/history$/)
  if (match) return state.masteryHistory.filter(item => item.masteryId === Number(match[1]))

  match = route.match(/^\/api\/students\/(\d+)\/learning-paths(?:\/(\d+)\/steps\/(\d+))?(?:\/(generate))?$/)
  if (match) {
    const studentId = Number(match[1])
    if (current.role === 'STUDENT' && current.id !== studentId) fail(403, 'Bạn chỉ có thể xem lộ trình của mình.')
    if (current.role === 'TEACHER' && !state.classrooms.some(item => item.teacherId === current.id && item.studentIds.includes(studentId))) fail(403, 'Bạn không có quyền xem lộ trình này.')
    if (match[4] && method === 'POST') {
      requireActor(['ADMIN', 'TEACHER'])
      const gaps = state.knowledgeGaps.filter(item => item.studentId === studentId && item.status !== 'RESOLVED')
      if (!gaps.length) fail(409, 'Học sinh hiện không có khoảng trống cần ôn.')
      const pathItem = { id: nextId++, studentId, status: 'ACTIVE', version: 1, createdAt: new Date().toISOString(), updatedAt: new Date().toISOString(), steps: gaps.map((gap, index) => ({ id: nextId++, stepOrder: index + 1, status: index === 0 ? 'IN_PROGRESS' : 'LOCKED', targetScore: 70, lessonId: gap.lessonId, lessonName: lessonById(gap.lessonId)?.name || '' })) }
      state.learningPaths.unshift(pathItem)
      gaps.forEach(item => { item.status = 'IN_REMEDIATION' })
      return pathView(pathItem)
    }
    if (match[2] && match[3] && method === 'PATCH') {
      const pathItem = state.learningPaths.find(item => item.id === Number(match[2]) && item.studentId === studentId)
      const step = pathItem?.steps.find(item => item.id === Number(match[3]))
      if (!step || step.status !== 'IN_PROGRESS' || body.status !== 'COMPLETED') fail(409, 'Bước học này chưa thể hoàn thành.')
      step.status = 'COMPLETED'
      const nextStep = pathItem.steps.find(item => item.stepOrder === step.stepOrder + 1)
      if (nextStep) nextStep.status = 'IN_PROGRESS'
      else pathItem.status = 'COMPLETED'
      pathItem.updatedAt = new Date().toISOString()
      return pathView(pathItem)
    }
    return state.learningPaths.filter(item => item.studentId === studentId).map(pathView)
  }

  if (route === '/api/exercises/pending-review') {
    requireActor(['ADMIN', 'TEACHER'])
    return state.generatedExercises.filter(item => item.approvalStatus === 'PENDING').map(exerciseView)
  }
  match = route.match(/^\/api\/exercises\/steps\/(\d+)$/)
  if (match) {
    requireActor(['ADMIN', 'TEACHER', 'STUDENT'])
    const approvedOnly = current.role === 'STUDENT'
    return state.generatedExercises
      .filter(item => item.learningStepId === Number(match[1]) && (!approvedOnly || item.approvalStatus === 'APPROVED'))
      .map(item => approvedOnly ? questionView({ ...item, id: item.questionId }) : exerciseView(item))
  }
  match = route.match(/^\/api\/exercises\/questions\/(\d+)(?:\/(review))?$/)
  if (match) {
    requireActor(['ADMIN', 'TEACHER'])
    const item = state.generatedExercises.find(value => value.questionId === Number(match[1]))
    if (!item) fail(404, 'Không tìm thấy bài tập.')
    if (match[2] && method === 'PATCH') { item.approvalStatus = body.status; return exerciseView(item) }
    if (method === 'PATCH') { Object.assign(item, body); return exerciseView(item) }
  }

  match = route.match(/^\/api\/(?:assessments\/(\d+)\/attempts|exercises\/steps\/(\d+)\/attempts)$/)
  if (match && method === 'POST') {
    const user = requireActor(['STUDENT'])
    const isExercise = Boolean(match[2])
    const exerciseIds = isExercise ? state.generatedExercises.filter(item => item.learningStepId === Number(match[2]) && item.approvalStatus === 'APPROVED').map(item => item.questionId) : []
    const assessmentId = isExercise ? null : Number(match[1])
    const assessment = state.assessments.find(item => item.id === assessmentId)
    if (!isExercise && (!assessment?.published || !assessment.classroomIds.includes(user.classroomId))) fail(403, 'Bài kiểm tra chưa được giao cho em.')
    const created = { id: nextId++, assessmentId, learningStepId: isExercise ? Number(match[2]) : null, exerciseQuestionIds: isExercise ? exerciseIds : [], studentId: user.id, status: 'IN_PROGRESS', startedAt: new Date().toISOString(), submittedAt: null, totalScore: 0, answers: [] }
    state.attempts.unshift(created)
    return attemptSummary(created)
  }
  match = route.match(/^\/api\/attempts\/student\/(\d+)$/)
  if (match) {
    const studentId = Number(match[1])
    if (current.role === 'STUDENT' && current.id !== studentId) fail(403, 'Bạn không có quyền xem lịch sử này.')
    return state.attempts.filter(item => item.studentId === studentId).map(attemptSummary)
  }
  match = route.match(/^\/api\/attempts\/(\d+)(?:\/(draft|submit|result))?(\/answers)?$/)
  if (match) {
    const attempt = state.attempts.find(item => item.id === Number(match[1]))
    if (!attempt) fail(404, 'Không tìm thấy lượt làm bài.')
    if (current.role === 'STUDENT' && current.id !== attempt.studentId) fail(403, 'Bạn không có quyền xem lượt làm này.')
    if (match[2] === 'draft' && method === 'GET') return attempt.answers.map(answerDraftView)
    if (match[2] === 'result' && method === 'GET') {
      if (attempt.status !== 'SUBMITTED') fail(409, 'Kết quả sẽ hiển thị sau khi nộp bài.')
      return resultView(attempt)
    }
    if (match[2] === 'submit' && method === 'POST') {
      if (attempt.status !== 'IN_PROGRESS') fail(409, 'Bài làm đã được nộp.')
      publishAttempt(attempt)
      return attemptSummary(attempt)
    }
    if (match[3] && method === 'PUT') {
      if (attempt.status !== 'IN_PROGRESS') fail(409, 'Bài làm đã được nộp.')
      const question = questionForAttempt(attempt, body.questionId)
      if (!question) fail(400, 'Câu hỏi không thuộc lượt làm này.')
      const draft = { questionId: Number(body.questionId), selectedOptionIds: body.selectedOptionIds || [], blankAnswers: body.blankAnswers || [], content: body.content ?? null, correct: false, awardedScore: null }
      const index = attempt.answers.findIndex(item => item.questionId === draft.questionId)
      if (index < 0) attempt.answers.push(draft); else attempt.answers[index] = draft
      return { status: 'SAVED' }
    }
    if (method === 'GET') return attemptSummary(attempt)
  }

  fail(404, `Mock API chưa hỗ trợ ${method} ${route}.`)
}
