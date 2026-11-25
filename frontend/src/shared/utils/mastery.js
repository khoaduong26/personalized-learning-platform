export function aggregateLessonMastery(records) {
  const lessons = records.reduce((groups, record) => {
    const lessonId = Number(record.lessonId)
    groups.set(lessonId, [...(groups.get(lessonId) || []), record])
    return groups
  }, new Map())

  return [...lessons.entries()].map(([lessonId, items]) => ({
    lessonId,
    lessonName: items[0].lessonName || `Bài học ${lessonId}`,
    averageMastery: items.reduce((sum, item) => sum + (Number(item.score) || 0), 0) / items.length,
    studentsTracked: items.length,
    studentsNeedingReview: items.filter(item => Number(item.score) < 70).length,
  }))
}
