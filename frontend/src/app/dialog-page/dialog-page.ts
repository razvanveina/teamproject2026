import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { DialogColumn, DialogConfig, DialogsFile } from '../models/dialog-config.model';
import { DialogConfigService } from '../services/dialog-config.service';
import { Title } from '@angular/platform-browser';

@Component({
  selector: 'app-dialog-page',
  imports: [RouterLink],
  templateUrl: './dialog-page.html',
  styleUrl: './dialog-page.css',
})
export class DialogPage implements OnInit {
  private route = inject(ActivatedRoute);
  private service = inject(DialogConfigService);
  private titleService = inject(Title);

  // The dialog description (title, entity, columns). Empty until it arrives.
  dialog = signal<DialogConfig | null>(null);

  // The rows of the table. Empty until they arrive.
  rows = signal<any[]>([]);
  tableColumns = signal<DialogColumn[]>([]);
  filters: any = {};

  page = signal(0); //pagina current page number, starting from 0
  pages = signal(1); //total number of pages, starting from 1

  sortColumn = '';
  sortDirection: 'asc' | 'desc' = 'asc';

  // The error message shown above the table. Empty when everything is fine.
  error = signal('');
  // Runs once, when the page opens
  // Runs every time the address changes: /dialogs/users -> /dialogs/articles
  ngOnInit(): void {
    this.route.paramMap.subscribe((params) => {
      // 1. Read the entity (class name) from the address, e.g. /dialogs/ReceivingOrder
      const entity = params.get('entity');
      this.rows.set([]);
      this.error.set('');
      this.tableColumns.set([]);
      this.filters = {};
      this.page.set(0);
      // 2. Ask for the dialog file, e.g. ReceivingOrder.json
      this.service.getDialog(entity + '.json').subscribe((dialog: DialogConfig) => {
        this.dialog.set(dialog);
        this.titleService.setTitle(dialog.title);

        this.service.getDialogsFile().subscribe((dialogsFile: DialogsFile) => {
          this.tableColumns.set(this.makeColumns(dialog, dialogsFile));
        });

        // 3. Ask for the rows, e.g. /api/data/article/+ filter=page:0 
        this.loadRows();
      });
    });
  }

  // Runs when the user clicks Delete on a row
  deleteRow(row: any): void {
    const config = this.dialog();
    if (config === null) {
      return;
    }

    // Ask first, so a wrong click does not delete anything
    if (!confirm('Delete ' + config.entity + ' ' + row['ID'] + '?')) {
      return;
    }

    this.error.set('');

    this.service.deleteRow(config.entity, row['ID']).subscribe({
      // Deleted: ask for the rows again
      next: () => {
        this.loadRows();
      },
      // The server said no: show its message and write everything in the browser console (F12)
      error: (err) => {
        console.error('Error deleting ' + config.entity + ' ' + row['ID'], err);
        if (err.error && err.error.message) {
          this.error.set(err.error.message);
        } else {
          this.error.set('Delete failed (status ' + err.status + ')');
        }
      },
    });
  }

  setFilter(field: string, event: any): void {
    this.filters[field] = event.target.value;
    this.page.set(0);
    this.loadRows();
  }

  previousPage(): void {
    if (this.page() > 0) {
      this.page.set(this.page() - 1);
      this.loadRows();
    }
  }

  nextPage(): void {
    if (this.page() < this.pages() - 1) {
      this.page.set(this.page() + 1);
      this.loadRows();
    }
  }

  refresh(): void {
    this.error.set('');
    this.loadRows();
  }

  loadRows(): void {
    const config = this.dialog();
    if (config !== null) {
      const params = {
        ...this.filters,
        page: this.page(),
        sort: this.sortColumn,
        direction: this.sortDirection,
      };
      this.service.getRows(config.entity, params).subscribe((rows: any[]) => {
        this.rows.set(rows);
        if (rows.length > 0) {
          this.pages.set(Math.ceil(rows[0]['TOTAL_ROWS'] / 20));
        } else {
          this.pages.set(1);
        }
      });
    }
  }

  makeColumns(dialog: DialogConfig, dialogsFile: DialogsFile): DialogColumn[] {
    const result: DialogColumn[] = [];
    for (const column of dialog.columns) {
      if (!column.lookup) {
        result.push(column);
      } else {
        for (const spec of dialogsFile.entitySpec) {
          if (spec.name === column.lookup.entity) {
            for (const show of spec.show) {
              let label = column.label;
              if (spec.show.length > 1) {
                label = column.label + ' ' + show.label;
              }
              if (show.from) {
                label = show.label;
              }
              result.push({ field: column.field + '__' + show.field, label: label, filterable: column.filterable });
            }
          }
        }
      }
    }
    return result;
  }

  isDateColumn(field: string): boolean {
    return field === 'CREATED_AT'
      || field === 'FINISHED_AT'
      || field === 'COMPLETED_AT'
      || field === 'INVENTORY_ORDER_LINE_ID__COMPLETED_AT'
      || field === 'INVENTORY_ORDER_LINE_ID__INVENTORY_ORDER_ID';
  }

  formatDateTime(value: unknown): string {
    if (value == null || value === '') {
      return '—';
    }

    const date = value instanceof Date ? value : new Date(String(value).replace(' ', 'T'));
    if (Number.isNaN(date.getTime())) {
      return String(value);
    }

    return new Intl.DateTimeFormat('en-GB', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    }).format(date);
  }

  sort(column: string) {
    if (this.sortColumn === column) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortColumn = column;
      this.sortDirection = 'asc';
    }

    this.page.set(0);
    this.loadRows();
  }
}
