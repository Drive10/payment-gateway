import { render, screen } from '@testing-library/react';
import { PaymentsOverview } from '@/src/components/dashboard/PaymentsOverview';

// Mock axios
jest.mock('axios');
import axios from 'axios';

describe('PaymentsOverview', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('displays loading state initially', async () => {
    // Mock axios to return a delayed response
    axios.get.mockImplementation(() => 
      new Promise(resolve => 
        setTimeout(() => resolve({ data: {
          totalPayments: 100,
          successfulPayments: 90,
          failedPayments: 10,
          totalAmount: 5000
        } }), 100)
      )
    );

    render(<PaymentsOverview />);
    
    // Should show loading text initially
    expect(screen.getByText(/loading/i)).toBeInTheDocument();
  });

  it('displays payment overview data when loaded', async () => {
    // Mock axios response
    axios.get.mockResolvedValue({
      data: {
        totalPayments: 1245,
        successfulPayments: 1180,
        failedPayments: 65,
        totalAmount: 89450.50
      }
    });

    render(<PaymentsOverview />);
    
    // Wait for data to load
    await screen.findByText(/payment overview/i);
    
    // Check that data is displayed
    expect(screen.getByText(/1,245/i)).toBeInTheDocument(); // totalPayments
    expect(screen.getByText(/1,180/i)).toBeInTheDocument(); // successfulPayments
    expect(screen.getByText(/65/i)).toBeInTheDocument(); // failedPayments
    expect(screen.getByText(/\$89,450.50/i)).toBeInTheDocument(); // totalAmount
  });

  it('displays error message when API fails', async () => {
    // Mock axios to reject
    axios.get.mockRejectedValue(new Error('API Error'));

    render(<PaymentsOverview />);
    
    // Wait for error to appear
    await screen.findByText(/error loading data/i);
    
    // Check that error is displayed
    expect(screen.getByText(/error loading data/i)).toBeInTheDocument();
  });
});
