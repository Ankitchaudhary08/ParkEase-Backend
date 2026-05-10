import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { PaymentService, RazorpayOrderRequest, RazorpayOrderResponse, RazorpayVerifyRequest, Payment } from './payment.service';

describe('PaymentService', () => {
  let service: PaymentService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(PaymentService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('createRazorpayOrder()', () => {
    it('should POST to /api/v1/payments/razorpay/create-order and return order response', () => {
      const reqBody: RazorpayOrderRequest = { bookingId: 1, userId: 2, amount: 300, currency: 'INR', description: 'Parking fee' };
      const mockResponse: RazorpayOrderResponse = {
        razorpayOrderId: 'order_ABC123',
        amountInPaise: 30000,
        currency: 'INR',
        keyId: 'rzp_test_key',
        bookingId: 1,
        userId: 2,
        description: 'Parking fee'
      };

      service.createRazorpayOrder(reqBody).subscribe(res => expect(res).toEqual(mockResponse));

      const req = http.expectOne('/api/v1/payments/razorpay/create-order');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(reqBody);
      req.flush(mockResponse);
    });
  });

  describe('verifyRazorpayPayment()', () => {
    it('should POST to /api/v1/payments/razorpay/verify and return Payment', () => {
      const verifyReq: RazorpayVerifyRequest = {
        bookingId: 1, userId: 2,
        razorpayOrderId: 'order_ABC123',
        razorpayPaymentId: 'pay_XYZ789',
        razorpaySignature: 'sig_hash',
        amount: 300
      };
      const mockPayment: Payment = {
        paymentId: 10, bookingId: 1, userId: 2, amount: 300,
        currency: 'INR', mode: 'UPI', status: 'PAID',
        razorpayOrderId: 'order_ABC123', razorpayPaymentId: 'pay_XYZ789'
      };

      service.verifyRazorpayPayment(verifyReq).subscribe(p => expect(p).toEqual(mockPayment));

      const req = http.expectOne('/api/v1/payments/razorpay/verify');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(verifyReq);
      req.flush(mockPayment);
    });
  });

  describe('getMyPayments()', () => {
    it('should GET /api/v1/payments/user with X-User-Id header', () => {
      service.getMyPayments(5).subscribe();

      const req = http.expectOne('/api/v1/payments/user');
      expect(req.request.method).toBe('GET');
      expect(req.request.headers.get('X-User-Id')).toBe('5');
      req.flush([]);
    });
  });

  describe('getPaymentByBooking()', () => {
    it('should GET /api/v1/payments/booking/:id', () => {
      const payment: Payment = { paymentId: 1, bookingId: 10, userId: 2, amount: 200, currency: 'INR', mode: 'CARD', status: 'PAID' };

      service.getPaymentByBooking(10).subscribe(p => expect(p).toEqual(payment));

      const req = http.expectOne('/api/v1/payments/booking/10');
      expect(req.request.method).toBe('GET');
      req.flush(payment);
    });
  });

  describe('getPaymentById()', () => {
    it('should GET /api/v1/payments/:id', () => {
      service.getPaymentById(3).subscribe();

      const req = http.expectOne('/api/v1/payments/3');
      expect(req.request.method).toBe('GET');
      req.flush({ paymentId: 3, status: 'PAID' });
    });
  });

  describe('refundPayment()', () => {
    it('should POST to /api/v1/payments/:id/refund with empty body', () => {
      service.refundPayment(2).subscribe();

      const req = http.expectOne('/api/v1/payments/2/refund');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({});
      req.flush({ paymentId: 2, status: 'REFUNDED' });
    });
  });
});
