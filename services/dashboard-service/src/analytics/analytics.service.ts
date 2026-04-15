import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { Transaction, TransactionStatus } from '../dashboard/entities/transaction.entity';

export class PaymentTrend {
  date: string;
  successful: number;
  failed: number;
  total: number;
  revenue: number;
}

export class MerchantStats {
  merchantId: string;
  totalRevenue: number;
  transactionCount: number;
  successRate: number;
}

export class PaymentMethodStats {
  method: string;
  count: number;
  revenue: number;
  percentage: number;
}

export class TimeRangeMetrics {
  revenue: number;
  transactions: number;
  avgTransactionValue: number;
  successRate: number;
}

@Injectable()
export class AnalyticsService {
  constructor(
    @InjectRepository(Transaction)
    private transactionRepo: Repository<Transaction>,
  ) {}

  async getPaymentTrends(merchantId?: string, days = 30): Promise<PaymentTrend[]> {
    const startDate = new Date();
    startDate.setDate(startDate.getDate() - days);

    const query = this.transactionRepo
      .createQueryBuilder('t')
      .select("DATE_TRUNC('day', t.createdAt)", 'date')
      .addSelect('COUNT(CASE WHEN t.status = :success THEN 1 END)', 'successful')
      .addSelect('COUNT(CASE WHEN t.status = :failed THEN 1 END)', 'failed')
      .addSelect('COUNT(*)', 'total')
      .addSelect('SUM(CASE WHEN t.status = :success THEN t.amount ELSE 0 END)', 'revenue')
      .setParameter('success', TransactionStatus.SUCCESS)
      .setParameter('failed', TransactionStatus.FAILED)
      .where('t.createdAt >= :startDate', { startDate });

    if (merchantId) {
      query.andWhere('t.merchantId = :merchantId', { merchantId });
    }

    const results = await query
      .groupBy("DATE_TRUNC('day', t.createdAt)")
      .orderBy('date', 'ASC')
      .getRawMany();

    return results.map((r) => ({
      date: r.date,
      successful: parseInt(r.successful) || 0,
      failed: parseInt(r.failed) || 0,
      total: parseInt(r.total) || 0,
      revenue: parseFloat(r.revenue) || 0,
    }));
  }

  async getTopMerchants(limit = 10): Promise<MerchantStats[]> {
    const results = await this.transactionRepo
      .createQueryBuilder('t')
      .select('t.merchantId', 'merchantId')
      .addSelect('SUM(t.amount)', 'totalRevenue')
      .addSelect('COUNT(*)', 'transactionCount')
      .addSelect('COUNT(CASE WHEN t.status = :success THEN 1 END)::float / COUNT(*) * 100', 'successRate')
      .setParameter('success', TransactionStatus.SUCCESS)
      .groupBy('t.merchantId')
      .orderBy('totalRevenue', 'DESC')
      .limit(limit)
      .getRawMany();

    return results.map((r) => ({
      merchantId: r.merchantId,
      totalRevenue: parseFloat(r.totalRevenue) || 0,
      transactionCount: parseInt(r.transactionCount) || 0,
      successRate: parseFloat(r.successRate) || 0,
    }));
  }

  async getPaymentMethodBreakdown(merchantId?: string): Promise<PaymentMethodStats[]> {
    const query = this.transactionRepo
      .createQueryBuilder('t')
      .select('t.paymentMethod', 'method')
      .addSelect('COUNT(*)', 'count')
      .addSelect('SUM(t.amount)', 'revenue')
      .where('t.status = :status', { status: TransactionStatus.SUCCESS });

    if (merchantId) {
      query.andWhere('t.merchantId = :merchantId', { merchantId });
    }

    const results = await query
      .groupBy('t.paymentMethod')
      .orderBy('revenue', 'DESC')
      .getRawMany();

    const total = results.reduce((sum, r) => sum + parseInt(r.count), 0);

    return results.map((r) => ({
      method: r.method || 'unknown',
      count: parseInt(r.count) || 0,
      revenue: parseFloat(r.revenue) || 0,
      percentage: total > 0 ? (parseInt(r.count) / total) * 100 : 0,
    }));
  }

  async getRevenueByPeriod(
    merchantId?: string,
    period: 'day' | 'week' | 'month' = 'day',
  ): Promise<{ period: string; revenue: number }[]> {
    const truncate = period === 'day' ? 'day' : period === 'week' ? 'week' : 'month';

    const query = this.transactionRepo
      .createQueryBuilder('t')
      .select(`DATE_TRUNC('${truncate}', t.createdAt)`, 'period')
      .addSelect('SUM(t.amount)', 'revenue')
      .where('t.status = :status', { status: TransactionStatus.SUCCESS });

    if (merchantId) {
      query.andWhere('t.merchantId = :merchantId', { merchantId });
    }

    const results = await query
      .groupBy(`DATE_TRUNC('${truncate}', t.createdAt)`)
      .orderBy('period', 'DESC')
      .limit(12)
      .getRawMany();

    return results.map((r) => ({
      period: r.period,
      revenue: parseFloat(r.revenue) || 0,
    }));
  }

  async getTimeRangeMetrics(days: number, merchantId?: string): Promise<TimeRangeMetrics> {
    const startDate = new Date();
    startDate.setDate(startDate.getDate() - days);

    const query = this.transactionRepo
      .createQueryBuilder('t')
      .where('t.createdAt >= :startDate', { startDate });

    if (merchantId) {
      query.andWhere('t.merchantId = :merchantId', { merchantId });
    }

    const [totalRevenue, totalTransactions, successCount] = await Promise.all([
      query.clone().select('SUM(t.amount)', 'sum').getRawOne(),
      query.getCount(),
      query.clone().andWhere('t.status = :status', { status: TransactionStatus.SUCCESS }).getCount(),
    ]);

    const revenue = parseFloat(totalRevenue?.sum || '0') || 0;
    const successRate = totalTransactions > 0 ? (successCount / totalTransactions) * 100 : 0;

    return {
      revenue,
      transactions: totalTransactions,
      avgTransactionValue: totalTransactions > 0 ? revenue / totalTransactions : 0,
      successRate: Math.round(successRate * 100) / 100,
    };
  }
}