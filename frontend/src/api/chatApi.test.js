import { describe, expect, it, vi } from 'vitest'
import { getApiBaseUrl, sendChat } from './chatApi'

describe('chatApi', () => {
  it('normalizes /api configuration without doubling the path', () => {
    expect(getApiBaseUrl()).toBe('/api')
    expect(getApiBaseUrl()).not.toContain('/api/api')
  })

  it('sends a JSON chat request and returns the completed response', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify({ message: 'A recommendation' }), {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
    })))

    await expect(sendChat('abc', 'recommend a film')).resolves.toEqual({ message: 'A recommendation' })
    expect(fetch).toHaveBeenCalledWith('/api/conversations/abc/chat', expect.objectContaining({
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
      body: JSON.stringify({ message: 'recommend a film' }),
    }))
  })
})

