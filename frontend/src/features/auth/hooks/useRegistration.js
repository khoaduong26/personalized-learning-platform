import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getRegistrationOptions, registerAccount, verifyRegistrationOtp, resendRegistrationOtp } from '../services/authApi'
import { readPendingEmail, rememberEmail } from '../utils/registrationStorage'
import useOtpCountdown from './useOtpCountdown'
import { ROLES } from '../../../shared/constants/roles'

export default function useRegistration() {
  const navigate = useNavigate()
  const [savedEmail] = useState(readPendingEmail)
  const [step, setStep] = useState(savedEmail ? 'verify' : 'register')
  const [form, setForm] = useState({ fullName: '', email: '', password: '', confirmPassword: '', phone: '', role: ROLES.TEACHER })
  const [email, setEmail] = useState(savedEmail)
  const [otp, setOtp] = useState('')
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [fieldErrors, setFieldErrors] = useState({})
  const [resendAt, setResendAt] = useState(0)
  const [expiresAt, setExpiresAt] = useState(0)
  const [googleEnabled, setGoogleEnabled] = useState(false)

  useEffect(() => {
    document.title = 'Đăng ký tài khoản | TLCN'
    const controller = new AbortController()
    getRegistrationOptions(controller.signal)
      .then(data => setGoogleEnabled(data.googleEnabled))
      .catch(() => { /* Email registration stays available while options cannot load. */ })
    return () => controller.abort()
  }, [])

  const { remaining, expired } = useOtpCountdown(resendAt, expiresAt)
  const update = event => {
    const { name, value } = event.target
    setForm(current => ({ ...current, [name]: value }))
    setFieldErrors(current => ({ ...current, [name]: undefined }))
  }

  async function run(action) {
    setBusy(true)
    setError('')
    setMessage('')
    setFieldErrors({})
    try { await action() }
    catch (failure) {
      setError(failure.message)
      setFieldErrors(failure.fieldErrors || {})
    } finally { setBusy(false) }
  }

  function acceptPending(data) {
    setEmail(data.email)
    rememberEmail(data.email)
    setResendAt(Date.parse(data.resendAvailableAt))
    setExpiresAt(Date.parse(data.otpExpiresAt))
    setOtp('')
    setStep('verify')
    setMessage(data.message)
  }

  function register(event) {
    event.preventDefault()
    if (form.password !== form.confirmPassword) {
      setError('Mật khẩu xác nhận chưa khớp.')
      setFieldErrors({ confirmPassword: 'Nhập lại đúng mật khẩu đã chọn.' })
      return
    }
    run(async () => {
      const payload = { fullName: form.fullName, email: form.email, phone: form.phone, password: form.password, role: form.role }
      // Never persist passwords in browser storage.
      const data = await registerAccount(payload)
      setForm(current => ({ ...current, password: '', confirmPassword: '' }))
      acceptPending(data)
    })
  }

  function verify(event) {
    event.preventDefault()
    run(async () => {
      await verifyRegistrationOtp({ email, otp })
      rememberEmail('')
      navigate('/register/complete', { replace: true })
    })
  }

  function resend(event) {
    event?.preventDefault()
    run(async () => acceptPending(await resendRegistrationOtp({ email })))
  }

  function changeStep(next) {
    setStep(next)
    setError('')
    setMessage('')
    setFieldErrors({})
    setOtp('')
  }

  function restart() {
    rememberEmail('')
    changeStep('register')
  }

  return { step, form, email, setEmail, otp, setOtp, busy, message, error, fieldErrors, expiresAt, expired, remaining, googleEnabled, update, register, verify, resend, changeStep, restart }
}
