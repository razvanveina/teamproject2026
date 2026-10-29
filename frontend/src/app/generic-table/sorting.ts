import { ColumnType, DataRow, DialogColumn } from '../models/dialog-config.model';

export type SortDirection = 'asc' | 'desc';

// One sort criterion. The order in the list is the priority (first = most important).
export interface SortCriterion {
  field: string;
  direction: SortDirection;
}

// What a click on a column header does to the sort criteria:
// - a plain click keeps only this column and cycles it: ascending -> descending -> no sort;
// - Shift+click keeps the other columns and adds this one at the end (or cycles it if already there).
export function nextSort(criteria: SortCriterion[], field: string, addToExisting: boolean): SortCriterion[] {
  const current = criteria.find((criterion) => criterion.field === field);
  const nextDirection: SortDirection | null =
    current === undefined ? 'asc' : current.direction === 'asc' ? 'desc' : null;

  if (!addToExisting) {
    return nextDirection ? [{ field, direction: nextDirection }] : [];
  }

  if (current === undefined) {
    return [...criteria, { field, direction: 'asc' }];
  }

  return nextDirection
    ? criteria.map((criterion) =>
        criterion.field === field ? { field, direction: nextDirection } : criterion,
      )
    : criteria.filter((criterion) => criterion.field !== field);
}

// Returns a new sorted array; the input array is not changed.
export function sortRows(rows: DataRow[], criteria: SortCriterion[], columns: DialogColumn[]): DataRow[] {
  if (criteria.length === 0) {
    return rows;
  }

  const typeOf = (field: string): ColumnType =>
    columns.find((column) => column.field === field)?.type ?? 'text';

  return [...rows].sort((a, b) => {
    for (const criterion of criteria) {
      const result = compareValues(a[criterion.field], b[criterion.field], typeOf(criterion.field), criterion.direction);
      if (result !== 0) {
        return result;
      }
    }
    return 0;
  });
}

// Compares two cell values. Empty values always go last, whatever the direction.
export function compareValues(a: unknown, b: unknown, type: ColumnType, direction: SortDirection): number {
  const keyA = sortKey(a, type);
  const keyB = sortKey(b, type);

  if (keyA === null || keyB === null) {
    return keyA === keyB ? 0 : keyA === null ? 1 : -1;
  }

  const result =
    typeof keyA === 'string' && typeof keyB === 'string'
      ? keyA.localeCompare(keyB, 'ro', { sensitivity: 'base' })
      : Number(keyA) - Number(keyB);

  return direction === 'asc' ? result : -result;
}

// The value used for comparing, by column type; null means "empty".
function sortKey(value: unknown, type: ColumnType): string | number | null {
  if (value === null || value === undefined || (typeof value === 'string' && value.trim() === '')) {
    return null;
  }

  switch (type) {
    case 'number': {
      const number = Number(value);
      return Number.isNaN(number) ? null : number;
    }
    case 'date': {
      const time = Date.parse(String(value));
      return Number.isNaN(time) ? null : time;
    }
    case 'boolean':
      return value === true || value === 'true' ? 1 : 0;
    default:
      return String(value);
  }
}
