import { Component, inject, signal, OnInit, OnDestroy } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from './core/services/auth.service';
import { NotificationService, Notification } from './core/services/notification.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, CommonModule],
  template: `
    <nav class="navbar">
      <a routerLink="/" class="brand">
        <span class="brand-icon">P</span>
        <span class="brand-text">ParkEase</span>
      </a>
      <div class="nav-links">
        <a routerLink="/lots" routerLinkActive="active">Find Parking</a>
        @if (auth.isLoggedIn()) {
          @if (auth.getRole() === 'DRIVER') {
            <a routerLink="/driver" routerLinkActive="active">My Bookings</a>
          }
          @if (auth.getRole() === 'MANAGER') {
            <a routerLink="/manager" routerLinkActive="active">My Lots</a>
          }
          @if (auth.getRole() === 'ADMIN') {
            <a routerLink="/admin" routerLinkActive="active">Admin</a>
          }

          <!-- Notification Bell -->
          <div class="notif-wrap">
            <button class="bell-btn" (click)="toggleNotif()">
              🔔
              @if (unreadCount() > 0) {
                <span class="badge-dot">{{ unreadCount() > 9 ? '9+' : unreadCount() }}</span>
              }
            </button>
            @if (notifOpen()) {
              <div class="notif-dropdown">
                <div class="notif-header">
                  <span>Notifications</span>
                  @if (unreadCount() > 0) {
                    <button class="mark-all" (click)="markAllRead()">Mark all read</button>
                  }
                </div>
                @if (notifications().length === 0) {
                  <div class="notif-empty">No notifications yet</div>
                }
                @for (n of notifications(); track n.notificationId) {
                  <div [class]="'notif-item ' + (n.isRead ? 'read' : 'unread')" (click)="markRead(n)">
                    <div class="notif-title">{{ n.title }}</div>
                    <div class="notif-msg">{{ n.message }}</div>
                    <div class="notif-time">{{ n.sentAt | date:'dd MMM, hh:mm a' }}</div>
                  </div>
                }
              </div>
            }
          </div>

          <div class="user-pill">
            <span class="user-dot"></span>
            <span>{{ auth.currentUser()?.fullName?.split(' ')?.[0] }}</span>
          </div>
          <button class="btn-logout" (click)="auth.logout()">Logout</button>
        } @else {
          <a routerLink="/auth/login" routerLinkActive="active">Login</a>
          <a routerLink="/auth/register" class="btn-signup">Sign Up</a>
        }
      </div>
    </nav>
    <main (click)="closeNotifOutside($event)">
      <router-outlet />
    </main>
  `,
  styles: [`
    .navbar {
      display: flex; align-items: center; justify-content: space-between;
      background: #000; color: #f0f0f0;
      padding: 0 28px; height: 58px;
      border-bottom: 1px solid #1e1e1e;
      position: sticky; top: 0; z-index: 100;
    }
    .brand { display: flex; align-items: center; gap: 10px; text-decoration: none; }
    .brand-icon {
      width: 32px; height: 32px; background: #ffffff; color: #000000;
      border-radius: 7px; display: flex; align-items: center; justify-content: center;
      font-weight: 900; font-size: 16px;
    }
    .brand-text { font-size: 17px; font-weight: 700; color: #fff; letter-spacing: 0.3px; }

    .nav-links { display: flex; align-items: center; gap: 22px; }
    .nav-links a {
      color: #888; font-size: 13px; font-weight: 500;
      text-decoration: none; transition: color 0.15s;
    }
    .nav-links a:hover, .nav-links a.active { color: #ffffff; }

    /* ── Bell ── */
    .notif-wrap { position: relative; }
    .bell-btn {
      background: none; border: none; cursor: pointer;
      font-size: 18px; position: relative; padding: 4px 6px;
      line-height: 1; color: #888; transition: color 0.15s;
    }
    .bell-btn:hover { color: #fff; }
    .badge-dot {
      position: absolute; top: -2px; right: -4px;
      background: #fff; color: #000;
      font-size: 9px; font-weight: 800;
      min-width: 16px; height: 16px; border-radius: 8px;
      display: flex; align-items: center; justify-content: center;
      padding: 0 3px;
    }

    /* ── Dropdown ── */
    .notif-dropdown {
      position: absolute; top: calc(100% + 10px); right: 0;
      width: 320px; max-height: 420px; overflow-y: auto;
      background: #111; border: 1px solid #2a2a2a; border-radius: 10px;
      box-shadow: 0 8px 32px rgba(0,0,0,0.6);
      z-index: 999;
    }
    .notif-header {
      display: flex; justify-content: space-between; align-items: center;
      padding: 12px 14px; border-bottom: 1px solid #1e1e1e;
      font-size: 12px; font-weight: 700; color: #888; text-transform: uppercase; letter-spacing: 0.5px;
    }
    .mark-all {
      background: none; border: none; color: #666; font-size: 11px;
      cursor: pointer; padding: 0; transition: color 0.15s;
    }
    .mark-all:hover { color: #fff; }
    .notif-empty { padding: 24px; text-align: center; color: #444; font-size: 13px; }
    .notif-item {
      padding: 12px 14px; border-bottom: 1px solid #1a1a1a;
      cursor: pointer; transition: background 0.15s;
    }
    .notif-item:hover { background: #1a1a1a; }
    .notif-item.unread { border-left: 3px solid #fff; }
    .notif-item.read { border-left: 3px solid transparent; opacity: 0.6; }
    .notif-title { font-size: 13px; font-weight: 600; color: #f0f0f0; margin-bottom: 3px; }
    .notif-msg { font-size: 12px; color: #666; margin-bottom: 5px; line-height: 1.4; }
    .notif-time { font-size: 10px; color: #444; }

    .user-pill {
      display: flex; align-items: center; gap: 7px;
      background: #1a1a1a; border: 1px solid #2a2a2a;
      padding: 5px 12px; border-radius: 20px; font-size: 13px; color: #ccc;
    }
    .user-dot { width: 7px; height: 7px; border-radius: 50%; background: #ffffff; }

    .btn-logout {
      background: transparent; color: #666;
      border: 1px solid #2a2a2a; padding: 6px 14px;
      border-radius: 7px; cursor: pointer; font-size: 12px; font-weight: 500;
      transition: color 0.15s, border-color 0.15s;
    }
    .btn-logout:hover { color: #ccc; border-color: #555; }

    .btn-signup {
      background: #ffffff !important; color: #000000 !important;
      padding: 7px 16px; border-radius: 7px;
      font-weight: 700 !important; font-size: 13px !important;
      transition: background 0.15s, box-shadow 0.15s !important;
    }
    .btn-signup:hover { background: #e0e0e0 !important; box-shadow: 0 0 14px rgba(255,255,255,0.15); }

    main { min-height: calc(100vh - 58px); }
  `]
})
export class AppComponent implements OnInit, OnDestroy {
  auth = inject(AuthService);
  private notifService = inject(NotificationService);

