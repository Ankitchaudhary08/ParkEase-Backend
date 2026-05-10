import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ParkingService } from '../../../../core/services/parking.service';
import { BookingService } from '../../../../core/services/booking.service';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-lot-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="page-container">
      @if (loading()) {
        <div class="loading-row"><div class="spinner"></div> Loading lot details...</div>
      }
      @if (error()) { <div class="alert-error">{{ error() }}</div> }

      @if (lot()) {
        <!-- Header -->
        <div class="lot-hero">
          <div class="lot-hero-left">
            <div class="lot-hero-icon">🅿</div>
            <div>
              <h1>{{ lot().name }}</h1>
              <p class="addr">📍 {{ lot().address }}, {{ lot().city }}</p>
            </div>
          </div>
          <span [class]="'badge badge-lg ' + (lot().open ? 'badge-green' : 'badge-red')">
            {{ lot().open ? '● Open' : '● Closed' }}
          </span>
        </div>

        <!-- Meta chips -->
        <div class="meta-chips">
          <div class="chip">
            <span class="chip-label">Hourly Rate</span>
            <span class="chip-val accent">₹{{ lot().hourlyRate }}/hr</span>
          </div>
          <div class="chip">
            <span class="chip-label">Total Spots</span>
            <span class="chip-val">{{ lot().totalSpots }}</span>
          </div>
          <div class="chip">
            <span class="chip-label">Available Now</span>
            <span class="chip-val">{{ spots().length }}</span>
          </div>
        </div>

        <!-- Spots -->
        @if (spots().length > 0) {
          <div class="section">
            <h3 class="section-title">Select a Spot</h3>
            <div class="spots-grid">
              @for (spot of spots(); track spot.spotId) {
                <button
                  [class]="'spot-btn ' + (selectedSpotId() === spot.spotId ? 'selected' : '')"
                  (click)="selectSpot(spot.spotId)">
                  <span class="spot-num">{{ spot.spotNumber }}</span>
                  <span class="spot-type">{{ spot.spotType }}</span>
                </button>
              }
            </div>
          </div>
        } @else if (!loadingSpots() && lot().open) {
          <div class="no-spots">No available spots right now. Check back later.</div>
        } @else if (!loadingSpots() && !lot().open) {
          <div class="no-spots">This lot is currently closed.</div>
        }

        <!-- Booking form -->
        @if (auth.isLoggedIn() && auth.getRole() === 'DRIVER' && lot().open && spots().length > 0) {
          <div class="section booking-section">
            <h3 class="section-title">Book This Spot</h3>
            @if (bookingSuccess()) {
              <div class="book-success">
                <div class="bs-icon">✓</div>
                <div>
                  <div class="bs-title">Booking Confirmed!</div>
                  <div class="bs-sub">Booking ID: <strong>#{{ bookingSuccess() }}</strong></div>
                </div>
                <button class="btn btn-primary btn-sm" (click)="goToBookings()">My Bookings →</button>
              </div>
            } @else {
              @if (bookingError()) { <div class="alert-error">{{ bookingError() }}</div> }
              <div class="booking-form">
                <div class="form-group">
                  <label>Selected Spot</label>
                  <input type="text" [value]="selectedSpotLabel()" readonly placeholder="Click a spot above ↑" />
                </div>
                <div class="form-row">
                  <div class="form-group">
                    <label>Start Date <span class="hint">YYYY-MM-DD</span></label>
                    <input type="text" [(ngModel)]="startDate" placeholder="2026-05-06" maxlength="10" />
                  </div>
                  <div class="form-group">
                    <label>Start Time <span class="hint">HH:MM</span></label>
                    <input type="text" [(ngModel)]="startHour" placeholder="14:00" maxlength="5" />
                  </div>
                </div>
                <div class="form-row">
                  <div class="form-group">
                    <label>End Date <span class="hint">YYYY-MM-DD</span></label>
                    <input type="text" [(ngModel)]="endDate" placeholder="2026-05-06" maxlength="10" />
                  </div>
                  <div class="form-group">
                    <label>End Time <span class="hint">HH:MM</span></label>
                    <input type="text" [(ngModel)]="endHour" placeholder="16:00" maxlength="5" />
                  </div>
                </div>
                <div class="form-row">
                  <div class="form-group">
                    <label>Vehicle Type</label>
                    <select [(ngModel)]="vehicleType">
                      <option value="FOUR_WHEELER">🚗 Car / SUV</option>
                      <option value="TWO_WHEELER">🏍 Bike / Scooter</option>
                      <option value="HEAVY">🚛 Truck / Bus</option>
                    </select>
                  </div>
                  <div class="form-group">
                    <label>Vehicle Plate</label>
                    <input type="text" [(ngModel)]="vehiclePlate" placeholder="MH01AB1234" />
                  </div>
                </div>
                <button class="btn btn-primary" style="width:100%; padding:12px; font-size:15px"
                  [disabled]="booking()" (click)="createBooking()">
                  @if (booking()) { <span class="btn-spinner"></span> Booking... }
                  @else { Confirm Booking }
                </button>
              </div>
            }
          </div>
        } @else if (!auth.isLoggedIn()) {
          <div class="login-prompt">
            <span>🔒</span>
            <p><a routerLink="/auth/login">Login</a> as a Driver to book a spot.</p>
          </div>
        }
      }
    </div>
  `,
  styles: [`
    .hint { font-size: 9px; color: #444; font-weight: 400; margin-left: 4px; text-transform: none; letter-spacing: 0; }
    .loading-row { display: flex; align-items: center; gap: 10px; color: #555; padding: 32px 0; }
    .spinner {
      width: 18px; height: 18px; border: 2px solid #2a2a2a;
      border-top-color: #888888; border-radius: 50%; animation: spin 0.7s linear infinite;
    }
    @keyframes spin { to { transform: rotate(360deg); } }

    .lot-hero {
      display: flex; justify-content: space-between; align-items: flex-start;
      margin-bottom: 20px; gap: 16px;
    }
    .lot-hero-left { display: flex; align-items: flex-start; gap: 14px; }
    .lot-hero-icon {
      width: 52px; height: 52px; flex-shrink: 0;
      background: rgba(255,255,255,0.04); border: 1px solid rgba(255,255,255,0.1);
      border-radius: 10px; display: flex; align-items: center; justify-content: center; font-size: 20px;
    }
    .lot-hero h1 { margin: 0 0 4px; font-size: 22px; font-weight: 700; color: #f0f0f0; }
    .addr { color: #555; font-size: 13px; margin: 0; }
    .badge-lg { font-size: 12px; padding: 5px 14px; }

    .meta-chips { display: flex; gap: 12px; margin-bottom: 28px; flex-wrap: wrap; }
    .chip {
      background: #141414; border: 1px solid #2a2a2a; border-radius: 8px;
      padding: 12px 18px; display: flex; flex-direction: column; gap: 4px;
    }
    .chip-label { font-size: 10px; font-weight: 700; color: #444; text-transform: uppercase; letter-spacing: 0.5px; }
    .chip-val { font-size: 18px; font-weight: 700; color: #f0f0f0; }
    .chip-val.accent { color: #ffffff; }

    .section { margin-bottom: 28px; }
    .section-title { font-size: 14px; font-weight: 700; color: #888; text-transform: uppercase; letter-spacing: 0.5px; margin: 0 0 14px; }

    .spots-grid { display: flex; flex-wrap: wrap; gap: 8px; }
    .spot-btn {
      display: flex; flex-direction: column; align-items: center;
      padding: 10px 14px; min-width: 60px;
      background: #141414; border: 1px solid #2a2a2a; border-radius: 8px;
      cursor: pointer; transition: all 0.15s;
    }
    .spot-btn:hover { border-color: rgba(255,255,255,0.3); background: rgba(255,255,255,0.03); }
    .spot-btn.selected { background: rgba(255,255,255,0.06); border-color: #ffffff; }
    .spot-num { font-size: 14px; font-weight: 700; color: #f0f0f0; }
    .spot-btn.selected .spot-num { color: #ffffff; }
    .spot-type { font-size: 9px; color: #555; margin-top: 2px; text-transform: uppercase; letter-spacing: 0.3px; }

    .no-spots { color: #444; font-size: 13px; margin-bottom: 20px; }

    .booking-section { background: #141414; border: 1px solid #2a2a2a; border-radius: 12px; padding: 24px; }
    .booking-form { display: flex; flex-direction: column; }
    .form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
    .btn-spinner {
      display: inline-block; width: 14px; height: 14px;
      border: 2px solid rgba(0,0,0,0.2); border-top-color: #000;
      border-radius: 50%; animation: spin 0.7s linear infinite; margin-right: 6px;
    }

    .book-success {
      display: flex; align-items: center; gap: 14px;
      background: rgba(255,255,255,0.03); border: 1px solid rgba(255,255,255,0.12);
      border-radius: 10px; padding: 18px;
    }
    .bs-icon {
      width: 40px; height: 40px; flex-shrink: 0;
      background: rgba(255,255,255,0.08); color: #ffffff;
      border-radius: 50%; display: flex; align-items: center; justify-content: center;
      font-size: 18px; font-weight: 700;
    }
    .bs-title { font-size: 15px; font-weight: 700; color: #ffffff; }
    .bs-sub { font-size: 12px; color: #555; margin-top: 2px; }
    .bs-sub strong { color: #f0f0f0; }

    .login-prompt {
      display: flex; align-items: center; gap: 12px;
      background: #141414; border: 1px solid #2a2a2a; border-radius: 10px;
      padding: 18px 20px; color: #555;
    }
    .login-prompt p { margin: 0; font-size: 14px; }
    .login-prompt a { color: #ffffff; font-weight: 600; }
  `]
})
export class LotDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private parkingService = inject(ParkingService);
  private bookingService = inject(BookingService);
  auth = inject(AuthService);

  lot = signal<any>(null);
  spots = signal<any[]>([]);
  loading = signal(true);
  loadingSpots = signal(true);
  error = signal<string | null>(null);

  lotId = 0;
  selectedSpotId = signal<number | null>(null);
  startDate = '';
  startHour = '';
  endDate = '';
  endHour = '';
  vehicleType = 'FOUR_WHEELER';
  vehiclePlate = '';
  booking = signal(false);
  bookingError = signal<string | null>(null);
  bookingSuccess = signal<number | null>(null);

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.lotId = id;
    this.parkingService.getLotById(id).subscribe({
      next: (data: any) => { this.lot.set(data); this.loading.set(false); },
      error: () => { this.error.set('Lot not found.'); this.loading.set(false); }
    });
    this.parkingService.getAvailableSpots(id).subscribe({
      next: (data: any[]) => { this.spots.set(data); this.loadingSpots.set(false); },
      error: () => this.loadingSpots.set(false)
    });
  }

  selectSpot(spotId: number): void {
    this.selectedSpotId.set(this.selectedSpotId() === spotId ? null : spotId);
  }

  selectedSpotLabel(): string {
    const s = this.spots().find((x: any) => x.spotId === this.selectedSpotId());
    return s ? `${s.spotNumber} (${s.spotType})` : '';
  }

  createBooking(): void {
    const dateRe = /^\d{4}-\d{2}-\d{2}$/;
    const timeRe = /^\d{2}:\d{2}$/;
    if (!this.selectedSpotId()) { this.bookingError.set('Please select a spot.'); return; }
    if (!dateRe.test(this.startDate) || !timeRe.test(this.startHour)) {
      this.bookingError.set('Start date must be YYYY-MM-DD and time HH:MM (e.g. 2026-05-06 and 14:00).'); return;
    }
    if (!dateRe.test(this.endDate) || !timeRe.test(this.endHour)) {
      this.bookingError.set('End date must be YYYY-MM-DD and time HH:MM (e.g. 2026-05-06 and 16:00).'); return;
    }
    if (!this.vehiclePlate.trim()) { this.bookingError.set('Please enter your vehicle plate number.'); return; }
    const startTime = `${this.startDate}T${this.startHour}`;
    const endTime = `${this.endDate}T${this.endHour}`;
    if (new Date(startTime) < new Date()) {
      this.bookingError.set('Start time cannot be in the past.'); return;
    }
    if (new Date(startTime) >= new Date(endTime)) {
      this.bookingError.set('End time must be after start time.'); return;
    }
    this.booking.set(true);
    this.bookingError.set(null);
    const selectedSpot = this.spots().find((x: any) => x.spotId === this.selectedSpotId());
    const pricePerHour = selectedSpot?.pricePerHour ?? this.lot()?.hourlyRate ?? 0;
    this.bookingService.createBooking({
      lotId: this.lotId,
      spotId: this.selectedSpotId(),
      startTime,
      endTime,
      vehiclePlate: this.vehiclePlate.trim().toUpperCase(),
      bookingType: 'PRE_BOOKING',
      pricePerHour,
      vehicleType: this.vehicleType
    }).subscribe({
      next: (b: any) => { this.bookingSuccess.set(b.bookingId); this.booking.set(false); },
      error: (err: any) => {
        this.booking.set(false);
        this.bookingError.set(err?.error?.message ?? 'Booking failed. Please try again.');
      }
    });
  }

  goToBookings(): void { this.router.navigate(['/driver/bookings']); }
}
