import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { DialogConfig, DialogLookup, DialogsFile, EntitySpec } from '../models/dialog-config.model';
import { DialogConfigService } from '../services/dialog-config.service';

// One input of the form (description of a field)
interface FormField {
  field: string; // column name, e.g. "BARCODE"
  label: string; // text above the input, e.g. "Barcode"
  type: string; // "number", "text", "select" or "lookup"
  required: boolean; // true when the column cannot be empty
  choices: Choice[]; // the lines of the dropdown list (select and lookup)
  parent: string; // the field this one depends on, e.g. "ARTICLE_ID" (empty = none)
}

// One line of a dropdown list
interface Choice {
  id: any; // what we save, e.g. 13 or "NEW"
  text: string; // what the user sees, e.g. "5941234000031 - Milk"
  parentId: any; // e.g. the article of a packaging type
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

  // The dialog file (title, labels, editable, options, lookup)
  dialog = signal<DialogConfig | null>(null);

  // The inputs of the form
  fields = signal<FormField[]>([]);

  // The error message shown above the form
  error = signal('');

  // Which dropdown list is open now, e.g. "ARTICLE_ID" (empty = none)
  openField = signal('');

  // What is written in each input, e.g. { BARCODE: "123", ARTICLE_ID: "13 - Milk" }
  values: any = {};

  // What was picked from a dropdown list, e.g. { ARTICLE_ID: 13 }
  selected: any = {};

  // From dialogs.json: what the user sees of each entity, e.g. an article = barcode + description
  entitySpec: EntitySpec[] = [];

  ngOnInit(): void {
    this.entity = this.route.snapshot.paramMap.get('entity') + '';

    // 0. dialogs.json, for the entitySpec
    this.service.getDialogsFile().subscribe((dialogsFile: DialogsFile) => {
      this.entitySpec = dialogsFile.entitySpec;
      this.loadForm();
    });
  }

  loadForm(): void {
    // 1. The dialog file
    this.service.getDialog(this.entity + '.json').subscribe((dialog: DialogConfig) => {
      this.dialog.set(dialog);

      // 2. The columns from the database: type and required
      this.service.getColumns(this.entity).subscribe((columns: any[]) => {
        const fields: FormField[] = [];

        for (const column of dialog.columns) {
          for (const dbColumn of columns) {
            // only the editable columns, together with their database description
            if (column.addable && dbColumn['COLUMN_NAME'] === column.field) {
              const field: FormField = {
                field: column.field,
                label: column.label,
                type: 'text',
                required: false,
                choices: [],
                parent: '',
              };

              if (dbColumn['DATA_TYPE'] === 'NUMBER') {
                field.type = 'number';
              }
              if (dbColumn['NULLABLE'] === 'N') {
                field.required = true;
              }

              // fixed values, e.g. the status
              if (column.options) {
                field.type = 'select';
                for (const option of column.options) {
                  field.choices.push({ id: option, text: option, parentId: null });
                }
              }

              // rows of another entity, e.g. the articles
              if (column.lookup) {
                field.type = 'lookup';
                this.loadChoices(field, column.lookup);
              }

              fields.push(field);
            }
          }
        }

        this.fields.set(fields);
      });
    });
  }

  // 3. Asks for the rows of the other entity and makes a line for each, e.g. "5941234000031 - Milk"
  loadChoices(field: FormField, lookup: DialogLookup): void {
    if (lookup.parent) {
      field.parent = lookup.parent;
    }

    // which columns of that entity the user sees (from entitySpec)
    let show: string[] = [];
    for (const spec of this.entitySpec) {
      if (spec.name === lookup.entity) {
        for (const column of spec.show) {
          show.push(column.field);
        }
      }
    }
    // no entitySpec for it: show the ID
    if (show.length === 0) {
      show = ['ID'];
    }

    this.service.getRows(lookup.entity).subscribe((rows: any[]) => {
      for (const row of rows) {
        // e.g. BARCODE and DESCRIPTION glued together: "5941234000031 - Milk"
        let text = '';
        for (const column of show) {
          if (text !== '') {
            text = text + ' - ';
          }
          text = text + row[column];
        }

        const choice: Choice = { id: row['ID'], text: text, parentId: null };
        if (lookup.parent) {
          // e.g. the ARTICLE_ID of a packaging type
          choice.parentId = row[lookup.parent];
        }
        field.choices.push(choice);
      }
    });
  }

  // The lines the dropdown list shows now
  matches(field: FormField): Choice[] {
    const result: Choice[] = [];

    // the packaging type shows nothing until the article is picked
    const parentId = this.selected[field.parent];
    if (field.parent !== '' && parentId === undefined) {
      return result;
    }

    // what the user typed (after a pick we show the whole list again)
    let typed = '';
    if (this.values[field.field] && this.selected[field.field] === undefined) {
      typed = this.values[field.field].toLowerCase();
    }

    for (const choice of field.choices) {
      let ok = true;
      if (field.parent !== '' && choice.parentId !== parentId) {
        ok = false;
      }
      if (!choice.text.toLowerCase().includes(typed)) {
        ok = false;
      }
      if (ok) {
        result.push(choice);
      }

      // max 50 lines (there are 11000 locations), typing makes the list shorter
      if (result.length === 50) {
        break;
      }
    }
    return result;
  }

  // The text shown when the list is empty
  emptyText(field: FormField): string {
    for (const other of this.fields()) {
      if (other.field === field.parent && this.selected[field.parent] === undefined) {
        return 'Choose ' + other.label + ' first';
      }
    }
    return 'Nothing found';
  }

  // Click on a line of the list
  choose(field: FormField, choice: Choice): void {
    this.values[field.field] = choice.text;
    this.selected[field.field] = choice.id;
    this.clearChildren(field);
    this.openField.set('');
  }

  // The user types in a dropdown input, so the old pick is not valid anymore
  typed(field: FormField): void {
    this.selected[field.field] = undefined;
    this.clearChildren(field);
  }

  // Empties the fields that depend on this one, e.g. the packaging type when the article changes
  clearChildren(field: FormField): void {
    for (const child of this.fields()) {
      if (child.parent === field.field) {
        this.values[child.field] = '';
        this.selected[child.field] = undefined;
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

      if (!empty && (field.type === 'select' || field.type === 'lookup')) {
        // a dropdown sends what was picked: 13, not "13 - Milk"
        if (this.selected[field.field] === undefined) {
          this.error.set('Choose ' + field.label + ' from the list');
          return;
        }
        data[field.field] = this.selected[field.field];
      } else if (!empty) {
        data[field.field] = value;
      }
    }

    this.error.set('');

    this.service.addRow(this.entity, data).subscribe({
      // saved: back to the table
      next: () => {
        this.router.navigate(['/dialogs', this.entity]);
      },
      // the server said no: show the message and write everything in the console (F12)
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
