import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ParkingService } from '../../../../core/services/parking.service';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="page-container">
      <div class="dash-header">
        <div>
          <h1 class="page-title">Admin Dashboard</h1>
          <p class="sub">Review pending lots and manage the platform</p>
        </div>
      </div>

      <div class="tab-bar">
        <button [class]="'tab ' + (tab() === 'pending' ? 'active' : '')" (click)="switchTab('pending')">
          🔔 Pending Approvals
          @if (pending().length > 0) {
            <span class="count-dot">{{ pending().length }}</span>
          }
        </button>
        <button [class]="'tab ' + (tab() === 'all' ? 'active' : '')" (click)="switchTab('all')">
          🏢 All Lots
        </button>
      </div>

      @if (error()) { <div class="alert-error">{{ error() }}</div> }
      @if (actionMsg()) { <div class="alert-success">{{ actionMsg() }}</div> }

      <!-- Pending -->
      @if (tab() === 'pending') {
        @if (loading()) { <div class="loading-row"><div class="spinner"></div> Loading...</div> }
        @if (!loading() && pending().length === 0) {
          <div class="empty-state card">
            <div class="ei">🎉</div>
            <h3>All clear!</h3>
            <p>No parking lots pending approval.</p>
          </div>
        }
        <div class="grid-2">
          @for (lot of pending(); track lot.lotId) {
            <div class="pending-card card">
              <div class="pc-head">
                <div>
                  <div class="pc-name">{{ lot.name }}</div>
                  <div class="pc-addr">📍 {{ lot.address }}, {{ lot.city }}</div>
                  <div class="pc-meta">₹{{ lot.hourlyRate }}/hr &nbsp;·&nbsp; {{ lot.totalSpots }} spots</div>
                </div>
                <span class="badge badge-orange">PENDING</span>
              </div>
              <input
                type="text"
                [(ngModel)]="rejectReasons[lot.lotId]"
                placeholder="Rejection reason (if rejecting)"
                class="reason-input"
              />
              <div class="pc-actions">
                <button class="btn btn-success btn-sm" (click)="approve(lot.lotId)">✓ Approve</button>
                <button class="btn btn-danger btn-sm" (click)="reject(lot.lotId)">✗ Reject</button>
              </div>
            </div>
          }
        </div>
      }

      <!-- All Lots -->
      @if (tab() === 'all') {
        @if (loading()) { <div class="loading-row"><div class="spinner"></div> Loading...</div> }
        <div class="table-card card">
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>Name</th>
                <th>City</th>
                <th>Rate</th>
                <th>Spots</th>
                <th>Status</th>
                <th>Open</th>
              </tr>
            </thead>
            <tbody>
              @for (lot of allLots(); track lot.lotId) {
                <tr>
                  <td class="id-cell">#{{ lot.lotId }}</td>
                  <td><strong>{{ lot.name }}</strong></td>
                  <td>{{ lot.city }}</td>
                  <td class="rate-cell">₹{{ lot.hourlyRate }}</td>
                  <td>{{ lot.totalSpots }}</td>
                  <td><span [class]="'badge ' + statusBadge(lot.approvalStatus)">{{ lot.approvalStatus }}</span></td>
                  <td>
                    <span [class]="'badge ' + (lot.open ? 'badge-green' : 'badge-red')">
                      {{ lot.open ? 'Yes' : 'No' }}
                    </span>
                  </td>
                </tr>
              }
            </tbody>
          </table>
          @if (!loading() && allLots().length === 0) {
            <p class="state-msg">No lots found.</p>
          }
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
      font-size: 13px; font-weight: 500; color: #555; display: flex; align-items: center; gap: 8px;
      border-bottom: 2px solid transparent; margin-bottom: -1px; transition: color 0.15s;
    }
    .tab:hover { color: #f0f0f0; }
    .tab.active { color: #ffffff; border-bottom-color: #ffffff; font-weight: 600; }
    .count-dot {
      background: #ffffff; color: #000000; font-size: 10px; font-weight: 800;
      width: 18px; height: 18px; border-radius: 50%;
      display: flex; align-items: center; justify-content: center;
    }

    .loading-row { display: flex; align-items: center; gap: 10px; color: #555; padding: 28px 0; }
    .spinner {
      width: 16px; height: 16px; border: 2px solid #2a2a2a;
      border-top-color: #888888; border-radius: 50%; animation: spin 0.7s linear infinite;
    }
    @keyframes spin { to { transform: rotate(360deg); } }

    .empty-state { text-align: center; padding: 48px 24px; max-width: 320px; margin: 0 auto; }
    .ei { font-size: 40px; margin-bottom: 14px; }
    .empty-state h3 { color: #f0f0f0; margin: 0 0 8px; }
    .empty-state p { color: #555; margin: 0; }

    .pending-card { display: flex; flex-direction: column; gap: 12px; }
    .pc-head { display: flex; justify-content: space-between; align-items: flex-start; }
    .pc-name { font-size: 15px; font-weight: 600; color: #f0f0f0; margin-bottom: 4px; }
    .pc-addr { font-size: 12px; color: #555; margin-bottom: 2px; }
    .pc-meta { font-size: 12px; color: #888888; font-weight: 600; }
    .reason-input {
      width: 100%; padding: 9px 12px;
      background: #0d0d0d; border: 1px solid #2a2a2a;
      border-radius: 7px; font-size: 13px; color: #f0f0f0; outline: none;
    }
    .reason-input:focus { border-color: #ffffff; }
    .reason-input::placeholder { color: #333; }
    .pc-actions { display: flex; gap: 8px; }

    .table-card { padding: 0; overflow: hidden; }
    .id-cell { color: #555; font-size: 12px; }
    .rate-cell { color: #ffffff; font-weight: 600; }
  `]
})
export class AdminDashboardComponent implements OnInit {
  private parkingService = inject(ParkingService);

  tab = signal<'pending' | 'all'>('pending');
  pending = signal<any[]>([]);
  allLots = signal<any[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);
  actionMsg = signal<string | null>(null);
  rejectReasons: Record<number, string> = {};

  ngOnInit(): void { this.loadPending(); }

  switchTab(t: 'pending' | 'all'): void {
    this.tab.set(t);
    if (t === 'all' && this.allLots().length === 0) this.loadAll();
  }

  loadPending(): void {
    this.parkingService.getPendingLots().subscribe({
      next: (data: any[]) => { this.pending.set(data); this.loading.set(false); },
      error: () => { this.error.set('Failed to load pending lots.'); this.loading.set(false); }
    });
  }

  loadAll(): void {
    this.parkingService.getAllLots().subscribe({
      next: (data: any[]) => this.allLots.set(data),
      error: () => { this.error.set('Failed to load lots. Admin access required.'); }
    });
  }

  approve(lotId: number): void {
    this.parkingService.approveLot(lotId, true).subscribe({
      next: () => {
        this.actionMsg.set('Lot approved successfully.');
        this.pending.set(this.pending().filter((l: any) => l.lotId !== lotId));
        if (this.tab() === 'all') this.loadAll();
        setTimeout(() => this.actionMsg.set(null), 3000);
      },
      error: (err: any) => this.error.set(err?.error?.message ?? 'Approval failed.')
    });
  }

  reject(lotId: number): void {
    const reason = this.rejectReasons[lotId] ?? '';
    this.parkingService.approveLot(lotId, false, reason).subscribe({
      next: () => {
        this.actionMsg.set('Lot rejected.');
        this.pending.set(this.pending().filter((l: any) => l.lotId !== lotId));
        setTimeout(() => this.actionMsg.set(null), 3000);
      },
      error: (err: any) => this.error.set(err?.error?.message ?? 'Rejection failed.')
    });
  }

  statusBadge(status: string): string {
    const m: Record<string, string> = { APPROVED: 'badge-green', PENDING: 'badge-orange', REJECTED: 'badge-red' };
    return m[status] ?? 'badge-gray';
  }
}
