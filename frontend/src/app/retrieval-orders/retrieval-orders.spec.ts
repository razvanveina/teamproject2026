import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Subject, of, throwError } from 'rxjs';
import { DialogConfigService } from '../services/dialog-config.service';
import { PickingApi } from '../services/picking';
import { RetrievalOrders } from './retrieval-orders';

describe('RetrievalOrders', () => {
  let component: RetrievalOrders;
  let fixture: ComponentFixture<RetrievalOrders>;
  let getRows: any;
  let getOrder: any;

  const orders = [
    { ID: 2, ORDER_ID: 'RT-2', STATUS: 'PICKED', DESTINATION_ID__NAME: 'Sibiu' },
    { ID: 1, ORDER_ID: 'RT-1', STATUS: 'NEW', DESTINATION_ID__NAME: 'Cluj' },
  ];
  const orderWithLines = (orderId: string) => ({
    orderId,
    destination: 'Cluj',
    box: null,
    lines: [
      { barcode: 'A-1', description: 'Milk', quantity: 10, status: 'NEW', location: 'PICK-1' },
      { barcode: 'A-2', description: 'Bread', quantity: 5, status: 'FINISHED', location: 'PICK-2' },
    ],
  });

  beforeEach(async () => {
    getRows = () => of(orders);
    getOrder = (orderId: string) => of(orderWithLines(orderId));

    await TestBed.configureTestingModule({
      imports: [RetrievalOrders],
      providers: [
        { provide: DialogConfigService, useValue: { getRows: (e: string) => getRows(e) } },
        { provide: PickingApi, useValue: { getOrder: (id: string) => getOrder(id) } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(RetrievalOrders);
    component = fixture.componentInstance;
  });

  it('shows the orders with their destination sorted by id', async () => {
    await fixture.whenStable();

    expect(component.rows().map((row) => row['ID'])).toEqual([1, 2]);
    const firstRow = [...fixture.nativeElement.querySelectorAll('tr.selectable td')]
      .slice(0, 4)
      .map((cell: any) => cell.textContent);
    expect(firstRow).toEqual(['1', 'RT-1', 'NEW', 'Cluj']);
  });

  it('filters by order id, status and destination', async () => {
    await fixture.whenStable();

    component.setOrderIdFilter({ target: { value: 'rt-2' } });
    expect(component.rows().map((row) => row['ID'])).toEqual([2]);

    component.setOrderIdFilter({ target: { value: '' } });
    component.setStatusFilter({ target: { value: 'new' } });
    expect(component.rows().map((row) => row['ID'])).toEqual([1]);

    component.setStatusFilter({ target: { value: '' } });
    component.setDestinationFilter({ target: { value: 'sib' } });
    expect(component.rows().map((row) => row['ID'])).toEqual([2]);
  });

  it('shows a hint until an order is selected', async () => {
    await fixture.whenStable();

    expect(fixture.nativeElement.querySelector('.hint').textContent).toContain('Click a retrieval order');
  });

  it('shows the articles of the clicked order at the bottom', async () => {
    await fixture.whenStable();

    fixture.nativeElement.querySelector('tr.selectable').click();
    await fixture.whenStable();

    const panes = fixture.nativeElement.querySelectorAll('.pane');
    expect(panes[1].querySelector('h2').textContent).toContain('RT-1');
    const firstLine = [...panes[1].querySelectorAll('td')].slice(0, 4).map((cell: any) => cell.textContent);
    expect(firstLine).toEqual(['A-1', 'Milk', '10', 'NEW']);
    expect(fixture.nativeElement.querySelector('tr.selected').textContent).toContain('RT-1');
  });

  it('keeps the last clicked order when an older response arrives late', async () => {
    await fixture.whenStable();
    const responses: any = { 'RT-1': new Subject<any>(), 'RT-2': new Subject<any>() };
    getOrder = (orderId: string) => responses[orderId];

    component.select(component.rows()[0]);
    component.select(component.rows()[1]);
    responses['RT-2'].next(orderWithLines('RT-2'));
    responses['RT-1'].next(orderWithLines('RT-1'));

    expect(component.details().orderId).toBe('RT-2');
  });

  it('reloads the articles of the selected order on refresh', async () => {
    await fixture.whenStable();
    component.select(component.rows()[0]);

    getOrder = (orderId: string) => of({ ...orderWithLines(orderId), box: 'BOX-1' });
    component.refresh();

    expect(component.details().box).toBe('BOX-1');
  });

  it('shows an error when the articles cannot be loaded', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => {});
    await fixture.whenStable();
    getOrder = () => throwError(() => ({ error: { detail: 'Cannot find order' } }));

    component.select(component.rows()[0]);

    expect(component.detailsError()).toBe('Cannot find order');
  });

  it('shows an error when the orders cannot be loaded', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => {});
    getRows = () => throwError(() => ({ status: 500 }));
    await fixture.whenStable();

    expect(component.error()).toBe('Could not load the retrieval orders (status 500)');
  });
});
