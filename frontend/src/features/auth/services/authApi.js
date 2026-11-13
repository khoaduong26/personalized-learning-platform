import { httpRequest } from '../../../shared/services/httpClient'

const authPath = '/api/auth'

export const getRegistrationOptions = signal => httpRequest(`${authPath}/options`, { signal })
export const getRegistrationSession = signal => httpRequest(`${authPath}/registration-session`, { signal })
export const registerAccount = body => httpRequest(`${authPath}/register`, { method: 'POST', body })
export const verifyRegistrationOtp = body => httpRequest(`${authPath}/verify-otp`, { method: 'POST', body })
export const resendRegistrationOtp = body => httpRequest(`${authPath}/resend-otp`, { method: 'POST', body })
