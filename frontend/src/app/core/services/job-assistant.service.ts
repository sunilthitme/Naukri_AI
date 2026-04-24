import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import {
  ApiResponse,
  AiSuggestion,
  AutomationStatus,
  DailyReport,
  DashboardSummary,
  LogEntry,
  QuestionItem,
  ResumeUploadResponse,
  UserProfile
} from '../models/app.models';

@Injectable({ providedIn: 'root' })
export class JobAssistantService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = 'http://localhost:8080/api';

  getDashboard(token: string) {
    return this.http.get<ApiResponse<DashboardSummary>>(`${this.baseUrl}/automation/dashboard`, {
      headers: this.headers(token)
    });
  }

  getStatus(token: string) {
    return this.http.get<ApiResponse<AutomationStatus>>(`${this.baseUrl}/automation/status`, {
      headers: this.headers(token)
    });
  }

  startAutomation(token: string) {
    return this.http.post<ApiResponse<AutomationStatus>>(`${this.baseUrl}/automation/start`, {}, {
      headers: this.headers(token)
    });
  }

  stopAutomation(token: string) {
    return this.http.post<ApiResponse<AutomationStatus>>(`${this.baseUrl}/automation/stop`, {}, {
      headers: this.headers(token)
    });
  }

  getProfile(token: string) {
    return this.http.get<ApiResponse<UserProfile>>(`${this.baseUrl}/profile`, {
      headers: this.headers(token)
    });
  }

  updateProfile(token: string, payload: UserProfile) {
    const request = {
      ...payload,
      preferences: payload.preferences.map((item) => ({
        ...item,
        experience: item.experience ?? null
      }))
    };
    return this.http.put<ApiResponse<UserProfile>>(`${this.baseUrl}/profile`, request, {
      headers: this.headers(token)
    });
  }

  uploadResume(token: string, file: File) {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<ApiResponse<ResumeUploadResponse>>(`${this.baseUrl}/profile/resume`, formData, {
      headers: this.headers(token)
    });
  }

  getQuestions(token: string) {
    return this.http.get<ApiResponse<QuestionItem[]>>(`${this.baseUrl}/questions`, {
      headers: this.headers(token)
    });
  }

  answerQuestion(token: string, questionId: number, answer: string) {
    return this.http.post<ApiResponse<QuestionItem>>(`${this.baseUrl}/questions/${questionId}/answer`, { answer }, {
      headers: this.headers(token)
    });
  }

  suggestQuestionAnswer(token: string, questionId: number) {
    return this.http.post<ApiResponse<AiSuggestion>>(`${this.baseUrl}/questions/${questionId}/suggest`, {}, {
      headers: this.headers(token)
    });
  }

  getReport(token: string, date?: string) {
    const suffix = date ? `?date=${date}` : '';
    return this.http.get<ApiResponse<DailyReport>>(`${this.baseUrl}/reports/daily${suffix}`, {
      headers: this.headers(token)
    });
  }

  getLogs(token: string) {
    return this.http.get<ApiResponse<LogEntry[]>>(`${this.baseUrl}/logs`, {
      headers: this.headers(token)
    });
  }

  private headers(token: string) {
    return new HttpHeaders({
      Authorization: `Bearer ${token}`
    });
  }
}
