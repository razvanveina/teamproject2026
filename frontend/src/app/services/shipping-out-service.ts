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

    finishOrder(loadUnitBarcode: string) {
        return this.httpClient.put<any>(`${this.url}/${loadUnitBarcode}/finish`, {})
    }

    releaseLoadUnit(loadUnitBarcode: string) {
        return this.httpClient.put<void>(`${this.url}/${loadUnitBarcode}/release`, {})
    }
}
