import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { TransportOrderApi, TransportOrderResponse } from '../services/transport-order';

@Component({
  imports: [],
  selector: 'app-transport-app',
  styleUrl: './transport-app.css',
  templateUrl: './transport-app.html',
})
export class TransportApp implements OnInit, OnDestroy {
  private readonly transportOrderApi = inject(TransportOrderApi);

  order = signal<TransportOrderResponse | null>(null);
  instructions = signal('No new orders');
  hasError = signal(false);

  private checker: ReturnType<typeof setInterval> | undefined;
  private checkingForOrder = false;

  ngOnInit(): void {
    this.checkForNewOrder();
    this.checker = setInterval(() => this.checkForNewOrder(), 10_000);
  }

  ngOnDestroy(): void {
    if (this.checker !== undefined) {
      clearInterval(this.checker);
    }
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
        } else {
          this.order.set(null);
          this.instructions.set('No new orders');
          this.hasError.set(false);
        }
      },
      error: () => {
        this.checkingForOrder = false;
        this.instructions.set('Could not load transport orders. Retrying in 10 seconds.');
        this.hasError.set(true);
      },
    });
  }

  private updateOrder(order: TransportOrderResponse): void {
    this.order.set(order);
    this.hasError.set(false);
    if (order.status === 'NEW') {
      this.instructions.set(`New transport order: go to ${order.sourceLocation} and scan pallet ${order.palletBarcode}.`);
    } else if (order.status === 'ACTIVE') {
      this.instructions.set(`Move the pallet to ${order.destinationLocation} and scan the location barcode.`);
    } else if (order.status === 'FINISHED') {
      this.instructions.set(`Pallet stored at ${order.destinationLocation}. Waiting for the next order...`);
    }
  }

  onCancel(): void {
    history.back();
  }
}
