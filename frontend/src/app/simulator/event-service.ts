import { HttpClient } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { API_BASE_URL } from '../api.config';

@Service()
export class EventService {
    private readonly http = inject(HttpClient);
    private readonly url = `${API_BASE_URL}/events`;

    // findOpenOrders(): Observable<TransportOrderResponse[]> {
    //     return this.http.post<TransportOrderResponse[]>(`${this.url}/open`);
    // }

    triggerEvent(input: string) {
        return this.http.post(`${this.url}/sendEvent`, { name: input })
    }
}
