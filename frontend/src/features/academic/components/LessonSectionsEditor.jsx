export default function LessonSectionsEditor({ sections, onChange }) {
  function update(index, field, value) {
    onChange(sections.map((section, position) => position === index ? { ...section, [field]: value } : section))
  }

  function move(index, offset) {
    const next = [...sections]
    const target = index + offset
    if (target < 0 || target >= next.length) return
    ;[next[index], next[target]] = [next[target], next[index]]
    onChange(next)
  }

  return <section className="academic-lesson-sections-editor" aria-label="Các mục nhỏ của bài học">
    <header><div><h2>Các mục nhỏ</h2><p>Chia nội dung bài học thành từng phần để học sinh dễ ôn tập.</p></div>
      <button type="button" onClick={() => onChange([...sections, { title: '', content: '' }])}>+ Thêm mục</button>
    </header>
    {sections.map((section, index) => <fieldset className="academic-lesson-section-editor" key={`lesson-section-${index}`}>
      <legend>Mục {index + 1}</legend>
      <label>Tiêu đề<input maxLength={160} value={section.title} onChange={event => update(index, 'title', event.target.value)} placeholder="Ví dụ: Nhận biết phép nhân" /></label>
      <label>Nội dung<textarea maxLength={20000} rows="5" value={section.content} onChange={event => update(index, 'content', event.target.value)} placeholder="Giải thích kiến thức, cách làm và ví dụ cho mục này." /></label>
      <div className="academic-lesson-section-actions">
        <button type="button" disabled={index === 0} onClick={() => move(index, -1)} aria-label={`Đưa mục ${index + 1} lên`}>↑</button>
        <button type="button" disabled={index === sections.length - 1} onClick={() => move(index, 1)} aria-label={`Đưa mục ${index + 1} xuống`}>↓</button>
        <button type="button" className="is-remove" disabled={sections.length === 1} onClick={() => onChange(sections.filter((_, position) => position !== index))}>Xóa mục</button>
      </div>
    </fieldset>)}
    {!sections.length && <p className="academic-lesson-sections-empty">Chưa có mục nhỏ. Chọn “Thêm mục” để bắt đầu chia nội dung.</p>}
  </section>
}
