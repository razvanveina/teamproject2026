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

  findReadyOrders(): Observable<any[]> {
    return this.http.get<any[]>(API_BASE_URL + '/picking/orders');
  }

  takeOrder(orderId: string): Observable<any> {
    return this.http.post<any>(API_BASE_URL + '/picking/take', { orderId: orderId });
  }

  releaseOrders(): Observable<any> {
    return this.http.post<any>(API_BASE_URL + '/picking/release', {});
  }

  getOrder(orderId: string): Observable<any> {
    return this.http.get<any>(API_BASE_URL + '/picking/order/' + orderId);
  }

  createBox(orderId: string, box: string): Observable<any> {
    return this.http.post<any>(API_BASE_URL + '/picking/create-box', { orderId: orderId, box: box });
  }

  moveItemToBox(orderId: string, stockItemId: number, box: string): Observable<any> {
    return this.http.post<any>(API_BASE_URL + '/picking/move-to-box', { orderId: orderId, stockItemId: stockItemId, box: box });
  }

  reportError(orderId: string, stockItemId: number, box: string, reason: string, found: number): Observable<any> {
    return this.http.post<any>(API_BASE_URL + '/picking/error', { orderId: orderId, stockItemId: stockItemId, box: box, reason: reason, found: found });
  }

  logError(message: string): void {
    this.http.post(API_BASE_URL + '/picking/log-error', { message: message }).subscribe();
  }

  closeBox(orderId: string, box: string): Observable<any> {
    return this.http.post<any>(API_BASE_URL + '/picking/close-box', { orderId: orderId, box: box });
  }
}
