import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../api.config';
import { DataRow } from '../models/dialog-config.model';

@Injectable({
  providedIn: 'root',
})
export class EntityDataService {
  private http = inject(HttpClient);

  // `entity` comes from the dialog configuration, e.g. 'users' or 'dummy/articles'.
  getRows(entity: string): Observable<DataRow[]> {
    return this.http.get<DataRow[]>(`${API_BASE_URL}/${entity}`);
  }
}
