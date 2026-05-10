import { Routes } from '@angular/router';

export const DRIVER_ROUTES: Routes = [
  {
    path: '',
    redirectTo: 'bookings',
    pathMatch: 'full'
  },
  {
    path: 'bookings',
    loadComponent: () =>
      import('./pages/my-bookings/my-bookings.component').then(m => m.MyBookingsComponent)
  },
  {
    path: 'bookings/:id/pay',
    loadComponent: () =>
      import('./pages/booking-payment/booking-payment.component').then(m => m.BookingPaymentComponent)
  }
];
