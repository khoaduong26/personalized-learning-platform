export function groupStepsByTopic(steps = []) {
  const groups = new Map()
  for (const step of steps) {
    const name = step.topicName?.trim() || 'Kiến thức cần củng cố'
    const key = step.topicId == null ? name : String(step.topicId)
    if (!groups.has(key)) groups.set(key, { id: key, name, steps: [] })
    groups.get(key).steps.push(step)
  }
  return [...groups.values()]
}
