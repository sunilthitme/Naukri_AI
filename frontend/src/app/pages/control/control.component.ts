import { Component, inject, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ApiService } from '../../core/api.service';
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
          <div class="actions">
            <button mat-flat-button color="primary" (click)="command('start')"><mat-icon>play_arrow</mat-icon>Start Bot</button>
            <button mat-stroked-button color="warn" (click)="command('stop')"><mat-icon>stop</mat-icon>Stop Bot</button>
            <button mat-stroked-button (click)="command('pause')"><mat-icon>pause</mat-icon>Pause Bot</button>
            <button mat-stroked-button (click)="command('resume')"><mat-icon>resume</mat-icon>Resume Bot</button>
            <button mat-stroked-button (click)="command('test-login')"><mat-icon>verified_user</mat-icon>Test Login</button>
            <button mat-stroked-button (click)="command('test-apply')"><mat-icon>fact_check</mat-icon>Test Apply</button>
          </div>
        </mat-card-content>
      </mat-card>
    </section>
  `
})
export class ControlComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly snack = inject(MatSnackBar);
  readonly status = signal<BotStatus | null>(null);

  ngOnInit(): void {
    this.refresh();
  }

  command(command: 'start' | 'stop' | 'pause' | 'resume' | 'test-login' | 'test-apply'): void {
    this.api.botCommand(command).subscribe({
      next: (response) => {
        this.snack.open(response.message, 'Close', { duration: 3000 });
        this.refresh();
      },
      error: () => this.snack.open('Command failed', 'Close', { duration: 3000 })
    });
  }

  refresh(): void {
    this.api.botStatus().subscribe((response) => this.status.set(response));
  }
}
