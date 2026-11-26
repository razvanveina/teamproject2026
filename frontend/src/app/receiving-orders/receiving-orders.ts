import { Component, OnInit, inject, signal } from '@angular/core';
import { ReceivingOrder } from '../models/receiving-order.model';
import { DialogConfigService } from '../services/dialog-config.service';
import { ReceivingOrderService } from '../services/receiving-order-service';

@Component({
  selector: 'app-receiving-orders',
  imports: [],
  templateUrl: './receiving-orders.html',
  styleUrls: ['../dialog-page/dialog-page.css', './receiving-orders.css'],
})
export class ReceivingOrders implements OnInit {
  private service = inject(DialogConfigService);
  private receivingService = inject(ReceivingOrderService);

  rows = signal<any[]>([]);
  orderIdFilter = signal('');
  statusFilter = signal('');
  page = signal(0);
  pages = signal(1);
  error = signal('');

  selectedRow = signal<any | null>(null);
  details = signal<ReceivingOrder | null>(null);
  detailsError = signal('');

  ngOnInit(): void {
    this.loadOrders();
  }

  loadOrders(): void {
    this.error.set('');
    const params = { ORDER_ID: this.orderIdFilter(), STATUS: this.statusFilter(), page: this.page() };
    this.service.getRows('ReceivingOrder', params).subscribe({
      next: (rows: any[]) => {
        this.rows.set(rows);
        if (rows.length > 0) {
          this.pages.set(Math.ceil(rows[0]['TOTAL_ROWS'] / 20));
        } else {
          this.pages.set(1);
        }
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
    this.loadOrders();
  }

  setStatusFilter(event: any): void {
    this.statusFilter.set(event.target.value);
    this.page.set(0);
    this.loadOrders();
  }

  previousPage(): void {
    if (this.page() > 0) {
      this.page.set(this.page() - 1);
      this.loadOrders();
    }
  }

  nextPage(): void {
    if (this.page() < this.pages() - 1) {
      this.page.set(this.page() + 1);
      this.loadOrders();
    }
  }

  onBack(): void {
    history.back();
  }
}
