import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { tap } from 'rxjs/operators';
import { Observable } from 'rxjs';

export interface AuthUser {
  userId: number;
  email: string;
  fullName: string;
  role: 'DRIVER' | 'MANAGER' | 'ADMIN';
  accessToken: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly API = '/api/v1/auth';
  currentUser = signal<AuthUser | null>(this.loadFromStorage());

  constructor(private http: HttpClient, private router: Router) {}

  register(payload: any): Observable<AuthUser> {
    return this.http.post<AuthUser>(`${this.API}/register`, payload).pipe(
      tap(user => this.saveUser(user))
    );
  }

  login(email: string, password: string): Observable<AuthUser> {
    return this.http.post<AuthUser>(`${this.API}/login`, { email, password }).pipe(
      tap(user => this.saveUser(user))
    );
  }

  logout(): void {
    localStorage.removeItem('parkease_user');
    this.currentUser.set(null);
    this.router.navigate(['/auth/login']);
  }

  getProfile(): Observable<any> {
    return this.http.get(`${this.API}/profile`);
  }

  isLoggedIn(): boolean { return this.currentUser() !== null; }
  getToken(): string | null { return this.currentUser()?.accessToken ?? null; }
  getRole(): string | null { return this.currentUser()?.role ?? null; }

  private saveUser(user: AuthUser): void {
    localStorage.setItem('parkease_user', JSON.stringify(user));
    this.currentUser.set(user);
  }

  private loadFromStorage(): AuthUser | null {
    const raw = localStorage.getItem('parkease_user');
    return raw ? JSON.parse(raw) : null;
  }
}
