import { useState } from 'react'

const emptyOption = () => ({ name: '', correct: false })

export default function ManualExerciseForm({ step, saving, error, onCancel, onSubmit }) {
  const [form, setForm] = useState({
    content: '', difficulty: 'EASY', score: '1', kind: 'MULTIPLE_CHOICE', allowMultiple: false,
    options: [{ name: '', correct: true }, emptyOption()], blanks: [{ correctAnswer: '' }],
    correctAnswer: '', caseSensitive: false, explanation: '',
  })

  const change = (key, value) => setForm(current => ({ ...current, [key]: value }))
  const changeMultiple = checked => setForm(current => ({
    ...current,
    allowMultiple: checked,
    options: checked || current.options.filter(option => option.correct).length <= 1
      ? current.options
      : current.options.map((option, index) => ({
          ...option,
          correct: index === current.options.findIndex(item => item.correct),
        })),
  }))
  const changeOptionCorrect = (index, checked) => setForm(current => ({
    ...current,
    options: current.options.map((option, itemIndex) => ({
      ...option,
      correct: current.allowMultiple
        ? itemIndex === index ? checked : option.correct
        : itemIndex === index,
    })),
  }))
  const submit = event => {
    event.preventDefault()
    onSubmit({
      content: form.content.trim(),
      difficulty: form.difficulty,
      score: Number(form.score),
      lessonId: step.lessonId,
      kind: form.kind,
      allowMultiple: form.kind === 'MULTIPLE_CHOICE' ? form.allowMultiple : false,
      caseSensitive: form.kind !== 'MULTIPLE_CHOICE' && form.caseSensitive,
      correctAnswer: form.kind === 'SHORT_ANSWER' ? form.correctAnswer.trim() : null,
      options: form.kind === 'MULTIPLE_CHOICE'
        ? form.options.map((option, displayOrder) => ({ ...option, name: option.name.trim(), displayOrder }))
        : [],
      blanks: form.kind === 'FILL_BLANK'
        ? form.blanks.map((blank, position) => ({ correctAnswer: blank.correctAnswer.trim(), position }))
        : [],
      explanation: form.explanation.trim(),
    })
  }

  return <form className="learning-path-manual-form" onSubmit={submit}>
    <div className="learning-path-manual-heading">
      <div><strong>Soạn bài tập cho {step.lessonName}</strong><span>Bài tập sẽ chờ giáo viên duyệt trước khi học sinh nhìn thấy.</span></div>
      <button type="button" className="learning-path-manual-cancel" onClick={onCancel} disabled={saving}>Đóng</button>
    </div>
    <label>Nội dung câu hỏi<textarea required maxLength="4000" rows="3" value={form.content} onChange={event => change('content', event.target.value)} placeholder="Ví dụ: 8 + 7 bằng bao nhiêu?" /></label>
    <div className="learning-path-manual-row">
      <label>Dạng câu hỏi<select value={form.kind} onChange={event => change('kind', event.target.value)}><option value="MULTIPLE_CHOICE">Trắc nghiệm</option><option value="FILL_BLANK">Điền khuyết</option><option value="SHORT_ANSWER">Trả lời ngắn</option></select></label>
      <label>Độ khó<select value={form.difficulty} onChange={event => change('difficulty', event.target.value)}><option value="EASY">Dễ</option><option value="MEDIUM">Vừa</option><option value="HARD">Khó</option></select></label>
      <label>Điểm<input type="number" min="0.1" max="100" step="0.1" required value={form.score} onChange={event => change('score', event.target.value)} /></label>
    </div>

    {form.kind === 'MULTIPLE_CHOICE' && <fieldset className="learning-path-manual-options">
      <legend>Lựa chọn và đáp án đúng</legend>
      <label className="learning-path-manual-check"><input type="checkbox" checked={form.allowMultiple} onChange={event => changeMultiple(event.target.checked)} /> Cho phép chọn nhiều đáp án đúng</label>
      {form.options.map((option, index) => <div className="learning-path-manual-option" key={index}>
        <input aria-label={`Lựa chọn ${index + 1}`} required maxLength="1000" value={option.name} placeholder={`Lựa chọn ${index + 1}`} onChange={event => change('options', form.options.map((item, itemIndex) => itemIndex === index ? { ...item, name: event.target.value } : item))} />
        <label className="learning-path-manual-check"><input type={form.allowMultiple ? 'checkbox' : 'radio'} name={`correct-option-${step.id}`} required={!form.allowMultiple} checked={option.correct} onChange={event => changeOptionCorrect(index, event.target.checked)} /> Đáp án đúng</label>
        {form.options.length > 2 && <button type="button" className="learning-path-manual-remove" aria-label={`Xóa lựa chọn ${index + 1}`} onClick={() => change('options', form.options.filter((_, itemIndex) => itemIndex !== index))}>×</button>}
      </div>)}
      <button type="button" className="learning-path-manual-add" onClick={() => change('options', [...form.options, emptyOption()])}>+ Thêm lựa chọn</button>
    </fieldset>}

    {form.kind === 'FILL_BLANK' && <fieldset className="learning-path-manual-options">
      <legend>Đáp án theo thứ tự ô trống</legend>
      {form.blanks.map((blank, index) => <div className="learning-path-manual-option" key={index}>
        <label className="learning-path-manual-blank">Ô {index + 1}<input required maxLength="500" value={blank.correctAnswer} onChange={event => change('blanks', form.blanks.map((item, itemIndex) => itemIndex === index ? { correctAnswer: event.target.value } : item))} /></label>
        {form.blanks.length > 1 && <button type="button" className="learning-path-manual-remove" aria-label={`Xóa ô ${index + 1}`} onClick={() => change('blanks', form.blanks.filter((_, itemIndex) => itemIndex !== index))}>×</button>}
      </div>)}
      <button type="button" className="learning-path-manual-add" onClick={() => change('blanks', [...form.blanks, { correctAnswer: '' }])}>+ Thêm ô trống</button>
    </fieldset>}

    {form.kind === 'SHORT_ANSWER' && <label>Đáp án chuẩn<input required value={form.correctAnswer} onChange={event => change('correctAnswer', event.target.value)} /></label>}
    {form.kind !== 'MULTIPLE_CHOICE' && <label className="learning-path-manual-check"><input type="checkbox" checked={form.caseSensitive} onChange={event => change('caseSensitive', event.target.checked)} /> Phân biệt chữ hoa và chữ thường</label>}
    <label>Lời giải / gợi ý<textarea rows="2" value={form.explanation} onChange={event => change('explanation', event.target.value)} /></label>
    {error && <p className="learning-path-error" role="alert">{error}</p>}
    <div className="learning-path-manual-actions"><button type="button" className="learning-path-manual-cancel" onClick={onCancel} disabled={saving}>Hủy</button><button type="submit" className="learning-path-manual-submit" disabled={saving}>{saving ? 'Đang lưu…' : 'Lưu và gửi duyệt'}</button></div>
  </form>
}
