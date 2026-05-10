import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { ParkingService } from './parking.service';

describe('ParkingService', () => {
  let service: ParkingService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(ParkingService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('searchByCity()', () => {
    it('should GET /api/v1/lots/city/:city', () => {
      const lots = [{ lotId: 1, city: 'Mumbai', name: 'Central' }];

      service.searchByCity('Mumbai').subscribe(data => expect(data).toEqual(lots));

      const req = http.expectOne('/api/v1/lots/city/Mumbai');
      expect(req.request.method).toBe('GET');
      req.flush(lots);
    });

    it('should return empty array when no lots found in city', () => {
      service.searchByCity('UnknownCity').subscribe(data => expect(data).toEqual([]));

      http.expectOne('/api/v1/lots/city/UnknownCity').flush([]);
    });
  });

  describe('getNearby()', () => {
    it('should GET /api/v1/lots/nearby with lat, lon and radius params', () => {
      service.getNearby(19.07, 72.87, 10).subscribe();

      const req = http.expectOne(r => r.url === '/api/v1/lots/nearby');
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('lat')).toBe('19.07');
      expect(req.request.params.get('lon')).toBe('72.87');
      expect(req.request.params.get('radius')).toBe('10');
      req.flush([]);
    });

    it('should use default radius of 5 when not specified', () => {
      service.getNearby(18.52, 73.85).subscribe();

      const req = http.expectOne(r => r.url === '/api/v1/lots/nearby');
      expect(req.request.params.get('radius')).toBe('5');
      req.flush([]);
    });
  });

  describe('getLotById()', () => {
    it('should GET /api/v1/lots/:id', () => {
      const lot = { lotId: 5, name: 'Bay View', hourlyRate: 50 };

      service.getLotById(5).subscribe(l => expect(l).toEqual(lot));

      const req = http.expectOne('/api/v1/lots/5');
      expect(req.request.method).toBe('GET');
      req.flush(lot);
    });
  });

  describe('getAvailableSpots()', () => {
    it('should GET /api/v1/spots/lot/:id/available', () => {
      const spots = [{ spotId: 1, type: 'COMPACT', spotNumber: 'A1' }];

      service.getAvailableSpots(2).subscribe(s => expect(s).toEqual(spots));

      const req = http.expectOne('/api/v1/spots/lot/2/available');
      expect(req.request.method).toBe('GET');
      req.flush(spots);
    });
  });

  describe('getSpotCount()', () => {
    it('should GET /api/v1/spots/lot/:id/available/count', () => {
      service.getSpotCount(3).subscribe(count => expect(count).toBe(8));

      const req = http.expectOne('/api/v1/spots/lot/3/available/count');
      expect(req.request.method).toBe('GET');
      req.flush(8);
    });
  });

  describe('createLot()', () => {
    it('should POST payload to /api/v1/lots', () => {
      const payload = { name: 'New Lot', city: 'Delhi', address: '1 Main Rd', hourlyRate: 40, totalSpots: 30 };

      service.createLot(payload).subscribe();

      const req = http.expectOne('/api/v1/lots');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(payload);
      req.flush({ lotId: 99, ...payload });
    });
  });

  describe('updateLot()', () => {
    it('should PUT to /api/v1/lots/:id', () => {
      service.updateLot(5, { hourlyRate: 60 }).subscribe();

      const req = http.expectOne('/api/v1/lots/5');
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual({ hourlyRate: 60 });
      req.flush({ lotId: 5, hourlyRate: 60 });
    });
  });

  describe('toggleOpen()', () => {
    it('should PUT to /api/v1/lots/:id/toggle-open with empty body', () => {
      service.toggleOpen(5).subscribe();

      const req = http.expectOne('/api/v1/lots/5/toggle-open');
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual({});
      req.flush({ lotId: 5, open: false });
    });
  });

  describe('addSpot()', () => {
    it('should POST to /api/v1/spots/lot/:id', () => {
      const spot = { type: 'LARGE', spotNumber: 'A1' };
      service.addSpot(5, spot).subscribe();

      const req = http.expectOne('/api/v1/spots/lot/5');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(spot);
      req.flush({ spotId: 10, ...spot });
    });
  });

  describe('addBulkSpots()', () => {
    it('should POST array to /api/v1/spots/lot/:id/bulk', () => {
      const spots = [{ type: 'COMPACT', spotNumber: 'B1' }, { type: 'COMPACT', spotNumber: 'B2' }];

      service.addBulkSpots(5, spots).subscribe(result => expect(result.length).toBe(2));

      const req = http.expectOne('/api/v1/spots/lot/5/bulk');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(spots);
      req.flush(spots.map((s, i) => ({ ...s, spotId: i + 1 })));
    });
  });

  describe('getManagerLots()', () => {
    it('should GET /api/v1/lots/manager', () => {
      service.getManagerLots().subscribe();

      const req = http.expectOne('/api/v1/lots/manager');
      expect(req.request.method).toBe('GET');
      req.flush([]);
    });
  });

  describe('getPendingLots()', () => {
    it('should GET /api/v1/lots/pending', () => {
      service.getPendingLots().subscribe();

      const req = http.expectOne('/api/v1/lots/pending');
      expect(req.request.method).toBe('GET');
      req.flush([]);
    });
  });

  describe('getAllLots()', () => {
    it('should GET /api/v1/lots/all', () => {
      service.getAllLots().subscribe();

      const req = http.expectOne('/api/v1/lots/all');
      expect(req.request.method).toBe('GET');
      req.flush([]);
    });
  });

  describe('approveLot()', () => {
    it('should PUT with approved=true and reason to /api/v1/lots/:id/approve', () => {
      service.approveLot(10, true, 'Documents verified').subscribe();

      const req = http.expectOne('/api/v1/lots/10/approve');
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual({ approved: true, reason: 'Documents verified' });
      req.flush({ lotId: 10, status: 'APPROVED' });
    });

    it('should PUT with approved=false when rejecting', () => {
      service.approveLot(11, false, 'Incomplete docs').subscribe();

      const req = http.expectOne('/api/v1/lots/11/approve');
      expect(req.request.body).toEqual({ approved: false, reason: 'Incomplete docs' });
      req.flush({ lotId: 11, status: 'REJECTED' });
    });
  });
});
