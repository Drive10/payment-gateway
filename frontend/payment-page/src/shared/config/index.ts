/**
 * Environment Configuration
 * 
 * Centralized config for different environments
 */

export const config = {
  // API Configuration
  api: {
    baseUrl: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
    gatewayUrl: import.meta.env.VITE_API_GATEWAY_URL || 'http://localhost:8080',
    timeout: parseInt(import.meta.env.VITE_API_TIMEOUT || '30000'),
  },
  
  // Merchant Configuration  
  merchant: {
    id: import.meta.env.VITE_MERCHANT_ID || null,
    apiKey: import.meta.env.VITE_MERCHANT_API_KEY || null,
  },
  
  // Feature Flags
  features: {
    enable3ds: import.meta.env.VITE_ENABLE_3DS !== 'false',
    enableUpi: import.meta.env.VITE_ENABLE_UPI !== 'false',
    enableWallet: import.meta.env.VITE_ENABLE_WALLET !== 'false',
    enableNetbanking: import.meta.env.VITE_ENABLE_NETBANKING !== 'false',
  },
  
  // Limits
  limits: {
    minAmount: parseInt(import.meta.env.VITE_MIN_AMOUNT || '10'),
    maxAmount: parseInt(import.meta.env.VITE_MAX_AMOUNT || '100000'),
  },
  
  // UI Configuration
  ui: {
    checkoutUrl: import.meta.env.VITE_CHECKOUT_URL || 'http://localhost:5173',
    theme: import.meta.env.VITE_THEME || 'light',
  },
};

/**
 * Check if running in production mode
 */
export function isProduction(): boolean {
  return import.meta.env.MODE === 'production';
}

/**
 * Check if running in development mode  
 */
export function isDevelopment(): boolean {
  return import.meta.env.DEV;
}

/**
 * Get current environment
 */
export function getEnvironment(): 'development' | 'production' | 'test' {
  if (isProduction()) return 'production';
  if (isDevelopment()) return 'development';
  return 'test';
}