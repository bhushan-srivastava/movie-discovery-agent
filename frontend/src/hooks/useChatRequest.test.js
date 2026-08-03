import { act, renderHook, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { useChatRequest } from './useChatRequest'

describe('useChatRequest', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
  })

  it('returns a completed assistant response', async () => {
    vi.mocked(fetch).mockResolvedValue(new Response(JSON.stringify({ message: 'Hello from the assistant' }), { status: 200 }))
    const { result } = renderHook(() => useChatRequest())

    await act(async () => {
      await result.current.sendMessage('conversation-1', 'Find a movie')
    })

    expect(result.current.assistantText).toBe('Hello from the assistant')
    expect(result.current.completed).toBe(true)
    expect(result.current.isLoading).toBe(false)
    expect(result.current.error).toBeNull()
  })

  it('exposes request errors and stops loading', async () => {
    vi.mocked(fetch).mockResolvedValue(new Response(JSON.stringify({ message: 'Request failed' }), { status: 500 }))
    const { result } = renderHook(() => useChatRequest())

    await act(async () => {
      await expect(result.current.sendMessage('conversation-1', 'Find a movie')).rejects.toThrow('Request failed')
    })

    await waitFor(() => expect(result.current.error).toBe('Request failed'))
    expect(result.current.isLoading).toBe(false)
    expect(result.current.completed).toBe(false)
  })

  it('resets the completed response', async () => {
    vi.mocked(fetch).mockResolvedValue(new Response(JSON.stringify({ message: 'Response' }), { status: 200 }))
    const { result } = renderHook(() => useChatRequest())

    await act(async () => {
      await result.current.sendMessage('conversation-1', 'Find a movie')
      result.current.reset()
    })

    expect(result.current.assistantText).toBe('')
    expect(result.current.completed).toBe(false)
    expect(result.current.isLoading).toBe(false)
  })
})



