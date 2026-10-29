import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { GenericTable } from '../generic-table/generic-table';
import { DataRow, DialogConfig } from '../models/dialog-config.model';
import { DialogConfigService } from '../services/dialog-config.service';
import { EntityDataService } from '../services/entity-data.service';
import { httpErrorMessage } from '../services/http-error-message';

// Page for /dialogs/:id: loads the dialog configuration, then its data, and shows them in the generic table.
@Component({
  selector: 'app-dialog-page',
  standalone: true,
  imports: [GenericTable, RouterLink],
  templateUrl: './dialog-page.html',
  styleUrl: './dialog-page.css',
})
export class DialogPage {
  private route = inject(ActivatedRoute);
  private dialogConfigService = inject(DialogConfigService);
  private entityDataService = inject(EntityDataService);

  config = signal<DialogConfig | null>(null);
  rows = signal<DataRow[]>([]);
  loading = signal(true);
  error = signal('');

  // Romanian: 1 înregistrare, 2 înregistrări, 20 de înregistrări.
  countLabel = computed(() => {
    const count = this.rows().length;
    if (count === 1) {
      return 'înregistrare';
    }
    const lastTwoDigits = count % 100;
    return count > 0 && (lastTwoDigits === 0 || lastTwoDigits >= 20) ? 'de înregistrări' : 'înregistrări';
  });

  private currentId = '';

  constructor() {
    // Angular reuses this page when only :id changes (e.g. browser Back), so we react to every id.
    this.route.paramMap
      .pipe(takeUntilDestroyed())
      .subscribe((params) => this.load(params.get('id') ?? ''));
  }

  private load(id: string): void {
    this.currentId = id;
    this.config.set(null);
    this.rows.set([]);
    this.error.set('');
    this.loading.set(true);

    this.dialogConfigService.getDialog(id).subscribe({
      next: (config) => {
        if (id !== this.currentId) {
          return; // the user already went to another dialog
        }
        this.config.set(config);
        this.loadRows(id, config.entity);
      },
      error: (err) => this.showError(id, err, 'Dialogul nu există.'),
    });
  }

  private loadRows(id: string, entity: string): void {
    this.entityDataService.getRows(entity).subscribe({
      next: (rows) => {
        if (id !== this.currentId) {
          return;
        }
        this.rows.set(rows);
        this.loading.set(false);
      },
      error: (err) => this.showError(id, err, 'Datele dialogului nu au fost găsite.'),
    });
  }

  private showError(id: string, err: HttpErrorResponse, notFoundMessage: string): void {
    if (id !== this.currentId) {
      return;
    }
    this.error.set(httpErrorMessage(err, notFoundMessage));
    this.loading.set(false);
  }
}
