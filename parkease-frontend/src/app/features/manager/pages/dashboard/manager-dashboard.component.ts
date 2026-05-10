import { Component, OnInit, OnDestroy, signal, inject, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ParkingService } from '../../../../core/services/parking.service';
import { NotificationService } from '../../../../core/services/notification.service';
import * as L from 'leaflet';

@Component({
  selector: 'app-manager-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="page-container">
      <div class="dash-header">
        <div>
          <h1 class="page-title">My Parking Lots</h1>
          <p class="sub">Manage your lots, add spots, and monitor availability</p>
        </div>
      </div>

      <div class="tab-bar">
        @for (t of tabs; track t.id) {
          <button [class]="'tab ' + (tab() === t.id ? 'active' : '')" (click)="tab.set(t.id)">
            {{ t.icon }} {{ t.label }}
          </button>
        }
      </div>

      <!-- My Lots -->
      @if (tab() === 'lots') {
        @if (loading()) { <div class="loading-row"><div class="spinner"></div> Loading lots...</div> }
        @if (error()) { <div class="alert-error">{{ error() }}</div> }
        @if (!loading() && lots().length === 0) {
          <div class="empty-state card">
            <div class="ei">🏢</div>
            <h3>No lots yet</h3>
            <p>Create your first parking lot to get started.</p>
            <button class="btn btn-primary" (click)="tab.set('addLot')">Create Lot</button>
          </div>
        }
        <div class="grid-2">
          @for (lot of lots(); track lot.lotId) {
            <div class="lot-card card">
              <div class="lc-head">
                <div>
                  <div class="lc-name">{{ lot.name }}</div>
                  <div class="lc-addr">📍 {{ lot.address }}, {{ lot.city }}</div>
                </div>
                <span [class]="'badge ' + statusBadge(lot.approvalStatus)">{{ lot.approvalStatus }}</span>
              </div>
              <div class="lc-stats">
                <div class="stat"><span class="sl">Rate</span><span class="sv">₹{{ lot.hourlyRate }}/hr</span></div>
                <div class="stat"><span class="sl">Spots</span><span class="sv">{{ lot.totalSpots }}</span></div>
                <div class="stat">
                  <span class="sl">Status</span>
                  <span [class]="'sv ' + (lot.open ? 'accent' : 'muted')">{{ lot.open ? 'Open' : 'Closed' }}</span>
                </div>
              </div>
              <div class="lc-actions">
                <button class="btn btn-ghost btn-sm" (click)="toggleOpen(lot.lotId)">
                  {{ lot.open ? '🔴 Close Lot' : '🟢 Open Lot' }}
                </button>
                <button class="btn btn-secondary btn-sm" (click)="selectLotForSpots(lot.lotId)">
                  + Add Spots
                </button>
              </div>
            </div>
          }
        </div>
      }

      <!-- Add Lot -->
      @if (tab() === 'addLot') {
        <div class="form-card card">
          <h3 class="form-title">Create New Parking Lot</h3>
          @if (lotError()) { <div class="alert-error">{{ lotError() }}</div> }
          @if (lotSuccess()) { <div class="alert-success">{{ lotSuccess() }}</div> }
          <div class="form-row">
            <div class="form-group">
              <label>Lot Name *</label>
              <input type="text" [(ngModel)]="newLot.name" placeholder="City Center Parking" />
            </div>
            <div class="form-group">
              <label>City *</label>
              <input type="text" [(ngModel)]="newLot.city" placeholder="Mumbai" />
            </div>
          </div>
          <div class="form-group">
            <label>Address *</label>
            <input type="text" [(ngModel)]="newLot.address" placeholder="123 Main Street, Near Bus Stop" />
          </div>
          <div class="form-row">
            <div class="form-group">
              <label>Hourly Rate (₹) *</label>
              <input type="number" [(ngModel)]="newLot.hourlyRate" placeholder="50" min="1" />
            </div>
            <div class="form-group">
              <label>Total Spots *</label>
              <input type="number" [(ngModel)]="newLot.totalSpots" placeholder="50" min="1" />
            </div>
          </div>
          <div class="form-group">
            <label>Pick Location on Map <span class="hint">Click the map to set coordinates</span></label>
            <div class="map-actions">
              <button type="button" class="btn btn-ghost btn-sm" (click)="useMyLocation()">📍 Use My Location</button>
              @if (newLot.latitude && newLot.longitude) {
                <span class="coords-badge">{{ newLot.latitude | number:'1.4-4' }}, {{ newLot.longitude | number:'1.4-4' }}</span>
              }
            </div>
            <div id="lot-map"></div>
          </div>
          <button class="btn btn-primary" style="padding: 11px 28px" [disabled]="saving()" (click)="createLot()">
            @if (saving()) { <span class="btn-spinner"></span> Creating... }
            @else { Create Lot }
          </button>
          <p class="form-hint">New lots require admin approval before going live.</p>
        </div>
      }

      <!-- Analytics -->
      @if (tab() === 'analytics') {
        <div class="form-card card">
          <h3 class="form-title">Lot Analytics</h3>
          <div class="form-group">
            <label>Select Lot</label>
            <select (change)="loadAnalytics(+$any($event.target).value)">
              <option value="">-- Choose a lot --</option>
              @for (lot of lots(); track lot.lotId) {
                <option [value]="lot.lotId">{{ lot.name }} ({{ lot.city }})</option>
              }
            </select>
          </div>

          @if (analyticsLoading()) {
            <div class="loading-row"><div class="spinner"></div> Loading analytics...</div>
          }

          @if (!analyticsLoading() && analyticsLotId()) {

            <!-- Occupancy Rate -->
            <div class="an-section">
              <div class="an-label">Average Occupancy Rate</div>
              <div class="occ-bar-wrap">
                <div class="occ-bar" [style.width.%]="occupancyRate() ?? 0"></div>
              </div>
              <div class="occ-val">{{ occupancyRate() ?? 0 }}%</div>
            </div>

            <!-- Peak Hours -->
            @if (peakHours().length > 0) {
              <div class="an-section">
                <div class="an-label">Top Peak Hours</div>
                <div class="peak-pills">
                  @for (h of peakHours(); track h) {
                    <span class="peak-pill">{{ fmt24(h.toString()) }}</span>
                  }
                </div>
              </div>
            }

            <!-- Hourly Occupancy Chart -->
            @if (hourMax() > 0) {
              <div class="an-section">
                <div class="an-label">Occupancy by Hour</div>
                <div class="hour-chart">
                  @for (h of hourKeys(); track h) {
                    <div class="hour-col">
                      <div class="hour-bar-wrap">
                        <div class="hour-bar" [style.height.%]="(hourVal(h) / hourMax()) * 100"></div>
                      </div>
                      <div class="hour-label">{{ fmt24(h) }}</div>
                    </div>
                  }
                </div>
              </div>
            }

            <!-- Spot Utilisation -->
            @if (spotUtilKeys().length > 0) {
              <div class="an-section">
                <div class="an-label">Spot Type Utilisation</div>
                @for (key of spotUtilKeys(); track key) {
                  <div class="util-row">
                    <span class="util-key">{{ key }}</span>
                    <div class="util-bar-wrap">
                      <div class="util-bar" [style.width.%]="(spotUtilisation()[key] / spotUtilMax()) * 100"></div>
                    </div>
                    <span class="util-count">{{ spotUtilisation()[key] }}</span>
                  </div>
                }
              </div>
            }

            @if (peakHours().length === 0 && spotUtilKeys().length === 0 && (occupancyRate() ?? 0) === 0) {
              <div class="state-msg">No activity data yet for this lot. Data appears after check-ins.</div>
            }
          }

          @if (!analyticsLotId() && !analyticsLoading()) {
            <div class="state-msg">Select a lot above to view its analytics.</div>
          }
        </div>
      }

      <!-- Add Spots -->
      @if (tab() === 'addSpot') {
        <div class="form-card card">
          <h3 class="form-title">Add Spots to a Lot</h3>
          @if (spotError()) { <div class="alert-error">{{ spotError() }}</div> }
          @if (spotSuccess()) { <div class="alert-success">{{ spotSuccess() }}</div> }
          <div class="form-group">
            <label>Select Lot *</label>
            <select [(ngModel)]="selectedLotId">
              <option value="">-- Select a lot --</option>
              @for (lot of lots(); track lot.lotId) {
                <option [value]="lot.lotId">{{ lot.name }} ({{ lot.city }})</option>
              }
            </select>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label>Prefix (e.g. A)</label>
              <input type="text" [(ngModel)]="spotPrefix" placeholder="A" maxlength="3" />
            </div>
            <div class="form-group">
              <label>Floor *</label>
              <input type="number" [(ngModel)]="spotFloor" min="0" placeholder="0" />
            </div>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label>Spot Type *</label>
              <select [(ngModel)]="spotType">
                <option value="STANDARD">🚗 Standard</option>
                <option value="COMPACT">🚘 Compact</option>
                <option value="LARGE">🚛 Large</option>
                <option value="MOTORBIKE">🏍 Motorbike</option>
                <option value="EV_ONLY">⚡ EV Only</option>
              </select>
            </div>
            <div class="form-group">
              <label>Vehicle Type *</label>
              <select [(ngModel)]="vehicleType">
                <option value="FOUR_WHEELER">🚗 Four Wheeler</option>
                <option value="TWO_WHEELER">🏍 Two Wheeler</option>
                <option value="HEAVY">🚛 Heavy</option>
              </select>
            </div>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label>Start Number</label>
              <input type="number" [(ngModel)]="spotStart" min="1" />
            </div>
            <div class="form-group">
              <label>End Number</label>
              <input type="number" [(ngModel)]="spotEnd" min="1" />
            </div>
          </div>
          <div class="form-group">
            <label>Price per Hour (₹) *</label>
            <input type="number" [(ngModel)]="spotPrice" min="1" placeholder="50" />
          </div>
          <div class="preview-box">
            Will create <strong>{{ spotEnd - spotStart + 1 }} spots</strong>:
            {{ spotPrefix }}{{ spotStart }} → {{ spotPrefix }}{{ spotEnd }}
            ({{ spotType }})
          </div>
          <button class="btn btn-primary" style="padding: 11px 28px" [disabled]="savingSpots()" (click)="addBulkSpots()">
            @if (savingSpots()) { <span class="btn-spinner"></span> Adding... }
            @else { Add Spots }
          </button>
        </div>
      }
    </div>
  `,
  styles: [`
    .dash-header { margin-bottom: 24px; }
    .sub { color: #555; font-size: 13px; margin: 4px 0 0; }

    .tab-bar { display: flex; gap: 2px; margin-bottom: 24px; border-bottom: 1px solid #1e1e1e; }
    .tab {
      padding: 10px 18px; border: none; background: none; cursor: pointer;
      font-size: 13px; font-weight: 500; color: #555;
      border-bottom: 2px solid transparent; margin-bottom: -1px;
      transition: color 0.15s;
    }
    .tab:hover { color: #f0f0f0; }
    .tab.active { color: #ffffff; border-bottom-color: #ffffff; font-weight: 600; }

    .loading-row { display: flex; align-items: center; gap: 10px; color: #555; padding: 28px 0; }
    .spinner {
      width: 16px; height: 16px; border: 2px solid #2a2a2a;
      border-top-color: #888888; border-radius: 50%; animation: spin 0.7s linear infinite;
    }
    @keyframes spin { to { transform: rotate(360deg); } }

    .empty-state { text-align: center; padding: 48px 24px; max-width: 340px; margin: 0 auto; }
    .ei { font-size: 40px; margin-bottom: 14px; }
    .empty-state h3 { color: #f0f0f0; margin: 0 0 8px; }
    .empty-state p { color: #555; margin: 0 0 20px; }

    .lot-card { display: flex; flex-direction: column; gap: 12px; }
    .lc-head { display: flex; justify-content: space-between; align-items: flex-start; }
    .lc-name { font-size: 15px; font-weight: 600; color: #f0f0f0; margin-bottom: 4px; }
    .lc-addr { font-size: 12px; color: #555; }
    .lc-stats { display: flex; gap: 20px; }
    .stat { display: flex; flex-direction: column; gap: 2px; }
    .sl { font-size: 10px; color: #444; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px; }
    .sv { font-size: 15px; font-weight: 700; color: #ccc; }
    .sv.accent { color: #ffffff; }
    .sv.muted { color: #555; }
    .lc-actions { display: flex; gap: 8px; border-top: 1px solid #1e1e1e; padding-top: 12px; }

    .form-card { max-width: 640px; }
    .form-title { margin: 0 0 22px; font-size: 17px; color: #f0f0f0; }
    .form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
    .form-hint { color: #444; font-size: 12px; margin: 12px 0 0; }
    .preview-box {
      background: #0d0d0d; border: 1px solid #1e1e1e; border-radius: 7px;
      padding: 10px 14px; font-size: 13px; color: #666; margin-bottom: 16px;
    }
    .preview-box strong { color: #ffffff; }
    .btn-spinner {
      display: inline-block; width: 14px; height: 14px;
      border: 2px solid rgba(0,0,0,0.2); border-top-color: #000;
      border-radius: 50%; animation: spin 0.7s linear infinite; margin-right: 6px;
    }
    /* ── Analytics ── */
    .an-section { margin-bottom: 24px; }
    .an-label { font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.6px; color: #444; margin-bottom: 10px; }

    .occ-bar-wrap { height: 10px; background: #1a1a1a; border-radius: 5px; overflow: hidden; margin-bottom: 6px; }
    .occ-bar { height: 100%; background: #fff; border-radius: 5px; transition: width 0.6s ease; }
    .occ-val { font-size: 28px; font-weight: 800; color: #fff; }

    .peak-pills { display: flex; flex-wrap: wrap; gap: 8px; }
    .peak-pill { background: #1a1a1a; border: 1px solid #2a2a2a; border-radius: 6px; padding: 5px 12px; font-size: 13px; font-weight: 600; color: #ccc; }

    .hour-chart { display: flex; align-items: flex-end; gap: 3px; height: 80px; }
    .hour-col { display: flex; flex-direction: column; align-items: center; flex: 1; height: 100%; }
    .hour-bar-wrap { flex: 1; width: 100%; display: flex; align-items: flex-end; }
    .hour-bar { width: 100%; background: #fff; border-radius: 2px 2px 0 0; min-height: 2px; transition: height 0.4s ease; }
    .hour-label { font-size: 8px; color: #444; margin-top: 3px; }

    .util-row { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; }
    .util-key { font-size: 11px; color: #888; width: 90px; flex-shrink: 0; }
    .util-bar-wrap { flex: 1; height: 8px; background: #1a1a1a; border-radius: 4px; overflow: hidden; }
    .util-bar { height: 100%; background: #555; border-radius: 4px; transition: width 0.5s ease; }
    .util-count { font-size: 12px; color: #666; width: 24px; text-align: right; }

    #lot-map { width: 100%; height: 300px; border-radius: 8px; border: 1px solid #2a2a2a; margin-top: 8px; cursor: crosshair; display: block; position: relative; z-index: 0; }
    .map-actions { display: flex; align-items: center; gap: 10px; margin-bottom: 4px; }
    .coords-badge { font-size: 12px; color: #aaa; background: #1a1a1a; border: 1px solid #2a2a2a; border-radius: 5px; padding: 3px 8px; }
    .hint { font-size: 10px; color: #444; font-weight: 400; margin-left: 6px; }
  `]
})
export class ManagerDashboardComponent implements OnInit, OnDestroy {
  private parkingService = inject(ParkingService);
  private notifService = inject(NotificationService);
  private map: L.Map | null = null;
  private marker: L.Marker | null = null;

  tabs = [
    { id: 'lots' as const, icon: '🏢', label: 'My Lots' },
    { id: 'addLot' as const, icon: '＋', label: 'Add Lot' },
    { id: 'addSpot' as const, icon: '＋', label: 'Add Spots' },
    { id: 'analytics' as const, icon: '📊', label: 'Analytics' },
  ];
  tab = signal<'lots' | 'addLot' | 'addSpot' | 'analytics'>('lots');

  // Analytics
  analyticsLotId = signal<number | null>(null);
  occupancyRate = signal<number | null>(null);
  peakHours = signal<number[]>([]);
  spotUtilisation = signal<Record<string, number>>({});
  occupancyByHour = signal<Record<string, number>>({});
  analyticsLoading = signal(false);

  lots = signal<any[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);

  newLot: any = { name: '', city: '', address: '', hourlyRate: null, totalSpots: null, latitude: null, longitude: null };
  saving = signal(false);
  lotError = signal<string | null>(null);
  lotSuccess = signal<string | null>(null);

  selectedLotId: any = '';
  spotStart = 1;
  spotEnd = 10;
  spotPrefix = 'A';
  spotFloor = 0;
  spotType = 'STANDARD';
  vehicleType = 'FOUR_WHEELER';
  spotPrice = 50;
  savingSpots = signal(false);
  spotError = signal<string | null>(null);
  spotSuccess = signal<string | null>(null);

  constructor() {
    effect(() => {
      if (this.tab() === 'addLot') {
        setTimeout(() => this.initMap(), 50);
      } else {
        this.destroyMap();
      }
    });
  }

  ngOnInit(): void { this.loadLots(); }

  ngOnDestroy(): void { this.destroyMap(); }

  private initMap(): void {
    if (this.map) return;
    const defaultLat = 20.5937, defaultLng = 78.9629;
    (L.Icon.Default.prototype as any)._getIconUrl = undefined;
    L.Icon.Default.mergeOptions({
      iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
      iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
      shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
    });
    this.map = L.map('lot-map').setView([defaultLat, defaultLng], 5);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '© OpenStreetMap contributors'
    }).addTo(this.map);
    this.map.on('click', (e: L.LeafletMouseEvent) => {
      const { lat, lng } = e.latlng;
      this.newLot.latitude = Math.round(lat * 10000) / 10000;
      this.newLot.longitude = Math.round(lng * 10000) / 10000;
      if (this.marker) {
        this.marker.setLatLng(e.latlng);
      } else {
        this.marker = L.marker(e.latlng).addTo(this.map!);
      }
      this.marker.bindPopup(`📍 ${this.newLot.latitude}, ${this.newLot.longitude}`).openPopup();
    });
  }

  private destroyMap(): void {
    if (this.map) { this.map.remove(); this.map = null; this.marker = null; }
  }

  useMyLocation(): void {
    if (!navigator.geolocation) return;
    navigator.geolocation.getCurrentPosition(pos => {
      const lat = Math.round(pos.coords.latitude * 10000) / 10000;
      const lng = Math.round(pos.coords.longitude * 10000) / 10000;
      this.newLot.latitude = lat;
      this.newLot.longitude = lng;
      if (this.map) {
        this.map.setView([lat, lng], 15);
        if (this.marker) {
          this.marker.setLatLng([lat, lng]);
        } else {
          this.marker = L.marker([lat, lng]).addTo(this.map);
        }
        this.marker.bindPopup(`📍 ${lat}, ${lng}`).openPopup();
      }
    });
  }

  loadLots(): void {
    this.parkingService.getManagerLots().subscribe({
      next: (data: any[]) => { this.lots.set(data); this.loading.set(false); },
      error: () => { this.error.set('Failed to load lots.'); this.loading.set(false); }
    });
  }

  statusBadge(status: string): string {
    const m: Record<string, string> = { APPROVED: 'badge-green', PENDING: 'badge-orange', REJECTED: 'badge-red' };
    return m[status] ?? 'badge-gray';
  }

  toggleOpen(lotId: number): void {
    this.parkingService.toggleOpen(lotId).subscribe({ next: () => this.loadLots() });
  }

  selectLotForSpots(lotId: number): void { this.selectedLotId = lotId; this.tab.set('addSpot'); }

  createLot(): void {
    if (!this.newLot.name || !this.newLot.city || !this.newLot.address || !this.newLot.hourlyRate || !this.newLot.totalSpots) {
      this.lotError.set('Please fill all required fields.'); return;
    }
    this.saving.set(true); this.lotError.set(null); this.lotSuccess.set(null);
    this.parkingService.createLot(this.newLot).subscribe({
      next: () => {
        this.lotSuccess.set('Lot created! Pending admin approval.');
        this.newLot = { name: '', city: '', address: '', hourlyRate: null, totalSpots: null, latitude: null, longitude: null };
        if (this.marker) { this.marker.remove(); this.marker = null; }
        this.saving.set(false); this.loadLots();
      },
      error: (err: any) => { this.saving.set(false); this.lotError.set(err?.error?.message ?? 'Failed to create lot.'); }
    });
  }

  loadAnalytics(lotId: number): void {
    this.analyticsLotId.set(lotId);
    this.analyticsLoading.set(true);
    let done = 0;
    const finish = () => { if (++done === 4) this.analyticsLoading.set(false); };
    this.notifService.getOccupancyRate(lotId).subscribe({ next: v => { this.occupancyRate.set(Math.round(v)); finish(); }, error: () => { finish(); } });
    this.notifService.getPeakHours(lotId).subscribe({ next: v => { this.peakHours.set(v); finish(); }, error: () => { finish(); } });
    this.notifService.getSpotUtilisation(lotId).subscribe({ next: v => { this.spotUtilisation.set(v); finish(); }, error: () => { finish(); } });
    this.notifService.getOccupancyByHour(lotId).subscribe({ next: v => { this.occupancyByHour.set(v); finish(); }, error: () => { finish(); } });
  }

  spotUtilKeys(): string[] { return Object.keys(this.spotUtilisation()); }
  spotUtilMax(): number { return Math.max(1, ...Object.values(this.spotUtilisation())); }
  hourKeys(): string[] { return Array.from({length: 24}, (_, i) => i.toString()); }
  hourMax(): number { return Math.max(1, ...Object.values(this.occupancyByHour())); }
  hourVal(h: string): number { return this.occupancyByHour()[h] ?? 0; }
  fmt24(h: string): string { const n = +h; return n === 0 ? '12a' : n < 12 ? `${n}a` : n === 12 ? '12p' : `${n-12}p`; }

  addBulkSpots(): void {
    if (!this.selectedLotId) { this.spotError.set('Please select a lot.'); return; }
    if (this.spotEnd < this.spotStart) { this.spotError.set('End must be ≥ start.'); return; }
    if (!this.spotPrice || this.spotPrice < 1) { this.spotError.set('Price per hour must be at least 1.'); return; }
    this.savingSpots.set(true); this.spotError.set(null); this.spotSuccess.set(null);
    const spots = [];
    for (let i = this.spotStart; i <= this.spotEnd; i++) {
      spots.push({
        spotNumber: `${this.spotPrefix}${i}`,
        spotType: this.spotType,
        vehicleType: this.vehicleType,
        floor: this.spotFloor,
        pricePerHour: this.spotPrice,
        isHandicapped: false,
        isEVCharging: this.spotType === 'EV_ONLY'
      });
    }
    this.parkingService.addBulkSpots(Number(this.selectedLotId), spots).subscribe({
      next: (res: any[]) => { this.spotSuccess.set(`${res.length} spots added successfully.`); this.savingSpots.set(false); this.loadLots(); },
      error: (err: any) => { this.savingSpots.set(false); this.spotError.set(err?.error?.message ?? 'Failed to add spots.'); }
    });
  }
}
