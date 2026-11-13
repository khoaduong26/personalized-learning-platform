import { useState } from 'react'

export default function PracticePreview({ topic }) {
  const [questionIndex, setQuestionIndex] = useState(0)
  const [selected, setSelected] = useState(null)
  const [showHint, setShowHint] = useState(false)
  const question = topic.questions[questionIndex]
  const correct = selected === question.answer

  function nextQuestion() {
    setQuestionIndex((current) => (current + 1) % topic.questions.length)
    setSelected(null)
    setShowHint(false)
  }

  return (
    <div className="practice-card">
      <div className="practice-topline"><span>GÓC THỬ SỨC</span><span>{questionIndex + 1} / {topic.questions.length}</span></div>
      <h3>{question.prompt}</h3>
      <div className="practice-expression">{question.expression}</div>
      <div className="practice-options" role="group" aria-label="Chọn đáp án">
        {question.options.map((option) => (
          <button key={option} type="button" aria-pressed={selected === option}
            className={selected === option ? (correct ? 'is-correct' : 'try-again') : ''}
            disabled={correct} onClick={() => setSelected(option)}>
            {option}{selected === option && <span className="answer-mark" aria-hidden="true">{correct ? '✓' : '↻'}</span>}
          </button>
        ))}
      </div>
      <div className="practice-feedback" role="status" aria-live="polite" aria-atomic="true">
        {selected === null ? 'Em hãy chọn một đáp án ở trên nhé.' : correct ? `Đúng rồi, giỏi lắm! ${question.explanation}` : 'Chưa đúng rồi. Không sao, em thử lại nhé!'}
      </div>
      <div className="practice-actions">
        <button className="hint-button" type="button" aria-expanded={showHint} onClick={() => setShowHint(!showHint)}>{showHint ? 'Ẩn gợi ý' : 'Cho em một gợi ý'}</button>
        <button className="next-button" type="button" onClick={nextQuestion}>Câu tiếp theo <span aria-hidden="true">→</span></button>
      </div>
      {showHint && <p className="practice-hint">{question.hint}</p>}
      <p className="practice-note">Bài tập minh họa · Kết quả không được lưu vào tài khoản.</p>
    </div>
  )
}
