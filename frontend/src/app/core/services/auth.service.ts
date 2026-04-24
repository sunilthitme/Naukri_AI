import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { ApiResponse, AuthResponse, LoginRequest } from '../models/app.models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = 'http://localhost:8080/api/auth';

  login(payload: LoginRequest) {
    return this.http.post<ApiResponse<AuthResponse>>(`${this.baseUrl}/login`, payload);
  }
}
