// One column of the table, as in users.json
export interface DialogColumn {
  field: string; // name of the field in the data, e.g. "email"
  label: string; // text in the column header, e.g. "Email"
  addable?: boolean;
  editable?: boolean;
  options?: string[]; // the form shows a dropdown with these values, e.g. ["NEW", "ACTIVE"]
  lookup?: DialogLookup; // the form lets you pick a row of another entity, e.g. an article
  filterable?: boolean;
}

// A link to another entity, e.g. { "entity": "Article" }
// What the user sees of that entity is written once, in "entitySpec" in dialogs.json
export interface DialogLookup {
  entity: string; // where the choices come from: /api/data/<entity>
  parent?: string; // only the rows that belong to what is picked in this field, e.g. "ARTICLE_ID"
}

// One dialog = the content of users.json
export interface DialogConfig {
  id: string;
  title: string;
  entity: string; // the rows come from /api/<entity>
  actions: string[]; // what the user may do: "add", "edit", "delete"
  columns: DialogColumn[];
}

// dialogs.json = the main list
export interface DialogsFile {
  dialogs: DialogEntry[];
  entitySpec: EntitySpec[];
}

// What the user sees of an entity, e.g. an article = barcode + description
export interface EntitySpec {
  name: string; // the entity, e.g. "Article"
  show: ShowColumn[]; // its columns that are shown instead of the ID
}

// One column of an entity that is shown, e.g. { "field": "BARCODE", "label": "Barcode" }
export interface ShowColumn {
  field: string;
  label: string;
  from?: string;
  column?: string;
}

// One entry of dialogs.json: { "file": "users.json" }
export interface DialogEntry {
  file: string;
}