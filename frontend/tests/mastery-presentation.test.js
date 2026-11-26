import test from 'node:test'
import assert from 'node:assert/strict'
import { gapStatusLabel, masteryLevelLabel, openGapCount, triggerSourceLabel } from '../src/features/mastery/utils/masteryPresentation.js'
import { aggregateLessonMastery } from '../src/shared/utils/mastery.js'

test('mastery labels explain backend levels in Vietnamese', () => {
  assert.equal(masteryLevelLabel('PROFICIENT'), 'Thành thạo')
  assert.equal(masteryLevelLabel('UNKNOWN'), 'Chưa xác định')
})

test('open gap count excludes resolved knowledge gaps', () => {
  assert.equal(openGapCount([
    { status: 'IDENTIFIED' },
    { status: 'IN_REMEDIATION' },
    { status: 'RESOLVED' },
  ]), 2)
  assert.equal(openGapCount([{ status: 'RESOLVED' }]), 0)
  assert.equal(openGapCount([]), 0)
  assert.equal(gapStatusLabel('RESOLVED'), 'Đã củng cố')
})

test('mastery history source is readable for assessment and practice', () => {
  assert.equal(triggerSourceLabel('ASSESSMENT_9401'), 'Bài kiểm tra 9401')
  assert.equal(triggerSourceLabel('LEARNING_STEP_8_ATTEMPT_12'), 'Luyện tập bước 8 · lượt 12')
})

test('class dashboard aggregates lesson scores and counts scores below the 70 point review threshold', () => {
  assert.deepEqual(aggregateLessonMastery([
    { lessonId: 4, lessonName: 'Cộng có nhớ', score: 82 },
    { lessonId: 4, lessonName: 'Cộng có nhớ', score: 38 },
    { lessonId: 5, lessonName: 'Phép trừ', score: 70 },
  ]), [
    { lessonId: 4, lessonName: 'Cộng có nhớ', averageMastery: 60, studentsTracked: 2, studentsNeedingReview: 1 },
    { lessonId: 5, lessonName: 'Phép trừ', averageMastery: 70, studentsTracked: 1, studentsNeedingReview: 0 },
  ])
  assert.deepEqual(aggregateLessonMastery([]), [])
})
