# Frontend Rules

> React/TypeScript development standards for PayFlow

---

## 1. React Standards

### Components
- **Functional components with hooks** only
- No class components
- Use TypeScript for all new components

### Component Structure

```tsx
interface Props {
  amount: number;
  currency: string;
  onSuccess: (result: PaymentResult) => void;
  onError: (error: Error) => void;
}

export function PaymentForm({ amount, currency, onSuccess, onError }: Props) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  
  const handleSubmit = useCallback(async () => {
    setLoading(true);
    try {
      const result = await processPayment({ amount, currency });
      onSuccess(result);
    } catch (err) {
      setError(err.message);
      onError(err);
    } finally {
      setLoading(false);
    }
  }, [amount, currency, onSuccess, onError]);
  
  if (loading) return <LoadingSpinner />;
  
  return (
    <form onSubmit={handleSubmit}>
      {/* form content */}
    </form>
  );
}
```

---

## 2. Styling

### Tailwind CSS
- Use Tailwind for all styling
- No inline styles
- Use consistent spacing scale

### Responsive Design

```tsx
<div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
  {/* responsive content */}
</div>
```

---

## 3. State Management

### Local State
- Use `useState` for component state
- Use `useReducer` for complex state

### Server State
- Use React Query or SWR for API calls
- Implement proper caching

```tsx
const { data, isLoading, error } = useQuery({
  queryKey: ['payments', orderId],
  queryFn: () => fetchPayments(orderId),
  staleTime: 5 * 60 * 1000,
});
```

---

## 4. Form Handling

### React Hook Form

```tsx
const { register, handleSubmit, formState: { errors } } = useForm<PaymentFormData>();

const onSubmit = (data: PaymentFormData) => {
  // process form
};

<form onSubmit={handleSubmit(onSubmit)}>
  <input 
    {...register('cardNumber', { 
      required: 'Card number is required',
      validate: validateLuhn 
    })} 
  />
  {errors.cardNumber && <span>{errors.cardNumber.message}</span>}
</form>
```

---

## 5. Error Handling

### API Errors

```tsx
try {
  await processPayment(data);
} catch (error) {
  if (error instanceof PaymentDeclinedError) {
    // Handle declined
  } else if (error instanceof NetworkError) {
    // Handle network issue
  }
}
```

---

## 6. Testing

### Component Tests

```tsx
test('renders payment form', () => {
  render(<PaymentForm {...defaultProps} />);
  expect(screen.getByLabelText('Card Number')).toBeInTheDocument();
});

test('submits payment on button click', async () => {
  const onSuccess = jest.fn();
  render(<PaymentForm {...defaultProps} onSuccess={onSuccess} />);
  
  await userEvent.click(screen.getByRole('button'));
  await waitFor(() => expect(onSuccess).toHaveBeenCalled());
});
```

---

## Quick Reference

| Pattern | Implementation |
|---------|---------------|
| Components | Functional + hooks |
| Styling | Tailwind CSS |
| Forms | React Hook Form |
| API | React Query |
| Testing | Jest + Testing Library |