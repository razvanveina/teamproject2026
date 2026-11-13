import { HttpClient } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { API_BASE_URL } from '../api.config';

@Service()
export class SimulatorService {
    private readonly http = inject(HttpClient);
    private readonly url = `${API_BASE_URL}/simulator`;

    triggerEvent(input: string) {
        return this.http.post(`${this.url}/sendEvent`, { name: input })
    }

    createLU(barcode: string, location: string){
        return this.http.post(`${this.url}/createLU`, {barcode: barcode, location: location})
    }

    getEntities() {
        return this.http.get<string[]>(`${this.url}/entities`)
    }
}
