import { useState } from 'react'
import { useQuery, useMutation } from '@tanstack/react-query'
import { simulatorApi, type CreatePaymentTestRequest } from '../../services/simulator.service'
import { Button } from '../../components/ui/button'
import { Input } from '../../components/ui/input'
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/card'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../../components/ui/select'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../../components/ui/tabs'
import { useToast } from '../../hooks/use-toast'
import { Loader2, Play, Pause, RotateCcw, Zap, Webhook, Send, CheckCircle, XCircle, Clock } from 'lucide-react'

export function AdminDebugger() {
  const [paymentTest, setPaymentTest] = useState<CreatePaymentTestRequest>({
    amount: 1000,
    currency: 'INR',
    method: 'CARD',
    simulateStatus: 'SUCCESS',
  })
  const [webhookUrl, setWebhookUrl] = useState('')
  const [webhookPayload, setWebhookPayload] = useState('{"event": "payment.completed", "data": {}}')

  const { toast } = useToast()

  const { data: simulatorStatus, refetch } = useQuery({
    queryKey: ['simulator', 'status'],
    queryFn: () => simulatorApi.getStatus(),
    refetchInterval: 5000,
  })

  const { data: transactions, isLoading: transactionsLoading } = useQuery({
    queryKey: ['simulator', 'transactions'],
    queryFn: () => simulatorApi.getTransactions({ limit: 20 }),
  })

  const { data: stats } = useQuery({
    queryKey: ['simulator', 'stats'],
    queryFn: () => simulatorApi.getStats(),
  })

  const startSimulator = useMutation({
    mutationFn: () => simulatorApi.start(),
    onSuccess: () => {
      toast({ title: 'Simulator started' })
      refetch()
    },
  })

  const stopSimulator = useMutation({
    mutationFn: () => simulatorApi.stop(),
    onSuccess: () => {
      toast({ title: 'Simulator stopped' })
      refetch()
    },
  })

  const resetStats = useMutation({
    mutationFn: () => simulatorApi.resetStats(),
    onSuccess: () => {
      toast({ title: 'Stats reset' })
      refetch()
    },
  })

  const testPayment = useMutation({
    mutationFn: (request: CreatePaymentTestRequest) => simulatorApi.simulatePayment(request),
    onSuccess: () => {
      toast({ title: 'Test payment executed' })
      refetch()
    },
    onError: () => {
      toast({ title: 'Test payment failed', variant: 'destructive' })
    },
  })

  const testWebhook = useMutation({
    mutationFn: () => simulatorApi.testWebhook(webhookUrl, JSON.parse(webhookPayload)),
    onSuccess: () => {
      toast({ title: 'Webhook test sent' })
    },
    onError: () => {
      toast({ title: 'Webhook test failed', variant: 'destructive' })
    },
  })

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'SUCCESS':
      case 'IDLE':
        return <CheckCircle className="h-4 w-4 text-green-500" />
      case 'FAILED':
        return <XCircle className="h-4 w-4 text-red-500" />
      case 'PENDING':
      case 'RUNNING':
        return <Clock className="h-4 w-4 text-yellow-500" />
      default:
        return <Clock className="h-4 w-4 text-muted-foreground" />
    }
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Interactive Debugger</h1>
        <p className="text-muted-foreground">Test payment flows and webhooks</p>
      </div>

      <div className="grid gap-6 md:grid-cols-4">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium">Status</CardTitle>
            <Zap className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="flex items-center gap-2">
              {getStatusIcon(simulatorStatus?.status || 'IDLE')}
              <span className="text-2xl font-bold">{simulatorStatus?.status || 'IDLE'}</span>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium">Total Tests</CardTitle>
            <Zap className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{stats?.totalSimulations || 0}</div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium">Success Rate</CardTitle>
            <CheckCircle className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {stats && stats.totalSimulations > 0
                ? ((stats.successfulSimulations / stats.totalSimulations) * 100).toFixed(1)
                : 0}%
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium">Avg Response</CardTitle>
            <Clock className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{stats?.averageResponseTime?.toFixed(0) || 0}ms</div>
          </CardContent>
        </Card>
      </div>

      <div className="flex gap-4">
        <Button
          onClick={() => simulatorStatus?.status === 'RUNNING' ? stopSimulator.mutate() : startSimulator.mutate()}
          disabled={startSimulator.isPending || stopSimulator.isPending}
          variant={simulatorStatus?.status === 'RUNNING' ? 'destructive' : 'default'}
        >
          {simulatorStatus?.status === 'RUNNING' ? (
            <>
              <Pause className="mr-2 h-4 w-4" />
              Stop Simulator
            </>
          ) : (
            <>
              <Play className="mr-2 h-4 w-4" />
              Start Simulator
            </>
          )}
        </Button>
        <Button variant="outline" onClick={() => resetStats.mutate()} disabled={resetStats.isPending}>
          <RotateCcw className="mr-2 h-4 w-4" />
          Reset Stats
        </Button>
      </div>

      <Tabs defaultValue="payment" className="space-y-4">
        <TabsList>
          <TabsTrigger value="payment">Payment Tester</TabsTrigger>
          <TabsTrigger value="webhook">Webhook Tester</TabsTrigger>
          <TabsTrigger value="transactions">Recent Transactions</TabsTrigger>
        </TabsList>

        <TabsContent value="payment" className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle>Test Payment Flow</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid gap-4 md:grid-cols-3">
                <div className="space-y-2">
                  <label className="text-sm font-medium">Amount (INR)</label>
                  <Input
                    type="number"
                    value={paymentTest.amount}
                    onChange={(e) => setPaymentTest({ ...paymentTest, amount: Number(e.target.value) })}
                  />
                </div>
                <div className="space-y-2">
                  <label className="text-sm font-medium">Currency</label>
                  <Select value={paymentTest.currency} onValueChange={(v) => setPaymentTest({ ...paymentTest, currency: v })}>
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="INR">INR</SelectItem>
                      <SelectItem value="USD">USD</SelectItem>
                      <SelectItem value="EUR">EUR</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
                <div className="space-y-2">
                  <label className="text-sm font-medium">Method</label>
                  <Select value={paymentTest.method} onValueChange={(v: any) => setPaymentTest({ ...paymentTest, method: v })}>
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="CARD">Card</SelectItem>
                      <SelectItem value="UPI">UPI</SelectItem>
                      <SelectItem value="WALLET">Wallet</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
              </div>
              <div className="space-y-2">
                <label className="text-sm font-medium">Simulate Status</label>
                <Select value={paymentTest.simulateStatus} onValueChange={(v: any) => setPaymentTest({ ...paymentTest, simulateStatus: v })}>
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="SUCCESS">Success</SelectItem>
                    <SelectItem value="FAILURE">Failure</SelectItem>
                    <SelectItem value="RANDOM">Random</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              <Button onClick={() => testPayment.mutate(paymentTest)} disabled={testPayment.isPending}>
                {testPayment.isPending ? (
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                ) : (
                  <Send className="mr-2 h-4 w-4" />
                )}
                Execute Test Payment
              </Button>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="webhook" className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle>Test Webhook</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <label className="text-sm font-medium">Webhook URL</label>
                <Input
                  placeholder="https://your-domain.com/webhook"
                  value={webhookUrl}
                  onChange={(e) => setWebhookUrl(e.target.value)}
                />
              </div>
              <div className="space-y-2">
                <label className="text-sm font-medium">Payload (JSON)</label>
                <textarea
                  className="min-h-[150px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm font-mono"
                  value={webhookPayload}
                  onChange={(e) => setWebhookPayload(e.target.value)}
                />
              </div>
              <Button onClick={() => testWebhook.mutate()} disabled={!webhookUrl || !webhookPayload || testWebhook.isPending}>
                {testWebhook.isPending ? (
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                ) : (
                  <Webhook className="mr-2 h-4 w-4" />
                )}
                Send Test Webhook
              </Button>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="transactions" className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle>Recent Test Transactions</CardTitle>
            </CardHeader>
            <CardContent>
              {transactionsLoading ? (
                <div className="flex items-center justify-center py-8">
                  <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
                </div>
              ) : (
                <div className="rounded-md border">
                  <table className="w-full">
                    <thead>
                      <tr className="border-b bg-muted/50">
                        <th className="px-4 py-3 text-left text-sm font-medium">ID</th>
                        <th className="px-4 py-3 text-left text-sm font-medium">Amount</th>
                        <th className="px-4 py-3 text-left text-sm font-medium">Status</th>
                        <th className="px-4 py-3 text-left text-sm font-medium">Mode</th>
                        <th className="px-4 py-3 text-left text-sm font-medium">Time</th>
                      </tr>
                    </thead>
                    <tbody>
                      {transactions?.data.map((tx) => (
                        <tr key={tx.id} className="border-b">
                          <td className="px-4 py-3 text-sm font-mono">{tx.id.slice(0, 8)}...</td>
                          <td className="px-4 py-3 text-sm font-medium">
                            {tx.currency} {tx.amount.toLocaleString()}
                          </td>
                          <td className="px-4 py-3">
                            <div className="flex items-center gap-2">
                              {getStatusIcon(tx.status)}
                              <span className="text-sm">{tx.status}</span>
                            </div>
                          </td>
                          <td className="px-4 py-3 text-sm">{tx.mode}</td>
                          <td className="px-4 py-3 text-sm text-muted-foreground">
                            {new Date(tx.createdAt).toLocaleTimeString()}
                          </td>
                        </tr>
                      ))}
                      {(!transactions?.data || transactions.data.length === 0) && (
                        <tr>
                          <td colSpan={5} className="px-4 py-8 text-center text-muted-foreground">
                            No test transactions yet
                          </td>
                        </tr>
                      )}
                    </tbody>
                  </table>
                </div>
              )}
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  )
}
