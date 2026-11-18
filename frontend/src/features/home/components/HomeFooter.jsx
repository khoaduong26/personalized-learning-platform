import { Link } from 'react-router-dom'

const guides = [
  ['Dành cho giáo viên', 'Chức năng đánh giá, theo dõi tiến độ và đề xuất bài học đang được phát triển.'],
  ['Dành cho học sinh', 'Chọn chủ đề, thử bài toán và xem phản hồi ngay. Có gợi ý khi em cần.'],
  ['Câu hỏi thường gặp', 'Em có thể luyện thử mà không cần tài khoản. Kết quả luyện thử không được lưu.'],
]

const policies = [
  { id: 'footer-privacy', title: 'Bảo mật & dữ liệu học sinh', description: 'Chính sách bảo mật và bảo vệ dữ liệu học sinh chưa được công bố.' },
  { id: 'footer-terms', title: 'Điều khoản sử dụng', description: 'Điều khoản sử dụng đang được chuẩn bị.' },
  { id: 'footer-ai', title: 'Vai trò của AI', description: 'AI hỗ trợ phân tích và gợi ý bài học; giáo viên xem xét và quyết định. Chức năng đang phát triển.' },
]

function FooterIcon({ type }) {
  const paths = {
    book: 'M4 5h5a3 3 0 0 1 3 3v12a4 4 0 0 0-3-1H4V5Zm16 0h-5a3 3 0 0 0-3 3m0 12a4 4 0 0 1 3-1h5V5',
    help: 'M5 13v-2a7 7 0 0 1 14 0v2M5 12H3v6h4v-6H5Zm14 0h2v6h-4v-6h2Zm0 6c0 3-3 3-6 3',
    shield: 'M12 3 4 6v6c0 5 8 9 8 9s8-4 8-9V6l-8-3Zm-4 9 3 3 5-6',
  }
  return <svg className="footer-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d={paths[type]} /></svg>
}

export default function HomeFooter() {
  const year = new Intl.DateTimeFormat('vi-VN', { year: 'numeric', timeZone: 'Asia/Ho_Chi_Minh' }).format(new Date())

  return (
    <footer className="home-footer" aria-label="Thông tin về EduLearn">
      <div className="home-container">
        <div className="footer-grid">
          <section className="footer-about" aria-labelledby="footer-about-title">
            <Link className="home-brand" to="/" aria-label="EduLearn — Trang chủ">
              <img className="brand-symbol" src="/math-bunny.svg" width="44" height="44" alt="" />
              <span>EduLearn<span className="brand-dot">.</span></span>
            </Link>
            <h2 id="footer-about-title" className="footer-about-title">Cùng thầy cô, hiểu từng em.</h2>
            <p>Hướng đến hỗ trợ giáo viên đánh giá năng lực và cá nhân hóa việc học cho học sinh tiểu học.</p>
          </section>

          <section className="footer-group" aria-labelledby="footer-guides-title">
            <h2 id="footer-guides-title"><FooterIcon type="book" />Hướng dẫn</h2>
            <div className="footer-details-list">
              {guides.map(([title, description]) => (
                <details className="footer-detail" key={title}>
                  <summary>{title}</summary>
                  <p>{description}</p>
                </details>
              ))}
            </div>
            <a className="footer-link" href="/#thu-suc">Thử một bài toán <span aria-hidden="true">↗</span></a>
          </section>

          <section className="footer-group" aria-labelledby="footer-support-title">
            <h2 id="footer-support-title"><FooterIcon type="help" />Hỗ trợ</h2>
            <ul className="footer-support-list">
              <li><span>Liên hệ & email</span></li>
              <li><span>Góp ý & báo lỗi</span></li>
            </ul>
          </section>

          <section className="footer-group" aria-labelledby="footer-policies-title">
            <h2 id="footer-policies-title"><FooterIcon type="shield" />Chính sách</h2>
            <div className="footer-details-list">
              {policies.map(({ id, title, description }) => (
                <details className="footer-detail" key={id}>
                  <summary>{title}</summary>
                  <p id={id}>{description}</p>
                </details>
              ))}
            </div>
          </section>
        </div>

        <div className="footer-bottom">
          <p>© {year} EduLearn.</p>
          <nav aria-label="Chính sách của EduLearn">
            <a href="#footer-privacy">Chính sách bảo mật</a>
            <a href="#footer-terms">Điều khoản sử dụng</a>
          </nav>
        </div>
      </div>
    </footer>
  )
}
