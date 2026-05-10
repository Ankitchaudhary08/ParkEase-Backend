import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface RazorpayOrderRequest {
  bookingId: number;
  userId: number;
  amount: number;
  currency?: string;
  description?: string;
}

export interface RazorpayOrderResponse {
  razorpayOrderId: string;
  amountInPaise: number;
  currency: string;
  keyId: string;
  bookingId: number;
  userId: number;
  description?: string;
}

export interface RazorpayVerifyRequest {
  bookingId: number;
  userId: number;
  razorpayOrderId: string;
  razorpayPaymentId: string;
  razorpaySignature: string;
  amount: number;
}

export interface Payment {
  paymentId: number;
  bookingId: number;
  userId: number;
  amount: number;
  currency: string;
  mode: 'CARD' | 'UPI' | 'WALLET' | 'CASH';
  status: 'PENDING' | 'PAID' | 'REFUNDED' | 'FAILED';
  transactionId?: string;
  razorpayOrderId?: string;
  razorpayPaymentId?: string;
  paidAt?: string;
}

@Injectable({ providedIn: 'root' })
export class PaymentService {
  private readonly API = '/api/v1/payments';

  constructor(private http: HttpClient) {}

  createRazorpayOrder(req: RazorpayOrderRequest): Observable<RazorpayOrderResponse> {
    return this.http.post<RazorpayOrderResponse>(`${this.API}/razorpay/create-order`, req);
  }

  verifyRazorpayPayment(req: RazorpayVerifyRequest): Observable<Payment> {
    return this.http.post<Payment>(`${this.API}/razorpay/verify`, req);
  }

  getMyPayments(userId: number): Observable<Payment[]> {
    return this.http.get<Payment[]>(`${this.API}/user`, {
      headers: { 'X-User-Id': userId.toString() }
    });
  }

  getPaymentByBooking(bookingId: number): Observable<Payment> {
    return this.http.get<Payment>(`${this.API}/booking/${bookingId}`);
  }

  getPaymentById(paymentId: number): Observable<Payment> {
    return this.http.get<Payment>(`${this.API}/${paymentId}`);
  }

  refundPayment(paymentId: number): Observable<Payment> {
    return this.http.post<Payment>(`${this.API}/${paymentId}/refund`, {});
  }
}
