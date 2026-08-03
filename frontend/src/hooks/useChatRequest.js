import { useCallback, useState } from 'react'
import { sendChat } from '../api/chatApi'

const initialState = {
  assistantText: '',
  isLoading: false,
  completed: false,
  error: null,
}

export function useChatRequest() {
  const [state, setState] = useState(initialState)

  const sendMessage = useCallback(async (conversationId, message) => {
    setState({ ...initialState, isLoading: true })
    try {
      const response = await sendChat(conversationId, message)
      setState({ ...initialState, assistantText: response.message ?? '', completed: true })
      return response
    } catch (error) {
      setState((current) => ({
        ...current,
        isLoading: false,
        error:
          error instanceof Error
            ? error.message
            : 'The assistant request failed.',
      }))
      throw error
    }
  }, [])

  const reset = useCallback(() => setState(initialState), [])
  return { ...state, sendMessage, reset }
}

