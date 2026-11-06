import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { PickingApi } from '../services/picking';
import { LogService } from '../services/log';

@Component({
  selector: 'app-picking-app',
  templateUrl: './picking-app.html',
  styleUrl: './picking-app.css',
})
export class PickingApp implements OnInit, OnDestroy {
  private api = inject(PickingApi);
  private log = inject(LogService);
  private router = inject(Router);

  order = signal<any>(null);
  instructions = signal('No order ready for picking. Please wait, the screen checks again every 10 seconds');
  level = signal('');
  box = signal('');
  line = signal<any>(null);
  waitingConfirm = signal(false);
  private checker: any;

  ngOnInit(): void {
    this.loadOrder();
    this.checker = setInterval(() => this.checkAgain(), 10000);
  }

  ngOnDestroy(): void {
    clearInterval(this.checker);
  }

  show(text: string, level: string): void {
    this.instructions.set(text);
    this.level.set(level);
  }

  checkAgain(): void {
    if (this.order() === null) {
      this.loadOrder();
    }
  }

  loadOrder(): void {
    this.api.findNextOrder().subscribe({
      next: (order: any) => {
        if (!order) {
          this.order.set(null);
          this.show('No order ready for picking. Please wait, the screen checks again every 10 seconds', '');
          return;
        }
        if (order.box && this.box() === '') {
          this.box.set(order.box);
        }
        if (this.box() === '') {
          this.order.set(order);
          this.show('Order ' + order.orderId + ': take an empty box and scan its barcode', '');
        } else {
          this.order.set(order);
          this.showNextLine();
        }
      },
      error: () => {
        this.show('Cannot reach the server. Try again in a moment', 'error');
      },
    });
  }

  takeText(line: any): string {
    return 'Go to ' + line.location + ', take ' + line.quantity + ' ' + line.description + ' and scan the article barcode';
  }

  showNextLine(): void {
    this.waitingConfirm.set(false);
    for (const line of this.order().lines) {
      if (line.status !== 'FINISHED') {
        this.line.set(line);
        this.show(this.takeText(line), 'ok');
        return;
      }
    }
    this.line.set(null);
    this.show('All articles are in box ' + this.box() + '. Scan the box barcode to close it', 'ok');
  }

  onScan(barcode: string): void {
    if (this.order() === null || barcode === '') {
      return;
    }
    if (this.box() === '') {
      this.scanBox(barcode);
    } else if (this.line() === null) {
      this.closeBox(barcode);
    } else {
      this.scanArticle(barcode);
    }
  }

  scanBox(barcode: string): void {
    this.api.createBox(this.order().orderId, barcode).subscribe({
      next: () => {
        this.box.set(barcode);
        this.showNextLine();
      },
      error: (err) => {
        if (err.status === 400) {
          this.show(err.error.message + '. Take another empty box and scan it', 'warning');
        } else {
          this.show('Cannot create the box. Try again in a moment', 'error');
        }
      },
    });
  }

  scanArticle(barcode: string): void {
    const line = this.line();
    if (this.waitingConfirm()) {
      this.show('First put ' + line.quantity + ' ' + line.description + ' into box ' + this.box() + ', then click the Confirm button', 'warning');
      return;
    }
    if (barcode !== line.barcode) {
      this.api.logError('wrong article ' + barcode + ' instead of ' + line.barcode + ' in box ' + this.box());
      this.show('Wrong article (' + barcode + '). ' + this.takeText(line), 'warning');
      return;
    }
    this.waitingConfirm.set(true);
    this.log.action('PICKING scan ' + barcode + ' for ' + line.quantity + ' ' + line.description);
    this.show('Put all ' + line.quantity + ' ' + line.description + ' into box ' + this.box() + ', then click the Confirm button', 'ok');
  }

  onConfirm(): void {
    const line = this.line();
    this.api.moveLineToBox(this.order().orderId, line.barcode, this.box()).subscribe({
      next: () => {
        this.loadOrder();
      },
      error: () => {
        this.show('Cannot save ' + line.description + '. Press Confirm again', 'error');
      },
    });
  }

  closeBox(barcode: string): void {
    if (barcode !== this.box()) {
      this.api.logError('wrong box ' + barcode + ' instead of ' + this.box());
      this.show('Wrong barcode (' + barcode + '). Scan box ' + this.box() + ' to close it', 'warning');
      return;
    }
    this.api.closeBox(this.order().orderId, this.box()).subscribe({
      next: () => {
        this.show('Box ' + this.box() + ' is closed and goes to OUT. Put it aside, the next order comes in a few seconds', 'ok');
        this.order.set(null);
        this.box.set('');
      },
      error: (err) => {
        if (err.status === 400) {
          this.show(err.error.message, 'warning');
        } else {
          this.show('Cannot close the box. Scan it again', 'error');
        }
      },
    });
  }

  onOrders(): void {
    this.router.navigate(['/picking/orders']);
  }

  onCancel(): void {
    this.log.action('PICKING cancel');
    history.back();
  }
}
