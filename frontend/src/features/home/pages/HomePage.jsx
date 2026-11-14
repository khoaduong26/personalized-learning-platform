import { useEffect, useState } from 'react'
import MathGarden from '../components/MathGarden'
import PracticePreview from '../components/PracticePreview'
import { practiceTopics } from '../data/practice'
import '../styles/home.css'

export default function HomePage() {
  const [topicId, setTopicId] = useState('operations')
  const topic = practiceTopics.find((item) => item.id === topicId)

  useEffect(() => { document.title = 'TLCN · Cùng em khám phá Toán lớp 2' }, [])

  return (
    <main id="noi-dung">
      <section className="home-container home-hero" aria-labelledby="hero-title">
        <div className="home-hero-copy">
          <p className="home-eyebrow"><span className="eyebrow-dot" /> TOÁN LỚP 2 · HỌC THEO NHỊP CỦA EM</p>
          <h1 id="hero-title">Một chút tò mò.<br />Một thế giới <span>Toán vui.<svg viewBox="0 0 300 16" aria-hidden="true"><path d="M3 10Q125-4 295 9M27 15Q156 4 273 14" /></svg></span></h1>
          <p className="home-lead">Cùng khám phá những con số, thử sức với bài toán nhỏ và tự tin hơn mỗi ngày. Từng bước một, em làm được mà!</p>
          <div className="home-hero-actions"><a className="home-button" href="#thu-suc">Em thử sức nhé <span aria-hidden="true">→</span></a><a className="home-text-link" href="#giao-vien">Tôi là giáo viên <span aria-hidden="true">↗</span></a></div>
          <div className="home-reassurance"><span aria-hidden="true">✓</span> Không cần tài khoản để thử bài toán bên dưới</div>
        </div>
        <div className="home-hero-art"><div className="garden-label"><span aria-hidden="true">✦</span> Mỗi ngày, một khám phá mới</div><MathGarden /><div className="garden-caption"><span aria-hidden="true">♡</span> Sai một chút cũng không sao.<br /><strong>Mình cùng thử lại nhé!</strong></div></div>
      </section>

      <div className="home-values"><div className="home-container"><span><span aria-hidden="true">✳</span> Bài toán nhỏ, niềm vui lớn</span><span><span aria-hidden="true">◇</span> Học từng bước, hiểu thật chắc</span><span><span aria-hidden="true">♡</span> Có thầy cô cùng đồng hành</span></div></div>

      <section id="kham-pha" className="home-container home-explore" aria-labelledby="explore-title">
        <div className="home-section-heading"><div><p className="home-eyebrow">THẾ GIỚI TOÁN HỌC</p><h2 id="explore-title">Hôm nay, em muốn khám phá gì?</h2></div><p>Chọn một chủ đề.<br />Cùng thử một bài toán nhỏ nhé!</p></div>
        <div className="home-topics">{practiceTopics.map((item, index) => <a key={item.id} href="#thu-suc" className={`topic-card ${item.color}`} onClick={() => setTopicId(item.id)}><div className="topic-top"><span className="topic-symbol" aria-hidden="true">{item.symbol}</span><span className="topic-number">0{index + 1}</span></div><h3>{item.title}</h3><p>{item.description}</p><span className="topic-link">Thử một bài toán <span aria-hidden="true">↗</span></span></a>)}</div>
      </section>

      <section id="thu-suc" className="home-practice-section" aria-labelledby="practice-title"><div className="home-container home-practice-grid">
        <div className="home-practice-copy"><p className="home-eyebrow">HỌC BẰNG CÁCH THỬ</p><h2 id="practice-title">Một bài toán nhỏ.<br />Em sẵn sàng chưa?</h2><p>Không cần vội. Em cứ suy nghĩ, chọn đáp án và xem lời giải. Nếu thấy khó, đã có gợi ý giúp em!</p><div className="topic-picker" role="group" aria-label="Chủ đề bài tập">{practiceTopics.map((item) => <button key={item.id} type="button" aria-pressed={topicId === item.id} onClick={() => setTopicId(item.id)}>{item.title}</button>)}</div><div className="practice-encouragement"><span aria-hidden="true">✦</span> Mỗi lần thử là một lần em học thêm điều mới.</div></div>
        <PracticePreview key={topic.id} topic={topic} />
      </div></section>

      <section id="giao-vien" className="home-container home-teachers" aria-labelledby="teacher-title"><div className="home-section-heading"><div><p className="home-eyebrow">GÓC DÀNH CHO THẦY CÔ</p><h2 id="teacher-title">Hiểu từng em.<br />Đồng hành từng bước.</h2></div><p>Mỗi học sinh có một nhịp học riêng.<br />TLCN hướng đến việc hỗ trợ thầy cô<br />tìm ra bước tiếp theo phù hợp.</p></div><p className="teacher-roadmap-label">Các chức năng đang được phát triển</p><div className="teacher-steps"><article><span>01</span><h3>Nhìn rõ năng lực</h3><p>Đánh giá theo từng đơn vị kiến thức để nhận biết điều học sinh đã hiểu và phần cần củng cố.</p></article><article><span>02</span><h3>Chọn đúng bước học</h3><p>Đề xuất lộ trình học tập dựa trên kết quả và những kiến thức mỗi em cần luyện thêm.</p></article><article><span>03</span><h3>Thầy cô luôn đồng hành</h3><p>Giáo viên xem xét, duyệt bài tập được đề xuất trước khi giao cho học sinh luyện tập.</p></article></div><div className="teacher-invite"><div><strong>Bắt đầu bằng một tài khoản TLCN</strong><p>Đăng ký và xác minh email để chuẩn bị cho hành trình cùng học sinh.</p></div><a className="home-button" href="/register">Đăng ký tài khoản <span aria-hidden="true">→</span></a></div></section>

      <section className="home-closing"><div className="home-container"><span className="closing-star" aria-hidden="true">✳</span><h2>Không cần giỏi ngay.<br />Chỉ cần vui học mỗi ngày.</h2><p>Hành trình khám phá Toán học bắt đầu từ một bước nhỏ.</p><a className="home-button" href="#thu-suc">Cùng thử một bài toán <span aria-hidden="true">→</span></a></div></section>
    </main>
  )
}
