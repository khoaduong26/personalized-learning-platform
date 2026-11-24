import assert from 'node:assert/strict'
import { test } from 'node:test'
import { restoreExerciseAnswers, toExerciseAnswerPayload } from '../src/features/exercises/services/exerciseAttempt.js'

test('multiple choice exercise answers preserve selected option ids', () => {
  assert.deepEqual(toExerciseAnswerPayload(
    { id: 14, kind: 'MULTIPLE_CHOICE' },
    { selectedOptionIds: [3, 5] },
  ), { questionId: 14, selectedOptionIds: [3, 5] })
})

test('fill blank answers always match the question blank count', () => {
  assert.deepEqual(toExerciseAnswerPayload(
    { id: 22, kind: 'FILL_BLANK', blankCount: 3 },
    { blankAnswers: ['12'] },
  ), { questionId: 22, blankAnswers: ['12', '', ''] })
})

test('short answer exercise answers use the shared attempt payload', () => {
  assert.deepEqual(toExerciseAnswerPayload(
    { id: 31, kind: 'SHORT_ANSWER' },
    { content: ' 17 ' },
  ), { questionId: 31, content: ' 17 ' })
})

test('saved answer drafts restore all answer forms by question id', () => {
  assert.deepEqual(restoreExerciseAnswers([
    { questionId: 14, selectedOptionIds: [5] },
    { questionId: 22, blankAnswers: ['12', '13'] },
    { questionId: 31, content: '17' },
  ]), {
    14: { selectedOptionIds: [5], blankAnswers: [], content: '' },
    22: { selectedOptionIds: [], blankAnswers: ['12', '13'], content: '' },
    31: { selectedOptionIds: [], blankAnswers: [], content: '17' },
  })
})

