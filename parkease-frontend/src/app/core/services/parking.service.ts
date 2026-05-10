import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class ParkingService {
  private readonly LOTS_API = '/api/v1/lots';
  private readonly SPOTS_API = '/api/v1/spots';

  constructor(private http: HttpClient) {}

  searchByCity(city: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.LOTS_API}/city/${city}`);
  }

  getNearby(lat: number, lon: number, radius = 5): Observable<any[]> {
    const params = new HttpParams().set('lat', lat).set('lon', lon).set('radius', radius);
    return this.http.get<any[]>(`${this.LOTS_API}/nearby`, { params });
  }

  getLotById(lotId: number): Observable<any> {
    return this.http.get(`${this.LOTS_API}/${lotId}`);
  }

  getAvailableSpots(lotId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.SPOTS_API}/lot/${lotId}/available`);
  }

  getSpotCount(lotId: number): Observable<number> {
    return this.http.get<number>(`${this.SPOTS_API}/lot/${lotId}/available/count`);
  }

  createLot(payload: any): Observable<any> {
    return this.http.post(this.LOTS_API, payload);
  }

  updateLot(lotId: number, payload: any): Observable<any> {
    return this.http.put(`${this.LOTS_API}/${lotId}`, payload);
  }

  toggleOpen(lotId: number): Observable<any> {
    return this.http.put(`${this.LOTS_API}/${lotId}/toggle-open`, {});
  }

  addSpot(lotId: number, payload: any): Observable<any> {
    return this.http.post(`${this.SPOTS_API}/lot/${lotId}`, payload);
  }

  addBulkSpots(lotId: number, spots: any[]): Observable<any[]> {
    return this.http.post<any[]>(`${this.SPOTS_API}/lot/${lotId}/bulk`, spots);
  }

  getManagerLots(): Observable<any[]> {
    return this.http.get<any[]>(`${this.LOTS_API}/manager`);
  }

  getPendingLots(): Observable<any[]> {
    return this.http.get<any[]>(`${this.LOTS_API}/pending`);
  }

  getAllLots(): Observable<any[]> {
    return this.http.get<any[]>(`${this.LOTS_API}/all`);
  }

  approveLot(lotId: number, approved: boolean, reason?: string): Observable<any> {
    return this.http.put(`${this.LOTS_API}/${lotId}/approve`, { approved, reason });
  }
}
