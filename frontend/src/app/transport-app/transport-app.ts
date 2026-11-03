import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { TransportOrderApi, TransportOrderResponse } from '../services/transport-order';
import { LogService } from '../services/log';

@Component({
  imports: [],
  selector: 'app-transport-app',
  styleUrl: './transport-app.css',
  templateUrl: './transport-app.html',
})
export class TransportApp implements OnInit, OnDestroy {
  private readonly transportOrderApi = inject(TransportOrderApi);
  private readonly log = inject(LogService);

  order = signal<TransportOrderResponse | null>(null);
  instructions = signal('No new orders. Waiting for the next order...');
  hasError = signal(false);

  private checker: ReturnType<typeof setInterval> | undefined;
  private checkingForOrder = false;
  private noOrderTimer: ReturnType<typeof setTimeout> | undefined;

  ngOnInit(): void {
    this.checkForNewOrder();
    this.checker = setInterval(() => this.checkForNewOrder(), 10_000);
  }

  ngOnDestroy(): void {
    if (this.checker !== undefined) {
      clearInterval(this.checker);
    }
    clearTimeout(this.noOrderTimer);
  }

  canScan(): boolean {
    const order = this.order();
    return order !== null && (order.status === 'NEW' || order.status === 'ACTIVE');
  }

  onScan(barcode: string): void {
    const order = this.order();
    const scanned = barcode.trim();
    if (!order || !this.canScan() || !scanned) {
      return;
    }

    this.transportOrderApi.scan(order.id, scanned).subscribe({
      next: (updatedOrder) => this.updateOrder(updatedOrder),
      error: (err: HttpErrorResponse) => {
        this.instructions.set(err.error?.detail ?? 'Scan failed. Please try again.');
        this.hasError.set(true);
      },
    });
  }

  private checkForNewOrder(): void {
    if (this.canScan() || this.checkingForOrder) {
      return;
    }

    this.checkingForOrder = true;
    this.transportOrderApi.findNextOrder().subscribe({
      next: (nextOrder) => {
        this.checkingForOrder = false;
        if (nextOrder) {
          this.updateOrder(nextOrder);
        } else if (this.order() || this.hasError()) {
          this.showNoOrders();
        }
      },
      error: () => {
        this.checkingForOrder = false;
        this.instructions.set('Token expired.');
        this.hasError.set(true);
      },
    });
  }

  private updateOrder(order: TransportOrderResponse): void {
    clearTimeout(this.noOrderTimer);

    if (order.status === 'FINISHED') {
      this.order.set(null);
      this.hasError.set(false);
      this.instructions.set(`Pallet stored at ${order.destinationLocation}. Waiting for the next order...`);
      this.noOrderTimer = setTimeout(() => {
        if (!this.order() && !this.hasError()) {
          this.instructions.set('No new orders. Waiting for the next order...');
        }
      }, 10_000);
      this.checkForNewOrder();
      return;
    }

    this.order.set(order);
    this.hasError.set(false);
    if (order.status === 'NEW') {
      this.instructions.set(`New transport order: go to ${order.sourceLocation} and scan pallet ${order.palletBarcode}.`);
    } else if (order.status === 'ACTIVE') {
      this.instructions.set(`Move the pallet to ${order.destinationLocation} and scan the location barcode.`);
    }
  }

  private showNoOrders(): void {
    this.order.set(null);
    this.instructions.set('No new orders');
    this.hasError.set(false);
  }

  onCancel(): void {
    this.log.action('CANCEL');
    history.back();
  }
}
