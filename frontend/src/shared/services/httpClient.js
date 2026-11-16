// Relative URLs keep requests on the frontend origin (Vite proxies /api in dev).
export async function httpRequest(path, { method = 'GET', body, signal, headers } = {}) {
  let response
  try {
    response = await fetch(path, {
      method,
      headers: { ...(body === undefined ? {} : { 'Content-Type': 'application/json' }), ...headers },
      body: body === undefined ? undefined : JSON.stringify(body),
      signal,
    })
  } catch (error) {
    if (error.name === 'AbortError') throw error
    throw new Error('Không kết nối được máy chủ. Vui lòng thử lại.', { cause: error })
  }

  const data = await response.json().catch(() => null)
  if (!response.ok) {
    const error = new Error(data?.message || 'Chưa thực hiện được yêu cầu. Vui lòng thử lại.')
    error.status = response.status
    error.code = data?.code
    error.fieldErrors = data?.fieldErrors || {}
    throw error
  }
  return data
}
