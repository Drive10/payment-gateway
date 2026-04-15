import { Controller, Get, Post, Query, UseGuards, Request } from '@nestjs/common';
import { DashboardService, DashboardSummaryDto, TransactionDto } from './dashboard.service';
import { JwtGuard } from '../auth/jwt.guard';
import { TransactionStatus } from './entities/transaction.entity';

class PaginatedResponse<T> {
  data: T[];
  total: number;
  page: number;
  limit: number;
}

@Controller('dashboard')
@UseGuards(JwtGuard)
export class DashboardController {
  constructor(private dashboardService: DashboardService) {}

  @Get('summary')
  async getSummary(@Request() req): Promise<DashboardSummaryDto> {
    const merchantId = req.user?.role === 'MERCHANT' ? req.user.sub : undefined;
    return this.dashboardService.getSummary(merchantId);
  }

  @Get('transactions')
  async getTransactions(
    @Request() req,
    @Query('page') page?: string,
    @Query('limit') limit?: string,
    @Query('status') status?: TransactionStatus,
  ): Promise<PaginatedResponse<TransactionDto>> {
    const merchantId = req.user?.role === 'MERCHANT' ? req.user.sub : undefined;
    return this.dashboardService.getTransactions(
      merchantId,
      page ? parseInt(page) : 1,
      limit ? parseInt(limit) : 20,
      status,
    );
  }

  @Get('revenue-chart')
  async getRevenueChart(
    @Request() req,
    @Query('days') days?: string,
  ): Promise<{ date: string; revenue: number }[]> {
    const merchantId = req.user?.role === 'MERCHANT' ? req.user.sub : undefined;
    return this.dashboardService.getRevenueChart(merchantId, days ? parseInt(days) : 30);
  }

  @Post('seed')
  async seed(): Promise<{ message: string }> {
    await this.dashboardService.seedTransactions();
    return { message: 'Transactions seeded successfully' };
  }
}