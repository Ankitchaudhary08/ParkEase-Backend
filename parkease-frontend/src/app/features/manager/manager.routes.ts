import { Routes } from '@angular/router';

export const MANAGER_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/dashboard/manager-dashboard.component').then(m => m.ManagerDashboardComponent)
  }
];
