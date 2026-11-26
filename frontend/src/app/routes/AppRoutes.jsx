import { Navigate, Route, Routes, useSearchParams } from 'react-router-dom'
import { LoginPage, RegistrationPage, RegistrationCompletePage } from '../../features/auth'
import { HomePage } from '../../features/home'
import AcademicPage from '../../features/academic/pages/AcademicPage'
import AccountsPage from '../../features/accounts/pages/AccountsPage'
import AssessmentsPage from '../../features/assessments/pages/AssessmentsPage'
import ClassroomsPage from '../../features/classrooms/pages/ClassroomsPage'
import PublicLayout from '../layouts/PublicLayout'
import NotFoundPage from '../pages/NotFoundPage'
import ProfilePage from '../../features/profile/pages/ProfilePage'
import StudentLearningPathPage from '../../features/learningpath/pages/StudentLearningPathPage'
import ExercisesPage from '../../features/exercises/pages/ExercisesPage'
import AdminDashboardPage from '../../features/admin/pages/AdminDashboardPage'
import { NotificationProvider } from '../../shared/components/notifications/NotificationContext'

function LoginRoute() {
  const [params] = useSearchParams()
  // Keep links from the previous registration flow working.
  return params.get('registered') === '1'
    ? <Navigate to="/register/complete" replace />
    : <LoginPage />
}

export default function AppRoutes() {
  return (
    <NotificationProvider><Routes>
      <Route path="classrooms" element={<ClassroomsPage />} />
      <Route path="admin" element={<AdminDashboardPage />} />
      <Route element={<PublicLayout />}>
        <Route index element={<HomePage />} />
        <Route path="profile" element={<ProfilePage key="profile" />} />
        <Route path="profile/teacher" element={<ProfilePage key="teacher-profile" teacherOnly />} />
        <Route path="profile/student" element={<ProfilePage key="student-profile" />} />
        <Route path="teacher/profile" element={<Navigate to="/profile/teacher" replace />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
      <Route path="register" element={<RegistrationPage />} />
      <Route path="register/complete" element={<RegistrationCompletePage />} />
      <Route path="login" element={<LoginRoute />} />
      <Route path="academic" element={<AcademicPage />} />
      <Route path="admin/accounts" element={<AccountsPage />} />
      <Route path="assessments" element={<AssessmentsPage />} />
      <Route path="my-learning-path" element={<StudentLearningPathPage />} />
      <Route path="exercises/review" element={<ExercisesPage />} />
      <Route path="exercises/steps/:stepId" element={<ExercisesPage />} />
    </Routes></NotificationProvider>
  )
}
