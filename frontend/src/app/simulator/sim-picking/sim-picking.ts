import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { SimPickingApi } from '../../services/sim-picking';
import { LogService } from '../../services/log';

@Component({
  selector: 'app-sim-picking',
  templateUrl: './sim-picking.html',
  styleUrl: './sim-picking.css',
})
export class SimPicking implements OnInit, OnDestroy {
  private api = inject(SimPickingApi);
  private log = inject(LogService);
  private checker: any;
  private leaving = '';

  source = signal<any>(null);
  target = signal<any>(null);
  message = signal('');

  ngOnInit(): void {
    this.refresh();
    this.checker = setInterval(() => this.refresh(), 1000);
  }

  ngOnDestroy(): void {
    clearInterval(this.checker);
  }

  refresh(): void {
    this.api.getStation().subscribe({
      next: (station: any) => {
        let source = station.source;
        if (source !== null && source.barcode === this.leaving) {
          source = null;
        } else {
          this.leaving = '';
        }
        this.source.set(source);
        this.target.set(station.target);
      },
    });
  }

  amountText(item: any): string {
    if (item.piecesPerBox === 1) {
      return item.quantity + ' pieces';
    }
    return item.quantity + ' box(es) of ' + item.piecesPerBox;
  }

  toMove(): any[] {
    const items: any[] = [];
    for (const item of this.source().items) {
      if (item.orderId !== null) {
        items.push(item);
      }
    }
    return items;
  }

  instruction(): string {
    if (this.target() === null) {
      return 'Waiting for an empty target tote at PTTT1';
    }
    if (this.source() === null) {
      return this.message() + ' Waiting for the next source tote at PTTS1';
    }
    const items = this.toMove();
    if (items.length === 0) {
      return 'Tote ' + this.source().barcode + ' has nothing for an order. Press Confirm to send it on';
    }
    let text = '';
    for (const item of items) {
      if (text !== '') {
        text = text + ' and ';
      }
      text = text + this.amountText(item) + ' ' + item.article;
    }
    return 'Move ' + text + ' from tote ' + this.source().barcode + ' to tote ' + this.target().barcode + ', then press Confirm';
  }

  onConfirm(): void {
    const source = this.source();
    const target = this.target();
    this.log.action('SIM PICKING confirm ' + source.barcode + ' into ' + target.barcode);
    this.api.confirm(source.barcode, target.barcode).subscribe({
      next: (res: any) => {
        this.message.set(res.message + '.');
        this.leaving = source.barcode;
        this.source.set(null);
      },
    });
  }

  onCancel(): void {
    history.back();
  }
}