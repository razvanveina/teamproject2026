import { HttpClient } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { SimEntity } from './models/sim-entity-model';
import { SIM_URL } from '../api.config';

@Service()
export class SimDataService {
    private readonly http = inject(HttpClient);
    private readonly url = `${SIM_URL}/sim`;

    getVizData() {
        return this.http.get<SimEntity[]>(`${this.url}/visualization`);
    }

    canPick() {
        return this.http.get(`${this.url}/picking`);
    }
}
