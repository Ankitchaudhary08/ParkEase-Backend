import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./features/home/home.component').then(m => m.HomeComponent)
  },
  {
    path: 'auth',
    loadChildren: () => import('./features/auth/auth.routes').then(m => m.AUTH_ROUTES)
  },
  {
    path: 'lots',
    loadChildren: () => import('./features/lots/lots.routes').then(m => m.LOTS_ROUTES)
  },
  {
    path: 'driver',
    canActivate: [authGuard, roleGuard],
    data: { role: 'DRIVER' },
    loadChildren: () => import('./features/driver/driver.routes').then(m => m.DRIVER_ROUTES)
  },
  {
    path: 'manager',
    canActivate: [authGuard, roleGuard],
    data: { role: 'MANAGER' },
    loadChildren: () => import('./features/manager/manager.routes').then(m => m.MANAGER_ROUTES)
  },
  {
    path: 'admin',
    canActivate: [authGuard, roleGuard],
    data: { role: 'ADMIN' },
    loadChildren: () => import('./features/admin/admin.routes').then(m => m.ADMIN_ROUTES)
  },
  { path: '**', redirectTo: '' }
];
