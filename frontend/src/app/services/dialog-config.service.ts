import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL, DIALOGS_BASE_URL } from '../api.config';
import { DialogConfig, DialogsFile } from '../models/dialog-config.model';

//  DIALOGS_BASE_URL este "http://localhost:8080/api/dialogs"
@Injectable({ providedIn: 'root' })
export class DialogConfigService {
  private http = inject(HttpClient);

  // dialogs.json: the list of files
  getDialogsFile(): Observable<DialogsFile> {
    return this.http.get<DialogsFile>(DIALOGS_BASE_URL + '/dialogs.json'); // lista principala
  }

  // One dialog file, e.g. "users.json"
  getDialog(file: string): Observable<DialogConfig> {
    return this.http.get<DialogConfig>(DIALOGS_BASE_URL + '/' + file); //dialogul propriu-zis, e.g. users.json
  }

  // The rows of an entity, e.g. "users" -> /api/users
  getRows(entity: string): Observable<any[]> {
    return this.http.get<any[]>(API_BASE_URL + '/' + entity); //coloanele si datele din tabel, e.g. /api/users
  }
}