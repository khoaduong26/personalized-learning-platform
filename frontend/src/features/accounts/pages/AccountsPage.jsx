import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import HomeHeader from '../../home/components/HomeHeader'
import { getCurrentUser } from '../../auth/services/authApi.js'
import { initials, roleNames } from '../../profile/utils/profile.js'
import AccountDialog from '../components/AccountDialog.jsx'
import { createAccount, listAccounts, setAccountLocked, updateAccount } from '../services/accountApi.js'
import '../styles/accounts.css'

const statuses = { ACTIVE: 'Hoạt động', LOCKED: 'Đã khóa', PENDING_OTP: 'Chờ xác minh' }
const defaultFilters = { keyword: '', role: '', status: '', page: 0 }
const dateFormat = new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeZone: 'Asia/Ho_Chi_Minh' })
const loginPath = '/login?returnTo=/admin/accounts'

export default function AccountsPage() {
  const [session, setSession] = useState({ state: 'loading' })
  const [sessionRevision, setSessionRevision] = useState(0)
  const [keyword, setKeyword] = useState('')
  const [filters, setFilters] = useState(defaultFilters)
  const [listRevision, setListRevision] = useState(0)
  const [result, setResult] = useState(null)
  const [dialog, setDialog] = useState(null)
  const [pending, setPending] = useState(false)
  const [notice, setNotice] = useState('')
  const allowed = session.state === 'allowed'
  const listKey = JSON.stringify([filters, listRevision])
  const current = result?.key === listKey ? result : null

  useEffect(() => {
    document.title = 'Quản lý tài khoản · TLCN'
    const controller = new AbortController()
    getCurrentUser(controller.signal).then(user => {
      if (!controller.signal.aborted) setSession({ state: user?.role === 'ADMIN' ? 'allowed' : 'forbidden', user })
    }).catch(error => {
      if (!controller.signal.aborted) setSession({
        state: error.status === 401 ? 'signed-out' : error.status === 403 ? 'forbidden' : 'error',
        message: error.message,
      })
    })
    return () => controller.abort()
  }, [sessionRevision])

  useEffect(() => {
    if (!allowed) return
    const controller = new AbortController()
    listAccounts(filters, controller.signal).then(data => {
      if (controller.signal.aborted) return
      const lastPage = Math.max(0, data.totalPages - 1)
      if (filters.page > lastPage) setFilters(previous => ({ ...previous, page: lastPage }))
      else setResult({ key: listKey, data })
    }).catch(error => {
      if (controller.signal.aborted) return
      if (error.status === 401 || error.status === 403) {
        setSession({ state: error.status === 401 ? 'signed-out' : 'forbidden' })
      } else setResult({ key: listKey, error: error.message })
    })
    return () => controller.abort()
  }, [allowed, filters, listKey])

  function search(event) {
    event.preventDefault()
    setFilters(previous => ({ ...previous, keyword: keyword.trim(), page: 0 }))
  }

  async function save(value) {
    setPending(true)
    setNotice('')
    try {
      if (dialog.kind === 'create') {
        await createAccount(value)
        setNotice('Đã tạo tài khoản.')
        setFilters(previous => ({ ...previous, page: 0 }))
      } else if (dialog.kind === 'edit') {
        await updateAccount(dialog.account.id, value)
        setNotice('Đã cập nhật tài khoản.')
      } else {
        await setAccountLocked(dialog.account.id, value)
        setNotice(value ? 'Đã khóa tài khoản.' : 'Đã mở khóa tài khoản.')
      }
      setDialog(null)
      setListRevision(previous => previous + 1)
    } catch (error) {
      if (error.status === 401 || error.status === 403) {
        setDialog(null)
        setSession({ state: error.status === 401 ? 'signed-out' : 'forbidden' })
      } else throw error
    } finally {
      setPending(false)
    }
  }

  if (!allowed) {
    return <main className="accounts-shell accounts-gate-shell">
      <section className="accounts-gate">
        {session.state === 'loading' ? <p role="status">Đang kiểm tra phiên đăng nhập…</p> : <>
          <h1>{session.state === 'signed-out' ? 'Đăng nhập để quản lý tài khoản'
            : session.state === 'forbidden' ? 'Trang dành cho quản trị viên' : 'Chưa kết nối được'}</h1>
          {notice && <p role="status">{notice}</p>}
          <p>{session.message || (session.state === 'signed-out'
            ? 'Đăng nhập bằng tài khoản quản trị viên để tiếp tục.'
            : 'Tài khoản hiện tại không có quyền quản lý người dùng.')}</p>
          <div className="accounts-gate-actions">
            {session.state === 'signed-out' && <Link className="accounts-button accounts-primary" to={loginPath}>Đăng nhập</Link>}
            {session.state === 'error' && <button type="button" className="accounts-button accounts-primary"
              onClick={() => { setSession({ state: 'loading' }); setSessionRevision(value => value + 1) }}>Thử lại</button>}
            <Link className="accounts-button accounts-secondary" to="/">Về trang chủ</Link>
          </div>
        </>}
      </section>
    </main>
  }

  const admin = session.user
  const data = current?.data
  const filtered = Boolean(filters.keyword || filters.role || filters.status)
  const goToPage = page => setFilters(previous => ({ ...previous, page }))
  return (
    <div className="accounts-shell">
      <HomeHeader user={admin} onLogout={() => setSession({ state: 'signed-out' })} />

      <main id="noi-dung" tabIndex={-1} className="accounts-content">
        <div className="accounts-heading">
          <div>
            <h1>Quản lý tài khoản</h1>
            <p>Cập nhật thông tin người dùng, phân quyền và quản lý trạng thái đăng nhập.</p>
          </div>
          <button type="button" className="accounts-button accounts-primary" onClick={() => setDialog({ kind: 'create' })}>
            <span aria-hidden="true">＋</span> Thêm tài khoản
          </button>
        </div>

        {notice && <p className="accounts-notice" role="status">{notice}</p>}

        <section className="accounts-panel" aria-labelledby="accounts-list-title" aria-busy={!current}>
          <div className="accounts-panel-head">
            <h2 id="accounts-list-title">Danh sách tài khoản</h2>
            {data && <span className="accounts-count">{data.totalElements.toLocaleString('vi-VN')} tài khoản</span>}
          </div>
          <div className="accounts-toolbar">
            <form className="accounts-search" onSubmit={search} role="search">
              <label className="accounts-sr-only" htmlFor="accounts-keyword">Tìm theo họ và tên hoặc email</label>
              <input id="accounts-keyword" type="search" placeholder="Tìm theo họ và tên hoặc email" maxLength={190}
                value={keyword} onChange={event => setKeyword(event.target.value)} />
              <button type="submit" className="accounts-button accounts-secondary">Tìm</button>
            </form>
            <div className="accounts-filters">
              <select aria-label="Lọc theo vai trò" value={filters.role}
                onChange={event => setFilters(previous => ({ ...previous, role: event.target.value, page: 0 }))}>
                <option value="">Tất cả vai trò</option>
                {Object.entries(roleNames).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
              </select>
              <select aria-label="Lọc theo trạng thái" value={filters.status}
                onChange={event => setFilters(previous => ({ ...previous, status: event.target.value, page: 0 }))}>
                <option value="">Tất cả trạng thái</option>
                {Object.entries(statuses).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
              </select>
              {filtered && <button type="button" className="accounts-reset"
                onClick={() => { setKeyword(''); setFilters(defaultFilters) }}>Xóa bộ lọc</button>}
            </div>
          </div>

          {!current && <p className="accounts-state" role="status">Đang tải danh sách tài khoản…</p>}
          {current?.error && <div className="accounts-state">
            <p role="alert">{current.error}</p>
            <button type="button" className="accounts-button accounts-secondary" onClick={() => setListRevision(value => value + 1)}>Thử lại</button>
          </div>}
          {data && data.items.length === 0 && <p className="accounts-state" role="status">
            {filtered ? 'Không tìm thấy tài khoản phù hợp.' : 'Chưa có tài khoản.'}
          </p>}
          {data && data.items.length > 0 && <>
            <div className="accounts-table-wrap" tabIndex={0} role="region" aria-label="Bảng tài khoản">
              <table className="accounts-table" role="table">
                <thead role="rowgroup"><tr role="row"><th scope="col" role="columnheader">Người dùng</th><th scope="col" role="columnheader">Vai trò</th><th scope="col" role="columnheader">Trạng thái</th>
                  <th scope="col" role="columnheader">Ngày tạo</th><th scope="col" role="columnheader">Thao tác</th></tr></thead>
                <tbody role="rowgroup">
                  {data.items.map(account => {
                    const ownAccount = account.id === admin.id
                    const locked = account.status === 'LOCKED'
                    return <tr key={account.id} role="row">
                      <th scope="row" role="rowheader">
                        <div className="accounts-identity">
                          <span className={`accounts-avatar accounts-role-${account.role.toLowerCase()}`} aria-hidden="true">{initials(account.fullName)}</span>
                          <span><strong>{account.fullName}{ownAccount && <span className="accounts-own">Bạn</span>}</strong><small>{account.email}</small></span>
                        </div>
                      </th>
                      <td role="cell"><span className={`accounts-role accounts-role-${account.role.toLowerCase()}`}>{roleNames[account.role]}</span></td>
                      <td role="cell" className="accounts-status-cell"><span className={`accounts-status accounts-status-${account.status.toLowerCase()}`}>{statuses[account.status]}</span></td>
                      <td role="cell" className="accounts-date"><span aria-hidden="true">Ngày tạo</span>{account.createdAt ? dateFormat.format(new Date(account.createdAt)) : 'Chưa ghi nhận'}</td>
                      <td role="cell"><div className="accounts-row-actions">
                        <button type="button" onClick={() => setDialog({ kind: 'edit', account })} aria-label={`Sửa tài khoản ${account.fullName}`}>Chỉnh sửa</button>
                        {!ownAccount && <button type="button" className={locked ? '' : 'accounts-lock'} onClick={() => setDialog({ kind: 'status', account })}
                          aria-label={`${locked ? 'Mở khóa' : 'Khóa'} tài khoản ${account.fullName}`}>{locked ? 'Mở khóa' : 'Khóa'}</button>}
                      </div></td>
                    </tr>
                  })}
                </tbody>
              </table>
            </div>
            <div className="accounts-pagination">
              <span>{data.page * data.size + 1}–{data.page * data.size + data.items.length} / {data.totalElements.toLocaleString('vi-VN')} tài khoản</span>
              {data.totalPages > 1 && <nav aria-label="Phân trang danh sách tài khoản">
                <button type="button" className="accounts-button accounts-secondary" disabled={data.page === 0} onClick={() => goToPage(data.page - 1)}>Trang trước</button>
                <span>Trang {data.page + 1}/{data.totalPages}</span>
                <button type="button" className="accounts-button accounts-secondary" disabled={data.page + 1 === data.totalPages} onClick={() => goToPage(data.page + 1)}>Trang sau</button>
              </nav>}
            </div>
          </>}
        </section>
      </main>
      {dialog && <AccountDialog dialog={dialog} adminId={admin.id} pending={pending} onClose={() => setDialog(null)} onSubmit={save} />}
    </div>
  )
}
