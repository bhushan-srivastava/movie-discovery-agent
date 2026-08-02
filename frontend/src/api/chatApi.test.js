import { describe, expect, it, vi } from 'vitest'
import { getApiBaseUrl, streamChat } from './chatApi'

function responseFromChunks(chunks) {
  const encoder = new TextEncoder()
  const stream = new ReadableStream({
    start(controller) { chunks.forEach((chunk) => controller.enqueue(encoder.encode(chunk))); controller.close() },
  })
  return new Response(stream, { status: 200 })
}

describe('chatApi', () => {
  it('normalizes /api configuration without doubling the path', () => {
    expect(getApiBaseUrl()).toBe('/api')
    expect(getApiBaseUrl()).not.toContain('/api/api')
  })

  it('buffers incomplete NDJSON chunks and emits complete events in order', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(responseFromChunks([
      '{"eventType":"text-delta","data":{"delta":"Hel',
      'lo"}}\n{"eventType":"completion","data":{"message":"done"}}\n',
    ])))
    const events = []
    await streamChat('abc', 'recommend a film', (event) => events.push(event.eventType))
    expect(events).toEqual(['text-delta', 'completion'])
    expect(fetch).toHaveBeenCalledWith('/api/conversations/abc/chat/stream', expect.objectContaining({ method: 'POST' }))
  })

  it('handles multiple events and an incomplete final line', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(responseFromChunks([
      '{"eventType":"text-delta","data":{"delta":"A"}}\n{"eventType":"text-delta","data":{"delta":"B"}}\n{"eventType":"completion","data":{"message":"done"}}',
    ])))
    const deltas = []
    await streamChat('abc', 'hello', (event) => { if (event.eventType === 'text-delta') deltas.push(String(event.data?.delta)) })
    expect(deltas).toEqual(['A', 'B'])
  })
})

