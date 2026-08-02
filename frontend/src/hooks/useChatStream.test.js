import { act, renderHook, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { useChatStream } from './useChatStream'

function streamResponse(chunks, status = 200, body) {
  if (status !== 200) {
    return new Response(JSON.stringify(body ?? { message: `Request failed (${status})` }), {
      status,
      headers: { 'Content-Type': 'application/json' },
    })
  }

  const encoder = new TextEncoder()
  const stream = new ReadableStream({
    start(controller) {
      chunks.forEach((chunk) => controller.enqueue(encoder.encode(chunk)))
      controller.close()
    },
  })
  return new Response(stream, { status, headers: { 'Content-Type': 'application/x-ndjson' } })
}

async function start(result, conversationId = 'conversation-1') {
  await act(async () => {
    await result.current.startStream(conversationId, 'Find a movie')
  })
}

describe('useChatStream', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
  })

  it('handles one complete NDJSON event', async () => {
    vi.mocked(fetch).mockResolvedValue(streamResponse(['{"eventType":"text-delta","data":{"delta":"Hello"}}\n']))
    const { result } = renderHook(() => useChatStream())

    await start(result)

    expect(result.current.assistantText).toBe('Hello')
    expect(result.current.error).toBeNull()
  })

  it('handles one event split across multiple chunks', async () => {
    vi.mocked(fetch).mockResolvedValue(streamResponse([
      '{"eventType":"text-delta","data":{"del',
      'ta":"Split"}}\n',
    ]))
    const { result } = renderHook(() => useChatStream())

    await start(result)

    expect(result.current.assistantText).toBe('Split')
  })

  it('handles multiple events in one chunk', async () => {
    vi.mocked(fetch).mockResolvedValue(streamResponse([
      '{"eventType":"text-delta","data":{"delta":"One"}}\n{"eventType":"completion","data":{"message":"done"}}\n',
    ]))
    const { result } = renderHook(() => useChatStream())

    await start(result)

    expect(result.current.assistantText).toBe('One')
    expect(result.current.completed).toBe(true)
  })

  it('appends multiple text-delta events incrementally', async () => {
    vi.mocked(fetch).mockResolvedValue(streamResponse([
      '{"eventType":"text-delta","data":{"delta":"One"}}\n{"eventType":"text-delta","data":{"delta":" two"}}\n',
    ]))
    const { result } = renderHook(() => useChatStream())

    await start(result)

    expect(result.current.assistantText).toBe('One two')
  })

  it('handles a completion event', async () => {
    vi.mocked(fetch).mockResolvedValue(streamResponse(['{"eventType":"completion","data":{"message":"done"}}\n']))
    const { result } = renderHook(() => useChatStream())

    await start(result)

    expect(result.current.completed).toBe(true)
    expect(result.current.isStreaming).toBe(false)
    expect(result.current.error).toBeNull()
  })

  it('handles an error event', async () => {
    vi.mocked(fetch).mockResolvedValue(streamResponse(['{"eventType":"error","data":{"message":"stream failed"}}\n']))
    const { result } = renderHook(() => useChatStream())

    await start(result)

    expect(result.current.error).toBe('stream failed')
    expect(result.current.isStreaming).toBe(false)
    expect(result.current.completed).toBe(false)
  })

  it.each([400, 404, 504])('handles HTTP %i from the chat endpoint', async (status) => {
    vi.mocked(fetch).mockResolvedValue(streamResponse([], status, { message: `HTTP ${status} response` }))
    const { result } = renderHook(() => useChatStream())

    await start(result)

    expect(result.current.error).toBe(`HTTP ${status} response`)
    expect(result.current.isStreaming).toBe(false)
    expect(result.current.assistantText).toBe('')
  })

  it('handles incomplete final data safely', async () => {
    vi.mocked(fetch).mockResolvedValue(streamResponse(['{"eventType":"text-delta","data":{"delta":"Final fragment"}}']))
    const { result } = renderHook(() => useChatStream())

    await start(result)

    expect(result.current.assistantText).toBe('Final fragment')
    expect(result.current.error).toBeNull()
  })

  it('handles optional tool events without inventing activity when absent', async () => {
    vi.mocked(fetch).mockResolvedValue(streamResponse([
      '{"eventType":"tool-start","data":{"toolName":"search_movies"}}\n{"eventType":"tool-result","data":{"count":1}}\n{"eventType":"text-delta","data":{"delta":"Result"}}\n',
    ]))
    const { result } = renderHook(() => useChatStream())

    await start(result)

    expect(result.current.assistantText).toBe('Result')
    expect(result.current.activeTool).toBeNull()

    vi.mocked(fetch).mockResolvedValue(streamResponse(['{"eventType":"text-delta","data":{"delta":"No tool"}}\n']))
    await act(async () => { result.current.reset() })
    await start(result)
    await waitFor(() => expect(result.current.assistantText).toBe('No tool'))
    expect(result.current.activeTool).toBeNull()
  })

  it('reset() clears assistantText after streaming completes', async () => {
    vi.mocked(fetch).mockResolvedValue(streamResponse([
      '{"eventType":"text-delta","data":{"delta":"Response"}}\n{"eventType":"completion","data":{"message":"done"}}\n',
    ]))
    const { result } = renderHook(() => useChatStream())

    await start(result)

    expect(result.current.assistantText).toBe('Response')
    expect(result.current.completed).toBe(true)

    // After reset, assistantText should be empty (prevents duplicate display)
    await act(async () => { result.current.reset() })

    expect(result.current.assistantText).toBe('')
    expect(result.current.completed).toBe(false)
    expect(result.current.isStreaming).toBe(false)
  })

  it('second request starts with empty assistantText', async () => {
    vi.mocked(fetch).mockResolvedValue(streamResponse([
      '{"eventType":"text-delta","data":{"delta":"First"}}\n{"eventType":"completion","data":{"message":"done"}}\n',
    ]))
    const { result } = renderHook(() => useChatStream())

    await start(result)
    expect(result.current.assistantText).toBe('First')

    // Reset simulates ChatPage calling reset() after persisted reload
    await act(async () => { result.current.reset() })
    expect(result.current.assistantText).toBe('')

    // Second request with different response
    vi.mocked(fetch).mockResolvedValue(streamResponse([
      '{"eventType":"text-delta","data":{"delta":"Second"}}\n{"eventType":"completion","data":{"message":"done"}}\n',
    ]))
    await start(result)

    expect(result.current.assistantText).toBe('Second')
  })
})

