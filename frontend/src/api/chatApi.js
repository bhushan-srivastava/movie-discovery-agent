const configuredBaseUrl = import.meta.env.VITE_API_BASE_URL || '/api'
const apiBaseUrl = configuredBaseUrl.replace(/\/+$/, '').replace(/\/api$/i, '') + '/api'

export function getApiBaseUrl() {
  return apiBaseUrl
}

async function parseError(response) {
  let message = `Request failed (${response.status})`
  try {
    const body = await response.json()
    if (body.message) message = body.message
  } catch {
    // Keep the status-based message when the response is not JSON.
  }
  return new Error(message)
}

async function request(path, options) {
  const response = await fetch(`${apiBaseUrl}${path}`, {
    headers: { 'Content-Type': 'application/json', ...(options?.headers ?? {}) },
    ...options,
  })
  if (!response.ok) throw await parseError(response)
  return response.json()
}

export function listConversations() {
  return request('/conversations')
}

export function createConversation(title) {
  return request('/conversations', {
    method: 'POST',
    body: JSON.stringify(title ? { title } : {}),
  })
}

export function getMessages(conversationId) {
  return request(`/conversations/${encodeURIComponent(conversationId)}/messages`)
}

export async function sendChat(conversationId, message, signal) {
  const response = await fetch(`${apiBaseUrl}/conversations/${encodeURIComponent(conversationId)}/chat`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
    body: JSON.stringify({ message }),
    signal,
  })
  if (!response.ok) throw await parseError(response)
  return response.json()
}

