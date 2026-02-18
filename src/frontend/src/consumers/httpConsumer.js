const API_BASE = import.meta.env.VITE_API_BASE_URL ?? '/api'

export async function requestApi(path, options = {}) {
  const config = { ...options }
  const headers = { ...(config.headers ?? {}) }

  if (config.body && !headers['Content-Type']) {
    headers['Content-Type'] = 'application/json'
  }

  config.headers = headers

  const response = await fetch(`${API_BASE}${path}`, config)
  if (response.status === 204) {
    return null
  }

  const contentType = response.headers.get('content-type') ?? ''
  const payload = contentType.includes('application/json')
    ? await response.json()
    : await response.text()

  if (!response.ok) {
    const fallbackMessage = `Request failed with status ${response.status}`
    const message = typeof payload === 'string' && payload.trim() ? payload : fallbackMessage
    throw new Error(message)
  }

  return payload
}

export function toQueryString(params = {}) {
  const searchParams = new URLSearchParams()

  Object.entries(params).forEach(([key, value]) => {
    if (value === null || value === undefined) {
      return
    }

    if (typeof value === 'string') {
      const trimmed = value.trim()
      if (trimmed) {
        searchParams.set(key, trimmed)
      }
      return
    }

    searchParams.set(key, String(value))
  })

  const query = searchParams.toString()
  return query ? `?${query}` : ''
}
