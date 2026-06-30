/**
 * Environment configuration utilities
 * Centralized place for all environment variables
 */

/**
 * Get Backend API base URL
 * Priority: 1. Environment variable 2. Try to detect from window.location (if same origin) 3. Fallback to localhost
 */
export const getApiBaseUrl = (): string => {
  // If environment variable is set, use it
  if (process.env.NEXT_PUBLIC_API_BASE_URL) {
    return process.env.NEXT_PUBLIC_API_BASE_URL;
  }
  
  // If running in browser and on same origin, use relative URL
  if (typeof window !== 'undefined') {
    const hostname = window.location.hostname;
    
    // Auto-detect the custom tunnel domains
    if (hostname === 'app.dewjunior.id.vn') {
      return 'https://api.dewjunior.id.vn';
    }
    
    // Check if we're on a Cloudflare tunnel - if so, try to use backend tunnel
    // For now, fallback to localhost for local development
    if (hostname.includes('trycloudflare.com')) {
      // If frontend is on Cloudflare, backend should also be on Cloudflare
      // User needs to set NEXT_PUBLIC_API_BASE_URL environment variable
      console.warn('⚠️ Frontend is on Cloudflare but NEXT_PUBLIC_API_BASE_URL is not set. Using localhost fallback.');
    }
  }
  
  // Fallback to localhost for local development
  return 'http://localhost:8080';
};

/**
 * Get Frontend App URL (for callbacks, QR codes, etc.)
 */
export const getAppUrl = (): string => {
  // Prefer env variable, fallback to window.location.origin if available
  if (process.env.NEXT_PUBLIC_APP_URL) {
    return process.env.NEXT_PUBLIC_APP_URL;
  }
  
  // Server-side rendering fallback
  if (typeof window === 'undefined') {
    return 'http://localhost:3000';
  }
  
  return window.location.origin;
};

/**
 * Get PayOS Return URL
 */
export const getPayOSReturnUrl = (orderIds?: number[]): string => {
  const baseUrl = process.env.NEXT_PUBLIC_PAYOS_RETURN_URL || `${getAppUrl()}/payment/success`;
  
  if (orderIds && orderIds.length > 0) {
    return `${baseUrl}?orderIds=${orderIds.join(',')}`;
  }
  
  return baseUrl;
};

/**
 * Get PayOS Cancel URL
 */
export const getPayOSCancelUrl = (orderIds?: number[]): string => {
  const baseUrl = process.env.NEXT_PUBLIC_PAYOS_CANCEL_URL || `${getAppUrl()}/payment/cancel`;
  
  if (orderIds && orderIds.length > 0) {
    return `${baseUrl}?orderIds=${orderIds.join(',')}`;
  }
  
  return baseUrl;
};

/**
 * Check if running in development mode
 */
export const isDevelopment = (): boolean => {
  return process.env.NODE_ENV === 'development';
};

/**
 * Check if URL is localhost (not suitable for PayOS)
 */
export const isLocalhostUrl = (url: string): boolean => {
  return url.includes('localhost') || url.includes('127.0.0.1');
};

/**
 * Validate PayOS URLs (must not be localhost)
 */
export const validatePayOSUrls = (): { valid: boolean; warnings: string[] } => {
  const warnings: string[] = [];
  const appUrl = getAppUrl();
  
  if (isLocalhostUrl(appUrl)) {
    warnings.push('⚠️ App URL is localhost - PayOS will reject this!');
    warnings.push('   → Set NEXT_PUBLIC_APP_URL in .env.local to your ngrok URL');
    warnings.push('   → Example: NEXT_PUBLIC_APP_URL=https://abc123.ngrok.io');
  }
  
  const returnUrl = getPayOSReturnUrl();
  const cancelUrl = getPayOSCancelUrl();
  
  if (isLocalhostUrl(returnUrl)) {
    warnings.push('⚠️ PayOS Return URL is localhost');
  }
  
  if (isLocalhostUrl(cancelUrl)) {
    warnings.push('⚠️ PayOS Cancel URL is localhost');
  }
  
  return {
    valid: warnings.length === 0,
    warnings
  };
};

/**
 * Log environment configuration (for debugging)
 */
export const logEnvConfig = () => {
  // Debug logging removed
};
