import { HttpErrorResponse } from '@angular/common/http';

export function apiErrorMessage(error: unknown, fallback = 'Request failed'): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) {
      return 'Backend is not reachable. Check that the Spring Boot API is running.';
    }
    const payload = error.error;
    if (typeof payload === 'string' && payload.trim()) {
      return payload;
    }
    if (payload && typeof payload === 'object') {
      const message = readString(payload, 'message') || readString(payload, 'error');
      if (message) {
        return message;
      }
    }
    if (error.message) {
      return error.message;
    }
  }
  if (error instanceof Error && error.message) {
    return error.message;
  }
  return fallback;
}

function readString(payload: object, key: string): string | null {
  const value = (payload as Record<string, unknown>)[key];
  return typeof value === 'string' && value.trim() ? value : null;
}
