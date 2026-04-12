import { NextResponse } from 'next/server';

export async function GET() {
  try {
    // Generate mock trends data for the last 30 days
    const trends = [];
    const today = new Date();
    
    for (let i = 29; i >= 0; i--) {
      const date = new Date(today);
      date.setDate(today.getDate() - i);
      
      // Random revenue between $1000 and $5000
      const revenue = Math.floor(Math.random() * 4000) + 1000;
      // Random transaction count between 50 and 200
      const count = Math.floor(Math.random() * 150) + 50;
      
      trends.push({
        date: date.toISOString().split('T')[0],
        revenue,
        count
      });
    }
    
    const data = { trends };
    
    return NextResponse.json(data);
  } catch (error) {
    return NextResponse.json({ error: 'Failed to fetch payment trends' }, { status: 500 });
  }
}
