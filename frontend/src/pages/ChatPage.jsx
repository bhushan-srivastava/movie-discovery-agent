import { Layout, message as notification } from 'antd'
import { useCallback, useEffect, useRef, useState } from 'react'
import { ConversationSidebar } from '../components/ConversationSidebar'
import { SharedWarningBanner } from '../components/SharedWarningBanner'
import { ChatTranscript } from '../components/ChatTranscript'
import { ChatInput } from '../components/ChatInput'
import { ToolStatusPanel } from '../components/ToolStatusPanel'
import { createConversation, getMessages, listConversations, streamChat } from '../api/chatApi'
import { useChatStream } from '../hooks/useChatStream'

export function ChatPage() {
  const [conversations, setConversations] = useState([])
  const [selectedId, setSelectedId] = useState(null)
  const [messages, setMessages] = useState([])
  const [loadingConversations, setLoadingConversations] = useState(true)
  const [loadingMessages, setLoadingMessages] = useState(false)
  const [pageError, setPageError] = useState(null)
  const [isDraft, setIsDraft] = useState(false)
  const [inputValue, setInputValue] = useState('')
  const stream = useChatStream()
  const { reset: resetStream } = stream
  const inputRef = useRef(null)

  const loadConversations = useCallback(async (selectFirst = true) => {
    setLoadingConversations(true)
    try {
      const loaded = await listConversations()
      setConversations(loaded)
      if (selectFirst && loaded.length && !selectedId) setSelectedId(loaded[0].id)
    } catch (error) {
      setPageError(error instanceof Error ? error.message : 'Unable to load conversations.')
    } finally {
      setLoadingConversations(false)
    }
  }, [selectedId])

  useEffect(() => {
    void loadConversations()
  }, [loadConversations])

  useEffect(() => {
    if (!selectedId) {
      setMessages([])
      return
    }
    if (isDraft) {
      setMessages([])
      return
    }
    setLoadingMessages(true)
    resetStream()
    getMessages(selectedId)
      .then(setMessages)
      .catch((error) => setPageError(error instanceof Error ? error.message : 'Unable to load messages.'))
      .finally(() => setLoadingMessages(false))
  }, [selectedId, isDraft, resetStream])

  const handleClickNew = () => {
    setSelectedId(null)
    setIsDraft(true)
    setMessages([])
    resetStream()
    setInputValue('')
    setPageError(null)
    // Focus the input
    setTimeout(() => inputRef.current?.focus(), 0)
  }

  const handleSend = async (content) => {
    // Draft mode: create conversation first
    if (isDraft) {
      const trimmed = content.trim()
      if (!trimmed) {
        notification.error({ content: 'Message is empty.' })
        return
      }

      try {
        // Create conversation with trimmed message as title (max 255 chars)
        const title = trimmed.substring(0, 255)
        const newConv = await createConversation(title)

        setSelectedId(newConv.id)
        setIsDraft(false)
        setInputValue('')
        await loadConversations(false)

        // Now stream the message
        await stream.startStream(newConv.id, trimmed)
        setMessages(await getMessages(newConv.id))
        stream.reset()
      } catch (error) {
        // Stay in draft mode, preserve message, show error
        setInputValue(trimmed)
        setPageError(error instanceof Error ? error.message : 'Failed to create conversation.')
        notification.error({ content: error instanceof Error ? error.message : 'Failed to create conversation.' })
      }
      return
    }

    // Normal mode: send to existing conversation
    if (!selectedId) {
      notification.info({ content: 'Create a conversation before sending a message.' })
      return
    }

    // Add optimistic user message
    const optimistic = {
      id: `local-${Date.now()}`,
      conversationId: selectedId,
      role: 'USER',
      content,
      createdAt: new Date().toISOString(),
    }
    setMessages((current) => [...current, optimistic])
    setInputValue('')

    try {
      // Stream the message
      await stream.startStream(selectedId, content)
      // Reload persisted messages
      setMessages(await getMessages(selectedId))
      stream.reset()
      void loadConversations(false)
    } catch (error) {
      // If streaming fails, show error but keep the created conversation
      setPageError(error instanceof Error ? error.message : 'Failed to send message.')
      notification.error({ content: error instanceof Error ? error.message : 'Failed to send message.' })
    }
  }

  const handleSelectConversation = (id) => {
    setSelectedId(id)
    setIsDraft(false)
    setInputValue('')
    setPageError(null)
  }

  return (
    <Layout className="app-shell" style={{ height: '100vh', overflow: 'hidden' }}>
      <Layout.Sider width={280} theme="light">
        <ConversationSidebar
          conversations={conversations}
          selectedId={isDraft ? null : selectedId}
          isDraft={isDraft}
          loading={loadingConversations}
          onSelect={handleSelectConversation}
          onClickNew={handleClickNew}
        />
      </Layout.Sider>
      <Layout.Content className="content-area">
        <SharedWarningBanner />
        <main className="chat-panel">
          {pageError && <ToolStatusPanel isStreaming={false} activeTool={null} error={pageError} />}
          {loadingMessages ? (
            <div className="loading-panel">Loading conversation…</div>
          ) : (
            <ChatTranscript messages={messages} streamingText={stream.assistantText} isStreaming={stream.isStreaming} />
          )}
          <ToolStatusPanel isStreaming={stream.isStreaming} activeTool={stream.activeTool} error={stream.error} />
          <ChatInput
            ref={inputRef}
            disabled={stream.isStreaming}
            isDraft={isDraft}
            value={inputValue}
            onChange={setInputValue}
            onSend={handleSend}
          />
        </main>
      </Layout.Content>
    </Layout>
  )
}


