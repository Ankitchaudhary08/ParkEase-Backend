import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Notification {
  notificationId: number;
  recipientId: number;
  type: string;
  title: string;
  message: string;
  isRead: boolean;
  sentAt: string;
  relatedId?: number;
  relatedType?: string;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly API = '/api/v1/notifications';
  private readonly ANALYTICS = '/api/v1/analytics';

  constructor(private http: HttpClient) {}

  getAll(): Observable<Notification[]> {
    return this.http.get<Notification[]>(this.API);
  }

  getUnreadCount(): Observable<number> {
    return this.http.get<number>(`${this.API}/unread/count`);
  }

  markAsRead(id: number): Observable<void> {
    return this.http.put<void>(`${this.API}/${id}/read`, {});
  }

  markAllAsRead(): Observable<void> {
    return this.http.put<void>(`${this.API}/read-all`, {});
  }

  getOccupancyRate(lotId: number): Observable<number> {
    return this.http.get<number>(`${this.ANALYTICS}/lots/${lotId}/occupancy-rate`);
  }

  getOccupancyByHour(lotId: number): Observable<Record<string, number>> {
    return this.http.get<Record<string, number>>(`${this.ANALYTICS}/lots/${lotId}/occupancy-by-hour`);
  }

  getPeakHours(lotId: number): Observable<number[]> {
    return this.http.get<number[]>(`${this.ANALYTICS}/lots/${lotId}/peak-hours`);
  }

  getSpotUtilisation(lotId: number): Observable<Record<string, number>> {
    return this.http.get<Record<string, number>>(`${this.ANALYTICS}/lots/${lotId}/spot-utilisation`);
  }
}
