import { Alert } from 'antd'

export function SharedWarningBanner() {
  return <Alert message="Conversations are shared. Do not enter sensitive information." type="warning" showIcon />
}

