import { useEffect, useState, useMemo } from 'react'
import { Link } from 'react-router-dom'
import { Search, FileText, ChevronLeft, ChevronRight, RefreshCw, Loader2 } from 'lucide-react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Skeleton } from '@/components/ui/skeleton'
import { userService, type UserPayment } from '@/services/user.service'
import { useToast } from '@/hooks/useToast'
import { formatCurrency, formatDate } from '@/lib/utils'

const ITEMS_PER_PAGE = 10

export function UserPayments() {
  const [payments, setPayments] = useState<UserPayment[]>([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState<string>('all')
  const [currentPage, setCurrentPage] = useState(1)
  const [retryingId, setRetryingId] = useState<string | null>(null)
  const { addToast } = useToast()

  useEffect(() => {
    loadPayments()
  }, [])

  const loadPayments = async () => {
    try {
      const data = await userService.getPayments()
      setPayments(data)
    } catch (error) {
      console.error('Failed to load payments:', error)
      addToast('error', 'Failed to load payments', 'Please try again later')
    } finally {
      setLoading(false)
    }
  }

  const handleRetryPayment = async (payment: UserPayment) => {
    setRetryingId(payment.id)
    try {
      const result = await userService.retryPayment(payment.id)
      addToast('success', 'Payment retry successful', `Payment ${result.status} for ${formatCurrency(result.amount)}`)
      await loadPayments()
    } catch (error) {
      console.error('Failed to retry payment:', error)
      addToast('error', 'Payment retry failed', 'Please try again or contact support')
    } finally {
      setRetryingId(null)
    }
  }

  const filteredPayments = useMemo(() => {
    return payments.filter((p) => {
      const matchesSearch = p.orderReference.toLowerCase().includes(search.toLowerCase())
      const matchesStatus = statusFilter === 'all' || p.status === statusFilter
      return matchesSearch && matchesStatus
    })
  }, [payments, search, statusFilter])

  const paginatedPayments = useMemo(() => {
    const start = (currentPage - 1) * ITEMS_PER_PAGE
    return filteredPayments.slice(start, start + ITEMS_PER_PAGE)
  }, [filteredPayments, currentPage])

  const totalPages = Math.ceil(filteredPayments.length / ITEMS_PER_PAGE)

  const getStatusBadge = (status: string) => {
    switch (status.toUpperCase()) {
      case 'CAPTURED':
      case 'SUCCESS':
        return <Badge variant="success">Success</Badge>
      case 'FAILED':
        return <Badge variant="error">Failed</Badge>
      case 'PENDING':
      case 'CREATED':
        return <Badge variant="warning">Pending</Badge>
      case 'PARTIALLY_REFUNDED':
        return <Badge variant="warning">Partial Refund</Badge>
      case 'REFUNDED':
        return <Badge variant="secondary">Refunded</Badge>
      default:
        return <Badge variant="secondary">{status}</Badge>
    }
  }

  const handleDownloadInvoice = (payment: UserPayment) => {
    const invoiceContent = `
Invoice
=======
Order: ${payment.orderReference}
Amount: ${formatCurrency(payment.amount, payment.currency)}
Status: ${payment.status}
Date: ${formatDate(payment.date)}
Provider: ${payment.provider}
Method: ${payment.method}
`
    const blob = new Blob([invoiceContent], { type: 'text/plain' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `invoice-${payment.orderReference}.txt`
    a.click()
    URL.revokeObjectURL(url)
  }

  if (loading) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-10 w-full max-w-md" />
        <Skeleton className="h-64 w-full" />
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">My Payments</h1>
        <p className="text-muted-foreground">View and manage your payment history</p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Payment History</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex flex-col gap-4 mb-6">
            <div className="flex flex-col gap-4 sm:flex-row">
              <div className="relative flex-1">
                <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                <Input
                  placeholder="Search by order reference..."
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  className="pl-9"
                />
              </div>
              <Select value={statusFilter} onValueChange={setStatusFilter}>
                <SelectTrigger className="w-full sm:w-[180px]">
                  <SelectValue placeholder="Filter by status" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">All Status</SelectItem>
                  <SelectItem value="CAPTURED">Success</SelectItem>
                  <SelectItem value="FAILED">Failed</SelectItem>
                  <SelectItem value="CREATED">Pending</SelectItem>
                  <SelectItem value="REFUNDED">Refunded</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>

          <div className="rounded-md border">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Order Reference</TableHead>
                  <TableHead>Amount</TableHead>
                  <TableHead>Provider</TableHead>
                  <TableHead>Method</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead>Date</TableHead>
                  <TableHead className="text-right">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {paginatedPayments.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={7} className="text-center py-8 text-muted-foreground">
                      No payments found
                    </TableCell>
                  </TableRow>
                ) : (
                  paginatedPayments.map((payment) => (
                    <TableRow key={payment.id}>
                      <TableCell className="font-medium">{payment.orderReference}</TableCell>
                      <TableCell>{formatCurrency(payment.amount, payment.currency)}</TableCell>
                      <TableCell><Badge variant="outline">{payment.provider}</Badge></TableCell>
                      <TableCell>{payment.method}</TableCell>
                      <TableCell>{getStatusBadge(payment.status)}</TableCell>
                      <TableCell>{formatDate(payment.date)}</TableCell>
                      <TableCell className="text-right">
                        <div className="flex justify-end gap-2">
                          <Button variant="ghost" size="sm" asChild>
                            <Link to={`/user/payments/${payment.id}`}>
                              <FileText className="h-4 w-4" />
                            </Link>
                          </Button>
                          {payment.status === 'FAILED' && (
                            <Button
                              variant="outline"
                              size="sm"
                              onClick={() => handleRetryPayment(payment)}
                              disabled={retryingId === payment.id}
                            >
                              {retryingId === payment.id ? (
                                <Loader2 className="h-4 w-4 animate-spin" />
                              ) : (
                                <RefreshCw className="h-4 w-4" />
                              )}
                              <span className="ml-1">Retry</span>
                            </Button>
                          )}
                          <Button variant="ghost" size="icon" onClick={() => handleDownloadInvoice(payment)}>
                            <FileText className="h-4 w-4" />
                          </Button>
                        </div>
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </div>

          {totalPages > 1 && (
            <div className="flex items-center justify-between mt-4">
              <p className="text-sm text-muted-foreground">
                Showing {(currentPage - 1) * ITEMS_PER_PAGE + 1} to {Math.min(currentPage * ITEMS_PER_PAGE, filteredPayments.length)} of {filteredPayments.length} results
              </p>
              <div className="flex items-center gap-2">
                <Button variant="outline" size="sm" onClick={() => setCurrentPage((p) => Math.max(1, p - 1))} disabled={currentPage === 1}>
                  <ChevronLeft className="h-4 w-4" />
                </Button>
                <span className="text-sm">Page {currentPage} of {totalPages}</span>
                <Button variant="outline" size="sm" onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))} disabled={currentPage === totalPages}>
                  <ChevronRight className="h-4 w-4" />
                </Button>
              </div>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
