import { Component, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="auth-wrap">
      <div class="auth-card">
        <div class="card-brand">
          <span class="brand-icon">P</span>
          <span>ParkEase</span>
        </div>
        <h2>Welcome back</h2>
        <p class="sub">Sign in to your account</p>

        @if (error()) {
          <div class="alert-error">{{ error() }}</div>
        }

        <div class="form-group">
          <label>Email Address</label>
          <input type="email" [(ngModel)]="email" placeholder="you@email.com" autocomplete="email" />
        </div>
        <div class="form-group">
          <label>Password</label>
          <input type="password" [(ngModel)]="password" placeholder="••••••••" (keydown.enter)="login()" autocomplete="current-password" />
        </div>
        <button class="btn-submit" [disabled]="loading()" (click)="login()">
          @if (loading()) {
            <span class="btn-spinner"></span> Signing in...
          } @else {
            Sign In
          }
        </button>
        <p class="switch-link">Don't have an account? <a routerLink="/auth/register">Create one</a></p>
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
      width: 100%; max-width: 400px;
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
    .btn-submit {
      width: 100%; padding: 12px;
      background: #ffffff; color: #000000;
      border: none; border-radius: 8px;
      font-size: 15px; font-weight: 700; cursor: pointer;
      display: flex; align-items: center; justify-content: center; gap: 8px;
      transition: background 0.15s, box-shadow 0.15s;
      margin-top: 6px;
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
export class LoginComponent {
  private auth = inject(AuthService);
  private router = inject(Router);

  email = '';
  password = '';
  loading = signal(false);
  error = signal<string | null>(null);

  login(): void {
    if (!this.email || !this.password) { this.error.set('Please enter your email and password.'); return; }
    this.loading.set(true);
    this.error.set(null);
    this.auth.login(this.email, this.password).subscribe({
      next: user => {
        const role = user.role;
        if (role === 'DRIVER') this.router.navigate(['/driver']);
        else if (role === 'MANAGER') this.router.navigate(['/manager']);
        else if (role === 'ADMIN') this.router.navigate(['/admin']);
        else this.router.navigate(['/']);
      },
      error: err => {
        this.loading.set(false);
        const msg = err?.error?.message
          ?? (err?.status === 400 || err?.status === 401 ? 'Invalid email or password.' : null)
          ?? 'Login failed. Please try again.';
        this.error.set(msg);
      }
    });
  }
}
