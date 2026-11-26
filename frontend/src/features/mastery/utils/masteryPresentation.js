const masteryLevels = {
  NOT_STARTED: 'Chưa đánh giá',
  BEGINNING: 'Bắt đầu',
  DEVELOPING: 'Đang phát triển',
  PROFICIENT: 'Thành thạo',
  MASTERED: 'Nắm vững',
}

const gapStatuses = {
  IDENTIFIED: 'Cần ôn tập',
  IN_REMEDIATION: 'Đang ôn tập',
  RESOLVED: 'Đã củng cố',
}

export function masteryLevelLabel(level) {
  return masteryLevels[level] || 'Chưa xác định'
}

export function gapStatusLabel(status) {
  return gapStatuses[status] || 'Chưa xác định'
}

export function openGapCount(gaps) {
  return gaps.filter(gap => gap.status !== 'RESOLVED').length
}

export function triggerSourceLabel(source) {
  if (!source) return 'Lần đánh giá'
  if (source.startsWith('ASSESSMENT_')) return `Bài kiểm tra ${source.slice('ASSESSMENT_'.length)}`
  if (source.startsWith('LEARNING_STEP_')) {
    const match = source.match(/^LEARNING_STEP_(\d+)_ATTEMPT_(\d+)$/)
    return match ? `Luyện tập bước ${match[1]} · lượt ${match[2]}` : `Luyện tập ${source.slice('LEARNING_STEP_'.length)}`
  }
  return source
}
