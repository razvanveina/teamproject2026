import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
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
  private route = inject(ActivatedRoute);
  private checker: any;
  private leaving = '';
  private targetLeaving = '';

  station = signal(1);
  stations = signal<number[]>([1]);

  source = signal<any>(null);
  target = signal<any>(null);
  orderId = signal<string | null>(null);
  message = signal('');

  ngOnInit(): void {
    const requested = Number(this.route.snapshot.queryParamMap.get('station'));
    if (Number.isInteger(requested) && requested > 0) {
      this.station.set(requested);
    }
    this.api.getStationCount().subscribe({
      next: (count: number) => this.stations.set(Array.from({ length: count }, (_, i) => i + 1)),
    });
    this.refresh();
    this.checker = setInterval(() => this.refresh(), 1000);
  }

  ngOnDestroy(): void {
    clearInterval(this.checker);
  }

  selectStation(station: number): void {
    this.station.set(station);
    this.leaving = '';
    this.targetLeaving = '';
    this.message.set('');
    this.source.set(null);
    this.target.set(null);
    this.orderId.set(null);
    this.refresh();
  }

  refresh(): void {
    const zone = this.station();
    this.api.getStation(zone).subscribe({
      next: (station: any) => {
        if (zone !== this.station()) {
          return;
        }
        let source = station.source;
        if (source !== null && source.barcode === this.leaving) {
          source = null;
        } else {
          this.leaving = '';
        }
        let target = station.target;
        if (target !== null && target.barcode === this.targetLeaving) {
          target = null;
        } else {
          this.targetLeaving = '';
        }
        this.source.set(source);
        this.target.set(target);
        this.orderId.set(station.orderId);
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
      if (item.orderId !== null && item.orderId === this.orderId()) {
        items.push(item);
      }
    }
    return items;
  }

  instruction(): string {
    if (this.target() === null) {
      return 'Waiting for an empty target tote at PTTT' + this.station();
    }
    if (this.source() === null) {
      return this.message() + ' Waiting for the next source tote at PTTS' + this.station();
    }
    const items = this.toMove();
    if (items.length === 0) {
      return 'Tote ' + this.source().barcode + ' has nothing to pick here. Press Confirm to send it on';
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
    const zone = this.station();
    this.log.action('SIM PICKING zone ' + zone + ' confirm ' + source.barcode + ' into ' + target.barcode);
    this.api.confirm(zone, source.barcode, target.barcode).subscribe({
      next: (res: any) => {
        if (zone !== this.station()) {
          return;
        }
        this.message.set(res.message + '.');
        this.leaving = source.barcode;
        this.source.set(null);
        if (res.message.indexOf('goes to OUT') >= 0) {
          this.targetLeaving = target.barcode;
          this.target.set(null);
        }
      },
    });
  }

  onCancel(): void {
    history.back();
  }
}