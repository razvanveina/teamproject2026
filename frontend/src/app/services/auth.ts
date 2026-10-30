import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { tap } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private url = 'http://localhost:8080/auth';

  private tokenSignal = signal<string | null>(sessionStorage.getItem('token'));

  readonly isLoggedIn = computed(() => this.tokenSignal() !== null);

  get token(): string | null {
    return this.tokenSignal();
  }

  login(username: string, password: string) {
    return this.http.post<{ token: string }>(`${this.url}/login`, { username, password }).pipe(
      tap(res => {
        sessionStorage.setItem('token', res.token);
        this.tokenSignal.set(res.token);
      })
    );
  }

  register(username: string, password: string) {
    return this.http.post<void>(`${this.url}/register`, { username, password });
  }

  logout(): void {
    sessionStorage.removeItem('token');
    this.tokenSignal.set(null);
  }
}