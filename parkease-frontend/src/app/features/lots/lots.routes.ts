import { Routes } from '@angular/router';

export const LOTS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/lot-list/lot-list.component').then(m => m.LotListComponent)
  },
  {
    path: ':id',
    loadComponent: () => import('./pages/lot-detail/lot-detail.component').then(m => m.LotDetailComponent)
  }
];
