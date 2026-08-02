import { Button, Input } from 'antd'
import { SendOutlined } from '@ant-design/icons'
import { forwardRef } from 'react'

export const ChatInput = forwardRef(function ChatInput({ disabled, isDraft, value, onChange, onSend }, ref) {
  const submit = () => {
    const message = value.trim()
    if (!message || disabled) return
    onSend(message)
  }

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      submit()
    }
  }

  return (
    <div className="chat-input">
      <Input.TextArea
        ref={ref}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        onKeyDown={handleKeyDown}
        placeholder={isDraft ? "Ask about movies…" : "Follow up…"}
        autoSize={{ minRows: 2, maxRows: 5 }}
        disabled={disabled}
      />
      <Button
        type="primary"
        icon={<SendOutlined />}
        onClick={submit}
        loading={disabled}
        disabled={disabled || !value.trim()}
      >
        Send
      </Button>
    </div>
  )
})


