import { inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { tap } from 'rxjs';
import { environment } from './environment';
import { AuthResponse } from './models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  readonly userEmail = signal<string | null>(localStorage.getItem('userEmail'));

  login(email: string, password: string) {
    return this.http.post<AuthResponse>(`${environment.apiUrl}/auth/login`, { email, password }).pipe(
      tap((response) => {
        localStorage.setItem('token', response.token);
        localStorage.setItem('userEmail', response.email);
        this.userEmail.set(response.email);
      })
    );
  }

  logout(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('userEmail');
    this.userEmail.set(null);
    void this.router.navigate(['/login']);
  }

  isAuthenticated(): boolean {
    return Boolean(localStorage.getItem('token'));
  }

  token(): string | null {
    return localStorage.getItem('token');
  }
}
