import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../api.config';

@Injectable({ providedIn: 'root' })
export class SimPickingApi {
    private http = inject(HttpClient);

    getStation(): Observable<any> {
        return this.http.get<any>(API_BASE_URL + '/sim-picking/station');
    }

    confirm(sourceTote: string, targetTote: string): Observable<any> {
        return this.http.post<any>(API_BASE_URL + '/sim-picking/confirm', { sourceTote: sourceTote, targetTote: targetTote });
    }
}