import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { TransportOrderApi, TransportOrderErrorReason, TransportOrderResponse } from '../services/transport-order';
import { LogService } from '../services/log';

@Component({
  imports: [],
  selector: 'app-tote-transport',
  styleUrl: './tote-transport.css',
  templateUrl: './tote-transport.html',
})
export class ToteTransport implements OnInit, OnDestroy {
  private readonly transportOrderApi = inject(TransportOrderApi);
  private readonly log = inject(LogService);

  order = signal<TransportOrderResponse | null>(null);
  instructions = signal('No new orders. Waiting for the next order...');
  hasError = signal(false);
  showErrorReasons = signal(false);
  selectedErrorReason = signal<TransportOrderErrorReason | null>(null);
  showOrderPicker = signal(false);
  selectedOrderId = signal<number | null>(null);
  openOrders = signal<TransportOrderResponse[]>([]);

  private readonly errorReasonLabels: Record<TransportOrderErrorReason, string> = {
    LOCATION_EMPTY: 'Pallet is missing',
    WRONG_PALLET: 'Another pallet is at the source location',
    TARGET_LOCATION_OCCUPIED: 'Location is already occupied by another pallet',
  };

  private checker: ReturnType<typeof setInterval> | undefined;
  private checkingForOrder = false;
  private noOrderTimer: ReturnType<typeof setTimeout> | undefined;

  ngOnInit(): void {
    this.checkForNewToteTransport();
    this.checker = setInterval(() => this.checkForNewToteTransport(), 10_000);
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

  errorReasons(): TransportOrderErrorReason[] {
    const status = this.order()?.status;
    if (status === 'NEW') {
      return ['LOCATION_EMPTY', 'WRONG_PALLET'];
    }
    if (status === 'ACTIVE') {
      return ['TARGET_LOCATION_OCCUPIED'];
    }
    return [];
  }

  errorReasonLabel(reason: TransportOrderErrorReason): string {
    return this.errorReasonLabels[reason];
  }

  openErrorPopup(): void {
    this.selectedErrorReason.set(null);
    this.showErrorReasons.set(true);
  }

  closeErrorPopup(): void {
    this.selectedErrorReason.set(null);
    this.showErrorReasons.set(false);
  }

  selectErrorReason(value: string): void {
    const reason = this.errorReasons().find((r) => r === value);
    this.selectedErrorReason.set(reason ?? null);
  }

  confirmErrorReason(): void {
    const reason = this.selectedErrorReason();
    if (reason) {
      this.onReportError(reason);
    }
  }

  canChooseOrder(): boolean {
    return this.order()?.status !== 'ACTIVE';
  }
  selectOrder(order: TransportOrderResponse): void {
    this.selectedOrderId.set(order.id);
  }

  confirmOrder(): void {
    const chosen = this.openOrders().find((o) => o.id === this.selectedOrderId());
    if (chosen) {
      this.chooseOrder(chosen);
    }
  }

  openOrderPicker(): void {
    this.transportOrderApi.findOpenOrders().subscribe({
      next: (orders) => {
        this.openOrders.set(orders);
        this.selectedOrderId.set(this.order()?.id ?? null);
        this.showOrderPicker.set(true);
      },
      error: (err: HttpErrorResponse) => {
        this.instructions.set(err.error?.detail ?? 'Could not load transport orders.');
        this.hasError.set(true);
      },
    });
  }

  closeOrderPicker(): void {
    this.showOrderPicker.set(false);
  }

  chooseOrder(order: TransportOrderResponse): void {
    this.log.action(`CHOOSE TO ${order.id}`);
    this.closeOrderPicker();
    this.updateOrder(order);
  }

  onReportError(reason: TransportOrderErrorReason): void {
    const order = this.order();
    if (!order || !this.canScan()) {
      return;
    }

    this.closeErrorPopup();
    this.log.action(`ERROR ${reason}`);
    this.transportOrderApi.reportError(order.id, reason).subscribe({
      next: (updatedOrder) => this.updateOrder(updatedOrder),
      error: (err: HttpErrorResponse) => {
        this.instructions.set(err.error?.detail ?? 'Could not report the error. Please try again.');
        this.hasError.set(true);
      },
    });
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

  private checkForNewToteTransport(): void {
    if (this.canScan() || this.checkingForOrder) {
      return;
    }

    this.checkingForOrder = true;
    this.transportOrderApi.findNextToteTransport().subscribe({
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
    this.showErrorReasons.set(false);

    if (order.status === 'FINISHED' || order.status === 'ERROR') {
      this.order.set(null);
      this.hasError.set(false);
      this.instructions.set(order.status === 'FINISHED'
        ? `Pallet stored at ${order.destinationLocation}. Waiting for the next order...`
        : `No new orders. Waiting for the next order...`);
      this.noOrderTimer = setTimeout(() => {
        if (!this.order() && !this.hasError()) {
          this.instructions.set('No new orders. Waiting for the next order...');
        }
      }, 10_000);
      this.checkForNewToteTransport();
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

// lu type tote
// location decatinig