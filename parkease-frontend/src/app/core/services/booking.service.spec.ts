import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { BookingService } from './booking.service';

describe('BookingService', () => {
  let service: BookingService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(BookingService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('createBooking()', () => {
    it('should POST payload to /api/v1/bookings', () => {
      const payload = { spotId: 1, startTime: '2024-06-01T10:00', endTime: '2024-06-01T12:00', vehicleNumber: 'MH01AB1234' };
      const response = { bookingId: 100, ...payload, status: 'PENDING' };

      service.createBooking(payload).subscribe(b => expect(b).toEqual(response));

      const req = http.expectOne('/api/v1/bookings');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(payload);
      req.flush(response);
    });
  });

  describe('getMyBookings()', () => {
    it('should GET /api/v1/bookings/user', () => {
      const bookings = [{ bookingId: 1, status: 'ACTIVE' }, { bookingId: 2, status: 'COMPLETED' }];

      service.getMyBookings().subscribe(b => expect(b).toEqual(bookings));

      const req = http.expectOne('/api/v1/bookings/user');
      expect(req.request.method).toBe('GET');
      req.flush(bookings);
    });
  });

  describe('getBookingById()', () => {
    it('should GET /api/v1/bookings/:id', () => {
      const booking = { bookingId: 5, status: 'ACTIVE', vehicleNumber: 'DL01XY5678' };

      service.getBookingById(5).subscribe(b => expect(b).toEqual(booking));

      const req = http.expectOne('/api/v1/bookings/5');
      expect(req.request.method).toBe('GET');
      req.flush(booking);
    });
  });

  describe('checkIn()', () => {
    it('should PUT to /api/v1/bookings/:id/checkin with empty body', () => {
      service.checkIn(10).subscribe();

      const req = http.expectOne('/api/v1/bookings/10/checkin');
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual({});
      req.flush({ bookingId: 10, status: 'CHECKED_IN' });
    });
  });

  describe('checkOut()', () => {
    it('should PUT to /api/v1/bookings/:id/checkout with empty body', () => {
      service.checkOut(10).subscribe();

      const req = http.expectOne('/api/v1/bookings/10/checkout');
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual({});
      req.flush({ bookingId: 10, status: 'COMPLETED' });
    });
  });

  describe('cancelBooking()', () => {
    it('should PUT to /api/v1/bookings/:id/cancel', () => {
      service.cancelBooking(7).subscribe();

      const req = http.expectOne('/api/v1/bookings/7/cancel');
      expect(req.request.method).toBe('PUT');
      req.flush({ bookingId: 7, status: 'CANCELLED' });
    });
  });

  describe('extendBooking()', () => {
    it('should PUT with newEndTime to /api/v1/bookings/:id/extend', () => {
      service.extendBooking(3, '2024-06-01T14:00').subscribe();

      const req = http.expectOne('/api/v1/bookings/3/extend');
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual({ newEndTime: '2024-06-01T14:00' });
      req.flush({ bookingId: 3, endTime: '2024-06-01T14:00' });
    });
  });

  describe('calculateAmount()', () => {
    it('should GET /api/v1/bookings/:id/amount and return a number', () => {
      service.calculateAmount(4).subscribe(amount => expect(amount).toBe(150));

      const req = http.expectOne('/api/v1/bookings/4/amount');
      expect(req.request.method).toBe('GET');
      req.flush(150);
    });
  });
});
