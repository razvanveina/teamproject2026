import { HttpClient } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { SimEntity } from './models/sim-entity-model';

@Service()
export class SimDataService {
    private readonly http = inject(HttpClient);
    private readonly url = 'http://localhost:8888/';

    getVizData() {
        return this.http.get<SimEntity[]>(`${this.url}/visualization`)
    }
}
