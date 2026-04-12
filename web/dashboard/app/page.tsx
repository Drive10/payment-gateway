import { PaymentsOverview } from '@/src/components/dashboard/PaymentsOverview';
import { PaymentTrendsChart } from '@/src/components/dashboard/PaymentTrendsChart';
import { RecentTransactions } from '@/src/components/dashboard/RecentTransactions';
import { RevenueMetrics } from '@/src/components/dashboard/RevenueMetrics';

export default function DashboardPage() {
  return (
    <div className="min-h-screen bg-gray-50 p-6">
      <h1 className="text-2xl font-bold mb-6">PayFlow Dashboard</h1>
      
      <div className="grid gap-6 mb-6">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          <RevenueMetrics />
          <PaymentsOverview />
          <PaymentTrendsChart className="lg:col-span-2" />
        </div>
        <RecentTransactions className="lg:col-span-4" />
      </div>
    </div>
  );
}
