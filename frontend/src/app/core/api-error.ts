import { HttpErrorResponse } from '@angular/common/http';

export function apiErrorMessage(error: unknown, fallback = 'Request failed'): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 200 && error.message.includes('Http failure during parsing')) {
      return 'Frontend dev proxy is not active. Restart the Angular app with npm.cmd start so /api requests forward to Spring Boot.';
    }
    if (error.status === 0) {
      return 'Backend is not reachable. Check that the Spring Boot API is running.';
    }
    if (error.status === 401 || error.status === 403) {
      return 'Your app session expired. Please sign in again.';
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
