import { inject } from '@angular/core';
import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { clearStoredAuth, isJwtUsable, storedToken } from './auth-token';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const router = inject(Router);
  const token = storedToken();
  let authenticatedRequest = request;

  if (isJwtUsable(token)) {
    authenticatedRequest = request.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  } else if (token) {
    clearStoredAuth();
  }

  return next(authenticatedRequest).pipe(
    catchError((error) => {
      if (sessionRejected(error) && !request.url.includes('/auth/login')) {
        clearStoredAuth();
        void router.navigate(['/login'], { queryParams: { session: 'expired' } });
      }
      return throwError(() => error);
    })
  );
};

function sessionRejected(error: unknown): boolean {
  return error instanceof HttpErrorResponse && (error.status === 401 || error.status === 403);
}
