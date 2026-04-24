import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { DashboardSummary, ViewName } from '../../core/models/app.models';
import { StatusChipComponent } from '../../shared/components/status-chip/status-chip.component';

@Component({
  selector: 'app-dashboard-page',
  standalone: true,
  imports: [CommonModule, StatusChipComponent],
  templateUrl: './dashboard-page.component.html',
  styleUrl: './dashboard-page.component.css'
})
export class DashboardPageComponent {
  @Input() summary: DashboardSummary | null = null;
  @Input() busy = false;
  @Output() startApply = new EventEmitter<void>();
  @Output() stopApply = new EventEmitter<void>();
  @Output() refresh = new EventEmitter<void>();
  @Output() navigate = new EventEmitter<ViewName>();

  progressWidth(): number {
    if (!this.summary?.latestRun) {
      return 0;
    }

    const total = this.summary.latestRun.jobsSearched || 1;
    const completed =
      this.summary.latestRun.jobsApplied +
      this.summary.latestRun.jobsSkipped +
      this.summary.latestRun.jobsFailed;

    return Math.min(100, Math.round((completed / total) * 100));
  }
}
