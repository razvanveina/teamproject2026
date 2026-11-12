import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../api.config';

export interface Account {
    id: number;
    username: string;
    online: boolean;
}

@Injectable({ providedIn: 'root' })
export class AccountApi {
    private readonly http = inject(HttpClient);
    private readonly url = `${API_BASE_URL}/accounts`;

    list(): Observable<Account[]> {
        return this.http.get<Account[]>(this.url);
    }

    create(username: string, password: string): Observable<Account> {
        return this.http.post<Account>(this.url, { username, password });
    }

    delete(id: number): Observable<void> {
        return this.http.delete<void>(`${this.url}/${id}`);
    }
}