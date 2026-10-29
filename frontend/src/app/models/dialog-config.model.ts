// Shapes of the dialog configuration sent by the backend (GET /api/dialogs, GET /api/dialogs/{id}).
// Fields the backend may add later are simply ignored.

export type ColumnType = 'text' | 'number' | 'date' | 'boolean';

export interface DialogSummary {
  id: string;
  title: string;
}

export interface DialogColumn {
  field: string;
  label: string;
  type?: ColumnType; // missing means 'text'
  sortable?: boolean; // missing means true
}

export interface DialogConfig {
  id: string;
  title: string;
  entity: string; // data comes from GET /api/{entity}
  columns: DialogColumn[];
}

// One row from GET /api/{entity}: any object, read by column field.
export type DataRow = Record<string, unknown>;
