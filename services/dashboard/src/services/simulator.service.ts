import { apiClient } from '@/services/api-client'

export type SimulationMode = 'APPROVE_ALL' | 'DECLINE_ALL' | 'RANDOM' | 'DELAY' | 'ERROR'

export type SimulationStatus = 'IDLE' | 'RUNNING' | 'PAUSED'

export interface SimulationConfig {
  mode: SimulationMode
  delayMs?: number
  failureRate?: number
  enabled: boolean
}

export interface SimulationTransaction {
  id: string
  orderId: string
  orderReference: string
  amount: number
  currency: string
  status: 'PENDING' | 'SUCCESS' | 'FAILED' | 'TIMEOUT'
  mode: SimulationMode
  createdAt: string
  completedAt?: string
  errorMessage?: string
}

export interface SimulationStats {
  totalSimulations: number
  successfulSimulations: number
  failedSimulations: number
  averageResponseTime: number
}

export interface WebhookTestResult {
  id: string
  url: string
  payload: Record<string, unknown>
  responseStatus: number
  responseBody?: string
  latency: number
  success: boolean
  timestamp: string
}

export interface CreatePaymentTestRequest {
  amount: number
  currency: string
  method: 'CARD' | 'UPI' | 'WALLET'
  simulateStatus?: 'SUCCESS' | 'FAILURE' | 'RANDOM'
  userEmail?: string
}

class SimulatorApi {
  async getConfig(): Promise<SimulationConfig> {
    return apiClient.get<SimulationConfig>('/platform/simulator/config')
  }

  async updateConfig(config: SimulationConfig): Promise<SimulationConfig> {
    return apiClient.put<SimulationConfig>('/platform/simulator/config', config)
  }

  async getStatus(): Promise<{ status: SimulationStatus; config: SimulationConfig }> {
    return apiClient.get<{ status: SimulationStatus; config: SimulationConfig }>('/platform/simulator/status')
  }

  async start(): Promise<void> {
    return apiClient.post<void>('/platform/simulator/start')
  }

  async stop(): Promise<void> {
    return apiClient.post<void>('/platform/simulator/stop')
  }

  async getTransactions(params?: { page?: number; limit?: number; status?: string }): Promise<{
    data: SimulationTransaction[]
    total: number
    page: number
    limit: number
  }> {
    return apiClient.get<{
      data: SimulationTransaction[]
      total: number
      page: number
      limit: number
    }>('/platform/simulator/transactions', params as Record<string, unknown>)
  }

  async getStats(): Promise<SimulationStats> {
    return apiClient.get<SimulationStats>('/platform/simulator/stats')
  }

  async simulatePayment(request: CreatePaymentTestRequest): Promise<SimulationTransaction> {
    return apiClient.post<SimulationTransaction>('/platform/simulator/test-payment', request)
  }

  async triggerWebhook(paymentId: string, webhookUrl: string): Promise<WebhookTestResult> {
    return apiClient.post<WebhookTestResult>('/platform/simulator/trigger-webhook', {
      paymentId,
      webhookUrl
    })
  }

  async testWebhook(url: string, payload: Record<string, unknown>): Promise<WebhookTestResult> {
    return apiClient.post<WebhookTestResult>('/platform/simulator/test-webhook', {
      url,
      payload
    })
  }

  async resetStats(): Promise<void> {
    return apiClient.post<void>('/platform/simulator/reset-stats')
  }
}

export const simulatorApi = new SimulatorApi()
