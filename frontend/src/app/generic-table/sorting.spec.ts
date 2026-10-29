import { DataRow, DialogColumn } from '../models/dialog-config.model';
import { SortCriterion, nextSort, sortRows } from './sorting';

describe('nextSort', () => {
  it('cycles a column with plain clicks: ascending -> descending -> no sort', () => {
    let criteria: SortCriterion[] = [];

    criteria = nextSort(criteria, 'name', false);
    expect(criteria).toEqual([{ field: 'name', direction: 'asc' }]);

    criteria = nextSort(criteria, 'name', false);
    expect(criteria).toEqual([{ field: 'name', direction: 'desc' }]);

    criteria = nextSort(criteria, 'name', false);
    expect(criteria).toEqual([]);
  });

  it('a plain click on another column replaces the whole sort', () => {
    const criteria = nextSort([{ field: 'name', direction: 'asc' }, { field: 'id', direction: 'desc' }], 'email', false);
    expect(criteria).toEqual([{ field: 'email', direction: 'asc' }]);
  });

  it('Shift+click adds columns as secondary criteria and cycles them in place', () => {
    let criteria: SortCriterion[] = [{ field: 'status', direction: 'asc' }];

    criteria = nextSort(criteria, 'createdAt', true);
    expect(criteria).toEqual([
      { field: 'status', direction: 'asc' },
      { field: 'createdAt', direction: 'asc' },
    ]);

    criteria = nextSort(criteria, 'createdAt', true);
    expect(criteria).toEqual([
      { field: 'status', direction: 'asc' },
      { field: 'createdAt', direction: 'desc' },
    ]);

    criteria = nextSort(criteria, 'createdAt', true);
    expect(criteria).toEqual([{ field: 'status', direction: 'asc' }]);
  });
});

describe('sortRows', () => {
  const columns: DialogColumn[] = [
    { field: 'id', label: 'ID', type: 'number' },
    { field: 'code', label: 'Cod' },
    { field: 'status', label: 'Status', type: 'text' },
    { field: 'createdAt', label: 'Creat', type: 'date' },
    { field: 'reserved', label: 'Rezervat', type: 'boolean' },
    { field: 'packaging', label: 'Ambalaj' },
  ];

  const ids = (rows: DataRow[]) => rows.map((row) => row['id']);

  it('compares number columns numerically and text columns alphabetically', () => {
    const rows: DataRow[] = [
      { id: 10, code: '10' },
      { id: 9, code: '9' },
      { id: 100, code: '100' },
    ];

    expect(ids(sortRows(rows, [{ field: 'id', direction: 'asc' }], columns))).toEqual([9, 10, 100]);
    // As text, "10" < "100" < "9".
    expect(sortRows(rows, [{ field: 'code', direction: 'asc' }], columns).map((row) => row['code'])).toEqual([
      '10',
      '100',
      '9',
    ]);
  });

  it('sorts text in Romanian order, ignoring case (ă comes after a, before b)', () => {
    const rows: DataRow[] = [{ id: 1, status: 'ăla' }, { id: 2, status: 'Bun' }, { id: 3, status: 'alt' }];
    expect(ids(sortRows(rows, [{ field: 'status', direction: 'asc' }], columns))).toEqual([3, 1, 2]);
  });

  it('sorts dates chronologically and booleans false before true', () => {
    const rows: DataRow[] = [
      { id: 1, createdAt: '2026-10-29T08:15:00', reserved: true },
      { id: 2, createdAt: '2026-01-05T17:00:00', reserved: false },
      { id: 3, createdAt: '2026-10-29T07:59:00', reserved: true },
    ];

    expect(ids(sortRows(rows, [{ field: 'createdAt', direction: 'asc' }], columns))).toEqual([2, 3, 1]);
    expect(ids(sortRows(rows, [{ field: 'reserved', direction: 'asc' }], columns))).toEqual([2, 1, 3]);
  });

  it('sorts on several columns in priority order', () => {
    const rows: DataRow[] = [
      { id: 1, status: 'NEW', createdAt: '2026-10-29T10:00:00' },
      { id: 2, status: 'ACTIVE', createdAt: '2026-10-28T10:00:00' },
      { id: 3, status: 'NEW', createdAt: '2026-10-27T10:00:00' },
      { id: 4, status: 'ACTIVE', createdAt: '2026-10-30T10:00:00' },
    ];

    const sorted = sortRows(
      rows,
      [
        { field: 'status', direction: 'asc' },
        { field: 'createdAt', direction: 'desc' },
      ],
      columns,
    );

    expect(ids(sorted)).toEqual([4, 2, 1, 3]);
  });

  it('puts empty values last in both directions', () => {
    const rows: DataRow[] = [
      { id: 1, packaging: null },
      { id: 2, packaging: 'Cutie' },
      { id: 3, packaging: '' },
      { id: 4, packaging: 'Bax' },
      { id: 5 },
    ];

    expect(ids(sortRows(rows, [{ field: 'packaging', direction: 'asc' }], columns))).toEqual([4, 2, 1, 3, 5]);
    expect(ids(sortRows(rows, [{ field: 'packaging', direction: 'desc' }], columns))).toEqual([2, 4, 1, 3, 5]);
  });

  it('returns the rows unchanged when there is no sort', () => {
    const rows: DataRow[] = [{ id: 2 }, { id: 1 }];
    expect(sortRows(rows, [], columns)).toBe(rows);
  });
});
