import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { RegisterComponent } from './register.component';
import { AuthService, AuthUser } from '../../../../core/services/auth.service';

const makeUser = (role: AuthUser['role']): AuthUser => ({
  userId: 2, email: 'new@test.com', fullName: 'New User', role, accessToken: 'tok2'
});

describe('RegisterComponent', () => {
  let fixture: ComponentFixture<RegisterComponent>;
  let component: RegisterComponent;
  let authSpy: jasmine.SpyObj<AuthService>;
  let router: Router;

  beforeEach(async () => {
    authSpy = jasmine.createSpyObj('AuthService', ['register']);

    await TestBed.configureTestingModule({
      imports: [RegisterComponent],
      providers: [
        { provide: AuthService, useValue: authSpy },
        provideRouter([])
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(RegisterComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  it('should create the component', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize with DRIVER as default role', () => {
    expect(component.role).toBe('DRIVER');
  });

  it('should initialize with empty fields and no error', () => {
    expect(component.fullName).toBe('');
    expect(component.email).toBe('');
    expect(component.password).toBe('');
    expect(component.error()).toBeNull();
    expect(component.success()).toBeFalse();
  });

  describe('register() — validation', () => {
    it('should set error when required fields are missing', () => {
      component.register();
      expect(component.error()).toBe('Please fill all required fields.');
      expect(authSpy.register).not.toHaveBeenCalled();
    });

    it('should set error when fullName is missing', () => {
      component.email = 'test@test.com';
      component.password = 'password123';
      component.register();
      expect(component.error()).toBe('Please fill all required fields.');
    });

    it('should set error when email is missing', () => {
      component.fullName = 'John';
      component.password = 'password123';
      component.register();
      expect(component.error()).toBe('Please fill all required fields.');
    });

    it('should set error when password is missing', () => {
      component.fullName = 'John';
      component.email = 'john@test.com';
      component.register();
      expect(component.error()).toBe('Please fill all required fields.');
    });

    it('should set error when password is shorter than 8 characters', () => {
      component.fullName = 'John';
      component.email = 'john@test.com';
      component.password = 'short';
      component.register();
      expect(component.error()).toBe('Password must be at least 8 characters.');
    });

    it('should set error for password of exactly 7 characters', () => {
      component.fullName = 'John';
      component.email = 'john@test.com';
      component.password = '1234567';
      component.register();
      expect(component.error()).toBe('Password must be at least 8 characters.');
    });
  });

  describe('register() — success', () => {
    const fillForm = (comp: RegisterComponent) => {
      comp.fullName = 'New User';
      comp.email = 'new@test.com';
      comp.password = 'password123';
    };

    it('should call auth.register with correct payload', () => {
      authSpy.register.and.returnValue(of(makeUser('DRIVER')));
      fillForm(component);
      component.register();
      expect(authSpy.register).toHaveBeenCalledWith({
        fullName: 'New User',
        email: 'new@test.com',
        password: 'password123',
        role: 'DRIVER'
      });
    });

    it('should include phone in payload when provided', () => {
      authSpy.register.and.returnValue(of(makeUser('DRIVER')));
      fillForm(component);
      component.phone = '9876543210';
      component.register();
      expect(authSpy.register).toHaveBeenCalledWith(
        jasmine.objectContaining({ phone: '9876543210' })
      );
    });

    it('should NOT include phone when phone is empty', () => {
      authSpy.register.and.returnValue(of(makeUser('DRIVER')));
      fillForm(component);
      component.phone = '';
      component.register();
      const callArg = authSpy.register.calls.mostRecent().args[0];
      expect(callArg.phone).toBeUndefined();
    });

    it('should set success=true on successful registration', () => {
      authSpy.register.and.returnValue(of(makeUser('DRIVER')));
      fillForm(component);
      component.register();
      expect(component.success()).toBeTrue();
    });

    it('should navigate to /driver after 1200ms for DRIVER role', fakeAsync(() => {
      authSpy.register.and.returnValue(of(makeUser('DRIVER')));
      const navSpy = spyOn(router, 'navigate');
      fillForm(component);
      component.register();
      tick(1200);
      expect(navSpy).toHaveBeenCalledWith(['/driver']);
    }));

    it('should navigate to /manager after 1200ms for MANAGER role', fakeAsync(() => {
      authSpy.register.and.returnValue(of(makeUser('MANAGER')));
      const navSpy = spyOn(router, 'navigate');
      fillForm(component);
      component.role = 'MANAGER';
      component.register();
      tick(1200);
      expect(navSpy).toHaveBeenCalledWith(['/manager']);
    }));

    it('should navigate to /admin after 1200ms for ADMIN role', fakeAsync(() => {
      authSpy.register.and.returnValue(of(makeUser('ADMIN')));
      const navSpy = spyOn(router, 'navigate');
      fillForm(component);
      component.role = 'ADMIN';
      component.register();
      tick(1200);
      expect(navSpy).toHaveBeenCalledWith(['/admin']);
    }));

    it('should NOT navigate before 1200ms', fakeAsync(() => {
      authSpy.register.and.returnValue(of(makeUser('DRIVER')));
      const navSpy = spyOn(router, 'navigate');
      fillForm(component);
      component.register();
      tick(500);
      expect(navSpy).not.toHaveBeenCalled();
      tick(700);
    }));
  });

  describe('register() — errors', () => {
    const fillForm = (comp: RegisterComponent) => {
      comp.fullName = 'New User';
      comp.email = 'new@test.com';
      comp.password = 'password123';
    };

    it('should show 409 conflict error when email is already registered', fakeAsync(() => {
      authSpy.register.and.returnValue(throwError(() => ({ status: 409 })));
      fillForm(component);
      component.register();
      tick();
      expect(component.error()).toBe('Email already registered. Please login instead.');
      expect(component.loading()).toBeFalse();
    }));

    it('should show 400 validation hint when server returns bad request', fakeAsync(() => {
      authSpy.register.and.returnValue(throwError(() => ({ status: 400 })));
      fillForm(component);
      component.register();
      tick();
      expect(component.error()).toContain('password must be 8+ chars');
    }));

    it('should prefer server error.message over status-based message', fakeAsync(() => {
      authSpy.register.and.returnValue(throwError(() => ({ status: 400, error: { message: 'Phone already in use' } })));
      fillForm(component);
      component.register();
      tick();
      expect(component.error()).toBe('Phone already in use');
    }));

    it('should show errors array from server if present', fakeAsync(() => {
      authSpy.register.and.returnValue(throwError(() => ({ status: 400, error: { errors: ['Email invalid', 'Password too short'] } })));
      fillForm(component);
      component.register();
      tick();
      expect(component.error()).toBe('Email invalid, Password too short');
    }));

    it('should show generic error on unexpected 500 failure', fakeAsync(() => {
      authSpy.register.and.returnValue(throwError(() => ({ status: 500 })));
      fillForm(component);
      component.register();
      tick();
      expect(component.error()).toBe('Registration failed. Please try again.');
    }));
  });
});
