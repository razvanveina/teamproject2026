import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../api.config';

export interface DecantingItem {
    packagingBarcode: string;
    articleBarcode: string;
    description: string;
    packaging: string;
    quantity: number;
    reservedQuantity: number;
}

export interface DecantingResponse {
    loadUnitBarcode: string;
    toteBarcode: string | null;
    items: DecantingItem[];
    finished: boolean;
}

@Injectable({ providedIn: 'root' })
export class DecantingApi {
    private readonly http = inject(HttpClient);
    private readonly url = `${API_BASE_URL}/decanting`;

    start(loadUnitBarcode: string): Observable<DecantingResponse> {
        return this.http.post<DecantingResponse>(`${this.url}/start`, { loadUnitBarcode });
    }

    transfer(loadUnitBarcode: string, packagingBarcode: string, quantity: number): Observable<DecantingResponse> {
        return this.http.post<DecantingResponse>(`${this.url}/transfer`,
            { loadUnitBarcode, packagingBarcode, quantity });
    }
}