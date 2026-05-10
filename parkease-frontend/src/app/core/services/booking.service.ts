import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class BookingService {
  private readonly API = '/api/v1/bookings';

  constructor(private http: HttpClient) {}

  createBooking(payload: any): Observable<any> {
    return this.http.post(this.API, payload);
  }

  getMyBookings(): Observable<any[]> {
    return this.http.get<any[]>(`${this.API}/user`);
  }

  getBookingById(id: number): Observable<any> {
    return this.http.get(`${this.API}/${id}`);
  }

  checkIn(bookingId: number): Observable<any> {
    return this.http.put(`${this.API}/${bookingId}/checkin`, {});
  }

  checkOut(bookingId: number): Observable<any> {
    return this.http.put(`${this.API}/${bookingId}/checkout`, {});
  }

  cancelBooking(bookingId: number): Observable<any> {
    return this.http.put(`${this.API}/${bookingId}/cancel`, {});
  }

  extendBooking(bookingId: number, newEndTime: string): Observable<any> {
    return this.http.put(`${this.API}/${bookingId}/extend`, { newEndTime });
  }

  calculateAmount(bookingId: number): Observable<number> {
    return this.http.get<number>(`${this.API}/${bookingId}/amount`);
  }
}
