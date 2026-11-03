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

  // The columns of an entity from the database, e.g. /api/data/Article/columns
  getColumns(entity: string): Observable<any[]> {
    return this.http.get<any[]>(API_BASE_URL + '/data/' + entity + '/columns');
  }

  // Adds one row, e.g. POST /api/data/Article with { BARCODE: "123", DESCRIPTION: "Milk" }
  addRow(entity: string, data: any): Observable<any> {
    return this.http.post<any>(API_BASE_URL + '/data/' + entity, data);
  }

  // The rows of an entity, e.g. "users" -> /api/data/users
  getRows(entity: string, filters: any = {}): Observable<any[]> {
    return this.http.get<any[]>(API_BASE_URL + '/data/' + entity, { params: filters }); //coloanele si datele din tabel, e.g. /api/users
  }

  getRow(entity: string, id: string): Observable<any> {
    return this.http.get<any>(API_BASE_URL + '/data/' + entity + '/' + id);
  }

  updateRow(entity: string, id: string, data: any): Observable<any> {
    return this.http.put<any>(API_BASE_URL + '/data/' + entity + '/' + id, data);
  }

  // Deletes one row, e.g. DELETE /api/data/Article/5
  deleteRow(entity: string, id: number): Observable<any> {
    return this.http.delete<any>(API_BASE_URL + '/data/' + entity + '/' + id);
  }
}