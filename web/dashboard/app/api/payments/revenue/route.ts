import { NextResponse } from 'next/server';

export async function GET() {
  try {
    // Mock revenue data
    const revenue = {
      today: 2450.75,
      week: 12340.50,
      month: 45670.25,
      growth: 12.5
    };
    
    return NextResponse.json(revenue);
  } catch (error) {
    return NextResponse.json({ error: 'Failed to fetch revenue metrics' }, { status: 500 });
  }
}
