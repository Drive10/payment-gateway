import { Entity, Column, PrimaryGeneratedColumn, CreateDateColumn, UpdateDateColumn } from 'typeorm';

@Entity('dashboard_summary')
export class DashboardSummary {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ name: 'merchant_id' })
  merchantId: string;

  @Column({ name: 'total_revenue', type: 'decimal', precision: 15, scale: 2, default: 0 })
  totalRevenue: number;

  @Column({ name: 'today_revenue', type: 'decimal', precision: 15, scale: 2, default: 0 })
  todayRevenue: number;

  @Column({ name: 'week_revenue', type: 'decimal', precision: 15, scale: 2, default: 0 })
  weekRevenue: number;

  @Column({ name: 'month_revenue', type: 'decimal', precision: 15, scale: 2, default: 0 })
  monthRevenue: number;

  @Column({ name: 'total_transactions', default: 0 })
  totalTransactions: number;

  @Column({ name: 'successful_transactions', default: 0 })
  successfulTransactions: number;

  @Column({ name: 'failed_transactions', default: 0 })
  failedTransactions: number;

  @Column({ name: 'pending_transactions', default: 0 })
  pendingTransactions: number;

  @Column({ name: 'success_rate', type: 'decimal', precision: 5, scale: 2, default: 0 })
  successRate: number;

  @Column({ type: 'timestamp', name: 'last_updated' })
  lastUpdated: Date;

  @CreateDateColumn()
  createdAt: Date;

  @UpdateDateColumn()
  updatedAt: Date;
}