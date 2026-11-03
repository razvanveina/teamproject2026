import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { DialogColumn, DialogConfig, DialogsFile } from '../models/dialog-config.model';
import { DialogConfigService } from '../services/dialog-config.service';

@Component({
  selector: 'app-dialog-page',
  imports: [RouterLink],
  templateUrl: './dialog-page.html',
  styleUrl: './dialog-page.css',
})
export class DialogPage implements OnInit {
  private route = inject(ActivatedRoute);
  private service = inject(DialogConfigService);

  // The dialog description (title, entity, columns). Empty until it arrives.
  dialog = signal<DialogConfig | null>(null);

  // The rows of the table. Empty until they arrive.
  rows = signal<any[]>([]);
  tableColumns = signal<DialogColumn[]>([]);
  filters: any = {};
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
      // 2. Ask for the dialog file, e.g. ReceivingOrder.json
      this.service.getDialog(entity + '.json').subscribe((dialog: DialogConfig) => {
        this.dialog.set(dialog);

        this.service.getDialogsFile().subscribe((dialogsFile: DialogsFile) => {
          this.tableColumns.set(this.makeColumns(dialog, dialogsFile));
        });

        // 3. Ask for the rows, e.g. /api/data/article
        this.service.getRows(dialog.entity).subscribe((rows: any[]) => {
          this.rows.set(rows);
        });
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
    this.loadRows();
  }

  loadRows(): void {
    const config = this.dialog();
    if (config !== null) {
      this.service.getRows(config.entity, this.filters).subscribe((rows: any[]) => {
        this.rows.set(rows);
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
}