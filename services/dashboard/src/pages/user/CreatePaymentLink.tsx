import { useState } from 'react'
import { Plus, Link, Loader2, Copy, Check } from 'lucide-react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter } from '@/components/ui/dialog'
import { paymentApi, type CreatePaymentLinkRequest } from '@/services/payment.service'
import { useToast } from '@/hooks/useToast'

interface CreatePaymentLinkDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  merchantId: string
  onSuccess?: (paymentLink: { paymentLinkId: string; paymentLinkUrl: string }) => void
}

export function CreatePaymentLinkDialog({ open, onOpenChange, merchantId, onSuccess }: CreatePaymentLinkDialogProps) {
  const [loading, setLoading] = useState(false)
  const [copied, setCopied] = useState(false)
  const [createdLink, setCreatedLink] = useState<{ paymentLinkId: string; paymentLinkUrl: string } | null>(null)
  const { addToast } = useToast()
  
  const [form, setForm] = useState<CreatePaymentLinkRequest>({
    merchantId,
    amount: 0,
    currency: 'USD',
    description: '',
    customerName: '',
    customerEmail: '',
    customerPhone: ''
  })

  const handleSubmit = async () => {
    if (!form.amount || form.amount <= 0) {
      addToast('error', 'Invalid Amount', 'Please enter a valid amount')
      return
    }

    setLoading(true)
    try {
      const result = await paymentApi.createPaymentLink(form)
      setCreatedLink({
        paymentLinkId: result.paymentLinkId,
        paymentLinkUrl: result.paymentLinkUrl
      })
      addToast('success', 'Payment Link Created', 'The payment link has been created successfully')
      onSuccess?.(result)
    } catch (error) {
      console.error('Failed to create payment link:', error)
      addToast('error', 'Failed to create payment link', 'Please try again later')
    } finally {
      setLoading(false)
    }
  }

  const handleCopyLink = () => {
    if (createdLink) {
      const fullUrl = `${window.location.origin}/?ref=${createdLink.paymentLinkId}`
      navigator.clipboard.writeText(fullUrl)
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    }
  }

  const handleClose = () => {
    setForm({
      merchantId,
      amount: 0,
      currency: 'USD',
      description: '',
      customerName: '',
      customerEmail: '',
      customerPhone: ''
    })
    setCreatedLink(null)
    onOpenChange(false)
  }

  return (
    <Dialog open={open} onOpenChange={handleClose}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Create Payment Link</DialogTitle>
          <DialogDescription>
            Generate a payment link to share with customers
          </DialogDescription>
        </DialogHeader>

        {!createdLink ? (
          <div className="space-y-4">
            <div className="space-y-2">
              <label htmlFor="amount" className="text-sm font-medium">Amount</label>
              <Input
                id="amount"
                type="number"
                placeholder="0.00"
                value={form.amount || ''}
                onChange={(e) => setForm({ ...form, amount: parseFloat(e.target.value) || 0 })}
              />
            </div>

            <div className="space-y-2">
              <label htmlFor="currency" className="text-sm font-medium">Currency</label>
              <Select 
                value={form.currency} 
                onValueChange={(value) => setForm({ ...form, currency: value })}
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="USD">USD - US Dollar</SelectItem>
                  <SelectItem value="EUR">EUR - Euro</SelectItem>
                  <SelectItem value="GBP">GBP - British Pound</SelectItem>
                  <SelectItem value="INR">INR - Indian Rupee</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <label htmlFor="description" className="text-sm font-medium">Description (Optional)</label>
              <Input
                id="description"
                placeholder="Payment for order #123"
                value={form.description || ''}
                onChange={(e) => setForm({ ...form, description: e.target.value })}
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <label htmlFor="customerName" className="text-sm font-medium">Customer Name</label>
                <Input
                  id="customerName"
                  placeholder="John Doe"
                  value={form.customerName || ''}
                  onChange={(e) => setForm({ ...form, customerName: e.target.value })}
                />
              </div>
              <div className="space-y-2">
                <label htmlFor="customerEmail" className="text-sm font-medium">Customer Email</label>
                <Input
                  id="customerEmail"
                  type="email"
                  placeholder="john@example.com"
                  value={form.customerEmail || ''}
                  onChange={(e) => setForm({ ...form, customerEmail: e.target.value })}
                />
              </div>
            </div>

            <div className="space-y-2">
              <label className="text-sm font-medium">Share this link with your customer</label>
              <div className="flex gap-2">
                <Input
                  readOnly
                  value={`${window.location.origin}/?ref=${createdLink.paymentLinkId}`}
                  className="font-mono text-sm"
                />
                <Button size="icon" variant="outline" onClick={handleCopyLink}>
                  {copied ? <Check className="h-4 w-4" /> : <Copy className="h-4 w-4" />}
                </Button>
              </div>
            </div>

            <div className="text-sm text-gray-500 text-center">
              This link will expire in 24 hours
            </div>

            <DialogFooter>
              <Button variant="outline" onClick={handleClose}>Close</Button>
            </DialogFooter>
          </div>
        )}
      </DialogContent>
    </Dialog>
  )
}