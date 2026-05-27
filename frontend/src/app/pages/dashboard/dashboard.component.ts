import { Component, inject, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ApiService } from '../../core/api.service';
import { apiErrorMessage } from '../../core/api-error';
import { downloadBlob } from '../../core/download';
import { DashboardResponse } from '../../core/models';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [DatePipe, MatButtonModule, MatCardModule, MatIconModule, MatProgressBarModule, MatSnackBarModule],
  template: `
    <section class="page">
      <div class="page-header">
        <div>
          <h1 class="page-title">Dashboard</h1>
          <div class="muted">Last run: {{ data()?.lastRunTime ? (data()?.lastRunTime | date:'medium') : 'Not run yet' }}</div>
        </div>
        <div class="actions">
          <button mat-stroked-button (click)="download('csv')">
            <mat-icon>download</mat-icon>
            CSV
          </button>
          <button mat-stroked-button (click)="download('xlsx')">
            <mat-icon>table_view</mat-icon>
            Excel
          </button>
          <button mat-icon-button aria-label="Refresh dashboard" (click)="load()">
            <mat-icon>refresh</mat-icon>
          </button>
        </div>
      </div>
      @if (loading()) {
        <mat-progress-bar mode="indeterminate"></mat-progress-bar>
      }
      <div class="grid cols-4">
        <mat-card class="metric">
          <mat-card-header><mat-card-title>Total applied</mat-card-title></mat-card-header>
          <mat-card-content><div class="metric-value">{{ data()?.totalAppliedJobs ?? 0 }}</div></mat-card-content>
        </mat-card>
        <mat-card class="metric">
          <mat-card-header><mat-card-title>Failed</mat-card-title></mat-card-header>
          <mat-card-content><div class="metric-value">{{ data()?.totalFailedJobs ?? 0 }}</div></mat-card-content>
        </mat-card>
        <mat-card class="metric">
          <mat-card-header><mat-card-title>External redirects</mat-card-title></mat-card-header>
          <mat-card-content><div class="metric-value">{{ data()?.externalRedirectJobs ?? 0 }}</div></mat-card-content>
        </mat-card>
        <mat-card class="metric">
          <mat-card-header><mat-card-title>Today</mat-card-title></mat-card-header>
          <mat-card-content><div class="metric-value">{{ data()?.todaysApplications ?? 0 }}</div></mat-card-content>
        </mat-card>
      </div>
      <mat-card>
        <mat-card-header>
          <mat-icon mat-card-avatar>{{ data()?.botRunning ? 'play_circle' : 'pause_circle' }}</mat-icon>
          <mat-card-title>Bot status</mat-card-title>
          <mat-card-subtitle>{{ data()?.botStatus || 'IDLE' }}</mat-card-subtitle>
        </mat-card-header>
        @if (data()?.botMessage) {
          <mat-card-content>
            <p class="muted">{{ data()?.botMessage }}</p>
          </mat-card-content>
        }
      </mat-card>
    </section>
  `
})
export class DashboardComponent implements OnInit {
  readonly api = inject(ApiService);
  private readonly snack = inject(MatSnackBar);
  readonly data = signal<DashboardResponse | null>(null);
  readonly loading = signal(false);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.api.dashboard().subscribe({
      next: (response) => {
        this.data.set(response);
        this.loading.set(false);
      },
      error: (error) => {
        this.loading.set(false);
        this.snack.open(apiErrorMessage(error, 'Unable to load dashboard'), 'Close', { duration: 5000 });
      }
    });
  }

  download(format: 'csv' | 'xlsx'): void {
    this.api.exportReport(format).subscribe({
      next: (blob) => {
        const mimeType = format === 'csv'
          ? 'text/csv;charset=utf-8'
          : 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet';
        downloadBlob(blob, `applied-jobs.${format}`, mimeType);
        this.snack.open(`${format.toUpperCase()} export downloaded`, 'Close', { duration: 3000 });
      },
      error: (error) => this.snack.open(apiErrorMessage(error, 'Unable to export report'), 'Close', { duration: 5000 })
    });
  }
}
