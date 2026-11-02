// One column of the table, as in users.json
export interface DialogColumn {
  field: string; // name of the field in the data, e.g. "email"
  label: string; // text in the column header, e.g. "Email"
  editable?: boolean; // true = the column appears in the Add/Edit form
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