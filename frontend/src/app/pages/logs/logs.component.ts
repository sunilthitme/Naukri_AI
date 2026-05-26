import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { timer, switchMap } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { BotLog } from '../../core/models';

@Component({
  selector: 'app-logs',
  standalone: true,
  imports: [DatePipe, MatButtonModule, MatIconModule],
  template: `
    <section class="page">
      <div class="page-header">
        <h1 class="page-title">Live Logs</h1>
        <button mat-icon-button aria-label="Refresh logs" (click)="load()">
          <mat-icon>refresh</mat-icon>
        </button>
      </div>
      <div class="log-panel">
        @for (log of logs(); track log.id) {
          <div class="log-line" [class.warn]="log.level === 'WARN'" [class.error]="log.level === 'ERROR'">
            <span>{{ log.createdAt | date:'HH:mm:ss' }}</span>
            <strong>{{ log.level }}</strong>
            <span>{{ log.message }}</span>
          </div>
        } @empty {
          <div class="muted">No logs yet.</div>
        }
      </div>
    </section>
  `,
  styles: [`
    .log-panel {
      min-height: 520px;
      background: #0b1020;
      color: #d8e0ff;
      border-radius: 8px;
      padding: 14px;
      font-family: Consolas, Monaco, monospace;
      overflow: auto;
    }
    .log-line {
      display: grid;
      grid-template-columns: 76px 58px 1fr;
      gap: 10px;
      padding: 6px 0;
      border-bottom: 1px solid rgba(255, 255, 255, 0.06);
    }
    .log-line.warn { color: #ffe49a; }
    .log-line.error { color: #ffb4b4; }
  `]
})
export class LogsComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly destroyRef = inject(DestroyRef);
  readonly logs = signal<BotLog[]>([]);

  ngOnInit(): void {
    timer(0, 5000).pipe(
      switchMap(() => this.api.logs()),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((response) => this.logs.set(response));
  }

  load(): void {
    this.api.logs().subscribe((response) => this.logs.set(response));
  }
}
