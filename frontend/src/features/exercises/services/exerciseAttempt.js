export function toExerciseAnswerPayload(question, answer = {}) {
  if (question.kind === 'MULTIPLE_CHOICE') {
    return { questionId: question.id, selectedOptionIds: answer.selectedOptionIds || [] }
  }
  if (question.kind === 'FILL_BLANK') {
    const values = Array.from({ length: question.blankCount }, (_, index) => answer.blankAnswers?.[index] || '')
    return { questionId: question.id, blankAnswers: values }
  }
  return { questionId: question.id, content: answer.content || '' }
}

export function restoreExerciseAnswers(drafts) {
  return Object.fromEntries(drafts.map(draft => [draft.questionId, {
    selectedOptionIds: draft.selectedOptionIds || [],
    blankAnswers: draft.blankAnswers || [],
    content: draft.content || '',
  }]))
}

