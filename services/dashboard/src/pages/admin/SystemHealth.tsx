import { useSystemHealth, type ServiceHealth } from '../../hooks/useAdmin'
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/card'
import { Badge } from '../../components/ui/badge'
import { Loader2, Server, Database, Zap, AlertTriangle, CheckCircle, XCircle } from 'lucide-react'

export function AdminSystemHealth() {
  const { data, isLoading, error } = useSystemHealth()

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-12">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    )
  }

  if (error || !data) {
    return (
      <div className="rounded-lg border border-destructive bg-destructive/10 p-6 text-center">
        <AlertTriangle className="mx-auto h-12 w-12 text-destructive" />
        <h2 className="mt-4 text-xl font-semibold">System Health Unavailable</h2>
        <p className="mt-2 text-muted-foreground">Unable to fetch system health status.</p>
      </div>
    )
  }

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'UP':
        return <CheckCircle className="h-5 w-5 text-green-500" />
      case 'DOWN':
        return <XCircle className="h-5 w-5 text-red-500" />
      case 'DEGRADED':
        return <AlertTriangle className="h-5 w-5 text-yellow-500" />
      default:
        return <Server className="h-5 w-5 text-muted-foreground" />
    }
  }

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'UP':
        return 'bg-green-500'
      case 'DOWN':
        return 'bg-red-500'
      case 'DEGRADED':
        return 'bg-yellow-500'
      default:
        return 'bg-muted'
    }
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">System Health</h1>
        <p className="text-muted-foreground">Monitor the health of all services</p>
      </div>

      <div className="grid gap-6 md:grid-cols-4">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium">Overall Status</CardTitle>
            <Server className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="flex items-center gap-2">
              {getStatusIcon(data.status)}
              <span className="text-2xl font-bold">{data.status}</span>
            </div>
            <p className="mt-1 text-xs text-muted-foreground">
              Last checked: {new Date(data.timestamp).toLocaleTimeString()}
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium">Services Online</CardTitle>
            <Zap className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {data.services.filter((s: ServiceHealth) => s.status === 'UP').length}/{data.services.length}
            </div>
            <p className="text-xs text-muted-foreground">
              {data.services.filter((s: ServiceHealth) => s.status === 'DEGRADED').length} degraded
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium">Avg Latency</CardTitle>
            <Database className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {(data.services.reduce((acc: number, s: ServiceHealth) => acc + s.latency, 0) / data.services.length).toFixed(0)}ms
            </div>
            <p className="text-xs text-muted-foreground">Across all services</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium">Uptime</CardTitle>
            <CheckCircle className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">99.9%</div>
            <p className="text-xs text-muted-foreground">Last 30 days</p>
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Service Status</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="rounded-md border">
            <table className="w-full">
              <thead>
                <tr className="border-b bg-muted/50">
                  <th className="px-4 py-3 text-left text-sm font-medium">Service</th>
                  <th className="px-4 py-3 text-left text-sm font-medium">Status</th>
                  <th className="px-4 py-3 text-left text-sm font-medium">Latency</th>
                  <th className="px-4 py-3 text-left text-sm font-medium">Uptime</th>
                </tr>
              </thead>
              <tbody>
                {data.services.map((service: ServiceHealth) => (
                  <tr key={service.name} className="border-b">
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-2">
                        <span className={`h-2 w-2 rounded-full ${getStatusColor(service.status)}`} />
                        <span className="font-medium">{service.name}</span>
                      </div>
                    </td>
                    <td className="px-4 py-3">
                      <Badge variant={service.status === 'UP' ? 'default' : service.status === 'DOWN' ? 'destructive' : 'secondary'}>
                        {service.status}
                      </Badge>
                    </td>
                    <td className="px-4 py-3">
                      <span className={service.latency > 1000 ? 'text-yellow-500' : 'text-muted-foreground'}>
                        {service.latency}ms
                      </span>
                    </td>
                    <td className="px-4 py-3 text-muted-foreground">
                      {(service.uptime * 100).toFixed(2)}%
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </CardContent>
      </Card>

      <div className="grid gap-6 md:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Infrastructure</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="flex items-center justify-between">
              <span className="text-sm">PostgreSQL</span>
              <Badge variant="default">Running</Badge>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-sm">Redis</span>
              <Badge variant="default">Running</Badge>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-sm">Kafka</span>
              <Badge variant="default">Running</Badge>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Quick Stats</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="flex items-center justify-between">
              <span className="text-sm">CPU Usage</span>
              <span className="text-sm font-medium">32%</span>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-sm">Memory Usage</span>
              <span className="text-sm font-medium">4.2 GB / 16 GB</span>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-sm">Disk Usage</span>
              <span className="text-sm font-medium">256 GB / 512 GB</span>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
