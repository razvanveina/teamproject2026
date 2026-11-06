import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Subject, of, throwError } from 'rxjs';
import { ReceivingOrder } from '../models/receiving-order.model';
import { DialogConfigService } from '../services/dialog-config.service';
import { ReceivingOrderService } from '../services/receiving-order-service';
import { ReceivingOrders } from './receiving-orders';

describe('ReceivingOrders', () => {
  let component: ReceivingOrders;
  let fixture: ComponentFixture<ReceivingOrders>;
  let getRows: any;
  let getReceivingOrder: any;

  const newOrders = [
    { ID: 3, ORDER_ID: 'RO-3', STATUS: 'NEW' },
    { ID: 1, ORDER_ID: 'RO-1', STATUS: 'NEW' },
  ];
  const activeOrders = [{ ID: 2, ORDER_ID: 'RO-2', STATUS: 'ACTIVE' }];
  const finishedOrders = [{ ID: 4, ORDER_ID: 'RO-4', STATUS: 'FINISHED' }];
  const ordersByStatus: any = { NEW: newOrders, ACTIVE: activeOrders, FINISHED: finishedOrders };
  const orderWithLines = (orderId: string): ReceivingOrder => ({
    orderId,
    status: 'ACTIVE',
    lines: [
      { id: 1, barcode: 'A-1', description: 'Milk', quantity: 10, receivedQuantity: 4, status: 'ACTIVE' },
      { id: 2, barcode: 'A-2', description: 'Bread', quantity: 5, receivedQuantity: 5, status: 'FINISHED' },
    ],
  });

  beforeEach(async () => {
    getRows = (_entity: string, filters: any) => of(ordersByStatus[filters.STATUS]);
    getReceivingOrder = (orderId: string) => of(orderWithLines(orderId));

    await TestBed.configureTestingModule({
      imports: [ReceivingOrders],
      providers: [
        { provide: DialogConfigService, useValue: { getRows: (e: string, f: any) => getRows(e, f) } },
        { provide: ReceivingOrderService, useValue: { getReceivingOrder: (id: string) => getReceivingOrder(id) } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ReceivingOrders);
    component = fixture.componentInstance;
  });

  it('shows new, active and finished orders together sorted by id', async () => {
    await fixture.whenStable();

    expect(component.rows().map((row) => row['ID'])).toEqual([1, 2, 3, 4]);
    const cells = fixture.nativeElement.querySelectorAll('td');
    expect(cells.length).toBe(12);
  });

  it('filters by order id and status', async () => {
    await fixture.whenStable();

    component.setOrderIdFilter({ target: { value: 'ro-2' } });
    expect(component.rows().map((row) => row['ID'])).toEqual([2]);

    component.setOrderIdFilter({ target: { value: '' } });
    component.setStatusFilter({ target: { value: 'new' } });
    expect(component.rows().map((row) => row['ID'])).toEqual([1, 3]);
  });

  it('splits the orders into pages of 20', async () => {
    const many = Array.from({ length: 25 }, (_, i) => ({ ID: i + 1, ORDER_ID: 'RO-' + i, STATUS: 'NEW' }));
    getRows = (_entity: string, filters: any) => of(filters.STATUS === 'NEW' ? many : []);
    await fixture.whenStable();

    expect(component.pages()).toBe(2);
    expect(component.rows().length).toBe(20);

    component.nextPage();
    expect(component.rows().length).toBe(5);
  });

  it('shows a hint until an order is selected', async () => {
    await fixture.whenStable();

    expect(fixture.nativeElement.querySelector('.hint').textContent).toContain('Click a receiving order');
  });

  it('shows the articles of the clicked order at the bottom', async () => {
    await fixture.whenStable();

    fixture.nativeElement.querySelector('tr.selectable').click();
    await fixture.whenStable();

    const panes = fixture.nativeElement.querySelectorAll('.pane');
    expect(panes[1].querySelector('h2').textContent).toContain('RO-1');
    const firstLine = [...panes[1].querySelectorAll('td')].slice(0, 5).map((cell: any) => cell.textContent);
    expect(firstLine).toEqual(['A-1', 'Milk', '10', '4', 'ACTIVE']);
    expect(fixture.nativeElement.querySelector('tr.selected').textContent).toContain('RO-1');
  });

  it('keeps the last clicked order when an older response arrives late', async () => {
    await fixture.whenStable();
    const responses: any = { 'RO-1': new Subject<ReceivingOrder>(), 'RO-2': new Subject<ReceivingOrder>() };
    getReceivingOrder = (orderId: string) => responses[orderId];

    component.select(component.rows()[0]);
    component.select(component.rows()[1]);
    responses['RO-2'].next(orderWithLines('RO-2'));
    responses['RO-1'].next(orderWithLines('RO-1'));

    expect(component.details()?.orderId).toBe('RO-2');
  });

  it('reloads the articles of the selected order on refresh', async () => {
    await fixture.whenStable();
    component.select(component.rows()[0]);

    getReceivingOrder = (orderId: string) => of({ ...orderWithLines(orderId), status: 'FINISHED' });
    component.refresh();

    expect(component.details()?.status).toBe('FINISHED');
  });

  it('shows an error when the articles cannot be loaded', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => {});
    await fixture.whenStable();
    getReceivingOrder = () => throwError(() => ({ error: { detail: 'Cannot find order' } }));

    component.select(component.rows()[0]);

    expect(component.detailsError()).toBe('Cannot find order');
  });

  it('shows an error when the orders cannot be loaded', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => {});
    getRows = () => throwError(() => ({ status: 500 }));
    await fixture.whenStable();

    expect(component.error()).toBe('Could not load the receiving orders (status 500)');
  });
});
