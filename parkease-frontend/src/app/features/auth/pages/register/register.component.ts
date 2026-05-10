import { Component, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="auth-wrap">
      <div class="auth-card">
        <div class="card-brand">
          <span class="brand-icon">P</span>
          <span>ParkEase</span>
        </div>
        <h2>Create account</h2>
        <p class="sub">Join thousands of drivers & managers</p>

        @if (error()) {
          <div class="alert-error">{{ error() }}</div>
        }
        @if (success()) {
          <div class="alert-success">✓ Account created! Redirecting...</div>
        }

        <div class="form-row">
          <div class="form-group">
            <label>Full Name</label>
            <input type="text" [(ngModel)]="fullName" placeholder="John Doe" />
          </div>
          <div class="form-group">
            <label>Phone (optional)</label>
            <input type="text" [(ngModel)]="phone" placeholder="9876543210" />
          </div>
        </div>
        <div class="form-group">
          <label>Email Address</label>
          <input type="email" [(ngModel)]="email" placeholder="you@email.com" />
        </div>
        <div class="form-group">
          <label>Password <span class="label-hint">(min 8 characters)</span></label>
          <input type="password" [(ngModel)]="password" placeholder="••••••••" />
        </div>
        <div class="form-group">
          <label>I am a</label>
          <div class="role-select">
            <button
              [class]="'role-btn ' + (role === 'DRIVER' ? 'active' : '')"
              (click)="role = 'DRIVER'">
              🚗 Driver
            </button>
            <button
              [class]="'role-btn ' + (role === 'MANAGER' ? 'active' : '')"
              (click)="role = 'MANAGER'">
              🏢 Parking Manager
            </button>
            <button
              [class]="'role-btn ' + (role === 'ADMIN' ? 'active' : '')"
              (click)="role = 'ADMIN'">
              🛡️ Admin
            </button>
          </div>
        </div>

        <button class="btn-submit" [disabled]="loading()" (click)="register()">
          @if (loading()) {
            <span class="btn-spinner"></span> Creating Account...
          } @else {
            Create Account
          }
        </button>
        <p class="switch-link">Already have an account? <a routerLink="/auth/login">Sign in</a></p>
      </div>
    </div>
  `,
  styles: [`
    .auth-wrap {
      display: flex; justify-content: center; align-items: center;
      min-height: calc(100vh - 58px); background: #0a0a0a; padding: 24px;
    }
    .auth-card {
      background: #141414; border: 1px solid #2a2a2a;
      border-radius: 14px; padding: 40px 36px;
      width: 100%; max-width: 480px;
    }
    .card-brand {
      display: flex; align-items: center; gap: 10px;
      margin-bottom: 28px; font-size: 16px; font-weight: 700; color: #f0f0f0;
    }
    .brand-icon {
      width: 34px; height: 34px; background: #ffffff; color: #000000;
      border-radius: 7px; display: flex; align-items: center; justify-content: center;
      font-weight: 900; font-size: 16px;
    }
    h2 { margin: 0 0 6px; font-size: 24px; font-weight: 700; color: #fff; }
    .sub { margin: 0 0 28px; color: #555; font-size: 14px; }
    .label-hint { font-size: 10px; color: #555; font-weight: 400; text-transform: none; letter-spacing: 0; }
    .form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }

    .role-select { display: flex; gap: 10px; }
    .role-btn {
      flex: 1; padding: 10px 12px;
      background: #0d0d0d; border: 1px solid #2a2a2a;
      border-radius: 8px; color: #666; font-size: 13px; font-weight: 500;
      cursor: pointer; transition: all 0.15s;
    }
    .role-btn:hover { border-color: #3a3a3a; color: #ccc; }
    .role-btn.active { background: rgba(255,255,255,0.06); border-color: #ffffff; color: #ffffff; font-weight: 600; }

    .btn-submit {
      width: 100%; padding: 12px; margin-top: 6px;
      background: #ffffff; color: #000000;
      border: none; border-radius: 8px;
      font-size: 15px; font-weight: 700; cursor: pointer;
      display: flex; align-items: center; justify-content: center; gap: 8px;
      transition: background 0.15s, box-shadow 0.15s;
    }
    .btn-submit:hover:not(:disabled) { background: #e0e0e0; box-shadow: 0 0 18px rgba(255,255,255,0.1); }
    .btn-submit:disabled { opacity: 0.5; cursor: not-allowed; }
    .btn-spinner {
      width: 15px; height: 15px;
      border: 2px solid rgba(0,0,0,0.25); border-top-color: #000000;
      border-radius: 50%; animation: spin 0.7s linear infinite;
    }
    @keyframes spin { to { transform: rotate(360deg); } }
    .switch-link { text-align: center; margin-top: 20px; font-size: 13px; color: #555; }
    .switch-link a { color: #ffffff; font-weight: 600; }
    .switch-link a:hover { text-decoration: underline; }
  `]
})
export class RegisterComponent {
  private auth = inject(AuthService);
  private router = inject(Router);

  fullName = '';
  email = '';
  password = '';
  phone = '';
  role = 'DRIVER';
  loading = signal(false);
  error = signal<string | null>(null);
  success = signal(false);

  register(): void {
    if (!this.fullName || !this.email || !this.password) {
      this.error.set('Please fill all required fields.');
      return;
    }
    if (this.password.length < 8) {
      this.error.set('Password must be at least 8 characters.');
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    const payload: any = { fullName: this.fullName, email: this.email, password: this.password, role: this.role };
    if (this.phone) payload.phone = this.phone;

    this.auth.register(payload).subscribe({
      next: user => {
        this.success.set(true);
        setTimeout(() => {
          const r = user.role;
          if (r === 'DRIVER') this.router.navigate(['/driver']);
          else if (r === 'MANAGER') this.router.navigate(['/manager']);
          else if (r === 'ADMIN') this.router.navigate(['/admin']);
          else this.router.navigate(['/']);
        }, 1200);
      },
      error: err => {
        this.loading.set(false);
        const body = err?.error;
        const msg = body?.message
          ?? (Array.isArray(body?.errors) ? body.errors.join(', ') : null)
          ?? (err?.status === 400 ? 'Check your details: password must be 8+ chars, phone must be 10 digits starting with 6-9.' : null)
          ?? (err?.status === 409 ? 'Email already registered. Please login instead.' : null)
          ?? 'Registration failed. Please try again.';
        this.error.set(msg);
      }
    });
  }
}
