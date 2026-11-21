import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const fixture = JSON.parse(await readFile(new URL('./fixtures/mock-data.json', import.meta.url), 'utf8'))
const byId = records => new Map(records.map(record => [record.id, record]))

test('mock actors and classrooms cover teacher, enrolled students, and an unassigned student', () => {
  const actors = byId(fixture.actors)
  const classrooms = byId(fixture.classrooms)

  assert.equal(actors.get(9101).role, 'TEACHER')
  assert.equal(actors.get(9105).role, 'ADMIN')
  assert.equal(classrooms.get(9201).teacherId, 9101)
  assert.deepEqual(classrooms.get(9201).studentIds, [9102, 9103])
  assert.equal(actors.get(9104).classroomId, null)
})

test('academic and assessment fixtures reference existing lessons and classroom records', () => {
  const lessons = byId(fixture.academic.lessons)
  const classrooms = byId(fixture.classrooms)
  const topics = byId(fixture.academic.topics)

  for (const lesson of fixture.academic.lessons) {
    assert.ok(topics.has(lesson.topicId), `Missing topic ${lesson.topicId}`)
  }

  for (const assessment of fixture.assessments) {
    for (const classroomId of assessment.classroomIds) assert.ok(classrooms.has(classroomId))
    for (const question of assessment.questions) {
      assert.ok(lessons.has(question.lessonId), `Missing lesson ${question.lessonId}`)
      if (question.kind === 'MULTIPLE_CHOICE') {
        assert.ok(question.options.length >= 2)
        assert.equal(question.correctOptionIds.length, 1)
        assert.ok(question.options.some(option => question.correctOptionIds.includes(option.id)))
      }
    }
  }

  assert.equal(fixture.assessments.find(item => item.id === 9401).published, true)
  assert.equal(fixture.assessments.find(item => item.id === 9402).published, false)
})

test('attempt fixtures include a graded submission and a resumable draft', () => {
  const assessments = byId(fixture.assessments)
  const actors = byId(fixture.actors)

  for (const attempt of fixture.attempts) {
    assert.ok(assessments.has(attempt.assessmentId))
    assert.equal(actors.get(attempt.studentId).role, 'STUDENT')
    assert.ok(attempt.answers.every(answer => assessments.get(attempt.assessmentId)
      .questions.some(question => question.id === answer.questionId)))
  }

  const submitted = fixture.attempts.find(item => item.status === 'SUBMITTED')
  assert.equal(submitted.answers.reduce((sum, answer) => sum + answer.awardedScore, 0), submitted.totalScore)
  const draft = fixture.attempts.find(item => item.status === 'IN_PROGRESS')
  assert.equal(draft.submittedAt, null)
  assert.equal(draft.totalScore, null)
  assert.ok(draft.answers.every(answer => answer.awardedScore === null))
})

test('mastery, history, errors, and gaps form consistent student learning profiles', () => {
  const lessons = byId(fixture.academic.lessons)
  const mastery = new Map(fixture.masteryProfiles.flatMap(profile =>
    profile.records.map(record => [record.id, { ...record, studentId: profile.studentId }]),
  ))
  const levelFor = score => score >= 90 ? 'MASTERED'
    : score >= 75 ? 'PROFICIENT'
      : score >= 50 ? 'DEVELOPING'
        : score > 0 ? 'BEGINNING' : 'NOT_STARTED'

  for (const record of mastery.values()) {
    assert.ok(lessons.has(record.lessonId))
    assert.equal(record.level, levelFor(record.score))
    assert.ok(record.confidence >= 0 && record.confidence <= 1)
  }

  for (const item of fixture.masteryHistory) assert.ok(mastery.has(item.masteryId))
  for (const error of fixture.studentErrors) {
    assert.ok(lessons.has(error.lessonId))
    assert.ok(fixture.attempts.some(attempt => attempt.studentId === error.studentId
      && attempt.answers.some(answer => answer.id === error.answerId && !answer.correct)))
  }
  for (const gap of fixture.knowledgeGaps) {
    assert.ok(lessons.has(gap.lessonId))
    assert.ok(fixture.masteryProfiles.some(profile => profile.studentId === gap.studentId
      && profile.records.some(record => record.lessonId === gap.lessonId && record.score < 70)))
  }
})

test('learning paths order remediation steps and keep AI exercises reviewable', () => {
  const steps = new Map(fixture.learningPaths.flatMap(path => path.steps.map(step => [step.id, step])))
  const lessons = byId(fixture.academic.lessons)

  for (const path of fixture.learningPaths) {
    assert.deepEqual(path.steps.map(step => step.stepOrder), [...path.steps]
      .map(step => step.stepOrder).sort((left, right) => left - right))
    assert.equal(path.steps[0].status, 'IN_PROGRESS')
    assert.ok(path.steps.slice(1).every(step => step.status === 'LOCKED'))
    for (const step of path.steps) assert.ok(lessons.has(step.lessonId))
  }

  assert.ok(fixture.generatedExercises.every(exercise => steps.has(exercise.learningStepId)))
  assert.ok(fixture.generatedExercises.every(exercise => exercise.isAiGenerated))
  assert.deepEqual(new Set(fixture.generatedExercises.map(exercise => exercise.approvalStatus)),
    new Set(['PENDING', 'APPROVED']))
})
