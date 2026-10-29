import { ColumnType } from '../models/dialog-config.model';

export const EMPTY_CELL = '—';

export function isEmptyValue(value: unknown): boolean {
  return value === null || value === undefined || (typeof value === 'string' && value.trim() === '');
}

// The text shown in a cell, by column type.
export function formatCell(value: unknown, type: ColumnType): string {
  if (isEmptyValue(value)) {
    return EMPTY_CELL;
  }

  switch (type) {
    case 'boolean':
      return value === true || value === 'true' ? 'Da' : 'Nu';
    case 'date':
      return formatDate(String(value));
    default:
      return String(value);
  }
}

// '2026-10-29T08:15:00' -> '29.10.2026 08:15', '2026-10-29' -> '29.10.2026'.
// Read straight from the text, so the time zone of the browser does not move the date.
function formatDate(value: string): string {
  const match = /^(\d{4})-(\d{2})-(\d{2})(?:[T ](\d{2}):(\d{2}))?/.exec(value);
  if (!match) {
    return value;
  }

  const [, year, month, day, hour, minute] = match;
  const date = `${day}.${month}.${year}`;
  return hour ? `${date} ${hour}:${minute}` : date;
}
