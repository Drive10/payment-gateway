import { useQuery } from '@tanstack/react-query';
import axios from 'axios';

interface PaymentOverview {
  totalPayments: number;
  successfulPayments: number;
  failedPayments: number;
  totalAmount: number;
}

const fetchPaymentOverview = async (): Promise<PaymentOverview> => {
  const response = await axios.get('/api/payments/overview');
  return response.data;
};

export const PaymentsOverview = () => {
  const { data, isLoading, error } = useQuery({
    queryKey: ['paymentOverview'],
    queryFn: fetchPaymentOverview,
  });

  if (isLoading) return <div className="text-center py-8">Loading...</div>;
  if (error) return <div className="text-center py-8 text-red-500">Error loading data</div>;

  return (
    <div className="bg-white rounded-lg shadow p-6">
      <h2 className="text-xl font-bold mb-4">Payment Overview</h2>
      <div className="grid grid-cols-2 gap-4">
        <div className="text-center">
          <p className="text-sm text-gray-500">Total Payments</p>
          <p className="text-2xl font-bold">{data?.totalPayments ?? 0}</p>
        </div>
        <div className="text-center">
          <p className="text-sm text-gray-500">Successful</p>
          <p className="text-2xl font-bold text-green-500">{data?.successfulPayments ?? 0}</p>
        </div>
        <div className="text-center">
          <p className="text-sm text-gray-500">Failed</p>
          <p className="text-2xl font-bold text-red-500">{data?.failedPayments ?? 0}</p>
        </div>
        <div className="text-center">
          <p className="text-sm text-gray-500">Total Amount</p>
          <p className="text-2xl font-bold">${(data?.totalAmount ?? 0).toFixed(2)}</p>
        </div>
      </div>
    </div>
  );
};
