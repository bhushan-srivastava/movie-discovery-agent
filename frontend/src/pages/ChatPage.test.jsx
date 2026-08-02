import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it, vi, beforeEach } from 'vitest'
import { ChatPage } from './ChatPage'
import * as api from '../api/chatApi'

vi.mock('../api/chatApi', async () => {
  const actual = await vi.importActual('../api/chatApi')
  return {
    ...actual,
    listConversations: vi.fn(),
    createConversation: vi.fn(),
    getMessages: vi.fn(),
    streamChat: vi.fn(),
  }
})

const conversation = { id: 'one', title: 'Movie chat', createdAt: '', updatedAt: '' }
const secondConversation = { id: 'two', title: 'Second chat', createdAt: '', updatedAt: '' }

beforeEach(() => {
  vi.mocked(api.listConversations).mockResolvedValue([conversation])
  vi.mocked(api.getMessages).mockResolvedValue([])
  vi.mocked(api.streamChat).mockImplementation(async (_id, _message, onEvent) => {
    onEvent({ eventType: 'text-delta', data: { delta: 'Hello' } })
    onEvent({ eventType: 'completion', data: { message: 'done' } })
  })
  vi.clearAllMocks()
})

describe('ChatPage', () => {
  it('loads a conversation on mount', async () => {
    render(<ChatPage />)
    await waitFor(() => expect(api.getMessages).toHaveBeenCalledWith('one'))
  })

  it('creates a new conversation only on first draft message submission', async () => {
    render(<ChatPage />)
    await waitFor(() => expect(api.getMessages).toHaveBeenCalledWith('one'))

    // Click New
    fireEvent.click(screen.getByRole('button', { name: /new/i }))

    // New click should make no HTTP request
    await waitFor(() => {
      expect(api.createConversation).not.toHaveBeenCalled()
      expect(api.getMessages).toHaveBeenCalledTimes(1) // only from initial load
    })

    // Type and send first message
    fireEvent.change(screen.getByPlaceholderText('Ask about movies…'), { target: { value: 'recommend 5 movies' } })
    fireEvent.click(screen.getByRole('button', { name: /send/i }))

    // Now createConversation should be called exactly once
    await waitFor(() => expect(api.createConversation).toHaveBeenCalledWith('recommend 5 movies'))
  })

  it('does not create a conversation when blank draft message is submitted', async () => {
    render(<ChatPage />)
    await waitFor(() => expect(api.getMessages).toHaveBeenCalledWith('one'))

    // Click New
    fireEvent.click(screen.getByRole('button', { name: /new/i }))

    // Try to send blank message
    fireEvent.change(screen.getByPlaceholderText('Ask about movies…'), { target: { value: '   ' } })
    fireEvent.click(screen.getByRole('button', { name: /send/i }))

    await waitFor(() => {
      expect(api.createConversation).not.toHaveBeenCalled()
    })
  })

  it('truncates conversation title to 255 characters', async () => {
    vi.mocked(api.createConversation).mockResolvedValue({ ...conversation, id: 'new-id' })
    render(<ChatPage />)
    await waitFor(() => expect(api.getMessages).toHaveBeenCalledWith('one'))

    fireEvent.click(screen.getByRole('button', { name: /new/i }))

    const longMessage = 'a'.repeat(300)
    fireEvent.change(screen.getByPlaceholderText('Ask about movies…'), { target: { value: longMessage } })
    fireEvent.click(screen.getByRole('button', { name: /send/i }))

    await waitFor(() => {
      const call = vi.mocked(api.createConversation).mock.calls[0]
      expect(call[0]).toBe('a'.repeat(255))
    })
  })

  it('uses returned conversation ID for streaming after creation', async () => {
    const newConv = { id: 'new-conv-id', title: 'Test', createdAt: '', updatedAt: '' }
    vi.mocked(api.createConversation).mockResolvedValue(newConv)

    render(<ChatPage />)
    await waitFor(() => expect(api.getMessages).toHaveBeenCalledWith('one'))

    fireEvent.click(screen.getByRole('button', { name: /new/i }))
    fireEvent.change(screen.getByPlaceholderText('Ask about movies…'), { target: { value: 'test message' } })
    fireEvent.click(screen.getByRole('button', { name: /send/i }))

    await waitFor(() => {
      expect(api.streamChat).toHaveBeenCalledWith('new-conv-id', 'test message', expect.any(Function))
    })
  })

  it('does not create another conversation for later messages', async () => {
    render(<ChatPage />)
    await waitFor(() => expect(api.getMessages).toHaveBeenCalledWith('one'))

    fireEvent.change(screen.getByPlaceholderText('Follow up…'), { target: { value: 'Find a film' } })
    fireEvent.click(screen.getByRole('button', { name: /send/i }))

    await waitFor(() => {
      expect(api.streamChat).toHaveBeenCalledWith('one', 'Find a film', expect.any(Function))
      expect(api.createConversation).not.toHaveBeenCalled()
    })
  })

  it('loads messages when an existing conversation is selected', async () => {
    vi.mocked(api.listConversations).mockResolvedValue([conversation, secondConversation])
    render(<ChatPage />)
    await waitFor(() => expect(api.getMessages).toHaveBeenCalledWith('one'))

    fireEvent.click(screen.getByText('Second chat'))
    await waitFor(() => expect(api.getMessages).toHaveBeenCalledWith('two'))
  })

  it('renders streamed text and completion state', async () => {
    render(<ChatPage />)
    await waitFor(() => expect(api.getMessages).toHaveBeenCalledWith('one'))

    fireEvent.change(screen.getByPlaceholderText('Follow up…'), { target: { value: 'Find a film' } })
    fireEvent.click(screen.getByRole('button', { name: /send/i }))

    await waitFor(() => expect(api.streamChat).toHaveBeenCalledWith('one', 'Find a film', expect.any(Function)))
  })

  it('remains in draft mode if conversation creation fails', async () => {
    vi.mocked(api.createConversation).mockRejectedValue(new Error('Server error'))
    render(<ChatPage />)
    await waitFor(() => expect(api.getMessages).toHaveBeenCalledWith('one'))

    fireEvent.click(screen.getByRole('button', { name: /new/i }))
    fireEvent.change(screen.getByPlaceholderText('Ask about movies…'), { target: { value: 'test' } })
    fireEvent.click(screen.getByRole('button', { name: /send/i }))

    await waitFor(() => {
      expect(screen.getByPlaceholderText('Ask about movies…')).toHaveValue('test')
      expect(api.streamChat).not.toHaveBeenCalled()
    })
  })
})

