import { Button, Empty, List, Spin, Typography, Tooltip } from 'antd'
import { PlusOutlined } from '@ant-design/icons'

export function ConversationSidebar({ conversations, selectedId, isDraft, loading, onSelect, onClickNew }) {
  return (
    <aside className="conversation-sidebar">
      <div className="sidebar-heading">
        <Typography.Title level={4}>Conversations</Typography.Title>
        <Button type="primary" icon={<PlusOutlined />} onClick={onClickNew}>New</Button>
      </div>
      {isDraft && (
        <div className="draft-indicator">
          <span className="draft-badge">Draft</span>
          <span>New conversation</span>
        </div>
      )}
      {loading ? <Spin className="centered-state" /> : conversations.length === 0 ? <Empty description="No conversations yet" /> : (
        <List
          dataSource={conversations}
          renderItem={(conversation) => (
            <Tooltip title={conversation.title || 'Untitled conversation'}>
              <List.Item className={conversation.id === selectedId ? 'conversation-item selected' : 'conversation-item'} onClick={() => onSelect(conversation.id)}>
                <Typography.Text ellipsis>{conversation.title || 'Untitled conversation'}</Typography.Text>
              </List.Item>
            </Tooltip>
          )}
        />
      )}
    </aside>
  )
}

