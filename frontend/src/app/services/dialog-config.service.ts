import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, forkJoin, map, of, switchMap } from 'rxjs';
import { DIALOGS_BASE_URL } from '../api.config';
import { DialogConfig, DialogSummary } from '../models/dialog-config.model';


@Injectable({
  providedIn: 'root',
})
export class DialogConfigService {
  private http = inject(HttpClient);

  getDialogs(): Observable<DialogSummary[]> {
    return this.http.get<DialogsFile>(`${DIALOGS_BASE_URL}/dialogs.json`).pipe(
      // When dialogs.json arrives, request every file it points to, all at once.
      switchMap((dialogsFile) =>
        dialogsFile.dialogs.length === 0
          ? of([] as DialogConfig[])
          : forkJoin(
            dialogsFile.dialogs.map((entry: { file: any; }) =>
              this.http.get<DialogConfig>(`${DIALOGS_BASE_URL}/${entry.file}`),
            ),
          ),
      ),
      // The list page needs only the id and the title of each dialog.
      map((configs) => configs.map((config) => ({ id: config.id, title: config.title }))),
    );
  }

  getDialog(id: string): Observable<DialogConfig> {
    return this.http.get<DialogConfig>(`${DIALOGS_BASE_URL}/${encodeURIComponent(id)}.json`);
  }
}


// Shape of dialogs.json: the main list of dialogs, in display order.
export interface DialogsFile {
  dialogs: DialogEntry[];
}

// One entry of dialogs.json: the file that describes the dialog, e.g. 'users.json'.
export interface DialogEntry {
  file: string;
}