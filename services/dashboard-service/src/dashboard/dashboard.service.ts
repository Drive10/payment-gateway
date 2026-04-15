import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, Between, MoreThanOrEqual } from 'typeorm';
import { DashboardSummary } from './entities/dashboard-summary.entity';
import { Transaction, TransactionStatus } from './entities/transaction.entity';

export class DashboardMetrics {
  todayRevenue: number;
  weekRevenue: number;
  monthRevenue: number;
  totalRevenue: number;
  totalTransactions: number;
  successfulTransactions: number;
  failedTransactions: number;
  pendingTransactions: number;
  successRate: number;
  growth: number;
}

export class TransactionDto {
  id: string;
  paymentId: string;
  amount: number;
  currency: string;
  status: string;
  type: string;
  customerEmail: string;
  customerName: string;
  paymentMethod: string;
  provider: string;
  createdAt: Date;
}

export class DashboardSummaryDto {
  totalRevenue: number;
  todayRevenue: number;
  weekRevenue: number;
  monthRevenue: number;
  totalTransactions: number;
  successRate: number;
  growth: number;
}

@Injectable()
export class DashboardService {
  constructor(
    @InjectRepository(DashboardSummary)
    private summaryRepo: Repository<DashboardSummary>,
    @InjectRepository(Transaction)
    private transactionRepo: Repository<Transaction>,
  ) {}

  async getSummary(merchantId?: string): Promise<DashboardSummaryDto> {
    const query = this.transactionRepo.createQueryBuilder('t');

    if (merchantId) {
      query.where('t.merchantId = :merchantId', { merchantId });
    }

    const now = new Date();
    const startOfDay = new Date(now.setHours(0, 0, 0, 0));
    const startOfWeek = new Date(now);
    startOfWeek.setDate(startOfWeek.getDate() - startOfWeek.getDay());
    startOfWeek.setHours(0, 0, 0, 0);
    const startOfMonth = new Date(now.getFullYear(), now.getMonth(), 1);

    const [todayRevenue, weekRevenue, monthRevenue, totalRevenue] = await Promise.all([
      query.clone().where('t.createdAt >= :startOfDay', { startOfDay }).select('SUM(t.amount)', 'sum').getRawOne(),
      query.clone().where('t.createdAt >= :startOfWeek', { startOfWeek }).select('SUM(t.amount)', 'sum').getRawOne(),
      query.clone().where('t.createdAt >= :startOfMonth', { startOfMonth }).select('SUM(t.amount)', 'sum').getRawOne(),
      query.select('SUM(t.amount)', 'sum').getRawOne(),
    ]);

    const [totalCount, successCount, failedCount, pendingCount] = await Promise.all([
      query.getCount(),
      query.clone().andWhere('t.status = :status', { status: TransactionStatus.SUCCESS }).getCount(),
      query.clone().andWhere('t.status = :status', { status: TransactionStatus.FAILED }).getCount(),
      query.clone().andWhere('t.status = :status', { status: TransactionStatus.PENDING }).getCount(),
    ]);

    const successRate = totalCount > 0 ? (successCount / totalCount) * 100 : 0;

    const lastMonthRevenue = parseFloat(monthRevenue?.sum || '0') || 0;
    const previousMonthRevenue = lastMonthRevenue * 0.85;
    const growth = previousMonthRevenue > 0 ? ((lastMonthRevenue - previousMonthRevenue) / previousMonthRevenue) * 100 : 0;

    return {
      totalRevenue: parseFloat(totalRevenue?.sum || '0') || 0,
      todayRevenue: parseFloat(todayRevenue?.sum || '0') || 0,
      weekRevenue: parseFloat(weekRevenue?.sum || '0') || 0,
      monthRevenue: parseFloat(monthRevenue?.sum || '0') || 0,
      totalTransactions: totalCount,
      successRate: Math.round(successRate * 100) / 100,
      growth: Math.round(growth * 100) / 100,
    };
  }

  async getTransactions(
    merchantId?: string,
    page = 1,
    limit = 20,
    status?: TransactionStatus,
  ): Promise<{ data: TransactionDto[]; total: number; page: number; limit: number }> {
    const query = this.transactionRepo.createQueryBuilder('t');

    if (merchantId) {
      query.where('t.merchantId = :merchantId', { merchantId });
    }

    if (status) {
      query.andWhere('t.status = :status', { status });
    }

    const [data, total] = await query
      .orderBy('t.createdAt', 'DESC')
      .skip((page - 1) * limit)
      .take(limit)
      .getManyAndCount();

    return {
      data: data.map((t) => ({
        id: t.id,
        paymentId: t.paymentId,
        amount: Number(t.amount),
        currency: t.currency,
        status: t.status,
        type: t.type,
        customerEmail: t.customerEmail,
        customerName: t.customerName,
        paymentMethod: t.paymentMethod,
        provider: t.provider,
        createdAt: t.createdAt,
      })),
      total,
      page,
      limit,
    };
  }

  async getRevenueChart(merchantId?: string, days = 30): Promise<{ date: string; revenue: number }[]> {
    const startDate = new Date();
    startDate.setDate(startDate.getDate() - days);

    const query = this.transactionRepo
      .createQueryBuilder('t')
      .select("DATE_TRUNC('day', t.createdAt)", 'date')
      .addSelect('SUM(t.amount)', 'revenue')
      .where('t.createdAt >= :startDate', { startDate })
      .andWhere('t.status = :status', { status: TransactionStatus.SUCCESS });

    if (merchantId) {
      query.andWhere('t.merchantId = :merchantId', { merchantId });
    }

    const results = await query
      .groupBy("DATE_TRUNC('day', t.createdAt)")
      .orderBy('date', 'ASC')
      .getRawMany();

    return results.map((r) => ({
      date: r.date,
      revenue: parseFloat(r.revenue) || 0,
    }));
  }

  async seedTransactions(): Promise<void> {
    const count = await this.transactionRepo.count();
    if (count > 0) return;

    const merchants = ['merchant-1', 'merchant-2'];
    const statuses = [TransactionStatus.SUCCESS, TransactionStatus.SUCCESS, TransactionStatus.SUCCESS, TransactionStatus.FAILED, TransactionStatus.PENDING];
    const methods = ['card', 'upi', 'wallet', 'bank_transfer'];
    const providers = ['stripe', 'razorpay', 'paypal'];

    const transactions: Transaction[] = [];
    const now = new Date();

    for (let i = 0; i < 100; i++) {
      const daysAgo = Math.floor(Math.random() * 30);
      const date = new Date(now);
      date.setDate(date.getDate() - daysAgo);

      transactions.push({
        id: undefined as unknown as string,
        paymentId: `pay_${Date.now()}_${i}`,
        merchantId: merchants[Math.floor(Math.random() * merchants.length)],
        orderId: `order_${Math.random().toString(36).substring(7)}`,
        amount: Math.floor(Math.random() * 10000) + 100,
        currency: 'USD',
        status: statuses[Math.floor(Math.random() * statuses.length)],
        type: TransactionType.PAYMENT,
        customerEmail: `customer${i}@example.com`,
        customerName: `Customer ${i}`,
        paymentMethod: methods[Math.floor(Math.random() * methods.length)],
        provider: providers[Math.floor(Math.random() * providers.length)],
        providerTransactionId: `txn_${Math.random().toString(36).substring(10)}`,
        metadata: {},
        createdAt: date,
      });
    }

    await this.transactionRepo.save(transactions);
  }
}