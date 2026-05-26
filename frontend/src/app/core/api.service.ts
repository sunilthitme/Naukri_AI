import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from './environment';
import {
  AppliedJob,
  BotCommandResponse,
  BotLog,
  BotStatus,
  DashboardResponse,
  JobFilterResponse,
  NaukriCredentialsResponse,
  QuestionAnswer
} from './models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);

  dashboard() {
    return this.http.get<DashboardResponse>(`${environment.apiUrl}/dashboard`);
  }

  credentials() {
    return this.http.get<NaukriCredentialsResponse>(`${environment.apiUrl}/settings/credentials`);
  }

  saveCredentials(payload: { email: string; password: string; resumePath: string }) {
    return this.http.post<NaukriCredentialsResponse>(`${environment.apiUrl}/settings/credentials`, payload);
  }

  filters() {
    return this.http.get<JobFilterResponse>(`${environment.apiUrl}/settings/filters`);
  }

  saveFilters(payload: JobFilterResponse) {
    return this.http.post<JobFilterResponse>(`${environment.apiUrl}/settings/filters`, payload);
  }

  questions() {
    return this.http.get<QuestionAnswer[]>(`${environment.apiUrl}/questions`);
  }

  saveQuestion(payload: { question: string; answer: string }) {
    return this.http.post<QuestionAnswer>(`${environment.apiUrl}/questions`, payload);
  }

  history() {
    return this.http.get<AppliedJob[]>(`${environment.apiUrl}/jobs/history?limit=200`);
  }

  logs() {
    return this.http.get<BotLog[]>(`${environment.apiUrl}/logs?limit=250`);
  }

  botStatus() {
    return this.http.get<BotStatus>(`${environment.apiUrl}/bot/status`);
  }

  botCommand(command: 'start' | 'stop' | 'pause' | 'resume' | 'test-login' | 'test-apply') {
    return this.http.post<BotCommandResponse>(`${environment.apiUrl}/bot/${command}`, {});
  }

  exportUrl(format: 'csv' | 'xlsx'): string {
    return `${environment.apiUrl}/reports/export.${format}`;
  }

  exportReport(format: 'csv' | 'xlsx') {
    return this.http.get(`${environment.apiUrl}/reports/export.${format}`, {
      responseType: 'blob'
    });
  }
}
