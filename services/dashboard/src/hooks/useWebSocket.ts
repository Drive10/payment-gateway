import { useEffect, useState } from 'react'
import { wsService } from '../services/websocket'

export function useWebSocket() {
  const [isConnected, setIsConnected] = useState(false)

  useEffect(() => {
    const token = localStorage.getItem('payflow_auth')
    if (token) {
      try {
        const { accessToken } = JSON.parse(token)
        wsService.connect(accessToken)
      } catch (e) {
        console.error('Failed to parse auth token for WebSocket')
      }
    }

    const unsubscribe = wsService.onConnectionChange(setIsConnected)

    return () => {
      unsubscribe()
      wsService.disconnect()
    }
  }, [])

  return { isConnected }
}

export function usePaymentUpdates(callback: (payment: unknown) => void) {
  useEffect(() => {
    const unsubscribe = wsService.subscribe('PAYMENT_UPDATED', callback)
    return () => unsubscribe()
  }, [callback])
}

export function useOrderUpdates(callback: (order: unknown) => void) {
  useEffect(() => {
    const unsubscribe = wsService.subscribe('ORDER_UPDATED', callback)
    return () => unsubscribe()
  }, [callback])
}

export function useNotifications(callback: (notification: unknown) => void) {
  useEffect(() => {
    const unsubscribe = wsService.subscribe('NOTIFICATION', callback)
    return () => unsubscribe()
  }, [callback])
}

export function useSystemAlerts(callback: (alert: unknown) => void) {
  useEffect(() => {
    const unsubscribe = wsService.subscribe('SYSTEM_ALERT', callback)
    return () => unsubscribe()
  }, [callback])
}
