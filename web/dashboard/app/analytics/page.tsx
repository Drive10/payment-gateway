'use client';

import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import axios from 'axios';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
  BarChart,
  Bar,
  Legend,
} from 'recharts';
import { API_BASE_URL } from '@/src/lib/constants';
import { getAuthToken } from '@/src/lib/auth';

interface PaymentTrend {
  date: string;
  successful: number;
  failed: number;
  total: number;
  revenue: number;
}

interface PaymentMethodStats {
  method: string;
  count: number;
  revenue: number;
  percentage: number;
}

interface TimeMetrics {
  revenue: number;
  transactions: number;
  avgTransactionValue: number;
  successRate: number;
}

const COLORS = ['#4F46E5', '#10B981', '#F59E0B', '#EF4444', '#8B5CF6'];

const fetchTrends = async (days: number): Promise<PaymentTrend[]> => {
  const token = getAuthToken();
  const response = await axios.get(`${API_BASE_URL}/analytics/trends?days=${days}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  return response.data;
};

const fetchPaymentMethods = async (): Promise<PaymentMethodStats[]> => {
  const token = getAuthToken();
  const response = await axios.get(`${API_BASE_URL}/analytics/payment-methods`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  return response.data;
};

const fetchMetrics = async (days: number): Promise<TimeMetrics> => {
  const token = getAuthToken();
  const response = await axios.get(`${API_BASE_URL}/analytics/metrics?days=${days}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  return response.data;
};

export default function AnalyticsPage() {
  const [period, setPeriod] = useState(30);

  const { data: trends, isLoading: trendsLoading } = useQuery({
    queryKey: ['analyticsTrends', period],
    queryFn: () => fetchTrends(period),
  });

  const { data: methods, isLoading: methodsLoading } = useQuery({
    queryKey: ['analyticsMethods'],
    queryFn: fetchPaymentMethods,
  });

  const { data: metrics, isLoading: metricsLoading } = useQuery({
    queryKey: ['analyticsMetrics', period],
    queryFn: () => fetchMetrics(period),
  });

  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
    }).format(value);
  };

  const formatDate = (dateStr: string) => {
    const date = new Date(dateStr);
    return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
  };

  if (trendsLoading || methodsLoading || metricsLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600"></div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <h1 className="text-2xl font-bold text-gray-900">Analytics</h1>
        
        <div className="flex gap-2">
          {[7, 30, 90].map((days) => (
            <button
              key={days}
              onClick={() => setPeriod(days)}
              className={`px-4 py-2 text-sm font-medium rounded-md transition-colors ${
                period === days
                  ? 'bg-indigo-600 text-white'
                  : 'bg-white text-gray-700 border border-gray-300 hover:bg-gray-50'
              }`}
            >
              {days === 7 ? '7 Days' : days === 30 ? '30 Days' : '90 Days'}
            </button>
          ))}
        </div>
      </div>

      {/* Metrics cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="bg-white p-6 rounded-lg shadow">
          <p className="text-sm font-medium text-gray-500">Total Revenue</p>
          <p className="text-2xl font-bold text-gray-900 mt-1">
            {formatCurrency(metrics?.revenue || 0)}
          </p>
        </div>
        <div className="bg-white p-6 rounded-lg shadow">
          <p className="text-sm font-medium text-gray-500">Transactions</p>
          <p className="text-2xl font-bold text-gray-900 mt-1">
            {metrics?.transactions || 0}
          </p>
        </div>
        <div className="bg-white p-6 rounded-lg shadow">
          <p className="text-sm font-medium text-gray-500">Avg. Transaction</p>
          <p className="text-2xl font-bold text-gray-900 mt-1">
            {formatCurrency(metrics?.avgTransactionValue || 0)}
          </p>
        </div>
        <div className="bg-white p-6 rounded-lg shadow">
          <p className="text-sm font-medium text-gray-500">Success Rate</p>
          <p className="text-2xl font-bold text-gray-900 mt-1">
            {metrics?.successRate || 0}%
          </p>
        </div>
      </div>

      {/* Revenue trend chart */}
      <div className="bg-white p-6 rounded-lg shadow">
        <h2 className="text-lg font-semibold text-gray-900 mb-4">Revenue Trend</h2>
        <div className="h-80">
          <ResponsiveContainer width="100%" height="100%">
            <LineChart data={trends || []}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis
                dataKey="date"
                tickFormatter={formatDate}
                tick={{ fontSize: 12 }}
              />
              <YAxis
                tickFormatter={(value) => `$${value}`}
                tick={{ fontSize: 12 }}
              />
              <Tooltip
                formatter={(value: number) => [formatCurrency(value), 'Revenue']}
                labelFormatter={formatDate}
              />
              <Line
                type="monotone"
                dataKey="revenue"
                stroke="#4F46E5"
                strokeWidth={2}
                dot={false}
              />
            </LineChart>
          </ResponsiveContainer>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Payment methods pie chart */}
        <div className="bg-white p-6 rounded-lg shadow">
          <h2 className="text-lg font-semibold text-gray-900 mb-4">Payment Methods</h2>
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={methods || []}
                  dataKey="revenue"
                  nameKey="method"
                  cx="50%"
                  cy="50%"
                  outerRadius={80}
                  label={({ method, percentage }) =>
                    `${method} (${percentage.toFixed(1)}%)`
                  }
                >
                  {(methods || []).map((_, index) => (
                    <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip formatter={(value: number) => formatCurrency(value)} />
              </PieChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Success/Failed bar chart */}
        <div className="bg-white p-6 rounded-lg shadow">
          <h2 className="text-lg font-semibold text-gray-900 mb-4">Transaction Status</h2>
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={trends?.slice(-7) || []}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis
                  dataKey="date"
                  tickFormatter={formatDate}
                  tick={{ fontSize: 12 }}
                />
                <YAxis tick={{ fontSize: 12 }} />
                <Tooltip />
                <Legend />
                <Bar dataKey="successful" name="Successful" fill="#10B981" />
                <Bar dataKey="failed" name="Failed" fill="#EF4444" />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>
    </div>
  );
}