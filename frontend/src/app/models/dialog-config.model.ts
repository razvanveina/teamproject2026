// One column of the table, as in users.json
export interface DialogColumn {
  field: string; // name of the field in the data, e.g. "email"
  label: string; // text in the column header, e.g. "Email"
  editable?: boolean; // true = the column appears in the Add/Edit form
  options?: string[]; // the form shows a dropdown with these values, e.g. ["NEW", "ACTIVE"]
  lookup?: DialogLookup; // the form lets you pick a row of another entity, e.g. an article
}

// A link to another entity, e.g. { "entity": "Article", "show": "DESCRIPTION" }
export interface DialogLookup {
  entity: string; // where the choices come from: /api/data/<entity>
  show: string; // which column the user sees next to the ID, e.g. "DESCRIPTION"
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
}

// One entry of dialogs.json: { "file": "users.json" }
export interface DialogEntry {
  file: string;
}