import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { API_BASE_URL } from '../api.config';

@Injectable({ providedIn: 'root' })
export class LogService {
  private readonly http = inject(HttpClient);
  private readonly url = `${API_BASE_URL}/log`;

  action(action: string): void {
    this.http.post<void>(this.url, { action }).subscribe();
  }
}
