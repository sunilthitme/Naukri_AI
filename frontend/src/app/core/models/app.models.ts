export type RunStatus =
  | 'IDLE'
  | 'RUNNING'
  | 'COMPLETED'
  | 'STOPPED'
  | 'FAILED'
  | 'WAITING_FOR_USER';

export type ApplicationStatus =
  | 'SEARCHED'
  | 'APPLIED'
  | 'SKIPPED'
  | 'FAILED'
  | 'QUESTION_PENDING'
  | 'CAPTCHA_WAITING';

export type ViewName = 'dashboard' | 'profile' | 'questions' | 'reports' | 'logs';

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface JobPreference {
  id?: number;
  title: string;
  skills: string;
  experience: number | null;
  salaryRange: string;
  preferredLocation: string;
  keywords: string;
  postedTodayOnly: boolean;
  easyApplyFirst: boolean;
  relevantJobsOnly: boolean;
  minimumScore: number;
  active: boolean;
}

export interface UserProfile {
  id: number;
  fullName: string;
  email: string;
  phoneNumber: string;
  totalExperience: number | null;
  currentCompany: string;
  currentSalary: string;
  expectedSalary: string;
  noticePeriod: string;
  locationPreference: string;
  skills: string;
  preferredKeywords: string;
  naukriUsername: string;
  naukriPassword?: string;
  resumeFileName: string;
  role: 'ADMIN' | 'USER';
  preferences: JobPreference[];
}

export interface AuthResponse {
  token: string;
  expiresAt: string;
  user: UserProfile;
}

export interface AutomationStatus {
  runId: number | null;
  status: RunStatus;
  startedAt: string | null;
  endedAt: string | null;
  jobsSearched: number;
  jobsApplied: number;
  jobsSkipped: number;
  jobsFailed: number;
  credentialsCreated: number;
  summary: string;
}

export interface ReportRow {
  appliedDate: string | null;
  company: string;
  title: string;
  portal: string;
  status: ApplicationStatus;
  remarks: string;
  relevanceScore: number;
}

export interface DashboardSummary {
  latestRun: AutomationStatus;
  totalAppliedToday: number;
  totalFailedToday: number;
  totalSkippedToday: number;
  openQuestions: number;
  recentApplications: ReportRow[];
}

export interface QuestionItem {
  id: number;
  question: string;
  fieldLabel: string;
  sourcePortal: string;
  status: 'OPEN' | 'ANSWERED' | 'SKIPPED';
  answer: string;
}

export interface AiSuggestion {
  answer: string;
}

export interface DailyReport {
  date: string;
  totalSearched: number;
  totalApplied: number;
  totalSkipped: number;
  totalFailed: number;
  credentialsCreated: number;
  pdfPath: string;
  excelPath: string;
  csvPath: string;
  rows: ReportRow[];
}

export interface LogEntry {
  id: number;
  level: 'INFO' | 'WARN' | 'ERROR' | 'DEBUG';
  source: string;
  message: string;
  details: string | null;
  occurredAt: string;
}

export interface ResumeUploadResponse {
  fileName: string;
  storedPath: string;
  extractedSummary: string;
}

export function createEmptyPreference(): JobPreference {
  return {
    title: '',
    skills: '',
    experience: null,
    salaryRange: '',
    preferredLocation: '',
    keywords: '',
    postedTodayOnly: true,
    easyApplyFirst: true,
    relevantJobsOnly: true,
    minimumScore: 70,
    active: true
  };
}

export function createEmptyProfile(): UserProfile {
  return {
    id: 0,
    fullName: '',
    email: '',
    phoneNumber: '',
    totalExperience: null,
    currentCompany: '',
    currentSalary: '',
    expectedSalary: '',
    noticePeriod: '',
    locationPreference: '',
    skills: '',
    preferredKeywords: '',
    naukriUsername: '',
    naukriPassword: '',
    resumeFileName: '',
    role: 'USER',
    preferences: [createEmptyPreference()]
  };
}
