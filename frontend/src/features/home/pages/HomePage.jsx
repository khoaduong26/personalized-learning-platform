import { useEffect, useState } from 'react'
import { Link, useLocation, useOutletContext } from 'react-router-dom'
import MathGarden from '../components/MathGarden'
import PracticePreview from '../components/PracticePreview'
import { practiceTopics } from '../data/practice'
import '../styles/home.css'

export default function HomePage() {
  const { hash, key } = useLocation()
  const user = useOutletContext()
  const isStudent = user?.role === 'STUDENT'
  const isTeacher = user?.role === 'TEACHER'
  const isAdmin = user?.role === 'ADMIN'
  const [topicId, setTopicId] = useState('operations')
  const topic = practiceTopics.find((item) => item.id === topicId)

  useEffect(() => { document.title = 'EduLearn · Cùng em khám phá Toán lớp 2' }, [])

  useEffect(() => {
    const targetId = hash.slice(1)
    if (!targetId) return
    const frame = requestAnimationFrame(() => {
      const target = document.getElementById(targetId)
      target?.focus({ preventScroll: true })
      target?.scrollIntoView({ block: 'start' })
    })
    return () => cancelAnimationFrame(frame)
  }, [hash, key])

  return (
    <main id="noi-dung" tabIndex={-1}>
      <section className="home-container home-hero" aria-labelledby="hero-title">
        <div className="home-hero-copy">
          <h1 id="hero-title">{isTeacher ? 'Góc làm việc của thầy cô' : isAdmin ? 'Quản trị EduLearn' : 'Cùng em học Toán lớp 2!'}</h1>
          <p className="home-lead">{isTeacher ? 'Quản lý lớp, giao bài kiểm tra và xem từng em cần luyện thêm phần nào.' : isAdmin ? 'Quản lý tài khoản, lớp học và chương trình Toán lớp 2.' : 'Làm bài thầy cô giao, luyện thêm theo từng bước hoặc thử một bài Toán ngay.'}</p>
          <div className="home-hero-actions">
            <Link className="home-button" to={isAdmin ? '/admin' : isTeacher ? '/classrooms' : '/my-learning-path'}>
              {isAdmin ? 'Mở dashboard' : isTeacher ? 'Mở lớp học' : 'Lộ trình của em'} <span aria-hidden="true">→</span>
            </Link>
            {user ? <Link className="home-text-link" to="/assessments">
              {isAdmin ? 'Ngân hàng câu hỏi' : isTeacher ? 'Mở bài kiểm tra' : 'Bài kiểm tra của em'}
            </Link> : <Link className="home-text-link" to="/#thu-suc">Thử bài Toán</Link>}
          </div>
          {!user && <p className="home-reassurance">Em có thể thử bài Toán mà không cần đăng nhập.</p>}
        </div>
        <div className="home-hero-art"><MathGarden /></div>
      </section>

      <section id="kham-pha" className="home-container home-explore" aria-labelledby="explore-title" tabIndex={-1}>
        <div className="home-section-heading"><div><p className="home-eyebrow">THẾ GIỚI TOÁN HỌC</p><h2 id="explore-title">Hôm nay, em muốn khám phá gì?</h2></div><p>Chọn một chủ đề.<br />Cùng thử một bài toán nhỏ nhé!</p></div>
        <div className="home-topics">{practiceTopics.map(item => <Link key={item.id} to="/#thu-suc" className={`topic-card ${item.color}`} onClick={() => setTopicId(item.id)}><div className="topic-top"><span className="topic-symbol" aria-hidden="true">{item.symbol}</span></div><h3>{item.title}</h3><p>{item.description}</p><span className="topic-link">Thử một bài <span aria-hidden="true">↗</span></span></Link>)}</div>
      </section>

      <section id="thu-suc" className="home-practice-section" aria-labelledby="practice-title" tabIndex={-1}><div className="home-container home-practice-grid">
        <div className="home-practice-copy"><p className="home-eyebrow">HỌC BẰNG CÁCH THỬ</p><h2 id="practice-title">Một bài toán nhỏ.<br />Em sẵn sàng chưa?</h2><p>Không cần vội. Em cứ suy nghĩ, chọn đáp án và xem lời giải. Nếu thấy khó, đã có gợi ý giúp em!</p><div className="topic-picker" role="group" aria-label="Chủ đề bài tập">{practiceTopics.map((item) => <button key={item.id} type="button" aria-pressed={topicId === item.id} onClick={() => setTopicId(item.id)}>{item.title}</button>)}</div><div className="practice-encouragement"><span aria-hidden="true">✦</span> Mỗi lần thử là một lần em học thêm điều mới.</div></div>
        <PracticePreview key={topic.id} topic={topic} />
      </div></section>

      {isTeacher && <section id="giao-vien" className="home-container home-teachers" aria-labelledby="teacher-title" tabIndex={-1}>
        <div className="home-section-heading"><h2 id="teacher-title">Dành cho thầy cô</h2></div>
        <div className="teacher-steps">
          <Link to="/classrooms" className="teacher-step-link"><h3>Quản lý lớp học</h3><p>Thêm học sinh và xem kết quả học của từng em.</p></Link>
          <Link to="/assessments" className="teacher-step-link"><h3>Giao bài kiểm tra</h3><p>Tạo câu hỏi, giao bài cho lớp và xem kết quả.</p></Link>
          <Link to="/exercises/review" className="teacher-step-link"><h3>Duyệt bài tập</h3><p>Kiểm tra bài được đề xuất trước khi giao cho học sinh.</p></Link>
        </div>
      </section>}
    </main>
  )
}
