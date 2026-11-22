import { useEffect, useState } from 'react'
import { Outlet } from 'react-router-dom'
import { HomeHeader, HomeFooter } from '../../features/home'
import { getProfile } from '../../features/profile/services/profileApi'
import { ACCOUNT_CHANGED } from '../../shared/services/accountEvents'

export default function PublicLayout() {
  const [user, setUser] = useState(null)

  useEffect(() => {
    let controller
    function refresh() {
      controller?.abort()
      const request = new AbortController()
      controller = request
      getProfile(request.signal).then(profile => {
        if (!request.signal.aborted) setUser(profile)
      }).catch(error => {
        if (!request.signal.aborted && error.status === 401) setUser(null)
      })
    }
    refresh()
    window.addEventListener(ACCOUNT_CHANGED, refresh)
    return () => {
      controller?.abort()
      window.removeEventListener(ACCOUNT_CHANGED, refresh)
    }
  }, [])

  return (
    <div className="learning-home">
      <HomeHeader user={user} onLogout={() => setUser(null)} />
      <Outlet context={user} />
      <HomeFooter />
    </div>
  )
}
