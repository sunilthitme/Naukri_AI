import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';
import { LoginComponent } from './pages/login/login.component';
import { ShellComponent } from './layout/shell.component';
import { DashboardComponent } from './pages/dashboard/dashboard.component';
import { CredentialsComponent } from './pages/credentials/credentials.component';
import { FiltersComponent } from './pages/filters/filters.component';
import { QuestionsComponent } from './pages/questions/questions.component';
import { HistoryComponent } from './pages/history/history.component';
import { ControlComponent } from './pages/control/control.component';
import { LogsComponent } from './pages/logs/logs.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  {
    path: '',
    component: ShellComponent,
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      { path: 'dashboard', component: DashboardComponent },
      { path: 'credentials', component: CredentialsComponent },
      { path: 'filters', component: FiltersComponent },
      { path: 'questions', component: QuestionsComponent },
      { path: 'history', component: HistoryComponent },
      { path: 'control', component: ControlComponent },
      { path: 'logs', component: LogsComponent }
    ]
  },
  { path: '**', redirectTo: '' }
];
