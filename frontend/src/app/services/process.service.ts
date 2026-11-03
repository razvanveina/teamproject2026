import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../api.config';

export interface ProcessInfo {
  name: string;
  status: 'RUNNING' | 'PAUSED' | 'STOPPED';
}

@Injectable({ providedIn: 'root' })
export class ProcessService {
  private http = inject(HttpClient);

  getAll(): Observable<ProcessInfo[]> {
    return this.http.get<ProcessInfo[]>(API_BASE_URL + '/processes');
  }

  act(name: string, action: 'start' | 'pause' | 'stop'): Observable<ProcessInfo> {
    return this.http.post<ProcessInfo>(API_BASE_URL + '/processes/' + name + '/' + action, {});
  }
}