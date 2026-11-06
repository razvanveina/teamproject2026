import { HttpClient } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { Observable } from 'rxjs/internal/Observable';
import { ReceivingOrder, PackageInfo, ScannedPackage, PackageOption } from '../models/receiving-order.model';
import { API_BASE_URL } from '../api.config';

@Service()
export class ReceivingOrderService {
    private httpClient = inject(HttpClient)
    private url = API_BASE_URL + '/receiving';

    getReceivingOrder(orderId: string): Observable<ReceivingOrder> {
        return this.httpClient.get<ReceivingOrder>(`${this.url}/${orderId}`)
    }

    finishOrder(orderId: string, packages: ScannedPackage[]) {
        return this.httpClient.put<ReceivingOrder>(`${this.url}/finish`, { orderId, packages })
    }

    getPackage(barcode: string): Observable<PackageInfo> {
        return this.httpClient.get<PackageInfo>(`${this.url}/package/${barcode}`)
    }

    getPackageOptions(orderId: string): Observable<PackageOption[]> {
        return this.httpClient.get<PackageOption[]>(`${this.url}/${orderId}/packages`)
    }
}