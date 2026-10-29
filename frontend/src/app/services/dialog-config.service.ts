import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../api.config';
import { DialogConfig, DialogSummary } from '../models/dialog-config.model';

@Injectable({
  providedIn: 'root',
})
export class DialogConfigService {
  private http = inject(HttpClient);

  getDialogs(): Observable<DialogSummary[]> {
    return this.http.get<DialogSummary[]>(`${API_BASE_URL}/dialogs`);
  }

  getDialog(id: string): Observable<DialogConfig> {
    return this.http.get<DialogConfig>(`${API_BASE_URL}/dialogs/${encodeURIComponent(id)}`);
  }
}
