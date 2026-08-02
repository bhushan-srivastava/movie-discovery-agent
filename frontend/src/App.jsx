import { ConfigProvider } from 'antd'
import { ChatPage } from './pages/ChatPage'

export default function App() {
  return <ConfigProvider theme={{ token: { colorPrimary: '#1677ff', borderRadius: 8 } }}><ChatPage /></ConfigProvider>
}

