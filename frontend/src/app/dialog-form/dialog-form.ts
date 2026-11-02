import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { DialogConfig } from '../models/dialog-config.model';
import { DialogConfigService } from '../services/dialog-config.service';

// One input of the form
interface FormField {//description of a field
  field: string; // column name, e.g. "BARCODE"
  label: string; // text above the input, e.g. "Barcode"
  type: string; // "number", "text", "select" (fixed values) or "lookup" (rows of another entity)
  required: boolean; // true when the column cannot be empty
  choices: Choice[]; // select and lookup: what the dropdown list can show
  parent: string; // lookup only: the field it depends on, e.g. "ARTICLE_ID". Empty when it depends on nothing.
}

// One line of a dropdown list
interface Choice {
  id: any; // what is saved: the ID of the row (lookup) or the value itself (select), e.g. 5 or "NEW"
  text: string; // what the user sees, e.g. "5 - Milk" or "NEW"
  parentId: any; // lookup with a parent: the ID it belongs to, e.g. the article of a packaging type
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

  // Which dropdown list is open now, e.g. "ARTICLE_ID". Empty when none is open.
  openField = signal('');

  // What the user types or sees in each input, e.g. { BARCODE: "123", ARTICLE_ID: "5 - Milk" }
  values: any = {};

  // What the user picked from a dropdown list, e.g. { ARTICLE_ID: 5, STATUS: "NEW" }
  selected: any = {};

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
                let parent = '';
                const choices: Choice[] = [];

                if (dbColumn['DATA_TYPE'] === 'NUMBER') {
                  type = 'number';
                }
                if (column.options) {
                  // Fixed values from the JSON, e.g. the status
                  type = 'select';
                  for (const option of column.options) {
                    choices.push({ id: option, text: option, parentId: null });
                  }
                }
                if (column.lookup) {
                  // Rows of another entity: they are loaded below (step 3)
                  type = 'lookup';
                  if (column.lookup.parent) {
                    parent = column.lookup.parent;
                  }
                }

                fields.push({
                  field: column.field,
                  label: column.label,
                  type: type,
                  required: dbColumn['NULLABLE'] === 'N',
                  choices: choices,
                  parent: parent,
                });
              }
            }
          }
        }

        // 3. The lookups: ask for the rows of the other entities, e.g. all articles.
        // The form is shown when all of them have arrived.
        let waiting = 0;
        for (const field of fields) {
          if (field.type === 'lookup') {
            waiting = waiting + 1;
          }
        }

        if (waiting === 0) {
          this.fields.set(fields);
        }

        for (const column of dialog.columns) {
          const lookup = column.lookup;
          if (column.editable && lookup) {
            this.service.getRows(lookup.entity).subscribe((rows: any[]) => {
              // Find the field of this column and fill its choices, e.g. "5 - Milk"
              for (const field of fields) {
                if (field.field === column.field) {
                  for (const row of rows) {
                    let parentId = null;
                    if (lookup.parent) {
                      parentId = row[lookup.parent]; // e.g. the ARTICLE_ID of a packaging type
                    }
                    field.choices.push({ id: row['ID'], text: row['ID'] + ' - ' + row[lookup.show], parentId: parentId });
                  }
                }
              }

              waiting = waiting - 1;
              if (waiting === 0) {
                this.fields.set(fields);
              }
            });
          }
        }
      });
    });
  }

  // The lines the dropdown list of a field shows now
  matches(field: FormField): Choice[] {
    const result: Choice[] = [];

    // Depends on another field (e.g. packaging type on article): nothing until that one is picked
    let parentId = null;
    if (field.parent !== '') {
      parentId = this.selected[field.parent];
      if (parentId === undefined) {
        return result;
      }
    }

    // Filter by what the user typed. After a pick, show the whole list again.
    let typed = '';
    if (this.values[field.field] && this.selected[field.field] === undefined) {
      typed = String(this.values[field.field]).toLowerCase();
    }

    for (const choice of field.choices) {
      const sameParent = parentId === null || choice.parentId === parentId;
      if (sameParent && choice.text.toLowerCase().includes(typed)) {
        result.push(choice);
      }
    }
    return result;
  }

  // The text shown when the dropdown list has no lines
  emptyText(field: FormField): string {
    if (field.parent !== '' && this.selected[field.parent] === undefined) {
      for (const other of this.fields()) {
        if (other.field === field.parent) {
          return 'Choose ' + other.label + ' first';
        }
      }
    }
    return 'Nothing found';
  }

  // Runs when the user clicks a line of a dropdown list
  choose(field: FormField, choice: Choice): void {
    this.values[field.field] = choice.text;
    this.selected[field.field] = choice.id;
    this.clearChildren(field);
    this.openField.set('');
  }

  // Runs when the user types in a dropdown input: the old pick is no longer valid
  typed(field: FormField): void {
    delete this.selected[field.field];
    this.clearChildren(field);
  }

  // Empties the fields that depend on this one, e.g. the packaging type when the article changes
  clearChildren(field: FormField): void {
    for (const child of this.fields()) {
      if (child.parent === field.field) {
        this.values[child.field] = '';
        delete this.selected[child.field];
      }
    }
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

      if (!empty && (field.type === 'lookup' || field.type === 'select')) {
        // A dropdown shows "5 - Milk", but the database needs what was picked: 5
        if (this.selected[field.field] === undefined) {
          this.error.set('Choose ' + field.label + ' from the list');
          return;
        }
        data[field.field] = this.selected[field.field];
      } else if (!empty) {
        // Send only the fields the user filled in
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
