import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { AuthService, AuthUser } from './auth.service';

const MOCK_USER: AuthUser = {
  userId: 1,
  email: 'driver@test.com',
  fullName: 'Test Driver',
  role: 'DRIVER',
  accessToken: 'abc123'
};

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;
  let router: Router;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([])
      ]
    });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
  });

  afterEach(() => {
    http.verify();
    localStorage.clear();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('login()', () => {
    it('should POST credentials to /api/v1/auth/login', () => {
      service.login('driver@test.com', 'pass1234').subscribe();
      const req = http.expectOne('/api/v1/auth/login');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ email: 'driver@test.com', password: 'pass1234' });
      req.flush(MOCK_USER);
    });

    it('should persist user to signal and localStorage after login', () => {
      service.login('driver@test.com', 'pass1234').subscribe(user => {
        expect(user).toEqual(MOCK_USER);
        expect(service.currentUser()).toEqual(MOCK_USER);
        expect(JSON.parse(localStorage.getItem('parkease_user')!)).toEqual(MOCK_USER);
      });
      http.expectOne('/api/v1/auth/login').flush(MOCK_USER);
    });
  });

  describe('register()', () => {
    it('should POST payload to /api/v1/auth/register', () => {
      const payload = { fullName: 'Test Driver', email: 'driver@test.com', password: 'pass1234', role: 'DRIVER' };
      service.register(payload).subscribe();
      const req = http.expectOne('/api/v1/auth/register');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(payload);
      req.flush(MOCK_USER);
    });

    it('should save user on successful registration', () => {
      const payload = { fullName: 'Test Driver', email: 'driver@test.com', password: 'pass1234', role: 'DRIVER' };
      service.register(payload).subscribe(() => {
        expect(service.currentUser()).toEqual(MOCK_USER);
      });
      http.expectOne('/api/v1/auth/register').flush(MOCK_USER);
    });
  });

  describe('logout()', () => {
    it('should clear localStorage and nullify currentUser', () => {
      service.currentUser.set(MOCK_USER);
      localStorage.setItem('parkease_user', JSON.stringify(MOCK_USER));
      const navSpy = spyOn(router, 'navigate');

      service.logout();

      expect(localStorage.getItem('parkease_user')).toBeNull();
      expect(service.currentUser()).toBeNull();
      expect(navSpy).toHaveBeenCalledWith(['/auth/login']);
    });
  });

  describe('isLoggedIn()', () => {
    it('returns true when currentUser is set', () => {
      service.currentUser.set(MOCK_USER);
      expect(service.isLoggedIn()).toBeTrue();
    });

    it('returns false when no user', () => {
      service.currentUser.set(null);
      expect(service.isLoggedIn()).toBeFalse();
    });
  });

  describe('getToken()', () => {
    it('returns accessToken when user is set', () => {
      service.currentUser.set(MOCK_USER);
      expect(service.getToken()).toBe('abc123');
    });

    it('returns null when logged out', () => {
      service.currentUser.set(null);
      expect(service.getToken()).toBeNull();
    });
  });

  describe('getRole()', () => {
    it('returns role of the current user', () => {
      service.currentUser.set({ ...MOCK_USER, role: 'MANAGER' });
      expect(service.getRole()).toBe('MANAGER');
    });

    it('returns null when no user', () => {
      service.currentUser.set(null);
      expect(service.getRole()).toBeNull();
    });
  });

  describe('getProfile()', () => {
    it('should GET /api/v1/auth/profile', () => {
      const profile = { userId: 1, email: 'driver@test.com' };
      service.getProfile().subscribe(p => expect(p).toEqual(profile));
      const req = http.expectOne('/api/v1/auth/profile');
      expect(req.request.method).toBe('GET');
      req.flush(profile);
    });
  });

  describe('loadFromStorage()', () => {
    it('restores user from localStorage on service initialization', () => {
      localStorage.setItem('parkease_user', JSON.stringify(MOCK_USER));
      TestBed.resetTestingModule();
      TestBed.configureTestingModule({
        providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
      });
      const freshService = TestBed.inject(AuthService);
      expect(freshService.currentUser()).toEqual(MOCK_USER);
      TestBed.inject(HttpTestingController).verify();
    });

    it('initializes currentUser to null when localStorage is empty', () => {
      expect(service.currentUser()).toBeNull();
    });
  });
});
