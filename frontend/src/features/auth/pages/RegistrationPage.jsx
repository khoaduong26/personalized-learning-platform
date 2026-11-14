import { Link, useSearchParams } from 'react-router-dom'
import AuthLayout from '../components/AuthLayout'
import RegistrationForm from '../components/RegistrationForm'
import OtpVerificationForm from '../components/OtpVerificationForm'
import OtpRecoveryForm from '../components/OtpRecoveryForm'
import GoogleRegistrationCompletePage from './GoogleRegistrationCompletePage'
import useRegistration from '../hooks/useRegistration'

function RegistrationFlow({ googleResult }) {
  const registration = useRegistration()
  const { step, error, message } = registration
  const googleError = googleResult === 'exists'
    ? 'Email Google này đã có tài khoản. Hãy chuyển sang đăng nhập hoặc xác minh email đang chờ.'
    : googleResult === 'error' ? 'Chưa đăng ký được bằng Google. Bạn có thể thử lại hoặc dùng email.' : ''

  return (
    <AuthLayout>
      <span className="registration-step">{step === 'register' ? 'BƯỚC 1 / 2 · TẠO TÀI KHOẢN' : 'BƯỚC 2 / 2 · XÁC MINH EMAIL'}</span>
      <h2>{step === 'register' ? 'Bắt đầu hành trình mới' : step === 'recover' ? 'Gửi lại mã xác minh' : 'Kiểm tra hộp thư nhé!'}</h2>
      <p className="registration-description">{step === 'register' ? 'Chọn vai trò và điền thông tin để tham gia TLCN.' : 'Nhập mã gồm 6 chữ số được gửi đến email đăng ký. Mã có hiệu lực trong 5 phút.'}</p>
      {(error || googleError) && <div role="alert" className="registration-notice error">{error || googleError}</div>}
      {message && <div role="status" className="registration-notice success">{message}</div>}

      {step === 'register' ? <RegistrationForm {...registration} />
        : step === 'recover' ? <OtpRecoveryForm {...registration} />
          : <OtpVerificationForm {...registration} />}
      <p className="registration-signin">Đã có tài khoản? <Link to="/login">Đăng nhập</Link></p>
    </AuthLayout>
  )
}

export default function RegistrationPage() {
  const [params] = useSearchParams()
  const googleResult = params.get('google')
  return googleResult === 'success'
    ? <GoogleRegistrationCompletePage />
    : <RegistrationFlow googleResult={googleResult} />
}
