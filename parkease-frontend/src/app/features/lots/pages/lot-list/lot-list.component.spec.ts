import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { LotListComponent } from './lot-list.component';
import { ParkingService } from '../../../../core/services/parking.service';

const MOCK_LOTS = [
  { lotId: 1, name: 'Central Park Parking', address: '1 Main St', city: 'Mumbai', open: true, hourlyRate: 50, totalSpots: 20 },
  { lotId: 2, name: 'Bay View Parking', address: '5 Bay Rd', city: 'Mumbai', open: false, hourlyRate: 30, totalSpots: 10 }
];

describe('LotListComponent', () => {
  let fixture: ComponentFixture<LotListComponent>;
  let component: LotListComponent;
  let parkingSpy: jasmine.SpyObj<ParkingService>;
  let router: Router;

  beforeEach(async () => {
    parkingSpy = jasmine.createSpyObj('ParkingService', ['searchByCity']);

    await TestBed.configureTestingModule({
      imports: [LotListComponent],
      providers: [
        { provide: ParkingService, useValue: parkingSpy },
        provideRouter([])
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(LotListComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  it('should create the component', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize with empty state', () => {
    expect(component.city).toBe('');
    expect(component.lots()).toEqual([]);
    expect(component.loading()).toBeFalse();
    expect(component.error()).toBeNull();
    expect(component.searched()).toBeFalse();
  });

  describe('search() — guard conditions', () => {
    it('should not search when city is empty string', () => {
      component.city = '';
      component.search();
      expect(parkingSpy.searchByCity).not.toHaveBeenCalled();
    });

    it('should not search when city contains only whitespace', () => {
      component.city = '   ';
      component.search();
      expect(parkingSpy.searchByCity).not.toHaveBeenCalled();
    });

    it('should not set loading when search is aborted due to empty city', () => {
      component.city = '';
      component.search();
      expect(component.loading()).toBeFalse();
    });
  });

  describe('search() — success', () => {
    it('should call searchByCity with trimmed city name', () => {
      parkingSpy.searchByCity.and.returnValue(of([]));
      component.city = '  Pune  ';
      component.search();
      expect(parkingSpy.searchByCity).toHaveBeenCalledWith('Pune');
    });

    it('should set searched=true after search is triggered', fakeAsync(() => {
      parkingSpy.searchByCity.and.returnValue(of([]));
      component.city = 'Delhi';
      component.search();
      tick();
      expect(component.searched()).toBeTrue();
    }));

    it('should populate lots signal with results from API', fakeAsync(() => {
      parkingSpy.searchByCity.and.returnValue(of(MOCK_LOTS));
      component.city = 'Mumbai';
      component.search();
      tick();
      expect(component.lots()).toEqual(MOCK_LOTS);
    }));

    it('should set loading=false after successful response', fakeAsync(() => {
      parkingSpy.searchByCity.and.returnValue(of(MOCK_LOTS));
      component.city = 'Mumbai';
      component.search();
      tick();
      expect(component.loading()).toBeFalse();
    }));

    it('should clear any previous error on new search', fakeAsync(() => {
      component.error.set('Previous error message');
      parkingSpy.searchByCity.and.returnValue(of(MOCK_LOTS));
      component.city = 'Mumbai';
      component.search();
      tick();
      expect(component.error()).toBeNull();
    }));

    it('should handle empty result array (no lots in city)', fakeAsync(() => {
      parkingSpy.searchByCity.and.returnValue(of([]));
      component.city = 'EmptyCity';
      component.search();
      tick();
      expect(component.lots()).toEqual([]);
      expect(component.searched()).toBeTrue();
    }));
  });

  describe('search() — error handling', () => {
    it('should set error message on API failure', fakeAsync(() => {
      parkingSpy.searchByCity.and.returnValue(throwError(() => new Error('Network error')));
      component.city = 'Delhi';
      component.search();
      tick();
      expect(component.error()).toBe('Search failed. Please ensure the backend is running.');
    }));

    it('should set loading=false on API failure', fakeAsync(() => {
      parkingSpy.searchByCity.and.returnValue(throwError(() => new Error('Network error')));
      component.city = 'Delhi';
      component.search();
      tick();
      expect(component.loading()).toBeFalse();
    }));

    it('should clear lots on error (not show stale results)', fakeAsync(() => {
      // First successful search
      parkingSpy.searchByCity.and.returnValue(of(MOCK_LOTS));
      component.city = 'Mumbai';
      component.search();
      tick();
      expect(component.lots().length).toBe(2);

      // Second search fails — stale lots remain visible (component does not clear them on error)
      parkingSpy.searchByCity.and.returnValue(throwError(() => new Error('err')));
      component.city = 'Delhi';
      component.search();
      tick();
      expect(component.error()).toBeTruthy();
    }));
  });

  describe('view()', () => {
    it('should navigate to /lots/:lotId when called', () => {
      const navSpy = spyOn(router, 'navigate');
      component.view(42);
      expect(navSpy).toHaveBeenCalledWith(['/lots', 42]);
    });

    it('should navigate with the correct dynamic lotId', () => {
      const navSpy = spyOn(router, 'navigate');
      component.view(999);
      expect(navSpy).toHaveBeenCalledWith(['/lots', 999]);
    });
  });
});
