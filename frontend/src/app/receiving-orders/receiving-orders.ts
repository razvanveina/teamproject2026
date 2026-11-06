import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { forkJoin } from 'rxjs';
import { ReceivingOrder } from '../models/receiving-order.model';
import { DialogConfigService } from '../services/dialog-config.service';
import { ReceivingOrderService } from '../services/receiving-order-service';

const PAGE_SIZE = 20;

@Component({
  selector: 'app-receiving-orders',
  imports: [],
  templateUrl: './receiving-orders.html',
  styleUrls: ['../dialog-page/dialog-page.css', './receiving-orders.css'],
})
export class ReceivingOrders implements OnInit {
  private service = inject(DialogConfigService);
  private receivingService = inject(ReceivingOrderService);

  orders = signal<any[]>([]);
  orderIdFilter = signal('');
  statusFilter = signal('');
  page = signal(0);
  error = signal('');

  selectedRow = signal<any | null>(null);
  details = signal<ReceivingOrder | null>(null);
  detailsError = signal('');

  filteredOrders = computed(() => {
    const orderId = this.orderIdFilter().toUpperCase();
    const status = this.statusFilter().toUpperCase();
    return this.orders().filter(
      (order) => order['ORDER_ID'].toUpperCase().includes(orderId) && order['STATUS'].includes(status),
    );
  });

  pages = computed(() => Math.max(1, Math.ceil(this.filteredOrders().length / PAGE_SIZE)));

  rows = computed(() => {
    const start = this.page() * PAGE_SIZE;
    return this.filteredOrders().slice(start, start + PAGE_SIZE);
  });

  ngOnInit(): void {
    this.loadOrders();
  }

  loadOrders(): void {
    this.error.set('');
    forkJoin({
      newOrders: this.service.getRows('ReceivingOrder', { STATUS: 'NEW' }),
      activeOrders: this.service.getRows('ReceivingOrder', { STATUS: 'ACTIVE' }),
      finishedOrders: this.service.getRows('ReceivingOrder', { STATUS: 'FINISHED' }),
    }).subscribe({
      next: ({ newOrders, activeOrders, finishedOrders }) => {
        this.orders.set([...newOrders, ...activeOrders, ...finishedOrders].sort((a, b) => a['ID'] - b['ID']));
        this.page.set(Math.min(this.page(), this.pages() - 1));
      },
      error: (err) => {
        console.error('Error loading receiving orders', err);
        this.error.set('Could not load the receiving orders (status ' + err.status + ')');
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
    this.receivingService.getReceivingOrder(row['ORDER_ID']).subscribe({
      next: (order: ReceivingOrder) => {
        if (this.selectedRow()?.['ID'] === row['ID']) {
          this.details.set(order);
        }
      },
      error: (err) => {
        console.error('Error loading receiving order ' + row['ORDER_ID'], err);
        if (this.selectedRow()?.['ID'] === row['ID']) {
          this.detailsError.set(err.error?.detail ?? 'Could not load the articles of order ' + row['ORDER_ID']);
        }
      },
    });
  }

  setOrderIdFilter(event: any): void {
    this.orderIdFilter.set(event.target.value);
    this.page.set(0);
  }

  setStatusFilter(event: any): void {
    this.statusFilter.set(event.target.value);
    this.page.set(0);
  }

  previousPage(): void {
    if (this.page() > 0) {
      this.page.set(this.page() - 1);
    }
  }

  nextPage(): void {
    if (this.page() < this.pages() - 1) {
      this.page.set(this.page() + 1);
    }
  }

  onBack(): void {
    history.back();
  }
}
