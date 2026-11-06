import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DialogConfigService } from '../services/dialog-config.service';
import { PickingApi } from '../services/picking';

@Component({
  selector: 'app-retrieval-orders',
  imports: [],
  templateUrl: './retrieval-orders.html',
  styleUrls: ['../dialog-page/dialog-page.css', '../receiving-orders/receiving-orders.css'],
})
export class RetrievalOrders implements OnInit {
  private service = inject(DialogConfigService);
  private pickingApi = inject(PickingApi);

  orders = signal<any[]>([]);
  orderIdFilter = signal('');
  statusFilter = signal('');
  destinationFilter = signal('');
  error = signal('');

  selectedRow = signal<any | null>(null);
  details = signal<any | null>(null);
  detailsError = signal('');

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
  }

  loadOrders(): void {
    this.error.set('');
    this.service.getRows('RetrievalOrder').subscribe({
      next: (orders: any[]) => {
        this.orders.set([...orders].sort((a, b) => a['ID'] - b['ID']));
      },
      error: (err) => {
        console.error('Error loading retrieval orders', err);
        this.error.set('Could not load the retrieval orders (status ' + err.status + ')');
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

  onBack(): void {
    history.back();
  }
}
