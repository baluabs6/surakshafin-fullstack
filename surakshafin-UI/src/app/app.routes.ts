import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'literacy' },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'register',
    loadComponent: () => import('./features/auth/register.component').then((m) => m.RegisterComponent),
  },
  {
    path: 'literacy',
    loadComponent: () => import('./features/literacy/literacy.component').then((m) => m.LiteracyComponent),
  },
  {
    path: 'fraud',
    canActivate: [authGuard],
    loadComponent: () => import('./features/fraud/fraud.component').then((m) => m.FraudComponent),
  },
  {
    path: 'grievance',
    canActivate: [authGuard],
    loadComponent: () => import('./features/grievance/grievance.component').then((m) => m.GrievanceComponent),
  },
  {
    path: 'budget',
    canActivate: [authGuard],
    loadComponent: () => import('./features/budget/budget.component').then((m) => m.BudgetComponent),
  },
  { path: '**', redirectTo: 'literacy' },
];