  notifications = signal<Notification[]>([]);
  unreadCount = signal(0);
  notifOpen = signal(false);
  private pollInterval: any;

  ngOnInit(): void {
    if (this.auth.isLoggedIn()) this.loadNotifications();
    this.pollInterval = setInterval(() => {
      if (this.auth.isLoggedIn()) this.loadNotifications();
    }, 30000);
  }

  ngOnDestroy(): void {
    clearInterval(this.pollInterval);
  }

  private loadNotifications(): void {
    this.notifService.getAll().subscribe({
      next: (data) => {
        this.notifications.set(data);
        this.unreadCount.set(data.filter(n => !n.isRead).length);
      },
      error: () => {}
    });
  }

  toggleNotif(): void {
    this.notifOpen.set(!this.notifOpen());
    if (this.notifOpen()) this.loadNotifications();
  }

  closeNotifOutside(e: MouseEvent): void {
    const target = e.target as HTMLElement;
    if (!target.closest('.notif-wrap')) this.notifOpen.set(false);
  }

  markRead(n: Notification): void {
    if (n.isRead) return;
    this.notifService.markAsRead(n.notificationId).subscribe({
      next: () => {
        n.isRead = true;
        this.unreadCount.set(Math.max(0, this.unreadCount() - 1));
      },
      error: () => {}
    });
  }

  markAllRead(): void {
    this.notifService.markAllAsRead().subscribe({
      next: () => {
        this.notifications.update(list => list.map(n => ({ ...n, isRead: true })));
        this.unreadCount.set(0);
      },
      error: () => {}
    });
  }
}
