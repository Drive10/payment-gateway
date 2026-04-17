import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import CardForm from '../../src/components/CardForm';

describe('CardForm Component', () => {
  const defaultProps = {
    values: {
      cardNumber: '',
      expiry: '',
      cvv: '',
      cardholder: '',
    },
    errors: {},
    onChange: vi.fn(),
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should render all input fields', () => {
    render(<CardForm {...defaultProps} />);
    
    expect(screen.getByPlaceholderText('4242 4242 4242 4242')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('MM/YY')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('•••')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Jordan Lee')).toBeInTheDocument();
  });

  it('should show placeholder card when no values entered', () => {
    render(<CardForm {...defaultProps} />);
    
    expect(screen.getByText('•••• •••• •••• ••••')).toBeInTheDocument();
    expect(screen.getByText('Your Name')).toBeInTheDocument();
    expect(screen.getByText('MM/YY')).toBeInTheDocument();
  });

  it('should display card number when entered', () => {
    render(<CardForm {...defaultProps} values={{ ...defaultProps.values, cardNumber: '4242 4242 4242 4242', cardholder: 'John Doe' }} />);
    
    expect(screen.getByText('4242 4242 4242 4242')).toBeInTheDocument();
    expect(screen.getByText('John Doe')).toBeInTheDocument();
  });

  it('should display expiry when entered', () => {
    render(<CardForm {...defaultProps} values={{ ...defaultProps.values, expiry: '12/28' }} />);
    
    expect(screen.getByText('12/28')).toBeInTheDocument();
  });

  it('should show Visa badge when card starts with 4', () => {
    render(<CardForm {...defaultProps} values={{ ...defaultProps.values, cardNumber: '4111111111111111' }} />);
    
    expect(screen.getByText('Visa')).toBeInTheDocument();
  });

  it('should show Mastercard badge for Mastercard numbers', () => {
    render(<CardForm {...defaultProps} values={{ ...defaultProps.values, cardNumber: '5111111111111111' }} />);
    
    expect(screen.getByText('Mastercard')).toBeInTheDocument();
  });

  it('should show Amex badge for Amex cards', () => {
    render(<CardForm {...defaultProps} values={{ ...defaultProps.values, cardNumber: '371111111111111' }} />);
    
    expect(screen.getByText('Amex')).toBeInTheDocument();
  });

  it('should show RuPay badge for RuPay cards', () => {
    render(<CardForm {...defaultProps} values={{ ...defaultProps.values, cardNumber: '6011111111111111' }} />);
    
    expect(screen.getByText('RuPay')).toBeInTheDocument();
  });

  it('should show Card badge for unknown brands', () => {
    render(<CardForm {...defaultProps} values={{ ...defaultProps.values, cardNumber: '9111111111111111' }} />);
    
    expect(screen.getByText('Card')).toBeInTheDocument();
  });

  it('should call onChange when card number changes', () => {
    render(<CardForm {...defaultProps} />);
    
    const input = screen.getByPlaceholderText('4242 4242 4242 4242');
    fireEvent.change(input, { target: { value: '4242' } });
    
    expect(defaultProps.onChange).toHaveBeenCalledWith('cardNumber', '4242');
  });

  it('should call onChange when expiry changes', () => {
    render(<CardForm {...defaultProps} />);
    
    const input = screen.getByPlaceholderText('MM/YY');
    fireEvent.change(input, { target: { value: '12' } });
    
    expect(defaultProps.onChange).toHaveBeenCalledWith('expiry', '12');
  });

  it('should call onChange when CVV changes', () => {
    render(<CardForm {...defaultProps} />);
    
    const input = screen.getByPlaceholderText('•••');
    fireEvent.change(input, { target: { value: '123' } });
    
    expect(defaultProps.onChange).toHaveBeenCalledWith('cvv', '123');
  });

  it('should call onChange when cardholder name changes', () => {
    render(<CardForm {...defaultProps} />);
    
    const input = screen.getByPlaceholderText('Jordan Lee');
    fireEvent.change(input, { target: { value: 'John Doe' } });
    
    expect(defaultProps.onChange).toHaveBeenCalledWith('cardholder', 'John Doe');
  });

  it('should display error message for card number', () => {
    const propsWithErrors = {
      ...defaultProps,
      errors: { cardNumber: 'Invalid card number' },
    };
    render(<CardForm {...propsWithErrors} />);
    
    expect(screen.getByText('Invalid card number')).toBeInTheDocument();
  });

  it('should display error message for expiry', () => {
    const propsWithErrors = {
      ...defaultProps,
      errors: { expiry: 'Invalid expiry' },
    };
    render(<CardForm {...propsWithErrors} />);
    
    expect(screen.getByText('Invalid expiry')).toBeInTheDocument();
  });

  it('should display error message for CVV', () => {
    const propsWithErrors = {
      ...defaultProps,
      errors: { cvv: 'Invalid CVV' },
    };
    render(<CardForm {...propsWithErrors} />);
    
    expect(screen.getByText('Invalid CVV')).toBeInTheDocument();
  });

  it('should display error message for cardholder', () => {
    const propsWithErrors = {
      ...defaultProps,
      errors: { cardholder: 'Name required' },
    };
    render(<CardForm {...propsWithErrors} />);
    
    expect(screen.getByText('Name required')).toBeInTheDocument();
  });

  it('should show validation hint when card is valid', () => {
    render(<CardForm {...defaultProps} values={{ ...defaultProps.values, cardNumber: '4242424242424242' }} />);
    
    expect(screen.getByText('✓ Card number is valid')).toBeInTheDocument();
  });

  it('should show validation hint when card is incomplete', () => {
    render(<CardForm {...defaultProps} values={{ ...defaultProps.values, cardNumber: '4242' }} />);
    
    expect(screen.getByText('Enter 16-digit card number')).toBeInTheDocument();
  });

  it('should display encryption notice', () => {
    render(<CardForm {...defaultProps} />);
    
    expect(screen.getByText(/Your card details are encrypted/)).toBeInTheDocument();
  });

  it('should have proper input types for security', () => {
    render(<CardForm {...defaultProps} />);
    
    const cvvInput = screen.getByPlaceholderText('•••');
    expect(cvvInput).toHaveAttribute('type', 'password');
  });

  it('should limit CVV to 3 digits', () => {
    render(<CardForm {...defaultProps} />);
    
    const input = screen.getByPlaceholderText('•••');
    fireEvent.change(input, { target: { value: '12345' } });
    
    expect(defaultProps.onChange).toHaveBeenCalledWith('cvv', '123');
  });

  it('should display correct card brand gradient for Visa', () => {
    render(<CardForm {...defaultProps} values={{ ...defaultProps.values, cardNumber: '4111111111111111' }} />);
    
    const cardDiv = screen.getByText('4242 4242 4242 4242').closest('div').parentElement.parentElement;
    expect(cardDiv).toHaveClass(/from-blue-/);
  });

  it('should display correct card brand gradient for Mastercard', () => {
    render(<CardForm {...defaultProps} values={{ ...defaultProps.values, cardNumber: '5111111111111111' }} />);
    
    const cardDiv = screen.getByText('4242 4242 4242 4242').closest('div').parentElement.parentElement;
    expect(cardDiv).toHaveClass(/from-orange-/);
  });
});
