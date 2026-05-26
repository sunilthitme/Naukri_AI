export interface AuthResponse {
  token: string;
  email: string;
  role: string;
}

export interface DashboardResponse {
  totalAppliedJobs: number;
  totalFailedJobs: number;
  externalRedirectJobs: number;
  todaysApplications: number;
  botStatus: string;
  botRunning: boolean;
  lastRunTime: string | null;
}

export interface NaukriCredentialsResponse {
  email: string;
  resumePath: string;
  updatedAt: string | null;
}

export interface JobFilterResponse {
  keywords: string;
  experience: string;
  location: string;
  salary: string;
  workMode: string;
  freshness: string;
  preferredCompanies: string[];
  blacklistedCompanies: string[];
  autoApply: boolean;
  externalCareerApply: boolean;
  dailyApplyLimit: number;
  easyApplyOnly: boolean;
  duplicatePreventionDays: number;
  updatedAt: string | null;
}

export interface QuestionAnswer {
  id: number;
  question: string;
  answer: string;
  confidenceScore: number;
  usageCount: number;
  lastUsedAt: string | null;
  updatedAt: string;
}

export interface AppliedJob {
  id: number;
  companyName: string;
  jobTitle: string;
  applyDate: string;
  status: string;
  redirectedExternalSite: boolean;
  csvFileName: string;
  failureReason: string;
  jobUrl: string;
  matchScore: number | null;
}

export interface BotLog {
  id: number;
  level: 'INFO' | 'WARN' | 'ERROR';
  message: string;
  createdAt: string;
}

export interface BotStatus {
  status: string;
  running: boolean;
  paused: boolean;
  captchaDetected: boolean;
  message: string;
  lastRunTime: string | null;
  updatedAt: string;
}

export interface BotCommandResponse {
  message: string;
  status: string;
}
