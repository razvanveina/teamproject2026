import { Component, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { DecantingApi, DecantingItem, DecantingResponse } from '../services/decanting';
import { LogService } from '../services/log';

type Step = 'LOAD_UNIT' | 'ITEM' | 'QUANTITY';

@Component({
  imports: [],
  selector: 'app-decanting-app',
  styleUrl: '../transport-app/transport-app.css',
  templateUrl: './decanting-app.html',
})
export class DecantingApp {
  private readonly api = inject(DecantingApi);
  private readonly log = inject(LogService);

  step = signal<Step>('LOAD_UNIT');
  session = signal<DecantingResponse | null>(null);
  currentItem = signal<DecantingItem | null>(null);
  instructions = signal('Scan the barcode of the load unit to decant.');
  hasError = signal(false);

  placeholder(): string {
    switch (this.step()) {
      case 'LOAD_UNIT': return 'Scan load unit';
      case 'ITEM': return 'Scan item barcode';
      case 'QUANTITY': return 'Enter quantity';
    }
  }

  onScan(value: string): void {
    const input = value.trim();
    if (!input) {
      return;
    }
    switch (this.step()) {
      case 'LOAD_UNIT': this.scanLoadUnit(input); break;
      case 'ITEM': this.scanItem(input); break;
      case 'QUANTITY': this.enterQuantity(input); break;
    }
  }

  private scanLoadUnit(barcode: string): void {
    this.log.action(`DECANT LU ${barcode}`);
    this.api.start(barcode).subscribe({
      next: (session) => this.showNextItem(session),
      error: (err: HttpErrorResponse) => this.showError(err.error?.detail ?? 'Could not start decanting.'),
    });
  }

  private scanItem(barcode: string): void {
    const session = this.session();
    const item = session?.items.find((i) => i.packagingBarcode === barcode);
    if (!session || !item) {
      this.showError(`Item ${barcode} is not on load unit ${session?.loadUnitBarcode}. Scan another item.`);
      return;
    }

    this.currentItem.set(item);
    this.step.set('QUANTITY');
    this.hasError.set(false);
    this.instructions.set(`${item.description} (${item.packaging}): ${item.quantity} available. `
      + `How many do you move into tote ${session.toteBarcode}?`);
  }

  private enterQuantity(value: string): void {
    const session = this.session();
    const item = this.currentItem();
    if (!session || !item || !session.toteBarcode) {
      return;
    }

    const quantity = Number(value);
    if (!Number.isInteger(quantity) || quantity <= 0) {
      this.showError('Enter a whole number greater than 0.');
      return;
    }
    if (quantity > item.quantity) {
      this.showError(`Cannot move ${quantity}: only ${item.quantity} available. Enter a smaller quantity.`);
      return;
    }

    const tote = session.toteBarcode;
    this.log.action(`DECANT ${quantity} x ${item.packagingBarcode} -> TOTE ${tote}`);
    this.api.transfer(session.loadUnitBarcode, tote, item.packagingBarcode, quantity).subscribe({
      next: (next) => this.showNextItem(next, `Moved ${quantity} into tote ${tote}. `),
      error: (err: HttpErrorResponse) => this.showError(err.error?.detail ?? 'Transfer failed. Please try again.'),
    });
  }

  private showNextItem(session: DecantingResponse, done = ''): void {
    this.currentItem.set(null);
    this.hasError.set(false);

    if (session.finished) {
      this.session.set(null);
      this.step.set('LOAD_UNIT');
      this.instructions.set(`${done}Load unit ${session.loadUnitBarcode} is empty. Scan the next load unit.`);
      return;
    }

    this.session.set(session);
    this.step.set('ITEM');
    this.instructions.set(`${done}New tote ${session.toteBarcode}. `
      + `Scan an item from load unit ${session.loadUnitBarcode}.`);
  }

  private showError(message: string): void {
    this.instructions.set(message);
    this.hasError.set(true);
  }

  onCancel(): void {
    this.log.action('CANCEL');
    history.back();
  }
}