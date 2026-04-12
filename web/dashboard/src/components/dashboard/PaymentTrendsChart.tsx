import { useQuery } from '@tanstack/react-query';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import axios from 'axios';

interface DailyTrend {
  date: string;
  revenue: number;
  count: number;
}

interface PaymentTrendsData {
  trends: DailyTrend[];
}

const fetchPaymentTrends = async (): Promise<PaymentTrendsData> => {
  const response = await axios.get('/api/payments/trends');
  return response.data;
};

export const PaymentTrendsChart = () => {
  const { data, isLoading, error } = useQuery({
    queryKey: ['paymentTrends'],
    queryFn: fetchPaymentTrends,
  });

  if (isLoading) return <div className="text-center py-8">Loading...</div>;
  if (error) return <div className="text-center py-8 text-red-500">Error loading data</div>;

  return (
    <div className="bg-white rounded-lg shadow p-6">
      <h2 className="text-xl font-bold mb-4">Payment Trends (Last 30 Days)</h2>
      <ResponsiveContainer width="100%" height={400}>
        <LineChart data={data?.trends ?? []}>
          <CartesianGrid strokeDasharray="3 3" />
          <XAxis dataKey="date" />
          <YAxis />
          <Tooltip />
          <Legend />
          <Line type="monotone" dataKey="revenue" stroke="#8884d8" activeDot={{ r: 8 }} />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
};
