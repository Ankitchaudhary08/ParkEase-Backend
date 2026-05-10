import { Component, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ParkingService } from '../../core/services/parking.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <!-- Hero -->
    <div class="hero">
      <div class="hero-inner">
        <div class="hero-badge">🅿 Smart Parking Platform</div>
        <h1>Find & Book Parking<br/><span class="accent">Instantly.</span></h1>
        <p class="hero-sub">Search available parking lots in any city. Reserve your spot in seconds.</p>
        <div class="search-box">
          <span class="search-icon">🔍</span>
          <input
            type="text"
            placeholder="Enter city — Mumbai, Pune, Delhi..."
            [(ngModel)]="city"
            (keydown.enter)="search()"
          />
          <button class="btn-search" (click)="search()" [disabled]="!city.trim()">
            Search
          </button>
        </div>
      </div>
      <div class="hero-bg-grid"></div>
    </div>

    <!-- Stats bar -->
    <div class="stats-bar">
      <div class="stat"><span class="stat-n">500+</span><span class="stat-l">Parking Lots</span></div>
      <div class="stat-div"></div>
      <div class="stat"><span class="stat-n">50K+</span><span class="stat-l">Happy Drivers</span></div>
      <div class="stat-div"></div>
      <div class="stat"><span class="stat-n">24/7</span><span class="stat-l">Availability</span></div>
      <div class="stat-div"></div>
      <div class="stat"><span class="stat-n">100%</span><span class="stat-l">Secure Payments</span></div>
    </div>

    <!-- Results -->
    <div class="page-container">
      @if (loading()) {
        <div class="loading-row">
          <div class="spinner"></div>
          <span>Searching parking lots in {{ city }}...</span>
        </div>
      }
      @if (error()) {
        <div class="alert-error">{{ error() }}</div>
      }
      @if (!loading() && lots().length === 0 && searched()) {
        <div class="empty-state">
          <div class="empty-icon">🅿</div>
          <h3>No lots found in "{{ city }}"</h3>
          <p>Try a different city name or check the spelling.</p>
        </div>
      }
      @if (lots().length > 0) {
        <div class="results-header">
          <h2>{{ lots().length }} lots found in <span class="accent">{{ city }}</span></h2>
        </div>
        <div class="grid-3">
          @for (lot of lots(); track lot.lotId) {
            <div class="lot-card" (click)="view(lot.lotId)">
              <div class="lot-top">
                <div class="lot-icon">🅿</div>
                <span [class]="'badge ' + (lot.isOpen ? 'badge-green' : 'badge-red')">
                  {{ lot.isOpen ? 'Open' : 'Closed' }}
                </span>
              </div>
              <h3>{{ lot.name }}</h3>
              <p class="lot-addr">📍 {{ lot.address }}</p>
              <div class="lot-footer">
                <div class="lot-rate">
                  <span class="rate-val">₹{{ lot.hourlyRate }}</span>
                  <span class="rate-unit">/hr</span>
                </div>
                <button class="btn-view">View →</button>
              </div>
            </div>
          }
        </div>
      }
    </div>

    <!-- Features section (shown when no search) -->
    @if (!searched()) {
      <div class="features-section">
        <div class="page-container">
          <h2 class="section-heading">Why Choose ParkEase?</h2>
          <div class="grid-3">
            @for (f of features; track f.icon) {
              <div class="feature-card card">
                <div class="feature-icon">{{ f.icon }}</div>
                <h4>{{ f.title }}</h4>
                <p>{{ f.desc }}</p>
              </div>
            }
          </div>
        </div>
      </div>
    }
  `,
  styles: [`
    /* Hero */
    .hero {
      position: relative; overflow: hidden;
      background: #000;
      padding: 80px 24px 72px;
      border-bottom: 1px solid #1a1a1a;
    }
    .hero-bg-grid {
      position: absolute; inset: 0; pointer-events: none;
      background-image:
        linear-gradient(rgba(255,255,255,0.02) 1px, transparent 1px),
        linear-gradient(90deg, rgba(255,255,255,0.02) 1px, transparent 1px);
      background-size: 40px 40px;
    }
    .hero-inner { position: relative; z-index: 1; max-width: 680px; margin: 0 auto; text-align: center; }
    .hero-badge {
      display: inline-block; margin-bottom: 22px;
      background: rgba(255,255,255,0.05); border: 1px solid rgba(255,255,255,0.15);
      color: #ffffff; padding: 6px 16px; border-radius: 20px;
      font-size: 12px; font-weight: 600; letter-spacing: 0.5px;
    }
    .hero h1 {
      font-size: 52px; font-weight: 800; line-height: 1.1;
      margin: 0 0 16px; color: #fff; letter-spacing: -1px;
    }
    .accent { color: #ffffff; }
    .hero-sub { font-size: 16px; color: #666; margin: 0 0 36px; }

    .search-box {
      display: flex; align-items: center;
      background: #141414; border: 1px solid #2a2a2a;
      border-radius: 10px; padding: 6px 6px 6px 16px;
      max-width: 540px; margin: 0 auto;
      transition: border-color 0.2s, box-shadow 0.2s;
    }
    .search-box:focus-within {
      border-color: #ffffff;
      box-shadow: 0 0 0 3px rgba(255,255,255,0.06);
    }
    .search-icon { font-size: 16px; margin-right: 10px; color: #555; }
    .search-box input {
      flex: 1; background: none; border: none; outline: none;
      font-size: 15px; color: #f0f0f0; padding: 6px 0;
    }
    .search-box input::placeholder { color: #444; }
    .btn-search {
      background: #ffffff; color: #000000; border: none;
      padding: 10px 22px; border-radius: 7px;
      font-size: 14px; font-weight: 700; cursor: pointer;
      transition: background 0.15s, box-shadow 0.15s;
      white-space: nowrap;
    }
    .btn-search:hover:not(:disabled) { background: #e0e0e0; box-shadow: 0 0 14px rgba(255,255,255,0.1); }
    .btn-search:disabled { opacity: 0.4; cursor: not-allowed; }

    /* Stats bar */
    .stats-bar {
      display: flex; align-items: center; justify-content: center; gap: 0;
      background: #0d0d0d; border-bottom: 1px solid #1a1a1a;
      padding: 18px 24px;
    }
    .stat { display: flex; flex-direction: column; align-items: center; padding: 0 40px; }
    .stat-n { font-size: 22px; font-weight: 800; color: #ffffff; }
    .stat-l { font-size: 11px; color: #555; font-weight: 500; letter-spacing: 0.5px; margin-top: 2px; }
    .stat-div { width: 1px; height: 36px; background: #222; }

    /* Loading */
    .loading-row {
      display: flex; align-items: center; gap: 12px;
      color: #666; padding: 32px 0;
    }
    .spinner {
      width: 18px; height: 18px;
      border: 2px solid #2a2a2a; border-top-color: #888888;
      border-radius: 50%; animation: spin 0.7s linear infinite;
    }
    @keyframes spin { to { transform: rotate(360deg); } }

    /* Empty state */
    .empty-state { text-align: center; padding: 60px 0; }
    .empty-icon { font-size: 48px; margin-bottom: 16px; }
    .empty-state h3 { color: #f0f0f0; margin: 0 0 8px; }
    .empty-state p { color: #555; }

    /* Results */
    .results-header { margin-bottom: 18px; }
    .results-header h2 { font-size: 17px; font-weight: 600; color: #ccc; margin: 0; }

    /* Lot cards */
    .lot-card {
      background: #141414; border: 1px solid #2a2a2a;
      border-radius: 10px; padding: 18px;
      cursor: pointer; transition: border-color 0.2s, transform 0.15s, box-shadow 0.2s;
      display: flex; flex-direction: column; gap: 10px;
    }
    .lot-card:hover {
      border-color: #ffffff;
      transform: translateY(-2px);
      box-shadow: 0 8px 24px rgba(0,0,0,0.4);
    }
    .lot-top { display: flex; justify-content: space-between; align-items: center; }
    .lot-icon {
      width: 38px; height: 38px; background: rgba(255,255,255,0.04);
      border-radius: 8px; display: flex; align-items: center; justify-content: center;
      font-size: 16px; border: 1px solid rgba(255,255,255,0.1);
    }
    .lot-card h3 { margin: 0; font-size: 15px; font-weight: 600; color: #f0f0f0; }
    .lot-addr { color: #555; font-size: 12px; margin: 0; }
    .lot-footer { display: flex; justify-content: space-between; align-items: center; margin-top: 4px; }
    .lot-rate { display: flex; align-items: baseline; gap: 3px; }
    .rate-val { font-size: 20px; font-weight: 800; color: #ffffff; }
    .rate-unit { font-size: 12px; color: #555; }
    .btn-view {
      background: none; border: 1px solid #2a2a2a; color: #888;
      padding: 5px 12px; border-radius: 6px; font-size: 12px;
      cursor: pointer; transition: all 0.15s;
    }
    .lot-card:hover .btn-view { border-color: #ffffff; color: #ffffff; }

    /* Features */
    .features-section { background: #0d0d0d; border-top: 1px solid #1a1a1a; padding: 56px 0; }
    .section-heading { font-size: 24px; font-weight: 700; margin: 0 0 28px; text-align: center; }
    .feature-card { text-align: center; transition: border-color 0.2s, transform 0.15s; }
    .feature-card:hover { border-color: rgba(255,255,255,0.2); transform: translateY(-2px); }
    .feature-icon { font-size: 32px; margin-bottom: 12px; }
    .feature-card h4 { margin: 0 0 8px; font-size: 15px; color: #f0f0f0; }
    .feature-card p { color: #555; font-size: 13px; margin: 0; line-height: 1.7; }
  `]
})
export class HomeComponent {
  private parkingService = inject(ParkingService);
  private router = inject(Router);

  city = '';
  lots = signal<any[]>([]);
  loading = signal(false);
  error = signal<string | null>(null);
  searched = signal(false);

  features = [
    { icon: '⚡', title: 'Instant Booking', desc: 'Reserve your parking spot in under 60 seconds. No calls, no queues.' },
    { icon: '🔒', title: 'Secure Payments', desc: 'Pay safely via UPI, debit/credit cards powered by Razorpay.' },
    { icon: '📍', title: 'Find Nearby', desc: 'Discover parking lots closest to your destination in real time.' },
    { icon: '🕐', title: '24/7 Access', desc: 'Book anytime, day or night. Your spot is waiting when you arrive.' },
    { icon: '📱', title: 'Easy Management', desc: 'Manage bookings, check-in and check-out right from your dashboard.' },
    { icon: '⭐', title: 'Verified Lots', desc: 'Every parking lot is reviewed and approved before listing.' },
  ];

  search(): void {
    if (!this.city.trim()) return;
    this.loading.set(true);
    this.error.set(null);
    this.searched.set(true);
    this.parkingService.searchByCity(this.city.trim()).subscribe({
      next: (data: any[]) => { this.lots.set(data); this.loading.set(false); },
      error: () => { this.error.set('Search failed. Make sure the backend is running.'); this.loading.set(false); }
    });
  }

  view(lotId: number): void { this.router.navigate(['/lots', lotId]); }
}
