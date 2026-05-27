import { Component, DestroyRef, computed, inject, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { EMPTY, catchError, finalize, switchMap, timer } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { apiErrorMessage } from '../../core/api-error';
import { BotStatus } from '../../core/models';

@Component({
  selector: 'app-control',
  standalone: true,
  imports: [DatePipe, MatButtonModule, MatCardModule, MatIconModule, MatSnackBarModule],
  template: `
    <section class="page">
      <div class="page-header">
        <h1 class="page-title">Bot Control</h1>
      </div>
      <mat-card>
        <mat-card-header>
          <mat-icon mat-card-avatar>smart_toy</mat-icon>
          <mat-card-title>{{ status()?.status || 'IDLE' }}</mat-card-title>
          <mat-card-subtitle>{{ status()?.message || 'Ready' }}</mat-card-subtitle>
        </mat-card-header>
        <mat-card-content>
          <p class="muted">Updated: {{ status()?.updatedAt ? (status()?.updatedAt | date:'medium') : '-' }}</p>
          @if (currentActivity()) {
            <div class="status-alert info">
              <mat-icon>pending_actions</mat-icon>
              <div>
                <strong>Current activity</strong>
                <span>{{ currentActivity() }}</span>
              </div>
            </div>
          }
          @if (failureMessage()) {
            <div class="status-alert error">
              <mat-icon>error</mat-icon>
              <div>
                <strong>{{ status()?.status === 'CAPTCHA_REQUIRED' ? 'Manual action required' : 'Failure reason' }}</strong>
                <span>{{ failureMessage() }}</span>
                @if (status()?.status === 'CAPTCHA_REQUIRED') {
                  <button mat-flat-button color="primary" class="inline-action" (click)="command('manual-login')" [disabled]="commandRunning()">
                    <mat-icon>open_in_new</mat-icon>
                    Login Manually & Continue
                  </button>
                }
              </div>
            </div>
          }
          @if (commandError()) {
            <div class="status-alert error">
              <mat-icon>report</mat-icon>
              <div>
                <strong>Command failed</strong>
                <span>{{ commandError() }}</span>
              </div>
            </div>
          }
          <div class="actions">
            <button mat-flat-button color="primary" (click)="command('start')" [disabled]="commandRunning()"><mat-icon>play_arrow</mat-icon>Start Bot</button>
            <button mat-stroked-button color="warn" (click)="command('stop')" [disabled]="commandRunning()"><mat-icon>stop</mat-icon>Stop Bot</button>
            <button mat-stroked-button (click)="command('pause')" [disabled]="commandRunning()"><mat-icon>pause</mat-icon>Pause Bot</button>
            <button mat-stroked-button (click)="command('resume')" [disabled]="commandRunning()"><mat-icon>resume</mat-icon>Resume Bot</button>
            @if (status()?.status === 'CAPTCHA_REQUIRED') {
              <button mat-flat-button color="accent" (click)="command('manual-login')" [disabled]="commandRunning()"><mat-icon>open_in_new</mat-icon>Login Manually</button>
            }
            <button mat-stroked-button (click)="command('test-login')" [disabled]="commandRunning()"><mat-icon>verified_user</mat-icon>Test Login</button>
            <button mat-stroked-button (click)="command('test-apply')" [disabled]="commandRunning()"><mat-icon>fact_check</mat-icon>Test Apply</button>
          </div>
        </mat-card-content>
      </mat-card>
    </section>
  `,
  styles: [`
    .status-alert {
      display: grid;
      grid-template-columns: 24px 1fr;
      gap: 10px;
      align-items: start;
      border-radius: 8px;
      padding: 12px;
      margin: 12px 0;
    }
    .status-alert.error {
      background: #fff1f0;
      color: #8a1f11;
      border: 1px solid #ffc9c2;
    }
    .status-alert.info {
      background: #eef6ff;
      color: #0b4f8a;
      border: 1px solid #b8dafc;
    }
    .status-alert div {
      display: grid;
      gap: 4px;
    }
    .inline-action {
      width: fit-content;
      margin-top: 8px;
    }
  `]
})
export class ControlComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly snack = inject(MatSnackBar);
  private readonly destroyRef = inject(DestroyRef);
  readonly status = signal<BotStatus | null>(null);
  readonly commandError = signal<string | null>(null);
  readonly commandRunning = signal(false);
  readonly currentActivity = computed(() => {
    const current = this.status();
    if (!current || current.status !== 'RUNNING') {
      return null;
    }
    return current.message || 'Starting automation...';
  });
  readonly failureMessage = computed(() => {
    const current = this.status();
    if (!current || (current.status !== 'FAILED' && current.status !== 'CAPTCHA_REQUIRED')) {
      return null;
    }
    return current.message || 'The bot stopped unexpectedly. Check live logs for details.';
  });

  ngOnInit(): void {
    timer(0, 5000).pipe(
      switchMap(() => this.api.botStatus().pipe(
        catchError((error) => {
          this.commandError.set(apiErrorMessage(error, 'Unable to load bot status'));
          return EMPTY;
        })
      )),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((response) => this.status.set(response));
  }

  command(command: 'start' | 'stop' | 'pause' | 'resume' | 'manual-login' | 'test-login' | 'test-apply'): void {
    this.commandError.set(null);
    this.commandRunning.set(true);
    this.api.botCommand(command).pipe(
      finalize(() => this.commandRunning.set(false))
    ).subscribe({
      next: (response) => {
        if (response.status === 'FAILED' || response.status === 'CAPTCHA_REQUIRED') {
          this.commandError.set(response.message);
        }
        this.snack.open(response.message, 'Close', { duration: 5000 });
        this.refresh();
      },
      error: (error) => {
        const message = apiErrorMessage(error, 'Command failed');
        this.commandError.set(message);
        this.snack.open(message, 'Close', { duration: 6000 });
        this.refresh();
      }
    });
  }

  refresh(): void {
    this.api.botStatus().subscribe({
      next: (response) => this.status.set(response),
      error: (error) => this.commandError.set(apiErrorMessage(error, 'Unable to load bot status'))
    });
  }
}
