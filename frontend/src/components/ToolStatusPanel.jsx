import { Alert, Spin } from 'antd'

export function ToolStatusPanel({ isLoading, error }) {
  if (error) return <Alert type="error" showIcon message={error} />
  if (isLoading) return <div className="stream-status"><Spin size="small" /> Assistant is thinking…</div>
  return null
}

