import { Component, inject, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ApiService } from '../../core/api.service';
import { apiErrorMessage } from '../../core/api-error';
import { downloadBlob } from '../../core/download';
import { AppliedJob } from '../../core/models';

@Component({
  selector: 'app-history',
  standalone: true,
  imports: [DatePipe, MatButtonModule, MatIconModule, MatSnackBarModule, MatTableModule, MatTooltipModule],
  template: `
    <section class="page">
      <div class="page-header">
        <h1 class="page-title">Applied Jobs History</h1>
        <div class="actions">
          <button mat-stroked-button (click)="download('csv')">
            <mat-icon>download</mat-icon>
            CSV
          </button>
          <button mat-stroked-button (click)="download('xlsx')">
            <mat-icon>table_view</mat-icon>
            Excel
          </button>
          <button mat-icon-button aria-label="Refresh history" (click)="load()">
            <mat-icon>refresh</mat-icon>
          </button>
        </div>
      </div>
      <div class="table-wrap">
        <table mat-table [dataSource]="jobs()">
          <ng-container matColumnDef="companyName">
            <th mat-header-cell *matHeaderCellDef>Company Name</th>
            <td mat-cell *matCellDef="let row">{{ row.companyName }}</td>
          </ng-container>
          <ng-container matColumnDef="jobTitle">
            <th mat-header-cell *matHeaderCellDef>Job Title</th>
            <td mat-cell *matCellDef="let row">
              <a [href]="row.jobUrl" target="_blank">{{ row.jobTitle }}</a>
            </td>
          </ng-container>
          <ng-container matColumnDef="applyDate">
            <th mat-header-cell *matHeaderCellDef>Apply Date</th>
            <td mat-cell *matCellDef="let row">{{ row.applyDate | date:'short' }}</td>
          </ng-container>
          <ng-container matColumnDef="status">
            <th mat-header-cell *matHeaderCellDef>Status</th>
            <td mat-cell *matCellDef="let row">{{ row.status }}</td>
          </ng-container>
          <ng-container matColumnDef="redirectedExternalSite">
            <th mat-header-cell *matHeaderCellDef>External</th>
            <td mat-cell *matCellDef="let row">{{ row.redirectedExternalSite ? 'Yes' : 'No' }}</td>
          </ng-container>
          <ng-container matColumnDef="csvFileName">
            <th mat-header-cell *matHeaderCellDef>CSV file</th>
            <td mat-cell *matCellDef="let row">{{ row.csvFileName }}</td>
          </ng-container>
          <ng-container matColumnDef="failureReason">
            <th mat-header-cell *matHeaderCellDef>Failure reason</th>
            <td mat-cell *matCellDef="let row">
              <span [matTooltip]="row.failureReason">{{ row.failureReason || '-' }}</span>
            </td>
          </ng-container>
          <tr mat-header-row *matHeaderRowDef="columns"></tr>
          <tr mat-row *matRowDef="let row; columns: columns"></tr>
        </table>
      </div>
    </section>
  `
})
export class HistoryComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly snack = inject(MatSnackBar);
  readonly columns = ['companyName', 'jobTitle', 'applyDate', 'status', 'redirectedExternalSite', 'csvFileName', 'failureReason'];
  readonly jobs = signal<AppliedJob[]>([]);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.api.history().subscribe({
      next: (response) => this.jobs.set(response),
      error: (error) => this.snack.open(apiErrorMessage(error, 'Unable to load job history'), 'Close', { duration: 5000 })
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
