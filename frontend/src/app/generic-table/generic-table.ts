import { Component, computed, input, signal } from '@angular/core';
import { DataRow, DialogColumn } from '../models/dialog-config.model';
import { formatCell, isEmptyValue } from './cell-format';
import { SortCriterion, SortDirection, nextSort, sortRows } from './sorting';

// A table that shows any rows with any columns and sorts them in the browser.
// It makes no HTTP requests: the parent page gives it the columns and the rows.
@Component({
  selector: 'app-generic-table',
  standalone: true,
  templateUrl: './generic-table.html',
  styleUrl: './generic-table.css',
})
export class GenericTable {
  columns = input.required<DialogColumn[]>();
  rows = input.required<DataRow[]>();

  sortCriteria = signal<SortCriterion[]>([]);
  sortedRows = computed(() => sortRows(this.rows(), this.sortCriteria(), this.columns()));

  // Also works from the keyboard: Enter/Space on the button sends a click, with Shift if it is held.
  onHeaderClick(column: DialogColumn, event: MouseEvent): void {
    this.sortCriteria.update((criteria) => nextSort(criteria, column.field, event.shiftKey));
  }

  isSortable(column: DialogColumn): boolean {
    return column.sortable !== false;
  }

  direction(field: string): SortDirection | null {
    return this.sortCriteria().find((criterion) => criterion.field === field)?.direction ?? null;
  }

  // 1, 2, 3... shown only when more than one column is sorted.
  priority(field: string): number | null {
    const criteria = this.sortCriteria();
    return criteria.length > 1 ? criteria.findIndex((criterion) => criterion.field === field) + 1 : null;
  }

  ariaSort(field: string): 'ascending' | 'descending' | 'none' {
    const direction = this.direction(field);
    return direction === 'asc' ? 'ascending' : direction === 'desc' ? 'descending' : 'none';
  }

  display(value: unknown, column: DialogColumn): string {
    return formatCell(value, column.type ?? 'text');
  }

  isEmpty(value: unknown): boolean {
    return isEmptyValue(value);
  }
}
