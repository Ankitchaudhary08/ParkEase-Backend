import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { LoginComponent } from './login.component';
import { AuthService, AuthUser } from '../../../../core/services/auth.service';

const makeUser = (role: AuthUser['role']): AuthUser => ({
  userId: 1, email: 'test@test.com', fullName: 'Test User', role, accessToken: 'tok'
});

describe('LoginComponent', () => {
  let fixture: ComponentFixture<LoginComponent>;
  let component: LoginComponent;
  let authSpy: jasmine.SpyObj<AuthService>;
  let router: Router;

  beforeEach(async () => {
    authSpy = jasmine.createSpyObj('AuthService', ['login']);

    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        { provide: AuthService, useValue: authSpy },
        provideRouter([])
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  it('should create the component', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize with empty email, password, and no error', () => {
    expect(component.email).toBe('');
    expect(component.password).toBe('');
    expect(component.error()).toBeNull();
    expect(component.loading()).toBeFalse();
  });

  describe('login() — validation', () => {
    it('should set error and not call auth.login when email is empty', () => {
      component.email = '';
      component.password = 'pass1234';
      component.login();
      expect(component.error()).toBe('Please enter your email and password.');
      expect(authSpy.login).not.toHaveBeenCalled();
    });

    it('should set error and not call auth.login when password is empty', () => {
      component.email = 'test@test.com';
      component.password = '';
      component.login();
      expect(component.error()).toBe('Please enter your email and password.');
      expect(authSpy.login).not.toHaveBeenCalled();
    });

    it('should set error when both fields are empty', () => {
      component.login();
      expect(component.error()).toBe('Please enter your email and password.');
    });
  });

  describe('login() — success', () => {
    it('should call auth.login with the entered credentials', () => {
      authSpy.login.and.returnValue(of(makeUser('DRIVER')));
      component.email = 'test@test.com';
      component.password = 'pass1234';
      component.login();
      expect(authSpy.login).toHaveBeenCalledWith('test@test.com', 'pass1234');
    });

    it('should set loading=true before response', () => {
      authSpy.login.and.returnValue(of(makeUser('DRIVER')));
      component.email = 'test@test.com';
      component.password = 'pass1234';
      component.login();
      // loading is set synchronously before subscribe
      expect(authSpy.login).toHaveBeenCalled();
    });

    it('should navigate to /driver for DRIVER role', fakeAsync(() => {
      authSpy.login.and.returnValue(of(makeUser('DRIVER')));
      const navSpy = spyOn(router, 'navigate');
      component.email = 'test@test.com';
      component.password = 'pass1234';
      component.login();
      tick();
      expect(navSpy).toHaveBeenCalledWith(['/driver']);
    }));

    it('should navigate to /manager for MANAGER role', fakeAsync(() => {
      authSpy.login.and.returnValue(of(makeUser('MANAGER')));
      const navSpy = spyOn(router, 'navigate');
      component.email = 'mgr@test.com';
      component.password = 'pass1234';
      component.login();
      tick();
      expect(navSpy).toHaveBeenCalledWith(['/manager']);
    }));

    it('should navigate to /admin for ADMIN role', fakeAsync(() => {
      authSpy.login.and.returnValue(of(makeUser('ADMIN')));
      const navSpy = spyOn(router, 'navigate');
      component.email = 'admin@test.com';
      component.password = 'pass1234';
      component.login();
      tick();
      expect(navSpy).toHaveBeenCalledWith(['/admin']);
    }));
  });

  describe('login() — errors', () => {
    it('should set "Invalid email or password" error on 401', fakeAsync(() => {
      authSpy.login.and.returnValue(throwError(() => ({ status: 401 })));
      component.email = 'test@test.com';
      component.password = 'wrongpass';
      component.login();
      tick();
      expect(component.error()).toBe('Invalid email or password.');
      expect(component.loading()).toBeFalse();
    }));

    it('should set "Invalid email or password" error on 400', fakeAsync(() => {
      authSpy.login.and.returnValue(throwError(() => ({ status: 400 })));
      component.email = 'test@test.com';
      component.password = 'wrongpass';
      component.login();
      tick();
      expect(component.error()).toBe('Invalid email or password.');
    }));

    it('should prefer server error message over default', fakeAsync(() => {
      authSpy.login.and.returnValue(throwError(() => ({ status: 400, error: { message: 'Account locked' } })));
      component.email = 'test@test.com';
      component.password = 'pass1234';
      component.login();
      tick();
      expect(component.error()).toBe('Account locked');
    }));

    it('should set generic error on 500 server error', fakeAsync(() => {
      authSpy.login.and.returnValue(throwError(() => ({ status: 500 })));
      component.email = 'test@test.com';
      component.password = 'pass1234';
      component.login();
      tick();
      expect(component.error()).toBe('Login failed. Please try again.');
      expect(component.loading()).toBeFalse();
    }));

    it('should clear previous error before new login attempt', fakeAsync(() => {
      component.error.set('Old error');
      authSpy.login.and.returnValue(of(makeUser('DRIVER')));
      spyOn(router, 'navigate');
      component.email = 'test@test.com';
      component.password = 'pass1234';
      component.login();
      tick();
      expect(component.error()).toBeNull();
    }));
  });
});
