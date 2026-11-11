import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { tap } from 'rxjs';
import { SERVER_URL } from '../api.config';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);
  private url = SERVER_URL + '/auth';

  private tokenSignal = signal<string | null>(localStorage.getItem('token'));
  private timer?: ReturnType<typeof setTimeout>;

  readonly isLoggedIn = computed(() => this.tokenSignal() !== null);
  private readPayload(token: string): any {
    return JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
  }
  readonly username = computed(() => {
    const token = this.tokenSignal();
    return token ? (this.readPayload(token).sub ?? '') : '';
  });
  readonly expired = signal(false);

  constructor() {
    this.scheduleExpiry();
  }

  get token(): string | null {
    return this.tokenSignal();
  }

  login(username: string, password: string) {
    return this.http.post<{ token: string }>(`${this.url}/login`, { username, password }).pipe(
      tap(res => {
        localStorage.setItem('token', res.token);
        this.tokenSignal.set(res.token);
        this.expired.set(false);
        this.scheduleExpiry();
      })
    );
  }

  logout(): void {
    clearTimeout(this.timer);
    localStorage.removeItem('token');
    this.tokenSignal.set(null);
    this.expired.set(false);
  }

  private scheduleExpiry(): void {
    clearTimeout(this.timer);
    const token = this.tokenSignal();
    if (!token) {
      return;
    }

    const payload = this.readPayload(token);
    const delay = payload.exp * 1000 - Date.now();

    this.timer = setTimeout(() => {
      this.logout();
      this.expired.set(true);
      this.router.navigate(['/login']);
    }, Math.max(delay, 0));
  }
}