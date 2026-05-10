import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { BookingService } from '../../../../core/services/booking.service';

@Component({
  selector: 'app-my-bookings',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="page-container">
      <div class="page-header">
        <div>
          <h1 class="page-title">My Bookings</h1>
          <p class="sub">Track and manage your parking reservations</p>
        </div>
      </div>

      @if (loading()) {
        <div class="loading-row"><div class="spinner"></div> Loading bookings...</div>
      }
      @if (error()) { <div class="alert-error">{{ error() }}</div> }

      @if (!loading() && bookings().length === 0) {
        <div class="empty-state card">
          <div class="ei">🅿</div>
          <h3>No bookings yet</h3>
          <p>Find a parking lot and make your first booking.</p>
          <button class="btn btn-primary" (click)="router.navigate(['/lots'])">Find Parking</button>
        </div>
      }

      <div class="bookings-list">
        @for (b of bookings(); track b.bookingId) {
          <div class="booking-card">
            <div class="bc-left">
              <div class="bc-id">#{{ b.bookingId }}</div>
              <div class="bc-info">
                <div class="bc-times">
                  <span class="time-label">From</span>
                  <span class="time-val">{{ b.startTime | date:'dd MMM, hh:mm a' }}</span>
                  <span class="time-arrow">→</span>
                  <span class="time-label">To</span>
                  <span class="time-val">{{ b.endTime | date:'dd MMM, hh:mm a' }}</span>
                </div>
                @if (b.totalAmount) {
                  <div class="bc-amount">₹{{ b.totalAmount }}</div>
                }
              </div>
            </div>
            <div class="bc-right">
              <span [class]="'badge ' + statusBadge(b.status)">{{ b.status }}</span>
              <div class="bc-actions">
                @if (b.status === 'RESERVED') {
                  <button class="btn btn-ghost btn-sm" (click)="checkIn(b.bookingId)">Check In</button>
                  <button class="btn btn-danger btn-sm" (click)="cancel(b.bookingId)">Cancel</button>
                }
                @if (b.status === 'ACTIVE') {
                  <button class="btn btn-ghost btn-sm" (click)="checkOut(b.bookingId)">Check Out</button>
                  <button class="btn btn-danger btn-sm" (click)="cancel(b.bookingId)">Cancel</button>
                }
                @if (b.status === 'COMPLETED') {
                  <button class="btn btn-primary btn-sm" (click)="pay(b.bookingId)">Pay Now</button>
                }
              </div>
            </div>
          </div>
        }
      </div>
    </div>
  `,
  styles: [`
    .page-header { margin-bottom: 24px; }
    .sub { color: #555; font-size: 13px; margin: 4px 0 0; }
    .loading-row { display: flex; align-items: center; gap: 10px; color: #555; padding: 28px 0; }
    .spinner {
      width: 16px; height: 16px; border: 2px solid #2a2a2a;
      border-top-color: #888888; border-radius: 50%;
      animation: spin 0.7s linear infinite;
    }
    @keyframes spin { to { transform: rotate(360deg); } }

    .empty-state {
      text-align: center; padding: 52px 24px; max-width: 360px; margin: 0 auto;
    }
    .ei { font-size: 44px; margin-bottom: 14px; }
    .empty-state h3 { margin: 0 0 8px; font-size: 18px; color: #f0f0f0; }
    .empty-state p { color: #555; margin: 0 0 20px; }

    .bookings-list { display: flex; flex-direction: column; gap: 10px; }
    .booking-card {
      background: #141414; border: 1px solid #2a2a2a; border-radius: 10px;
      padding: 18px 20px; display: flex; justify-content: space-between;
      align-items: center; gap: 16px;
      transition: border-color 0.2s;
    }
    .booking-card:hover { border-color: #3a3a3a; }

    .bc-left { display: flex; align-items: center; gap: 16px; }
    .bc-id {
      font-size: 12px; font-weight: 700; color: #888888;
      background: rgba(255,255,255,0.04); border: 1px solid rgba(255,255,255,0.1);
      padding: 4px 10px; border-radius: 6px; white-space: nowrap;
    }
    .bc-times { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
    .time-label { font-size: 11px; color: #444; font-weight: 600; text-transform: uppercase; letter-spacing: 0.5px; }
    .time-val { font-size: 13px; color: #ccc; font-weight: 500; }
    .time-arrow { color: #333; font-size: 12px; }
    .bc-amount { font-size: 13px; color: #ffffff; font-weight: 600; margin-top: 4px; }

    .bc-right { display: flex; flex-direction: column; align-items: flex-end; gap: 10px; }
    .bc-actions { display: flex; gap: 8px; }
  `]
})
export class MyBookingsComponent implements OnInit {
  private bookingService = inject(BookingService);
  router = inject(Router);

  loading = signal(true);
  bookings = signal<any[]>([]);
  error = signal<string | null>(null);

  ngOnInit(): void {
    this.bookingService.getMyBookings().subscribe({
      next: (data: any[]) => { this.bookings.set(data); this.loading.set(false); },
      error: () => { this.error.set('Failed to load bookings.'); this.loading.set(false); }
    });
  }

  pay(bookingId: number): void { this.router.navigate(['/driver/bookings', bookingId, 'pay']); }

  checkIn(bookingId: number): void {
    this.bookingService.checkIn(bookingId).subscribe({
      next: () => this.reload(),
      error: (err: any) => this.error.set(err?.error?.message ?? 'Check-in failed.')
    });
  }

  checkOut(bookingId: number): void {
    this.bookingService.checkOut(bookingId).subscribe({
      next: () => this.reload(),
      error: (err: any) => this.error.set(err?.error?.message ?? 'Check-out failed.')
    });
  }

  cancel(bookingId: number): void {
    this.bookingService.cancelBooking(bookingId).subscribe({
      next: () => this.reload(),
      error: (err: any) => this.error.set(err?.error?.message ?? 'Cancellation failed.')
    });
  }

  private reload(): void {
    this.loading.set(true);
    this.bookingService.getMyBookings().subscribe({
      next: (data: any[]) => { this.bookings.set(data); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  statusBadge(status: string): string {
    const map: Record<string, string> = {
      RESERVED: 'badge-yellow', ACTIVE: 'badge-blue',
      COMPLETED: 'badge-green', CANCELLED: 'badge-red'
    };
    return map[status] ?? 'badge-gray';
  }
}
