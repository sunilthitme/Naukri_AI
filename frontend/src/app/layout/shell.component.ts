import { Component, DestroyRef, inject, OnInit } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatToolbarModule } from '@angular/material/toolbar';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EMPTY, catchError, timer } from 'rxjs';
import { ApiService } from '../core/api.service';
import { apiErrorMessage } from '../core/api-error';
import { AuthService } from '../core/auth.service';
import { PendingQuestion } from '../core/models';
import { PendingQuestionDialogComponent } from './pending-question-dialog.component';

interface NavItem {
  label: string;
  icon: string;
  route: string;
}

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatButtonModule,
    MatDialogModule,
    MatIconModule,
    MatListModule,
    MatSnackBarModule,
    MatSidenavModule,
    MatToolbarModule
  ],
  template: `
    <mat-sidenav-container class="shell">
      <mat-sidenav mode="side" opened class="side">
        <div class="brand">
          <mat-icon>auto_awesome</mat-icon>
          <span>Naukri AI Bot</span>
        </div>
        <mat-nav-list>
          @for (item of navItems; track item.route) {
            <a mat-list-item [routerLink]="item.route" routerLinkActive="active-link">
              <mat-icon matListItemIcon>{{ item.icon }}</mat-icon>
              <span matListItemTitle>{{ item.label }}</span>
            </a>
          }
        </mat-nav-list>
      </mat-sidenav>
      <mat-sidenav-content>
        <mat-toolbar>
          <span class="toolbar-title">Naukri AI Job Apply Bot</span>
          <span class="spacer"></span>
          <span class="user">{{ auth.userEmail() }}</span>
          <button mat-icon-button aria-label="Logout" (click)="auth.logout()">
            <mat-icon>logout</mat-icon>
          </button>
        </mat-toolbar>
        <main>
          <router-outlet />
        </main>
      </mat-sidenav-content>
    </mat-sidenav-container>
  `,
  styles: [`
    .shell { min-height: 100vh; }
    .side {
      width: 252px;
      border-right: 1px solid #dde3ee;
      background: #ffffff;
    }
    .brand {
      height: 64px;
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 0 20px;
      font-weight: 700;
      color: #0f172a;
    }
    mat-toolbar {
      position: sticky;
      top: 0;
      z-index: 3;
      background: #ffffff;
      border-bottom: 1px solid #dde3ee;
    }
    main {
      padding: 24px;
      max-width: 1440px;
    }
    .spacer { flex: 1; }
    .user {
      font-size: 13px;
      color: #667085;
      margin-right: 8px;
    }
    .active-link {
      background: #e9efff;
      color: #174ea6;
    }
    @media (max-width: 820px) {
      .side { width: 74px; }
      .brand span, .user, [matListItemTitle] { display: none; }
      main { padding: 16px; }
      .toolbar-title { font-size: 16px; }
    }
  `]
})
export class ShellComponent implements OnInit {
  readonly auth = inject(AuthService);
  private readonly api = inject(ApiService);
  private readonly dialog = inject(MatDialog);
  private readonly snack = inject(MatSnackBar);
  private readonly destroyRef = inject(DestroyRef);
  private activeQuestionId: string | null = null;

  readonly navItems: NavItem[] = [
    { label: 'Dashboard', icon: 'dashboard', route: '/dashboard' },
    { label: 'Credentials', icon: 'vpn_key', route: '/credentials' },
    { label: 'Job Filters', icon: 'tune', route: '/filters' },
    { label: 'Q&A', icon: 'question_answer', route: '/questions' },
    { label: 'History', icon: 'work_history', route: '/history' },
    { label: 'Bot Control', icon: 'smart_toy', route: '/control' },
    { label: 'Logs', icon: 'terminal', route: '/logs' }
  ];

  ngOnInit(): void {
    timer(0, 3000).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => this.checkPendingQuestion());
  }

  private checkPendingQuestion(): void {
    this.api.pendingQuestion().pipe(
      catchError(() => EMPTY)
    ).subscribe((question) => {
      if (!question) {
        return;
      }
      this.openPendingQuestion(question);
    });
  }

  private openPendingQuestion(question: PendingQuestion): void {
    if (this.activeQuestionId === question.id) {
      return;
    }
    this.activeQuestionId = question.id;
    const dialogRef = this.dialog.open(PendingQuestionDialogComponent, {
      data: question,
      disableClose: true,
      width: '560px'
    });
    dialogRef.afterClosed().pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((answer) => {
      if (!answer) {
        this.activeQuestionId = null;
        return;
      }
      this.api.answerPendingQuestion(question.id, { answer }).subscribe({
        next: () => {
          this.activeQuestionId = null;
          this.snack.open('Answer saved. Automation is continuing.', 'Close', { duration: 4000 });
        },
        error: (error) => {
          this.activeQuestionId = null;
          this.snack.open(apiErrorMessage(error, 'Unable to submit answer'), 'Close', { duration: 5000 });
        }
      });
    });
  }
}
