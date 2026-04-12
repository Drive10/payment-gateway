import { useQuery } from '@tanstack/react-query';
import axios from 'axios';

interface RevenueMetrics {
  today: number;
  week: number;
  month: number;
  growth: number;
}

const fetchRevenueMetrics = async (): Promise<RevenueMetrics> => {
  const response = await axios.get('/api/payments/revenue');
  return response.data;
};

export const RevenueMetrics = () => {
  const { data, isLoading, error } = useQuery({
    queryKey: ['revenueMetrics'],
    queryFn: fetchRevenueMetrics,
  });

  if (isLoading) return <div className="text-center py-8">Loading...</div>;
  if (error) return <div className="text-center py-8 text-red-500">Error loading data</div>;

  return (
    <div className="bg-white rounded-lg shadow p-6">
      <h2 className="text-xl font-bold mb-4">Revenue Metrics</h2>
      <div className="grid grid-cols-2 gap-4">
        <div>
          <p className="text-sm text-gray-500">Today</p>
          <p className="text-2xl font-bold">${(data?.today ?? 0).toFixed(2)}</p>
        </div>
        <div>
          <p className="text-sm text-gray-500">This Week</p>
          <p className="text-2xl font-bold">${(data?.week ?? 0).toFixed(2)}</p>
        </div>
        <div>
          <p className="text-sm text-gray-500">This Month</p>
          <p className="text-2xl font-bold">${(data?.month ?? 0).toFixed(2)}</p>
        </div>
        <div>
          <p className="text-sm text-gray-500">Growth</p>
          <p className={`text-2xl font-bold ${data?.growth && data.growth >= 0 ? 'text-green-500' : 'text-red-500'}`}>
            {data?.growth ?? 0}%
          </p>
        </div>
      </div>
    </div>
  );
};
