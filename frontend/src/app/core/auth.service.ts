import { inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { tap } from 'rxjs';
import { environment } from './environment';
import { AuthResponse } from './models';
import { clearStoredAuth, isJwtUsable, saveStoredAuth, storedToken, storedUserEmail } from './auth-token';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  readonly userEmail = signal<string | null>(storedUserEmail());

  login(email: string, password: string) {
    return this.http.post<AuthResponse>(`${environment.apiUrl}/auth/login`, { email, password }).pipe(
      tap((response) => {
        saveStoredAuth(response.token, response.email);
        this.userEmail.set(response.email);
      })
    );
  }

  logout(): void {
    clearStoredAuth();
    this.userEmail.set(null);
    void this.router.navigate(['/login']);
  }

  expireSession(): void {
    clearStoredAuth();
    this.userEmail.set(null);
  }

  isAuthenticated(): boolean {
    const authenticated = isJwtUsable(storedToken());
    if (!authenticated) {
      this.expireSession();
    }
    return authenticated;
  }

  token(): string | null {
    const token = storedToken();
    return isJwtUsable(token) ? token : null;
  }
}
