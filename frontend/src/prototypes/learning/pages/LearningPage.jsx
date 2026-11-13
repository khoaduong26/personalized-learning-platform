import { useMemo, useState } from 'react'
import LearningResourceCard from '../components/LearningResourceCard'
import { LEARNING_RESOURCES, COMBOS, GRADES } from '../data/learningResources'
import Icon from '../../../shared/components/ui/Icon'
import { formatVnd } from '../../../shared/utils/formatCurrency'

export default function LearningPage() {
  const [grade, setGrade] = useState('all')
  const [cart, setCart] = useState(0)
  const [query, setQuery] = useState('')

  const visible = useMemo(() => {
    return LEARNING_RESOURCES.filter(
      (b) =>
        (grade === 'all' || b.grade === grade) &&
        (query.trim() === '' ||
          b.title.toLowerCase().includes(query.trim().toLowerCase()) ||
          b.subject.toLowerCase().includes(query.trim().toLowerCase()))
    )
  }, [grade, query])

  return (
    <div className="min-h-screen bg-[#faf6ee] text-[#1d2a24] antialiased">
      {/* Thanh thông báo */}
      <div className="bg-[#1d2a24] px-4 py-2 text-center text-xs tracking-wide text-[#e9e2d0]">
        Miễn phí giao hàng cho đơn từ 299.000đ · Giao nội thành TP.HCM trong 24 giờ
      </div>

      {/* Header */}
      <header className="sticky top-0 z-30 border-b border-[#e3dccb] bg-[#faf6ee]/95 backdrop-blur">
        <div className="mx-auto flex max-w-6xl items-center gap-6 px-5 py-4">
          <a href="#" className="shrink-0 font-serif text-2xl font-bold tracking-tight">
            TLCN<span className="text-[#a23b2a]">.</span>
            <span className="ml-1 hidden text-sm font-normal text-[#5b6a62] sm:inline">Sách giáo khoa</span>
          </a>

          <label className="relative hidden flex-1 md:block">
            <Icon name="search" className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-[#7a877f]" />
            <input
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Tìm theo tên sách hoặc môn học..."
              className="w-full rounded-full border border-[#ddd5c2] bg-white py-2.5 pl-10 pr-4 text-sm outline-none transition focus:border-[#1f4d3a] focus:ring-2 focus:ring-[#1f4d3a]/15"
            />
          </label>

          <nav className="ml-auto flex items-center gap-6 text-sm font-medium">
            <a href="/register" className="hover:text-[#a23b2a]">Đăng ký tài khoản</a>
            <a href="#sach" className="hidden hover:text-[#a23b2a] sm:block">Sách lẻ</a>
            <a href="#combo" className="hidden hover:text-[#a23b2a] sm:block">Trọn bộ</a>
            <a href="#lien-he" className="hidden hover:text-[#a23b2a] sm:block">Liên hệ</a>
            <button className="relative flex items-center gap-2 rounded-full bg-[#1f4d3a] px-4 py-2 text-[#faf6ee] transition hover:bg-[#173a2c]">
              <Icon name="cart" className="h-4 w-4" />
              Giỏ hàng
              <span className="grid h-5 min-w-5 place-items-center rounded-full bg-[#faf6ee] px-1 text-xs font-semibold text-[#1f4d3a]">
                {cart}
              </span>
            </button>
          </nav>
        </div>
      </header>

      {/* Hero */}
      <section className="mx-auto grid max-w-6xl items-center gap-12 px-5 pb-16 pt-14 md:grid-cols-[1.1fr_0.9fr] md:pt-20">
        <div>
          <p className="text-sm font-medium uppercase tracking-[0.2em] text-[#a23b2a]">
            Năm học 2026 – 2027
          </p>
          <h1 className="mt-4 font-serif text-4xl font-bold leading-[1.1] tracking-tight sm:text-5xl lg:text-6xl">
            Đủ sách cho cả năm học,
            <br />
            chọn một lần là xong.
          </h1>
          <p className="mt-6 max-w-lg text-base leading-relaxed text-[#4c5b53]">
            Sách giáo khoa chính hãng từ lớp 1 đến lớp 12, đủ các bộ Kết nối tri thức, Chân trời
            sáng tạo và Cánh Diều. Chọn theo lớp, giao tận nhà.
          </p>
          <div className="mt-8 flex flex-wrap items-center gap-4">
            <a href="#sach" className="inline-flex items-center gap-2 rounded-full bg-[#a23b2a] px-6 py-3 text-sm font-semibold text-white transition hover:bg-[#8a3123]">
              Chọn sách theo lớp
              <Icon name="arrow" className="h-4 w-4" />
            </a>
            <a href="#combo" className="text-sm font-semibold underline decoration-[#c9bfa6] underline-offset-4 hover:decoration-[#1d2a24]">
              Xem bộ sách trọn gói
            </a>
          </div>

          <dl className="mt-12 flex gap-10 border-t border-[#e3dccb] pt-6">
            {[
              ['12', 'khối lớp'],
              ['3', 'bộ sách chính thống'],
              ['24h', 'giao nội thành'],
            ].map(([n, l]) => (
              <div key={l}>
                <dt className="font-serif text-3xl font-bold">{n}</dt>
                <dd className="mt-1 text-xs text-[#5b6a62]">{l}</dd>
              </div>
            ))}
          </dl>
        </div>

        {/* Chồng sách */}
        <div className="relative mx-auto h-90 w-full max-w-sm sm:h-105">
          <LearningResourceCard resource={LEARNING_RESOURCES[5]} className="absolute left-0 top-10 w-40 -rotate-6 sm:w-48" />
          <LearningResourceCard resource={LEARNING_RESOURCES[0]} className="absolute right-0 top-0 w-44 rotate-3 sm:w-52" />
          <LearningResourceCard resource={LEARNING_RESOURCES[3]} className="absolute bottom-0 left-16 w-40 -rotate-2 sm:left-20 sm:w-48" />
        </div>
      </section>

      {/* Danh mục sách */}
      <section id="sach" className="border-t border-[#e3dccb] bg-white/60">
        <div className="mx-auto max-w-6xl px-5 py-16">
          <div className="flex flex-wrap items-end justify-between gap-4">
            <div>
              <h2 className="font-serif text-3xl font-bold tracking-tight">Sách giáo khoa</h2>
              <p className="mt-2 text-sm text-[#5b6a62]">Lọc theo lớp để xem đúng sách con bạn cần.</p>
            </div>
            <p className="text-sm text-[#5b6a62]">{visible.length} đầu sách</p>
          </div>

          {/* Bộ lọc lớp */}
          <div className="mt-8 flex gap-2 overflow-x-auto pb-2">
            {GRADES.map((g) => (
              <button
                key={g.id}
                onClick={() => setGrade(g.id)}
                className={`shrink-0 rounded-full border px-4 py-1.5 text-sm transition ${
                  grade === g.id
                    ? 'border-[#1d2a24] bg-[#1d2a24] text-[#faf6ee]'
                    : 'border-[#ddd5c2] bg-transparent hover:border-[#1d2a24]'
                }`}
              >
                {g.label}
              </button>
            ))}
          </div>

          {/* Lưới sách */}
          {visible.length === 0 ? (
            <div className="mt-12 rounded-lg border border-dashed border-[#d5ccb6] py-16 text-center text-sm text-[#5b6a62]">
              Chưa có sách phù hợp. Thử chọn lớp khác hoặc đổi từ khóa nhé.
            </div>
          ) : (
            <div className="mt-10 grid grid-cols-2 gap-x-6 gap-y-10 md:grid-cols-3 lg:grid-cols-4">
              {visible.map((b) => (
                <article key={b.id} className="group">
                  <a href="#" className="block transition group-hover:-translate-y-1">
                    <LearningResourceCard resource={b} />
                  </a>
                  <div className="mt-4">
                    <p className="text-xs uppercase tracking-wider text-[#7a877f]">
                      {b.subject} · Lớp {b.grade}
                    </p>
                    <h3 className="mt-1 text-sm font-semibold leading-snug">{b.title}</h3>
                    <div className="mt-2 flex items-baseline gap-2">
                      <span className="font-semibold text-[#a23b2a]">{formatVnd(b.price)}</span>
                      {b.oldPrice && (
                        <span className="text-xs text-[#8a958e] line-through">{formatVnd(b.oldPrice)}</span>
                      )}
                    </div>
                    <button
                      onClick={() => setCart((c) => c + 1)}
                      className="mt-3 w-full rounded-full border border-[#1d2a24] py-2 text-sm font-medium transition hover:bg-[#1d2a24] hover:text-[#faf6ee]"
                    >
                      Thêm vào giỏ
                    </button>
                  </div>
                </article>
              ))}
            </div>
          )}
        </div>
      </section>

      {/* Combo */}
      <section id="combo" className="mx-auto max-w-6xl px-5 py-16">
        <div className="grid gap-10 md:grid-cols-[0.8fr_1.2fr]">
          <div>
            <h2 className="font-serif text-3xl font-bold tracking-tight">Mua trọn bộ cho tiện</h2>
            <p className="mt-3 max-w-sm text-sm leading-relaxed text-[#4c5b53]">
              Đã gom sẵn toàn bộ sách giáo khoa của một khối lớp, rẻ hơn mua lẻ và đóng gói riêng
              từng bộ để không bị thiếu, nhầm.
            </p>
          </div>
          <ul className="divide-y divide-[#e3dccb] border-y border-[#e3dccb]">
            {COMBOS.map((c) => (
              <li key={c.name} className="flex items-center justify-between gap-4 py-5">
                <div>
                  <p className="font-serif text-xl font-semibold">{c.name}</p>
                  <p className="mt-1 text-sm text-[#5b6a62]">{c.count} quyển</p>
                </div>
                <div className="flex items-center gap-5">
                  <span className="font-semibold text-[#a23b2a]">{formatVnd(c.price)}</span>
                  <button
                    onClick={() => setCart((n) => n + 1)}
                    className="rounded-full bg-[#1f4d3a] px-4 py-2 text-sm font-medium text-[#faf6ee] transition hover:bg-[#173a2c]"
                  >
                    Đặt bộ này
                  </button>
                </div>
              </li>
            ))}
          </ul>
        </div>
      </section>

      {/* Cam kết */}
      <section className="border-t border-[#e3dccb] bg-[#efe8d6]">
        <div className="mx-auto grid max-w-6xl gap-8 px-5 py-12 sm:grid-cols-3">
          {[
            ['truck', 'Giao nhanh', 'Nội thành TP.HCM nhận trong 24 giờ, tỉnh khác 2–4 ngày.'],
            ['shield', 'Sách chính hãng', 'Nhập trực tiếp từ nhà xuất bản, có hóa đơn đầy đủ.'],
            ['refresh', 'Đổi trả 7 ngày', 'Sách lỗi in hoặc giao nhầm được đổi miễn phí.'],
          ].map(([icon, title, text]) => (
            <div key={title} className="flex gap-4">
              <span className="mt-0.5 text-[#1f4d3a]">
                <Icon name={icon} className="h-6 w-6" />
              </span>
              <div>
                <h3 className="font-semibold">{title}</h3>
                <p className="mt-1 text-sm leading-relaxed text-[#4c5b53]">{text}</p>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* Footer */}
      <footer id="lien-he" className="bg-[#1d2a24] text-[#cfd6cf]">
        <div className="mx-auto grid max-w-6xl gap-10 px-5 py-14 md:grid-cols-[1.4fr_1fr_1fr]">
          <div>
            <p className="font-serif text-2xl font-bold text-[#faf6ee]">
              TLCN<span className="text-[#d9a441]">.</span>
            </p>
            <p className="mt-3 max-w-xs text-sm leading-relaxed">
              Nhận thông báo khi có sách mới và ưu đãi đầu năm học.
            </p>
            <form
              onSubmit={(e) => e.preventDefault()}
              className="mt-5 flex max-w-sm overflow-hidden rounded-full border border-[#43524a]"
            >
              <input
                type="email"
                placeholder="Email của bạn"
                className="w-full bg-transparent px-4 py-2.5 text-sm text-[#faf6ee] outline-none placeholder:text-[#8a978f]"
              />
              <button className="bg-[#d9a441] px-5 text-sm font-semibold text-[#1d2a24] hover:bg-[#e4b556]">
                Đăng ký
              </button>
            </form>
          </div>

          <div className="text-sm">
            <p className="font-semibold text-[#faf6ee]">Danh mục</p>
            <ul className="mt-3 space-y-2">
              <li><a href="#sach" className="hover:text-white">Tiểu học (lớp 1–5)</a></li>
              <li><a href="#sach" className="hover:text-white">THCS (lớp 6–9)</a></li>
              <li><a href="#sach" className="hover:text-white">THPT (lớp 10–12)</a></li>
              <li><a href="#combo" className="hover:text-white">Bộ sách trọn gói</a></li>
            </ul>
          </div>

          <div className="text-sm">
            <p className="font-semibold text-[#faf6ee]">Liên hệ</p>
            <ul className="mt-3 space-y-2">
              <li>Hotline: 0900 000 000</li>
              <li>support@tlcn.vn</li>
              <li>8:00 – 20:00, mỗi ngày</li>
            </ul>
          </div>
        </div>
        <div className="border-t border-[#33423a] py-5 text-center text-xs text-[#8a978f]">
          © 2026 TLCN – Learning Platform. Đồ án tốt nghiệp.
        </div>
      </footer>
    </div>
  )
}
