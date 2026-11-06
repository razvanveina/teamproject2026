
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { User } from '../models/user.model';
import { API_BASE_URL } from '../api.config';

@Injectable({
    providedIn: 'root'
})
export class UserTableService {
    private http = inject(HttpClient);

    private apiUrl = API_BASE_URL + '/users';

    getUsers(): Observable<User[]> {
        return this.http.get<User[]>(this.apiUrl);
    }

    getUserById(id: number): Observable<User> {
        return this.http.get<User>(`${this.apiUrl}/${id}`);
    }

    addUser(user: Omit<User, 'id'>): Observable<User> {
        return this.http.post<User>(this.apiUrl, user);
    }

    updateUser(id: number, user: Omit<User, 'id'>): Observable<User> {
        return this.http.put<User>(`${this.apiUrl}/${id}`, user);
    }

    deleteUser(id: number): Observable<void> {
        return this.http.delete<void>(`${this.apiUrl}/${id}`);
    }
}
