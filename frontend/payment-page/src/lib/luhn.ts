export function luhnCheck(cardNumber: string): boolean {
  const digits = cardNumber.replace(/\D/g, '');
  
  if (digits.length < 13 || digits.length > 19) {
    return false;
  }

  let sum = 0;
  let isEven = false;

  for (let i = digits.length - 1; i >= 0; i--) {
    let digit = parseInt(digits[i], 10);

    if (isEven) {
      digit *= 2;
      if (digit > 9) {
        digit -= 9;
      }
    }

    sum += digit;
    isEven = !isEven;
  }

  return sum % 10 === 0;
}

export function formatCardNumber(value: string): string {
  const digits = value.replace(/\D/g, '');
  const groups = digits.match(/.{1,4}/g) || [];
  return groups.join(' ').substr(0, 19);
}

export function formatExpiry(value: string): string {
  const digits = value.replace(/\D/g, '');
  
  if (digits.length >= 2) {
    return digits.substr(0, 2) + '/' + digits.substr(2, 2);
  }
  
  return digits;
}

export function maskCardNumber(cardNumber: string): string {
  const digits = cardNumber.replace(/\D/g, '');
  if (digits.length < 4) return cardNumber;
  
  const lastFour = digits.slice(-4);
  const masked = '•'.repeat(digits.length - 4) + lastFour;
  
  return formatCardNumber(masked);
}

export function detectCardBrand(cardNumber: string): 'visa' | 'mastercard' | 'amex' | 'rupay' | 'unknown' {
  const digits = cardNumber.replace(/\D/g, '');
  
  if (/^4/.test(digits)) return 'visa';
  if (/^5[1-5]/.test(digits) || /^2[2-7]/.test(digits)) return 'mastercard';
  if (/^3[47]/.test(digits)) return 'amex';
  if (/^6(?:011|05)/.test(digits)) return 'rupay';
  
  return 'unknown';
}
