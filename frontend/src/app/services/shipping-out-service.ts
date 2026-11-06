import { Observable } from 'rxjs/internal/Observable';
import { ReceivingOrder } from '../models/receiving-order.model';
import { inject, Service } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { LoadUnit } from '../models/load-unit-model';

@Service()
export class ShippingService {
    private httpClient = inject(HttpClient)
    private url = 'http://localhost:8080/api/shipping';

    getLoadUnit(loadUnitBarcode: string) {
        return this.httpClient.get<LoadUnit>( `${this.url}/${loadUnitBarcode}`)
    }

    finishOrder(loadUnitBarcode: string){
        return this.httpClient.put<any>( `${this.url}/${loadUnitBarcode}/finish`, {})
    }
}
