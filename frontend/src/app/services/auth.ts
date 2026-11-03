import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { tap } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);
  private url = 'http://localhost:8080/auth';

  private tokenSignal = signal<string | null>(sessionStorage.getItem('token'));
  private timer?: ReturnType<typeof setTimeout>;

  readonly isLoggedIn = computed(() => this.tokenSignal() !== null);
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
        sessionStorage.setItem('token', res.token);
        this.tokenSignal.set(res.token);
        this.expired.set(false);
        this.scheduleExpiry();
      })
    );
  }

  logout(): void {
    clearTimeout(this.timer);
    sessionStorage.removeItem('token');
    this.tokenSignal.set(null);
    this.expired.set(false);
  }

  private scheduleExpiry(): void {
    clearTimeout(this.timer);
    const token = this.tokenSignal();
    if (!token) {
      return;
    }

    const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
    const delay = payload.exp * 1000 - Date.now();

    this.timer = setTimeout(() => {
      this.logout();
      this.expired.set(true);
      this.router.navigate(['/login']);
    }, Math.max(delay, 0));
  }
}