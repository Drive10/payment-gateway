import { z } from 'zod';
import { CheckoutFormValues } from '../types/payment';

export const paymentSchema = z.object({
  customerName: z.string()
    .min(2, 'Name must be at least 2 characters')
    .max(100, 'Name is too long'),
  customerEmail: z.string()
    .email('Invalid email address'),
  paymentMethod: z.enum(['CARD', 'UPI'], {
    errorMap: () => ({ message: 'Please select a payment method' }),
  }),
});

export type PaymentFormSchema = z.infer<typeof paymentSchema>;
