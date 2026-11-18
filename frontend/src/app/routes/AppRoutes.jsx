import { Navigate, Route, Routes, useSearchParams } from 'react-router-dom'
import { LoginPage, RegistrationPage, RegistrationCompletePage } from '../../features/auth'
import { HomePage } from '../../features/home'
import ClassroomsPage from '../../features/classrooms/pages/ClassroomsPage'
import PublicLayout from '../layouts/PublicLayout'
import NotFoundPage from '../pages/NotFoundPage'
import AcademicPage from '../../features/academic/pages/AcademicPage'

function LoginRoute() {
  const [params] = useSearchParams()
  // Keep links from the previous registration flow working.
  return params.get('registered') === '1'
    ? <Navigate to="/register/complete" replace />
    : <LoginPage />
}

export default function AppRoutes() {
  return (
    <Routes>
      <Route path="classrooms" element={<ClassroomsPage />} />
      <Route element={<PublicLayout />}>
        <Route index element={<HomePage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
      <Route path="register" element={<RegistrationPage />} />
      <Route path="register/complete" element={<RegistrationCompletePage />} />
      <Route path="login" element={<LoginRoute />} />
      <Route path="academic" element={<AcademicPage />} />
    </Routes>
  )
}
