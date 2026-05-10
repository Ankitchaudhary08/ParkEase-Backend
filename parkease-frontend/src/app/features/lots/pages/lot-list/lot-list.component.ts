import { Component, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ParkingService } from '../../../../core/services/parking.service';

@Component({
  selector: 'app-lot-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="page-container">
      <div class="top-bar">
        <div>
          <h1 class="page-title">Find Parking</h1>
          <p class="sub">Search lots by city name</p>
        </div>
      </div>

      <div class="search-row">
        <div class="search-wrap">
          <span class="si">🔍</span>
          <input
            type="text"
            [(ngModel)]="city"
            placeholder="City name — Mumbai, Pune, Delhi..."
            (keydown.enter)="search()"
          />
        </div>
        <button class="btn btn-primary" (click)="search()">Search</button>
      </div>

      @if (loading()) {
        <div class="loading-row">
          <div class="spinner"></div> Searching in {{ city }}...
        </div>
      }
      @if (error()) { <div class="alert-error">{{ error() }}</div> }
      @if (!loading() && lots().length === 0 && searched()) {
        <div class="empty-state">
          <div class="ei">🅿</div>
          <p>No lots found in "{{ city }}". Try another city.</p>
        </div>
      }

      @if (lots().length > 0) {
        <div class="results-label">{{ lots().length }} lots in <strong>{{ city }}</strong></div>
        <div class="grid-3">
          @for (lot of lots(); track lot.lotId) {
            <div class="lot-card" (click)="view(lot.lotId)">
              <div class="lc-top">
                <div class="lc-icon">🅿</div>
                <span [class]="'badge ' + (lot.open ? 'badge-green' : 'badge-red')">
                  {{ lot.open ? 'Open' : 'Closed' }}
                </span>
              </div>
              <h3>{{ lot.name }}</h3>
              <p class="lc-addr">📍 {{ lot.address }}, {{ lot.city }}</p>
              <div class="lc-footer">
                <span class="lc-rate">₹{{ lot.hourlyRate }}<small>/hr</small></span>
                <span class="lc-spots">{{ lot.totalSpots }} spots</span>
              </div>
              <div class="lc-cta">View Details →</div>
            </div>
          }
        </div>
      }
    </div>
  `,
  styles: [`
    .top-bar { margin-bottom: 6px; }
    .page-title { margin-bottom: 4px; }
    .sub { color: #555; font-size: 13px; margin: 0 0 24px; }

    .search-row { display: flex; gap: 10px; margin-bottom: 28px; }
    .search-wrap {
      flex: 1; display: flex; align-items: center; gap: 10px;
      background: #141414; border: 1px solid #2a2a2a; border-radius: 8px;
      padding: 0 14px; transition: border-color 0.2s, box-shadow 0.2s;
    }
    .search-wrap:focus-within { border-color: #ffffff; box-shadow: 0 0 0 3px rgba(255,255,255,0.05); }
    .si { color: #444; font-size: 15px; }
    .search-wrap input {
      flex: 1; background: none; border: none; outline: none;
      padding: 11px 0; font-size: 14px; color: #f0f0f0;
    }
    .search-wrap input::placeholder { color: #444; }

    .loading-row { display: flex; align-items: center; gap: 10px; color: #555; padding: 28px 0; }
    .spinner {
      width: 16px; height: 16px;
      border: 2px solid #2a2a2a; border-top-color: #888888;
      border-radius: 50%; animation: spin 0.7s linear infinite;
    }
    @keyframes spin { to { transform: rotate(360deg); } }

    .empty-state { text-align: center; padding: 50px 0; color: #444; }
    .ei { font-size: 40px; margin-bottom: 12px; }

    .results-label { font-size: 13px; color: #555; margin-bottom: 16px; }
    .results-label strong { color: #ffffff; }

    .lot-card {
      background: #141414; border: 1px solid #2a2a2a; border-radius: 10px;
      padding: 18px; cursor: pointer; display: flex; flex-direction: column; gap: 8px;
      transition: border-color 0.2s, transform 0.15s, box-shadow 0.2s;
    }
    .lot-card:hover {
      border-color: #ffffff; transform: translateY(-2px);
      box-shadow: 0 8px 24px rgba(0,0,0,0.4);
    }
    .lc-top { display: flex; justify-content: space-between; align-items: center; }
    .lc-icon {
      width: 36px; height: 36px; background: rgba(255,255,255,0.04);
      border: 1px solid rgba(255,255,255,0.1); border-radius: 8px;
      display: flex; align-items: center; justify-content: center; font-size: 14px;
    }
    .lot-card h3 { margin: 0; font-size: 15px; font-weight: 600; color: #f0f0f0; }
    .lc-addr { color: #555; font-size: 12px; margin: 0; }
    .lc-footer { display: flex; justify-content: space-between; align-items: baseline; }
    .lc-rate { font-size: 20px; font-weight: 800; color: #ffffff; }
    .lc-rate small { font-size: 11px; color: #555; font-weight: 400; margin-left: 2px; }
    .lc-spots { font-size: 12px; color: #555; }
    .lc-cta {
      font-size: 12px; color: #444; margin-top: 4px;
      transition: color 0.15s;
    }
    .lot-card:hover .lc-cta { color: #ffffff; }
  `]
})
export class LotListComponent {
  private parkingService = inject(ParkingService);
  private router = inject(Router);

  city = '';
  lots = signal<any[]>([]);
  loading = signal(false);
  error = signal<string | null>(null);
  searched = signal(false);

  search(): void {
    if (!this.city.trim()) return;
    this.loading.set(true);
    this.error.set(null);
    this.searched.set(true);
    this.parkingService.searchByCity(this.city.trim()).subscribe({
      next: (data: any[]) => { this.lots.set(data); this.loading.set(false); },
      error: () => { this.error.set('Search failed. Please ensure the backend is running.'); this.loading.set(false); }
    });
  }

  view(lotId: number): void { this.router.navigate(['/lots', lotId]); }
}
