import assert from 'node:assert/strict'
import { test } from 'node:test'
import { createExerciseDraft, toExerciseUpdate, validateExerciseUpdate } from '../src/features/exercises/services/exerciseDraft.js'

test('multiple choice review draft keeps the correct answer and option order', () => {
  const question = {
    kind: 'MULTIPLE_CHOICE', lessonId: 12, allowMultiple: false,
    content: '2 + 3 = ?', difficulty: 'EASY', score: 2,
    options: [{ name: '4', correct: false }, { name: '5', correct: true }],
  }
  const draft = createExerciseDraft(question)
  draft.content = '2 + 4 = ?'
  draft.choices = '5\n*6\n7'

  assert.deepEqual(toExerciseUpdate(question, draft), {
    content: '2 + 4 = ?', difficulty: 'EASY', score: 2, lessonId: 12,
    kind: 'MULTIPLE_CHOICE', explanation: '', allowMultiple: false,
    options: [
      { name: '5', correct: false, displayOrder: 0 },
      { name: '6', correct: true, displayOrder: 1 },
      { name: '7', correct: false, displayOrder: 2 },
    ], blanks: [], correctAnswer: '',
  })
})

test('fill blank review draft preserves one expected answer per blank', () => {
  const question = { kind: 'FILL_BLANK', lessonId: 4, expectedAnswers: ['12', '18'] }
  const draft = createExerciseDraft(question)
  draft.expectedAnswers = '13\n19'
  const payload = toExerciseUpdate(question, draft)

  assert.deepEqual(payload.blanks, [
    { correctAnswer: '13', position: 0 },
    { correctAnswer: '19', position: 1 },
  ])
  assert.equal(payload.lessonId, 4)
})

test('short answer review draft sends its answer and score as typed values', () => {
  const question = { kind: 'SHORT_ANSWER', lessonId: 7, expectedAnswers: ['20'] }
  const draft = createExerciseDraft({ ...question, score: 1 })
  draft.expectedAnswers = ' 21 '
  draft.score = '1.5'
  const payload = toExerciseUpdate(question, draft)

  assert.equal(payload.correctAnswer, '21')
  assert.equal(payload.score, 1.5)
  assert.deepEqual(payload.blanks, [])
})

test('review form validation rejects multiple correct options on a single-answer question', () => {
  const question = { kind: 'MULTIPLE_CHOICE', allowMultiple: false }
  const draft = { content: 'Câu hỏi', score: '1', choices: '*A\n*B', expectedAnswers: '', explanation: '' }
  assert.equal(validateExerciseUpdate(question, toExerciseUpdate(question, draft)), 'Câu hỏi một đáp án chỉ được có một lựa chọn đúng.')
})
