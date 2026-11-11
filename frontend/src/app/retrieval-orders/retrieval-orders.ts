import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DialogConfigService } from '../services/dialog-config.service';
import { PickingApi } from '../services/picking';

@Component({
  selector: 'app-retrieval-orders',
  imports: [],
  templateUrl: './retrieval-orders.html',
  styleUrls: ['../stock/stock.css', '../dialog-page/dialog-page.css', './retrieval-orders.css'],
})
export class RetrievalOrders implements OnInit {
  private service = inject(DialogConfigService);
  private pickingApi = inject(PickingApi);

  orders = signal<any[]>([]);
  destinations = signal<any[]>([]);
  articles = signal<any[]>([]);
  orderIdFilter = signal('');
  statusFilter = signal('');
  destinationFilter = signal('');
  error = signal('');
  choicesError = signal('');

  selectedRow = signal<any | null>(null);
  details = signal<any | null>(null);
  detailsError = signal('');
  showOrderForm = signal(false);
  showLineForm = signal(false);
  orderDraft = signal({ ORDER_ID: '', DESTINATION_ID: '' as number | '' });
  lineDraft = signal({ ARTICLE_ID: '' as number | '', quantity: 1 });

  rows = computed(() => {
    const orderId = this.orderIdFilter().toUpperCase();
    const status = this.statusFilter().toUpperCase();
    const destination = this.destinationFilter().toUpperCase();
    return this.orders().filter(
      (order) =>
        order['ORDER_ID'].toUpperCase().includes(orderId) &&
        order['STATUS'].includes(status) &&
        order['DESTINATION_ID__NAME'].toUpperCase().includes(destination),
    );
  });

  ngOnInit(): void {
    this.loadOrders();
    this.loadChoices();
  }

  loadOrders(selectOrderId?: string): void {
    this.error.set('');
    this.service.getRows('RetrievalOrder').subscribe({
      next: (orders: any[]) => {
        const sortedOrders = [...orders].sort((a, b) => a['ID'] - b['ID']);
        this.orders.set(sortedOrders);
        if (selectOrderId) {
          const createdOrder = sortedOrders.find((row) => row['ORDER_ID'] === selectOrderId);
          if (createdOrder) {
            this.select(createdOrder);
          }
        }
      },
      error: (err) => {
        console.error('Error loading retrieval orders', err);
        this.error.set('Could not load the retrieval orders (status ' + err.status + ')');
      },
    });
  }

  loadChoices(): void {
    this.choicesError.set('');
    this.service.getRows('Destination').subscribe({
      next: (destinations: any[]) => this.destinations.set(destinations),
      error: (err) => {
        console.error('Error loading retrieval order destinations', err);
        this.choicesError.set('Could not load available destinations.');
      },
    });
    this.service.getRows('Article').subscribe({
      next: (articles: any[]) => this.articles.set(articles),
      error: (err) => {
        console.error('Error loading articles for retrieval order lines', err);
        this.choicesError.set('Could not load available articles.');
      },
    });
  }

  refresh(): void {
    this.loadOrders();
    this.loadDetails();
  }

  select(row: any): void {
    this.selectedRow.set(row);
    this.details.set(null);
    this.showLineForm.set(false);
    this.loadDetails();
  }

  loadDetails(): void {
    const row = this.selectedRow();
    if (row === null) {
      return;
    }

    this.detailsError.set('');
    this.pickingApi.getOrder(row['ORDER_ID']).subscribe({
      next: (order: any) => {
        if (this.selectedRow()?.['ID'] === row['ID']) {
          this.details.set(order);
        }
      },
      error: (err) => {
        console.error('Error loading retrieval order ' + row['ORDER_ID'], err);
        if (this.selectedRow()?.['ID'] === row['ID']) {
          this.detailsError.set(err.error?.detail ?? 'Could not load the articles of order ' + row['ORDER_ID']);
        }
      },
    });
  }

  setOrderIdFilter(event: any): void {
    this.orderIdFilter.set(event.target.value);
  }

  setStatusFilter(event: any): void {
    this.statusFilter.set(event.target.value);
  }

  setDestinationFilter(event: any): void {
    this.destinationFilter.set(event.target.value);
  }

  updateOrderDraft(field: 'ORDER_ID' | 'DESTINATION_ID', value: string): void {
    if (field === 'DESTINATION_ID') {
      this.orderDraft.update((draft) => ({ ...draft, DESTINATION_ID: value ? Number(value) : '' }));
    } else {
      this.orderDraft.update((draft) => ({ ...draft, ORDER_ID: value }));
    }
  }

  toggleOrderForm(): void {
    this.showOrderForm.update((show) => !show);
    this.error.set('');
  }

  toggleLineForm(): void {
    this.showLineForm.update((show) => !show);
    this.detailsError.set('');
  }

  addOrder(): void {
    const value = this.orderDraft();
    const orderId = value.ORDER_ID.trim();
    if (!orderId || value.DESTINATION_ID === '') {
      this.error.set('Order ID and destination are required.');
      return;
    }

    this.error.set('');
    this.service.addRow('RetrievalOrder', {
      ORDER_ID: orderId,
      STATUS: 'NEW',
      DESTINATION_ID: value.DESTINATION_ID,
      AUTOMATIC: 0,
    }).subscribe({
      next: () => {
        this.orderDraft.set({ ORDER_ID: '', DESTINATION_ID: '' });
        this.showOrderForm.set(false);
        this.loadOrders(orderId);
      },
      error: (err) => {
        console.error('Error adding retrieval order ' + orderId, err);
        this.error.set(err.error?.message ?? 'Could not add retrieval order (status ' + err.status + ').');
      },
    });
  }

  updateLineDraft(field: 'ARTICLE_ID' | 'quantity', value: string): void {
    const nextValue = field === 'ARTICLE_ID'
      ? (value ? Number(value) : '')
      : Math.max(1, Number(value) || 1);
    this.lineDraft.update((draft) => ({
      ...draft,
      [field]: nextValue,
    }));
  }

  addLine(): void {
    const selected = this.selectedRow();
    if (selected === null) {
      return;
    }

    const line = this.lineDraft();
    if (line.ARTICLE_ID === '') {
      this.detailsError.set('Choose an article from the list.');
      return;
    }

    this.detailsError.set('');
    this.service.addRow('RetrievalOrderLine', {
      ARTICLE_ID: line.ARTICLE_ID,
      QUANTITY: line.quantity,
      ORDER_ID: selected['ID'],
      STATUS: 'NEW',
    }).subscribe({
      next: () => {
        this.lineDraft.set({ ARTICLE_ID: '', quantity: 1 });
        this.showLineForm.set(false);
        this.loadDetails();
      },
      error: (err) => {
        console.error('Error adding line to retrieval order ' + selected['ORDER_ID'], err);
        this.detailsError.set(err.error?.message ?? 'Could not add article to this order (status ' + err.status + ').');
      },
    });
  }

  onBack(): void {
    history.back();
  }
}
