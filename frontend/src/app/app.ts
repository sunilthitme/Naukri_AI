import { CommonModule } from '@angular/common';
import { Component, OnDestroy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import {
  AiSuggestion,
  AutomationStatus,
  DashboardSummary,
  DailyReport,
  LogEntry,
  QuestionItem,
  UserProfile,
  ViewName
} from './core/models/app.models';
import { AuthService } from './core/services/auth.service';
import { JobAssistantService } from './core/services/job-assistant.service';
import { DashboardPageComponent } from './pages/dashboard/dashboard-page.component';
import { LogsPageComponent } from './pages/logs/logs-page.component';
import { ProfilePageComponent } from './pages/profile/profile-page.component';
import { QuestionsPageComponent } from './pages/questions/questions-page.component';
import { ReportsPageComponent } from './pages/reports/reports-page.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    DashboardPageComponent,
    ProfilePageComponent,
    QuestionsPageComponent,
    ReportsPageComponent,
    LogsPageComponent
  ],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class AppComponent implements OnDestroy {
  readonly navItems: Array<{ label: string; view: ViewName }> = [
    { label: 'Dashboard', view: 'dashboard' },
    { label: 'Profile', view: 'profile' },
    { label: 'Questions', view: 'questions' },
    { label: 'Reports', view: 'reports' },
    { label: 'Logs', view: 'logs' }
  ];

  currentView: ViewName = 'dashboard';
  token = localStorage.getItem('jobAssistantToken') ?? '';
  loginEmail = 'admin@aijobapply.local';
  loginPassword = 'Admin@123';
  busyMessage = '';
  flashMessage = '';
  flashType: 'success' | 'error' | 'info' = 'info';

  authBusy = false;
  dashboardBusy = false;
  profileBusy = false;
  questionBusy = false;
  questionSuggestingId: number | null = null;
  reportBusy = false;
  logsBusy = false;

  summary: DashboardSummary | null = null;
  profile: UserProfile | null = null;
  questions: QuestionItem[] = [];
  suggestedAnswers: Record<number, string> = {};
  report: DailyReport | null = null;
  logs: LogEntry[] = [];

  private pollingHandle: number | null = null;

  constructor(
    private readonly authService: AuthService,
    private readonly jobAssistantService: JobAssistantService
  ) {
    if (this.isAuthenticated) {
      void this.refreshAll();
    }
  }

  get isAuthenticated(): boolean {
    return !!this.token;
  }

  get activeStatus(): AutomationStatus | null {
    return this.summary?.latestRun ?? null;
  }

  async login(): Promise<void> {
    this.authBusy = true;
    this.flashMessage = '';

    try {
      const response = await firstValueFrom(this.authService.login({
        email: this.loginEmail.trim(),
        password: this.loginPassword
      }));

      this.token = response.data.token;
      localStorage.setItem('jobAssistantToken', this.token);
      this.profile = response.data.user;
      this.showFlash('success', 'Login successful.');
      await this.refreshAll();
    } catch (error) {
      this.handleError(error, 'Login failed. Check that the backend is running and your credentials are valid.');
    } finally {
      this.authBusy = false;
    }
  }

  logout(): void {
    this.stopPolling();
    this.token = '';
    this.summary = null;
    this.profile = null;
    this.questions = [];
    this.report = null;
    this.logs = [];
    localStorage.removeItem('jobAssistantToken');
    this.showFlash('info', 'Session cleared.');
  }

  async refreshAll(): Promise<void> {
    await Promise.all([
      this.loadDashboard(),
      this.loadProfile(),
      this.loadQuestions(),
      this.loadReport(),
      this.loadLogs()
    ]);
  }

  async startAutomation(): Promise<void> {
    if (!this.token) {
      return;
    }

    this.dashboardBusy = true;
    try {
      await firstValueFrom(this.jobAssistantService.startAutomation(this.token));
      this.showFlash('success', 'Automation started.');
      await Promise.all([this.loadDashboard(), this.loadLogs()]);
    } catch (error) {
      this.handleError(error, 'Unable to start automation.');
    } finally {
      this.dashboardBusy = false;
    }
  }

  async stopAutomation(): Promise<void> {
    if (!this.token) {
      return;
    }

    this.dashboardBusy = true;
    try {
      await firstValueFrom(this.jobAssistantService.stopAutomation(this.token));
      this.showFlash('info', 'Stop requested.');
      await Promise.all([this.loadDashboard(), this.loadLogs()]);
    } catch (error) {
      this.handleError(error, 'Unable to stop automation.');
    } finally {
      this.dashboardBusy = false;
    }
  }

  async saveProfile(profile: UserProfile): Promise<void> {
    if (!this.token) {
      return;
    }

    this.profileBusy = true;
    try {
      const response = await firstValueFrom(this.jobAssistantService.updateProfile(this.token, profile));
      this.profile = response.data;
      this.showFlash('success', 'Profile saved.');
      await Promise.all([this.loadDashboard(), this.loadLogs()]);
    } catch (error) {
      this.handleError(error, 'Unable to save profile.');
    } finally {
      this.profileBusy = false;
    }
  }

  async uploadResume(file: File): Promise<void> {
    if (!this.token) {
      return;
    }

    this.profileBusy = true;
    try {
      const response = await firstValueFrom(this.jobAssistantService.uploadResume(this.token, file));
      this.showFlash('success', `Resume uploaded: ${response.data.fileName}`);
      await Promise.all([this.loadProfile(), this.loadLogs()]);
    } catch (error) {
      this.handleError(error, 'Resume upload failed.');
    } finally {
      this.profileBusy = false;
    }
  }

  async answerQuestion(event: { questionId: number; answer: string }): Promise<void> {
    if (!this.token) {
      return;
    }

    this.questionBusy = true;
    try {
      await firstValueFrom(this.jobAssistantService.answerQuestion(this.token, event.questionId, event.answer));
      delete this.suggestedAnswers[event.questionId];
      this.showFlash('success', 'Answer sent to the bot queue.');
      await Promise.all([this.loadQuestions(), this.loadDashboard(), this.loadLogs()]);
    } catch (error) {
      this.handleError(error, 'Unable to save answer.');
    } finally {
      this.questionBusy = false;
    }
  }

  async suggestQuestionAnswer(questionId: number): Promise<void> {
    if (!this.token) {
      return;
    }

    this.questionSuggestingId = questionId;
    try {
      const response = await firstValueFrom(this.jobAssistantService.suggestQuestionAnswer(this.token, questionId));
      this.suggestedAnswers = {
        ...this.suggestedAnswers,
        [questionId]: response.data.answer
      };
      this.showFlash('success', 'AI suggestion generated.');
    } catch (error) {
      this.handleError(error, 'Unable to generate AI suggestion.');
    } finally {
      this.questionSuggestingId = null;
    }
  }

  async loadReport(date?: string): Promise<void> {
    if (!this.token) {
      return;
    }

    this.reportBusy = true;
    try {
      const response = await firstValueFrom(this.jobAssistantService.getReport(this.token, date));
      this.report = response.data;
    } catch (error) {
      this.handleError(error, 'Unable to generate report.');
    } finally {
      this.reportBusy = false;
    }
  }

  async loadLogs(): Promise<void> {
    if (!this.token) {
      return;
    }

    this.logsBusy = true;
    try {
      const response = await firstValueFrom(this.jobAssistantService.getLogs(this.token));
      this.logs = response.data;
    } catch (error) {
      this.handleError(error, 'Unable to load logs.');
    } finally {
      this.logsBusy = false;
    }
  }

  async loadProfile(): Promise<void> {
    if (!this.token) {
      return;
    }

    try {
      const response = await firstValueFrom(this.jobAssistantService.getProfile(this.token));
      this.profile = response.data;
    } catch (error) {
      this.handleError(error, 'Unable to load profile.');
    }
  }

  async loadQuestions(): Promise<void> {
    if (!this.token) {
      return;
    }

    try {
      const response = await firstValueFrom(this.jobAssistantService.getQuestions(this.token));
      this.questions = response.data;
    } catch (error) {
      this.handleError(error, 'Unable to load questions.');
    }
  }

  async loadDashboard(): Promise<void> {
    if (!this.token) {
      return;
    }

    this.dashboardBusy = true;
    try {
      const response = await firstValueFrom(this.jobAssistantService.getDashboard(this.token));
      this.summary = response.data;
      this.togglePolling(response.data.latestRun?.status);
    } catch (error) {
      this.handleError(error, 'Unable to load dashboard.');
    } finally {
      this.dashboardBusy = false;
    }
  }

  ngOnDestroy(): void {
    this.stopPolling();
  }

  private togglePolling(status?: string): void {
    if (status === 'RUNNING') {
      if (this.pollingHandle == null) {
        this.pollingHandle = window.setInterval(() => {
          void Promise.all([this.loadDashboard(), this.loadQuestions(), this.loadLogs()]);
        }, 5000);
      }
      return;
    }
    this.stopPolling();
  }

  private stopPolling(): void {
    if (this.pollingHandle != null) {
      window.clearInterval(this.pollingHandle);
      this.pollingHandle = null;
    }
  }

  private showFlash(type: 'success' | 'error' | 'info', message: string): void {
    this.flashType = type;
    this.flashMessage = message;
  }

  private handleError(error: unknown, fallback: string): void {
    const message = this.extractErrorMessage(error) || fallback;
    this.showFlash('error', message);
  }

  private extractErrorMessage(error: unknown): string {
    if (typeof error === 'object' && error !== null) {
      const candidate = error as { error?: { message?: string } };
      return candidate.error?.message ?? '';
    }
    return '';
  }
}
