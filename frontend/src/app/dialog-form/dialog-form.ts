import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { DialogConfig } from '../models/dialog-config.model';
import { DialogConfigService } from '../services/dialog-config.service';

// One input of the form
interface FormField {//description of a filed 
  field: string; // column name, e.g. "BARCODE"
  label: string; // text above the input, e.g. "Barcode"
  type: string; // "number" or "text"
  required: boolean; // true when the column cannot be empty
}

@Component({
  selector: 'app-dialog-form',
  imports: [FormsModule],
  templateUrl: './dialog-form.html',
  styleUrl: './dialog-form.css',
})
export class DialogForm implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private service = inject(DialogConfigService);

  // The entity from the address, e.g. "Article" from /dialogs/Article/add
  entity = '';

  // The dialog file (title, labels, which columns are editable)
  dialog = signal<DialogConfig | null>(null);

  // The inputs of the form. Empty until the dialog file and the columns arrive.
  fields = signal<FormField[]>([]);

  // The error message shown above the form
  error = signal('');

  // What the user types, e.g. { BARCODE: "123", DESCRIPTION: "Milk" }
  values: any = {};

  ngOnInit(): void {
    this.entity = this.route.snapshot.paramMap.get('entity') + '';

    // 1. The dialog file: labels and "editable"
    this.service.getDialog(this.entity + '.json').subscribe((dialog: DialogConfig) => {
      this.dialog.set(dialog);

      // 2. The columns from the database: type and required
      this.service.getColumns(this.entity).subscribe((columns: any[]) => {
        const fields: FormField[] = [];

        for (const column of dialog.columns) {
          // Only the columns marked "editable": true in the JSON
          if (column.editable) {
            // Find the same column in the database description
            for (const dbColumn of columns) {
              if (dbColumn['COLUMN_NAME'] === column.field) {
                let type = 'text';
                if (dbColumn['DATA_TYPE'] === 'NUMBER') {
                  type = 'number';
                }

                fields.push({
                  field: column.field,
                  label: column.label,
                  type: type,
                  required: dbColumn['NULLABLE'] === 'N',
                });
              }
            }
          }
        }

        this.fields.set(fields);
      });
    });
  }

  // Runs when the user clicks Save
  save(): void {
    const data: any = {};

    for (const field of this.fields()) {
      const value = this.values[field.field];
      const empty = value === undefined || value === null || value === '';

      if (empty && field.required) {
        this.error.set(field.label + ' is required');
        return;
      }

      // Send only the fields the user filled in
      if (!empty) {
        data[field.field] = value;
      }
    }

    this.error.set('');

    this.service.addRow(this.entity, data).subscribe({
      // Saved: go back to the table
      next: () => {
        this.router.navigate(['/dialogs', this.entity]);
      },
      // The server said no: show its message and write everything in the browser console (F12)
      error: (err) => {
        console.error('Error adding ' + this.entity, err);
        if (err.error && err.error.message) {
          this.error.set(err.error.message);
        } else {
          this.error.set('Save failed (status ' + err.status + ')');
        }
      },
    });
  }

  // Runs when the user clicks Cancel: back to the table, nothing saved
  cancel(): void {
    this.router.navigate(['/dialogs', this.entity]);
  }
}