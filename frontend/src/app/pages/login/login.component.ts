import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { finalize } from 'rxjs';
import { apiErrorMessage } from '../../core/api-error';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSnackBarModule
  ],
  template: `
    <section class="login-page">
      <mat-card class="login-card">
        <mat-card-header>
          <mat-icon mat-card-avatar>auto_awesome</mat-icon>
          <mat-card-title>Naukri AI Job Apply Bot</mat-card-title>
          <mat-card-subtitle>Secure app login</mat-card-subtitle>
        </mat-card-header>
        <mat-card-content>
          <form [formGroup]="form" (ngSubmit)="submit()">
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Email</mat-label>
              <input matInput type="email" formControlName="email" autocomplete="username">
            </mat-form-field>
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Password</mat-label>
              <input matInput type="password" formControlName="password" autocomplete="current-password">
            </mat-form-field>
            <button mat-flat-button color="primary" class="full-width" type="submit" [disabled]="form.invalid || loading">
              @if (loading) {
                <mat-spinner diameter="18"></mat-spinner>
              } @else {
                <mat-icon>login</mat-icon>
              }
              Sign in
            </button>
          </form>
        </mat-card-content>
      </mat-card>
    </section>
  `,
  styles: [`
    .login-page {
      min-height: 100vh;
      display: grid;
      place-items: center;
      padding: 24px;
      background: linear-gradient(135deg, #eef5ff, #f8fafc 45%, #eefcf6);
    }
    .login-card {
      width: min(430px, 100%);
      border-radius: 8px;
      box-shadow: 0 18px 60px rgba(23, 32, 51, 0.14);
    }
    form {
      display: grid;
      gap: 14px;
      margin-top: 20px;
    }
    button {
      height: 46px;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
    }
  `]
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly snack = inject(MatSnackBar);
  loading = false;

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required]
  });

  constructor() {
    if (this.route.snapshot.queryParamMap.get('session') === 'expired') {
      queueMicrotask(() => this.snack.open('Your app session expired. Please sign in again.', 'Close', { duration: 5000 }));
    }
  }

  submit(): void {
    if (this.form.invalid) {
      return;
    }
    this.loading = true;
    this.auth.login(this.form.controls.email.value, this.form.controls.password.value)
      .pipe(finalize(() => this.loading = false))
      .subscribe({
        next: () => void this.router.navigate(['/dashboard']),
        error: (error) => this.snack.open(apiErrorMessage(error, 'Login failed'), 'Close', { duration: 5000 })
      });
  }
}
