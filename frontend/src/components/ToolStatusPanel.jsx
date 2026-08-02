import { Alert, Spin } from 'antd'

export function ToolStatusPanel({ isStreaming, activeTool, error }) {
  if (error) return <Alert type="error" showIcon message={error} />
  if (activeTool) return <Alert type="info" showIcon icon={<Spin size="small" />} message={`Using ${activeTool}…`} />
  if (isStreaming) return <div className="stream-status"><Spin size="small" /> Assistant is thinking…</div>
  return null
}

