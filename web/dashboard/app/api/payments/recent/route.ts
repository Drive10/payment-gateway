import { NextResponse } from 'next/server';

export async function GET(request: Request) {
  try {
    const { searchParams } = new URL(request.url);
    const page = parseInt(searchParams.get('page') || '1');
    const limit = parseInt(searchParams.get('limit') || '10');
    
    // Generate mock transaction data
    const transactions = [];
    const statuses = ['CAPTURED', 'FAILED', 'PENDING'];
    
    for (let i = 0; i < limit; i++) {
      const statusIndex = Math.floor(Math.random() * statuses.length);
      const status = statuses[statusIndex];
      
      transactions.push({
        id: `txn_${Math.floor(Math.random() * 1000000)}`,
        amount: parseFloat((Math.random() * 1000).toFixed(2)),
        status,
        createdAt: new Date(Date.now() - Math.random() * 86400000 * 7).toISOString(), // Last 7 days
        customerEmail: `customer${Math.floor(Math.random() * 1000)}@example.com`
      });
    }
    
    const data = {
      transactions,
      total: 1245 // Mock total count
    };
    
    return NextResponse.json(data);
  } catch (error) {
    return NextResponse.json({ error: 'Failed to fetch recent transactions' }, { status: 500 });
  }
}
