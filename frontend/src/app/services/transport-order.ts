import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../api.config';

export interface TransportOrderResponse {
  id: number;
  status: 'NEW' | 'ACTIVE' | 'FINISHED' | 'ERROR';
  palletBarcode: string;
  sourceLocation: string;
  destinationLocation: string;
  loadUnitLocation: string | null;
}

export type TransportOrderErrorReason = 'LOCATION_EMPTY' | 'WRONG_PALLET' | 'TARGET_LOCATION_OCCUPIED';

@Injectable({ providedIn: 'root' })
export class TransportOrderApi {
  private readonly http = inject(HttpClient);
  private readonly url = `${API_BASE_URL}/transport-orders`;

  findNextOrder(): Observable<TransportOrderResponse | null> {
    return this.http.get<TransportOrderResponse | null>(`${this.url}/next`);
  }

  findOpenOrders(): Observable<TransportOrderResponse[]> {
  return this.http.get<TransportOrderResponse[]>(`${this.url}/open`);
  }

  scan(id: number, barcode: string): Observable<TransportOrderResponse> {
    return this.http.post<TransportOrderResponse>(`${this.url}/${id}/scan`, { barcode });
  }

  reportError(id: number, reason: TransportOrderErrorReason): Observable<TransportOrderResponse> {
    return this.http.post<TransportOrderResponse>(`${this.url}/${id}/error`, { reason });
  }

  findNextToteTransport(){
    return this.http.get<TransportOrderResponse>(`${this.url}/nextTote`);
  }
}
