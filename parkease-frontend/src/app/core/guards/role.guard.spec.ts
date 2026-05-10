import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router } from '@angular/router';
import { roleGuard } from './role.guard';
import { AuthService } from '../services/auth.service';

const makeRoute = (role: string) =>
  ({ data: { role } } as unknown as ActivatedRouteSnapshot);

describe('roleGuard', () => {
  let authSpy: jasmine.SpyObj<AuthService>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(() => {
    authSpy = jasmine.createSpyObj('AuthService', ['getRole']);
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: authSpy },
        { provide: Router, useValue: routerSpy }
      ]
    });
  });

  it('should return true when user role matches required role', () => {
    authSpy.getRole.and.returnValue('MANAGER');

    const result = TestBed.runInInjectionContext(() =>
      roleGuard(makeRoute('MANAGER'), {} as any)
    );

    expect(result).toBeTrue();
    expect(routerSpy.navigate).not.toHaveBeenCalled();
  });

  it('should return false and redirect to / when role does not match required role', () => {
    authSpy.getRole.and.returnValue('DRIVER');

    const result = TestBed.runInInjectionContext(() =>
      roleGuard(makeRoute('ADMIN'), {} as any)
    );

    expect(result).toBeFalse();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/']);
  });

  it('should return false and redirect to / when user has no role (null)', () => {
    authSpy.getRole.and.returnValue(null);

    const result = TestBed.runInInjectionContext(() =>
      roleGuard(makeRoute('DRIVER'), {} as any)
    );

    expect(result).toBeFalse();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/']);
  });

  it('should allow ADMIN role to access ADMIN route', () => {
    authSpy.getRole.and.returnValue('ADMIN');

    const result = TestBed.runInInjectionContext(() =>
      roleGuard(makeRoute('ADMIN'), {} as any)
    );

    expect(result).toBeTrue();
  });

  it('should allow DRIVER role to access DRIVER route', () => {
    authSpy.getRole.and.returnValue('DRIVER');

    const result = TestBed.runInInjectionContext(() =>
      roleGuard(makeRoute('DRIVER'), {} as any)
    );

    expect(result).toBeTrue();
  });
});
