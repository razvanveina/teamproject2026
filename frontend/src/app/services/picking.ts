import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../api.config';

@Injectable({ providedIn: 'root' })
export class PickingApi {
  private http = inject(HttpClient);

  findNextOrder(): Observable<any> {
    return this.http.get<any>(API_BASE_URL + '/picking/next-order');
  }

  createBox(orderId: string, box: string): Observable<any> {
    return this.http.post<any>(API_BASE_URL + '/picking/create-box', { orderId: orderId, box: box });
  }

  moveLineToBox(orderId: string, barcode: string, box: string): Observable<any> {
    return this.http.post<any>(API_BASE_URL + '/picking/move-to-box', { orderId: orderId, barcode: barcode, box: box });
  }

  logError(message: string): void {
    this.http.post(API_BASE_URL + '/picking/log-error', { message: message }).subscribe();
  }

  closeBox(orderId: string, box: string): Observable<any> {
    return this.http.post<any>(API_BASE_URL + '/picking/close-box', { orderId: orderId, box: box });
  }
}
