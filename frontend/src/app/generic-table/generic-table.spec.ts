import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DialogColumn } from '../models/dialog-config.model';
import { GenericTable } from './generic-table';

describe('GenericTable', () => {
  let fixture: ComponentFixture<GenericTable>;
  let element: HTMLElement;

  const columns: DialogColumn[] = [
    { field: 'id', label: 'ID', type: 'number' },
    { field: 'name', label: 'Nume' },
    { field: 'note', label: 'Notă', sortable: false },
  ];

  beforeEach(async () => {
    fixture = TestBed.createComponent(GenericTable);
    fixture.componentRef.setInput('columns', columns);
    fixture.componentRef.setInput('rows', [
      { id: 2, name: 'Ion', note: null },
      { id: 10, name: 'Ana', note: 'x' },
      { id: 1, name: 'Ana', note: 'y' },
    ]);
    element = fixture.nativeElement;
    await fixture.whenStable();
  });

  const headers = () => [...element.querySelectorAll('th')];
  const firstColumn = () => [...element.querySelectorAll('tbody tr td:first-child')].map((td) => td.textContent?.trim());

  async function clickHeader(index: number, shiftKey = false) {
    headers()[index].querySelector('button')!.dispatchEvent(new MouseEvent('click', { shiftKey }));
    await fixture.whenStable();
  }

  it('shows the column labels and only sortable columns have a button', () => {
    expect(headers().map((th) => th.textContent?.trim().split('Sortează')[0].trim())).toEqual(['ID', 'Nume', 'Notă']);
    expect(headers()[2].querySelector('button')).toBeNull();
    expect(headers()[2].hasAttribute('aria-sort')).toBe(false);
  });

  it('shows empty values as a dash', () => {
    expect(element.querySelector('tbody tr td:last-child')?.textContent?.trim()).toBe('—');
  });

  it('cycles ascending -> descending -> no sort on click, with aria-sort', async () => {
    await clickHeader(0);
    expect(firstColumn()).toEqual(['1', '2', '10']);
    expect(headers()[0].getAttribute('aria-sort')).toBe('ascending');

    await clickHeader(0);
    expect(firstColumn()).toEqual(['10', '2', '1']);
    expect(headers()[0].getAttribute('aria-sort')).toBe('descending');

    await clickHeader(0);
    expect(firstColumn()).toEqual(['2', '10', '1']);
    expect(headers()[0].getAttribute('aria-sort')).toBe('none');
  });

  it('adds a secondary criterion with Shift+click and shows the priority numbers', async () => {
    await clickHeader(1);
    expect(element.querySelector('.sort-priority')).toBeNull();

    await clickHeader(0, true);
    expect(firstColumn()).toEqual(['1', '10', '2']);
    expect([...element.querySelectorAll('.sort-priority')].map((badge) => badge.textContent)).toEqual(['2', '1']);
  });
});
