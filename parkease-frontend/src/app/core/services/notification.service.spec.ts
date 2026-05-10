import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { NotificationService, Notification } from './notification.service';

const MOCK_NOTIFICATION: Notification = {
  notificationId: 1,
  recipientId: 2,
  type: 'BOOKING_CONFIRMED',
  title: 'Booking Confirmed',
  message: 'Your spot A1 is reserved.',
  isRead: false,
  sentAt: '2024-06-01T10:00:00',
  relatedId: 100,
  relatedType: 'BOOKING'
};

describe('NotificationService', () => {
  let service: NotificationService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(NotificationService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('getAll()', () => {
    it('should GET /api/v1/notifications and return notification array', () => {
      service.getAll().subscribe(n => {
        expect(n.length).toBe(1);
        expect(n[0]).toEqual(MOCK_NOTIFICATION);
      });

      const req = http.expectOne('/api/v1/notifications');
      expect(req.request.method).toBe('GET');
      req.flush([MOCK_NOTIFICATION]);
    });
  });

  describe('getUnreadCount()', () => {
    it('should GET /api/v1/notifications/unread/count and return a number', () => {
      service.getUnreadCount().subscribe(c => expect(c).toBe(3));

      const req = http.expectOne('/api/v1/notifications/unread/count');
      expect(req.request.method).toBe('GET');
      req.flush(3);
    });

    it('should return 0 when no unread notifications', () => {
      service.getUnreadCount().subscribe(c => expect(c).toBe(0));
      http.expectOne('/api/v1/notifications/unread/count').flush(0);
    });
  });

  describe('markAsRead()', () => {
    it('should PUT to /api/v1/notifications/:id/read', () => {
      service.markAsRead(5).subscribe();

      const req = http.expectOne('/api/v1/notifications/5/read');
      expect(req.request.method).toBe('PUT');
      req.flush(null);
    });
  });

  describe('markAllAsRead()', () => {
    it('should PUT to /api/v1/notifications/read-all', () => {
      service.markAllAsRead().subscribe();

      const req = http.expectOne('/api/v1/notifications/read-all');
      expect(req.request.method).toBe('PUT');
      req.flush(null);
    });
  });

  describe('Analytics — getOccupancyRate()', () => {
    it('should GET /api/v1/analytics/lots/:id/occupancy-rate', () => {
      service.getOccupancyRate(1).subscribe(rate => expect(rate).toBe(0.75));

      const req = http.expectOne('/api/v1/analytics/lots/1/occupancy-rate');
      expect(req.request.method).toBe('GET');
      req.flush(0.75);
    });
  });

  describe('Analytics — getOccupancyByHour()', () => {
    it('should GET /api/v1/analytics/lots/:id/occupancy-by-hour', () => {
      const data = { '9': 5, '10': 8, '14': 3 };

      service.getOccupancyByHour(1).subscribe(d => expect(d).toEqual(data));

      const req = http.expectOne('/api/v1/analytics/lots/1/occupancy-by-hour');
      expect(req.request.method).toBe('GET');
      req.flush(data);
    });
  });

  describe('Analytics — getPeakHours()', () => {
    it('should GET /api/v1/analytics/lots/:id/peak-hours and return number array', () => {
      service.getPeakHours(1).subscribe(h => expect(h).toEqual([9, 10, 17, 18]));

      const req = http.expectOne('/api/v1/analytics/lots/1/peak-hours');
      expect(req.request.method).toBe('GET');
      req.flush([9, 10, 17, 18]);
    });
  });

  describe('Analytics — getSpotUtilisation()', () => {
    it('should GET /api/v1/analytics/lots/:id/spot-utilisation', () => {
      const data = { 'A1': 0.9, 'A2': 0.5, 'B1': 0.3 };

      service.getSpotUtilisation(1).subscribe(d => expect(d).toEqual(data));

      const req = http.expectOne('/api/v1/analytics/lots/1/spot-utilisation');
      expect(req.request.method).toBe('GET');
      req.flush(data);
    });
  });
});
