import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../api.config';

export interface TransportOrderResponse {
  id: number;
  status: 'NEW' | 'ACTIVE' | 'FINISHED';
  palletBarcode: string;
  sourceLocation: string;
  destinationLocation: string;
  loadUnitLocation: string | null;
}

@Injectable({ providedIn: 'root' })
export class TransportOrderApi {
  private readonly http = inject(HttpClient);
  private readonly url = `${API_BASE_URL}/transport-orders`;

  findNextOrder(): Observable<TransportOrderResponse | null> {
    return this.http.get<TransportOrderResponse | null>(`${this.url}/next`);
  }

  scan(id: number, barcode: string): Observable<TransportOrderResponse> {
    return this.http.post<TransportOrderResponse>(`${this.url}/${id}/scan`, { barcode });
  }
}
