import { Avatar, Empty, Spin, List } from 'antd'
import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'

// Custom components for react-markdown
const components = {
  // Disable links: render link text without href
  a: ({ children }) => {
    return <span>{children}</span>
  },
  // Disable images
  img: () => null,
  // Disable iframes
  iframe: () => null,
}

export function ChatTranscript({ messages, streamingText, isStreaming }) {
  // Only show streaming message if actively streaming AND text is being accumulated
  const hasStreamingMessage = isStreaming && streamingText.length > 0

  if (!messages.length && !hasStreamingMessage) {
    return <Empty className="transcript-empty" description="Ask for a movie recommendation to get started." />
  }

  return (
    <div className="transcript" aria-live="polite" style={{scrollbarWidth: 'none', overflow: 'auto', msOverflowStyle: 'none'}}>
      <List
        dataSource={messages}
        renderItem={(message) => {
          const isUser = message.role.toUpperCase() === 'USER'

          if (isUser) {
            return (
              <List.Item className="message-row user-row">
                <div className="message-content">
                  <div className="message-role">You</div>
                  <div className="message-bubble user-bubble">
                    {message.content}
                  </div>
                </div>
                <Avatar>{message.role.charAt(0)}</Avatar>
              </List.Item>
            )
          }

          return (
            <List.Item className="message-row assistant-row">
              <Avatar>AI</Avatar>
              <div className="message-content">
                <div className="message-role">Assistant</div>
                <div className="message-bubble assistant-bubble">
                  <ReactMarkdown remarkPlugins={[remarkGfm]} components={components}>
                    {message.content}
                  </ReactMarkdown>
                </div>
              </div>
            </List.Item>
          )
        }}
      />
      {hasStreamingMessage && (
        <div className="message-row assistant-row streaming-message">
          <Avatar>AI</Avatar>
          <div className="message-content">
            <div className="message-role">
              Assistant
              <Spin size="small" style={{ marginLeft: '8px' }} />
            </div>
            <div className="message-bubble assistant-bubble">
              <ReactMarkdown remarkPlugins={[remarkGfm]} components={components}>
                {streamingText || ''}
              </ReactMarkdown>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

