const DEFAULT_SECTION_TITLE = 'Kiến thức cần nhớ'

export function parseLearningSections(content) {
  const sections = []
  let current = { title: DEFAULT_SECTION_TITLE, lines: [], isHeading: false }
  const flush = () => {
    if (current.isHeading || current.lines.some(line => line.trim())) sections.push(current)
  }

  for (const line of String(content ?? '').split(/\r?\n/)) {
    const text = line.trim()
    const markdownHeading = text.match(/^#{1,3}\s+(.+)$/)
    const uppercaseHeading = text.length > 1 && text.length <= 80 && /\p{L}/u.test(text)
      && text === text.toLocaleUpperCase('vi') && !/[.!?]$/.test(text)
    if (markdownHeading || uppercaseHeading) {
      flush()
      current = { title: (markdownHeading?.[1] || text).trim(), lines: [], isHeading: true }
    } else current.lines.push(line)
  }
  flush()
  if (!sections.length) sections.push({ title: DEFAULT_SECTION_TITLE, lines: [] })
  return sections.map(section => ({ title: section.title, content: section.lines.join('\n').trim() }))
}

export function serializeLearningSections(sections = []) {
  return sections
    .map(section => ({ title: String(section.title || '').trim(), content: String(section.content || '').trim() }))
    .filter(section => section.content || (section.title && section.title !== DEFAULT_SECTION_TITLE))
    .map(section => `# ${section.title || DEFAULT_SECTION_TITLE}\n${section.content}`)
    .join('\n\n')
    .trim()
}
