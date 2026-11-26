export function createExerciseDraft(question) {
  return {
    content: question.content || '',
    difficulty: question.difficulty || 'EASY',
    score: String(question.score ?? 1),
    explanation: question.explanation || '',
    choices: (question.options || []).map(option => `${option.correct ? '*' : ''}${option.name}`).join('\n'),
    expectedAnswers: (question.expectedAnswers || []).join('\n'),
  }
}

export function toExerciseUpdate(question, draft) {
  const options = question.kind === 'MULTIPLE_CHOICE'
    ? draft.choices.split('\n').map(line => line.trim()).filter(Boolean).map((line, displayOrder) => ({
      name: line.replace(/^\*/, '').trim(),
      correct: line.startsWith('*'),
      displayOrder,
    }))
    : []
  const expectedAnswers = draft.expectedAnswers.split('\n').map(value => value.trim()).filter(Boolean)

  return {
    content: draft.content.trim(),
    difficulty: draft.difficulty,
    score: Number(draft.score),
    lessonId: question.lessonId,
    kind: question.kind,
    explanation: draft.explanation.trim(),
    allowMultiple: question.allowMultiple,
    options,
    blanks: question.kind === 'FILL_BLANK'
      ? expectedAnswers.map((correctAnswer, position) => ({ correctAnswer, position }))
      : [],
    correctAnswer: question.kind === 'SHORT_ANSWER' ? expectedAnswers[0] || '' : '',
  }
}

export function validateExerciseUpdate(question, payload) {
  if (!payload.content) return 'Vui lòng nhập nội dung câu hỏi.'
  if (!Number.isFinite(payload.score) || payload.score <= 0) return 'Điểm số phải lớn hơn 0.'
  if (question.kind === 'MULTIPLE_CHOICE') {
    const correctCount = payload.options.filter(option => option.correct).length
    if (payload.options.length < 2) return 'Câu trắc nghiệm cần ít nhất hai lựa chọn.'
    if (correctCount === 0) return 'Hãy đánh dấu ít nhất một đáp án đúng bằng dấu *.'
    if (!question.allowMultiple && correctCount !== 1) return 'Câu hỏi một đáp án chỉ được có một lựa chọn đúng.'
  }
  if (question.kind === 'FILL_BLANK' && payload.blanks.length === 0) return 'Cần nhập đáp án cho ít nhất một ô trống.'
  if (question.kind === 'SHORT_ANSWER' && !payload.correctAnswer) return 'Vui lòng nhập đáp án chuẩn.'
  return ''
}
