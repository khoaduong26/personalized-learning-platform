import { Outlet } from 'react-router-dom'
import { HomeHeader, HomeFooter } from '../../features/home'

export default function PublicLayout() {
  return (
    <div className="learning-home">
      <HomeHeader />
      <Outlet />
      <HomeFooter />
    </div>
  )
}
