import { Component, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { SimulatorService } from '../simulator-service';
import { LogService } from '../../services/log';
import { OrderCheck } from '../models/order-check-model';

@Component({
  selector: 'app-sim-testing',
  styleUrl: './sim-testing.css',
  templateUrl: './sim-testing.html',
})
export class SimTesting {
  private log = inject(LogService);
  private service = inject(SimulatorService);

  statusMessage = signal('');
  isError = signal(false);
  orders = signal<OrderCheck[]>([]);
  ordersError = signal(false);
  selectedOrderId = signal('');

  // tabelul se reincarca la 2 s, cat dureaza o tura a proceselor din backend
  private ordersTimer: ReturnType<typeof setInterval> | undefined;

  ngOnInit(): void {
    this.loadOrders();
    this.ordersTimer = setInterval(() => this.loadOrders(), 2000);
  }

  ngOnDestroy(): void {
    clearInterval(this.ordersTimer);
  }

  loadOrders(): void {
    this.service.checkRetrievalOrders().subscribe({
      next: response => {
        this.orders.set(response);
        this.ordersError.set(false);
      },
      error: () => this.ordersError.set(true)
    });
  }

  select(order: OrderCheck): void {
    this.selectedOrderId.set(order.orderId);
  }

  // orderul selectat, cautat din nou dupa fiecare reincarcare a tabelului
  selectedOrder(): OrderCheck | null {
    for (const order of this.orders()) {
      if (order.orderId === this.selectedOrderId()) {
        return order;
      }
    }
    return null;
  }

  missingArticles(order: OrderCheck): number {
    let count = 0;
    for (const line of order.lines) {
      if (line.missing > 0) {
        count++;
      }
    }
    return count;
  }

  countByResult(result: string): number {
    let count = 0;
    for (const order of this.orders()) {
      if (order.result === result) {
        count++;
      }
    }
    return count;
  }

  createReceivingOrder(): void {
    this.log.action('User created a test receiving order file');
    this.service.createReceivingOrderFile().subscribe({
      next: response => this.showSuccess(`${response.fileName} created in host/in`),
      error: (err: HttpErrorResponse) => this.showError(err)
    });
  }

  createRetrievalOrder(): void {
    this.log.action('User created a test retrieval order file');
    this.service.createRetrievalOrderFile().subscribe({
      next: response => this.showSuccess(`${response.fileName} created in host/out`),
      error: (err: HttpErrorResponse) => this.showError(err)
    });
  }

  private showSuccess(message: string): void {
    this.isError.set(false);
    this.statusMessage.set(message);
  }

  private showError(err: HttpErrorResponse): void {
    this.isError.set(true);
    this.statusMessage.set(err.error?.message ?? 'File could not be created');
  }
}
