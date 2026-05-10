import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { HttpClient } from '@angular/common/http';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from '../services/auth.service';

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let authSpy: jasmine.SpyObj<AuthService>;

  beforeEach(() => {
    authSpy = jasmine.createSpyObj('AuthService', ['getToken']);

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: authSpy }
      ]
    });

    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should attach Bearer token header for /api requests when token exists', () => {
    authSpy.getToken.and.returnValue('my-secret-token');

    http.get('/api/v1/lots').subscribe();

    const req = httpMock.expectOne('/api/v1/lots');
    expect(req.request.headers.get('Authorization')).toBe('Bearer my-secret-token');
    req.flush([]);
  });

  it('should NOT attach Authorization header when token is null', () => {
    authSpy.getToken.and.returnValue(null);

    http.get('/api/v1/lots').subscribe();

    const req = httpMock.expectOne('/api/v1/lots');
    expect(req.request.headers.get('Authorization')).toBeNull();
    req.flush([]);
  });

  it('should NOT attach Authorization header for external non-/api URLs', () => {
    authSpy.getToken.and.returnValue('my-secret-token');

    http.get('https://tile.openstreetmap.org/tile.png').subscribe();

    const req = httpMock.expectOne('https://tile.openstreetmap.org/tile.png');
    expect(req.request.headers.get('Authorization')).toBeNull();
    req.flush('');
  });

  it('should pass through requests unmodified when no token and no /api path', () => {
    authSpy.getToken.and.returnValue(null);

    http.post('/auth/callback', { code: 'xyz' }).subscribe();

    const req = httpMock.expectOne('/auth/callback');
    expect(req.request.headers.get('Authorization')).toBeNull();
    req.flush({});
  });

  it('should attach header to POST /api requests as well', () => {
    authSpy.getToken.and.returnValue('post-token');

    http.post('/api/v1/bookings', { spotId: 1 }).subscribe();

    const req = httpMock.expectOne('/api/v1/bookings');
    expect(req.request.headers.get('Authorization')).toBe('Bearer post-token');
    req.flush({ bookingId: 1 });
  });
});
