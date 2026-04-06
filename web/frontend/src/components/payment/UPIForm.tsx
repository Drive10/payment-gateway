import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { UPI_BANKS } from '../../types/payment';

interface UPIFormProps {
  onSubmit: (data: { upiId: string; email: string }) => void;
  isLoading: boolean;
}

const upiSchema = z.object({
  upiId: z.string()
    .min(5, 'Invalid UPI ID')
    .regex(/^[a-zA-Z0-9._-]+@[a-z]+$/, 'Invalid UPI ID format'),
  email: z.string().email('Invalid email address'),
});

export function UPIForm({ onSubmit, isLoading }: UPIFormProps) {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<{ upiId: string; email: string }>({
    resolver: zodResolver(upiSchema),
    defaultValues: {
      upiId: '',
      email: '',
    },
  });

  const handleUpiChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    let value = e.target.value.toLowerCase();
    if (!value.includes('@')) {
      return value;
    }
    return value;
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
      <div>
        <label htmlFor="upiId" className="block text-sm font-medium text-gray-700 mb-1">
          UPI ID
        </label>
        <div className="relative">
          <input
            id="upiId"
            type="text"
            inputMode="email"
            {...register('upiId')}
            className={`w-full px-4 py-3 rounded-lg border ${
              errors.upiId ? 'border-red-500' : 'border-gray-300'
            } focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition-all pr-16`}
            placeholder="yourname@oksbi"
            aria-label="UPI ID"
            aria-invalid={!!errors.upiId}
          />
          <select
            className="absolute right-2 top-1/2 -translate-y-1/2 bg-gray-100 border-none text-sm py-1 px-2 rounded cursor-pointer"
            onChange={(e) => {
              const input = document.getElementById('upiId') as HTMLInputElement;
              const current = input?.value?.split('@')[0] || '';
              input.value = current ? `${current}@${e.target.value}` : `@${e.target.value}`;
            }}
          >
            <option value="">Select Bank</option>
            {UPI_BANKS.map((bank) => (
              <option key={bank} value={bank}>
                @{bank}
              </option>
            ))}
          </select>
        </div>
        {errors.upiId && (
          <p className="text-red-500 text-xs mt-1" role="alert">{errors.upiId.message}</p>
        )}
      </div>

      <div>
        <label htmlFor="upiEmail" className="block text-sm font-medium text-gray-700 mb-1">
          Email Address
        </label>
        <input
          id="upiEmail"
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
