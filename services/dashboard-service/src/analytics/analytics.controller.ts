import { Controller, Get, Query, UseGuards, Request } from '@nestjs/common';
import { AnalyticsService } from './analytics.service';
import { JwtGuard } from '../auth/jwt.guard';

@Controller('analytics')
@UseGuards(JwtGuard)
export class AnalyticsController {
  constructor(private analyticsService: AnalyticsService) {}

  @Get('trends')
  async getTrends(
    @Request() req,
    @Query('days') days?: string,
  ) {
    const merchantId = req.user?.role === 'MERCHANT' ? req.user.sub : undefined;
    return this.analyticsService.getPaymentTrends(merchantId, days ? parseInt(days) : 30);
  }

  @Get('top-merchants')
  async getTopMerchants(@Query('limit') limit?: string) {
    return this.analyticsService.getTopMerchants(limit ? parseInt(limit) : 10);
  }

  @Get('payment-methods')
  async getPaymentMethods(@Request() req) {
    const merchantId = req.user?.role === 'MERCHANT' ? req.user.sub : undefined;
    return this.analyticsService.getPaymentMethodBreakdown(merchantId);
  }

  @Get('revenue')
  async getRevenue(
    @Request() req,
    @Query('period') period?: 'day' | 'week' | 'month',
  ) {
    const merchantId = req.user?.role === 'MERCHANT' ? req.user.sub : undefined;
    return this.analyticsService.getRevenueByPeriod(merchantId, period || 'day');
  }

  @Get('metrics')
  async getMetrics(
    @Request() req,
    @Query('days') days?: string,
  ) {
    const merchantId = req.user?.role === 'MERCHANT' ? req.user.sub : undefined;
    return this.analyticsService.getTimeRangeMetrics(days ? parseInt(days) : 30, merchantId);
  }
}