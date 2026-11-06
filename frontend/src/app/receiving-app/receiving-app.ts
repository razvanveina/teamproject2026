import { Component, ElementRef, inject, signal, ViewChild } from '@angular/core';
import { PackageInfo, ReceivingOrder } from '../models/receiving-order.model';
import { ReceivingOrderService } from '../services/receiving-order-service';
import { HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { ScannedPackage } from '../models/receiving-order.model';

@Component({
  imports: [],
  selector: 'app-receiving-app',
  styleUrl: './receiving-app.css',
  templateUrl: './receiving-app.html',
})
export class ReceivingApp {
  receivingService = inject(ReceivingOrderService)
  private router = inject(Router)

  @ViewChild('barcodeInput')
  barcodeInput!: ElementRef<HTMLInputElement>;

  instructions = signal("Scan order barcode to start")
  state = ScanState.ORDER

  // the package scanned last, used while the user enters the quantity
  currentPackage: PackageInfo | null = null
  currentBarcode = ''
  scanned: ScannedPackage[] = []

  displayTable = signal(false)
  error = signal(false);
  order = signal<ReceivingOrder>({
    orderId: '',
    status: "",
    lines: []
  })

  onScan(input: any) {
    this.barcodeInput.nativeElement.value = '';

    if (this.state == ScanState.ORDER) {
      this.scanOrder(input)
    } else if (this.state == ScanState.ARTICLE) {
      this.scanPackage(input)
    } else if (this.state == ScanState.QUANTITY) {
      this.scanPackageQuantity(input)
    } else if (this.state == ScanState.FINISHED) {
      // Nothing to do here yet
    }
  }

  scanOrder(barcode: string) {
    this.receivingService.getReceivingOrder(barcode).subscribe({
      next: (order: ReceivingOrder) => {
        this.error.set(false)
        this.order.set(order)
        this.scanned = []

        if (!this.displayTable()) {
          this.displayTable.set(true)
        }

        if (this.order().status === "FINISHED") {
          this.instructions.set("Order Finished")
          this.state = ScanState.FINISHED;
        } else {
          this.instructions.set("Scan package")
          this.state = ScanState.ARTICLE
        }
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(true)
        if (this.displayTable())
          this.displayTable.set(false)
        this.instructions.set(err.error.detail ?? "Order " + barcode + " not found")
      }
    })
  }

  scanPackage(barcode: string) {
    this.receivingService.getPackage(barcode).subscribe({
      next: (pack: PackageInfo) => {
        let line = this.order().lines.find(l => l.barcode == pack.articleBarcode)

        if (!line) {
          this.error.set(true)
          this.instructions.set("The article of this package is not in the order")
          return
        }

        this.error.set(false)
        this.currentPackage = pack
        this.currentBarcode = barcode
        this.state = ScanState.QUANTITY
        this.instructions.set(pack.packagingUnit + " (" + pack.quantity + " pcs). Enter the number of packages")
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(true)
        this.instructions.set(err.error?.detail ?? "Could not find the package with barcode: " + barcode)
      }
    })
  }

  scanPackageQuantity(input: any) {
    let pack = this.currentPackage
    if (!pack) {
      return
    }

    let packages = Number(input)

    if (!Number.isInteger(packages) || packages <= 0) {
      this.error.set(true)
      this.instructions.set("Enter a whole number greater than 0")
      return
    }

    let localOrder = this.order()
    let line = localOrder.lines.find(l => l.barcode == pack.articleBarcode)

    if (!line) {
      this.error.set(true)
      this.instructions.set("The article of this package is not in the order")
      return
    }

    // the order and the received quantity are in pieces
    let pieces = packages * pack.quantity
    let remaining = line.quantity - line.receivedQuantity

    if (pieces > remaining) {
      this.error.set(true)
      this.instructions.set("Cannot add more than " + Math.floor(remaining / pack.quantity) + " x " + pack.packagingUnit
        + " (" + remaining + " pcs left)")
      return
    }

    line.receivedQuantity += pieces
    this.scanned.push({ packageBarcode: this.currentBarcode, packages })

    if (line.receivedQuantity == line.quantity) {
      line.status = "FINISHED"
    }

    if (localOrder.lines.every(line => line.status === "FINISHED")) {
      localOrder.status = "FINISHED"
      this.instructions.set("Order finished press confirm to confirm")
      this.state = ScanState.FINISHED
    } else {
      this.instructions.set("Scan package")
      this.state = ScanState.ARTICLE
    }

    this.currentPackage = null
    this.error.set(false)
    this.order.set(localOrder)
  }

  //onMakeLoadUnit() { }

  onConfirm() {
    // TODO check if order finished if not redirect user to make a loadUnit
    // TODO check if there is no line with the finished status set an error
    this.receivingService.finishOrder(this.order().orderId, this.scanned).subscribe({
      next: (order: ReceivingOrder) => {
        this.scanned = []
        this.error.set(false)
        this.instructions.set("Scan order barcode to start")
        this.state = ScanState.ORDER
        if (this.displayTable()) {
          this.displayTable.set(false)
        }
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(true)
        if (!this.displayTable())
          this.displayTable.set(true)
        this.instructions.set(err.error.detail)
      }
    })
  }

  onOrders() {
    this.router.navigate(['/receiving/orders'])
  }

  onCancel() {
    history.back();
  }
}

enum ScanState {
  ORDER, ARTICLE, QUANTITY, FINISHED
}