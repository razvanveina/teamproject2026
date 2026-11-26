import { HttpClient } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { API_BASE_URL } from '../api.config';
import { OrderCheck } from './models/order-check-model';

@Service()
export class SimulatorService {
    private readonly http = inject(HttpClient);
    private readonly url = `${API_BASE_URL}/simulator`;
    private readonly hostTestingUrl = `${API_BASE_URL}/host-testing`;

    triggerEvent(input: string) {
        return this.http.post(`${this.url}/sendEvent`, { name: input })
    }

    createLU(barcode: string, location: string, type: string){
        return this.http.post(`${this.url}/createLU?barcode=${barcode}&location=${location}&type=${type}`, {})
    }

    getEntities() {
        return this.http.get<string[]>(`${this.url}/entities`)
    }

    createReceivingOrderFile() {
        return this.http.post<{ fileName: string }>(`${this.hostTestingUrl}/receiving-order`, {})
    }

    createRetrievalOrderFile() {
        return this.http.post<{ fileName: string }>(`${this.hostTestingUrl}/retrieval-order`, {})
    }

    checkRetrievalOrders() {
        return this.http.get<OrderCheck[]>(`${this.hostTestingUrl}/order-check`)
    }
}
