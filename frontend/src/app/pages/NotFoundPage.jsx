import { Link } from 'react-router-dom'

export default function NotFoundPage() {
  return (
    <main id="noi-dung" className="home-container home-explore">
      <h1>Không tìm thấy trang</h1>
      <p>Đường dẫn này chưa tồn tại. Bạn có thể quay về trang chủ để tiếp tục.</p>
      <Link className="home-button" to="/">Về trang chủ</Link>
    </main>
  )
}
