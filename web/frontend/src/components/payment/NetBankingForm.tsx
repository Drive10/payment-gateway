import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { INDIAN_BANKS } from '../../types/payment';

interface NetBankingFormProps {
  onSubmit: (data: { bankCode: string; email: string }) => void;
  isLoading: boolean;
}

const netBankingSchema = z.object({
  bankCode: z.string().min(1, 'Please select a bank'),
  email: z.string().email('Invalid email address'),
});

export function NetBankingForm({ onSubmit, isLoading }: NetBankingFormProps) {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<{ bankCode: string; email: string }>({
    resolver: zodResolver(netBankingSchema),
    defaultValues: {
      bankCode: '',
      email: '',
    },
  });

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
      <div>
        <label htmlFor="bankCode" className="block text-sm font-medium text-gray-700 mb-1">
          Select Bank
        </label>
        <select
          id="bankCode"
          {...register('bankCode')}
          className={`w-full px-4 py-3 rounded-lg border ${
            errors.bankCode ? 'border-red-500' : 'border-gray-300'
          } focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition-all bg-white`}
          aria-label="Select bank"
          aria-invalid={!!errors.bankCode}
        >
          <option value="">-- Select Your Bank --</option>
          {INDIAN_BANKS.map((bank) => (
            <option key={bank.code} value={bank.code}>
              {bank.name}
            </option>
          ))}
        </select>
        {errors.bankCode && (
          <p className="text-red-500 text-xs mt-1" role="alert">{errors.bankCode.message}</p>
        )}
      </div>

      <div>
        <label htmlFor="nbEmail" className="block text-sm font-medium text-gray-700 mb-1">
          Email Address
        </label>
        <input
          id="nbEmail"
          type="email"
          autoComplete="email"
          {...register('email')}
          className={`w-full px-4 py-3 rounded-lg border ${
            errors.email ? 'border-red-500' : 'border-gray-300'
          } focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition-all`}
          placeholder="your@email.com"
          aria-label="Email address"
          aria-invalid={!!errors.email}
        />
        {errors.email && (
          <p className="text-red-500 text-xs mt-1" role="alert">{errors.email.message}</p>
        )}
      </div>
    </form>
  );
}
