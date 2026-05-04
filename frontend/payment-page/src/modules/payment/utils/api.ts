/**
 * API Error Utility
 */

export function createApiError(message: string, status: number, code: string): Error & { status: number; code: string } {
  const error = new Error(message) as Error & { status: number; code: string };
  error.status = status;
  error.code = code;
  return error;
}

export function isApiError(error: unknown): error is Error & { status: number; code: string } {
  return typeof error === 'object' && error !== null && 'status' in error && 'code' in error;
}

export function getErrorMessage(error: unknown): string {
  if (isApiError(error)) {
    return error.message;
  }
  if (error instanceof Error) {
    return error.message;
  }
  return 'An unexpected error occurred';
}