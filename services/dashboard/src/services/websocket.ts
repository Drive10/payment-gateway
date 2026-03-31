type WebSocketCallback<T = unknown> = (data: T) => void

interface WebSocketMessage {
  type: 'PAYMENT_UPDATED' | 'ORDER_UPDATED' | 'NOTIFICATION' | 'SYSTEM_ALERT'
  payload: unknown
  timestamp: string
}

class WebSocketService {
  private ws: WebSocket | null = null
  private reconnectAttempts = 0
  private maxReconnectAttempts = 5
  private reconnectDelay = 3000
  private listeners: Map<string, Set<WebSocketCallback>> = new Map()
  private connectionCallbacks: Set<(connected: boolean) => void> = new Set()

  connect(token: string): void {
    if (this.ws?.readyState === WebSocket.OPEN) {
      return
    }

    const wsUrl = this.getWebSocketUrl(token)
    this.ws = new WebSocket(wsUrl)

    this.ws.onopen = () => {
      console.log('WebSocket connected')
      this.reconnectAttempts = 0
      this.notifyConnectionStatus(true)
    }

    this.ws.onmessage = (event) => {
      try {
        const message: WebSocketMessage = JSON.parse(event.data)
        this.notifyListeners(message.type, message.payload)
      } catch (error) {
        console.error('Failed to parse WebSocket message:', error)
      }
    }

    this.ws.onerror = (error) => {
      console.error('WebSocket error:', error)
    }

    this.ws.onclose = () => {
      console.log('WebSocket disconnected')
      this.notifyConnectionStatus(false)
      this.attemptReconnect(token)
    }
  }

  private getWebSocketUrl(token: string): string {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const host = import.meta.env.VITE_API_URL || 'http://localhost:8080'
    const baseUrl = host.replace(/^http/, protocol)
    return `${baseUrl}/ws?token=${token}`
  }

  private attemptReconnect(token: string): void {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.log('Max reconnection attempts reached')
      return
    }

    this.reconnectAttempts++
    console.log(`Attempting to reconnect... (${this.reconnectAttempts}/${this.maxReconnectAttempts})`)

    setTimeout(() => {
      this.connect(token)
    }, this.reconnectDelay * this.reconnectAttempts)
  }

  disconnect(): void {
    if (this.ws) {
      this.ws.close()
      this.ws = null
    }
    this.reconnectAttempts = this.maxReconnectAttempts
  }

  subscribe<T = unknown>(eventType: string, callback: WebSocketCallback<T>): () => void {
    if (!this.listeners.has(eventType)) {
      this.listeners.set(eventType, new Set())
    }
    this.listeners.get(eventType)!.add(callback as WebSocketCallback)

    return () => {
      this.listeners.get(eventType)?.delete(callback as WebSocketCallback)
    }
  }

  onConnectionChange(callback: (connected: boolean) => void): () => void {
    this.connectionCallbacks.add(callback)
    return () => {
      this.connectionCallbacks.delete(callback)
    }
  }

  private notifyListeners(type: string, data: unknown): void {
    this.listeners.get(type)?.forEach((callback) => callback(data))
    this.listeners.get('*')?.forEach((callback) => callback({ type, data }))
  }

  private notifyConnectionStatus(connected: boolean): void {
    this.connectionCallbacks.forEach((callback) => callback(connected))
  }

  send(message: { type: string; payload: unknown }): void {
    if (this.ws?.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(message))
    }
  }

  get isConnected(): boolean {
    return this.ws?.readyState === WebSocket.OPEN
  }
}

export const wsService = new WebSocketService()

export type { WebSocketMessage }
