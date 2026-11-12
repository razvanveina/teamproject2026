import { Observable } from 'rxjs/internal/Observable';
import { ReceivingOrder } from '../models/receiving-order.model';
import { inject, Service } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { LoadUnit } from '../models/load-unit-model';
import { API_BASE_URL } from '../api.config';

@Service()
export class ShippingService {
    private httpClient = inject(HttpClient)
    private url = API_BASE_URL + '/shipping';

    getLoadUnit(loadUnitBarcode: string) {
        return this.httpClient.get<LoadUnit>(`${this.url}/${loadUnitBarcode}`)
    }

    finishOrder(loadUnitBarcode: string, cartonBarcode?: string) {
        const params = cartonBarcode ? `?carton=${encodeURIComponent(cartonBarcode)}` : ''
        return this.httpClient.put<any>(`${this.url}/${loadUnitBarcode}/finish${params}`, {})
    }

    releaseLoadUnit(loadUnitBarcode: string) {
        return this.httpClient.put<void>(`${this.url}/${loadUnitBarcode}/release`, {})
    }

    chooseCarton(toteBarcode: string, cartonBarcode: string) {
        return this.httpClient.post<LoadUnit>(`${this.url}/${toteBarcode}/carton/${encodeURIComponent(cartonBarcode)}`, {})
    }
}
