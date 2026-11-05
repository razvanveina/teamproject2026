import { HttpClient } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { Observable } from 'rxjs/internal/Observable';
import { ReceivingOrder, PackageInfo } from '../models/receiving-order.model';

@Service()
export class ReceivingOrderService {
    private httpClient = inject(HttpClient)
    private url = 'http://localhost:8080/api/receiving';

    getReceivingOrder(orderId: string): Observable<ReceivingOrder> {
        return this.httpClient.get<ReceivingOrder>( `${this.url}/${orderId}`)
    }

    finishOrder(order: ReceivingOrder){
        return this.httpClient.put<ReceivingOrder>( `${this.url}/finish`, order)
    }

    getPackage(barcode: string): Observable<PackageInfo> {
        return this.httpClient.get<PackageInfo>( `${this.url}/package/${barcode}`)
    }
}
