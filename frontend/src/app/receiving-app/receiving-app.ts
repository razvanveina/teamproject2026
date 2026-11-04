import { Component, ElementRef, inject, signal, ViewChild } from '@angular/core';
import { ReceivingOrder } from '../models/receiving-order.model';
import { ReceivingOrderService } from '../services/receiving-order-service';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
// import { httpErrorMessage } from '../services/http-error-message';

@Component({
  imports: [],
  selector: 'app-receiving-app',
  styleUrl: './receiving-app.css',
  templateUrl: './receiving-app.html',
})
export class ReceivingApp {
  receivingService = inject(ReceivingOrderService)

  @ViewChild('barcodeInput')
  barcodeInput!: ElementRef<HTMLInputElement>;

  instructions = "Scan order barcode to start"
  state = ScanState.ORDER

  articleBarcode: string = ''

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
      this.scanArticleBarcode(input)
    } else if (this.state == ScanState.QUANTITY) {
      this.scanArticleQuantity(this.articleBarcode, input)
    } else if (this.state == ScanState.FINISHED) {
      // Nothing to do here yet
    }
  }

  scanOrder(barcode: string) {
    this.receivingService.getReceivingOrder(barcode).subscribe({
      next: (order: ReceivingOrder) => {
        this.error.set(false)
        this.order.set(order)

        if (!this.displayTable()) {
          this.displayTable.set(true)
        }

        if (this.order().status === "FINISHED") {
          this.instructions = "Order Finished"
          this.state = ScanState.FINISHED;
        } else {
          this.instructions = "Scan article"
          this.state = ScanState.ARTICLE
        }
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(true)
        if (this.displayTable())
          this.displayTable.set(false)
        this.instructions = err.error.detail
      }
    })
  }

  scanArticleBarcode(barcode: string) {
    let localOrder = this.order()
    let line = localOrder.lines.find(l => l.barcode == barcode)

    if (line) {
      this.error.set(false)
      this.articleBarcode = barcode
      this.state = ScanState.QUANTITY
      this.instructions = "Enter article quantity"
    } else {
      this.error.set(true)
      this.instructions = "Could not find article with the barcode: " + barcode
    }
  }

  scanArticleQuantity(article: string, quantity: number) {
    quantity = Number(quantity)

    if (quantity < 0) {
      this.error.set(true)
      this.instructions = "Cannot add a negative value."
      return
    }

    let localOrder = this.order()

    let line = localOrder.lines.find(l => l.barcode == article)

    if (line) {
      if (line.quantity < line.receivedQuantity + quantity) {
        this.error.set(false)
        this.instructions = "Cannot add more than " + (line.quantity - line.receivedQuantity) + " items"
        return
      } else {
        line.receivedQuantity += quantity
      }
    } else {
      this.error.set(true)
      this.instructions = "Could not find article with the barcode: " + article
      return
    }

    if (line.receivedQuantity == line.quantity) {
      line.status = "FINISHED"
    }

    if (localOrder.lines.every(line => line.status === "FINISHED")) {
      localOrder.status = "FINISHED"
      this.instructions = "Order finished press confirm to confirm"
      this.state = ScanState.FINISHED
    } else {
      this.instructions = "Scan Article"
      this.state = ScanState.ARTICLE
    }

    this.error.set(false)
    this.order.set(localOrder)
  }

  onMakeLoadUnit() { }

  onConfirm() { 
    // TODO check if order finished if not redirect user to make a loadUnit
    // TODO check if there is no line with the finished status set an error
    this.receivingService.finishOrder(this.order()).subscribe({
      next: (order: ReceivingOrder) => {
        this.error.set(false)
        this.instructions = "Scan order barcode to start"
        this.state = ScanState.ORDER
        if (this.displayTable()) {
          this.displayTable.set(false)
        }
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(true)
        if (!this.displayTable())
          this.displayTable.set(true)
        this.instructions = err.error.detail
      }
    })
  }

  onCancel() {
    history.back();
  }
}

enum ScanState {
  ORDER, ARTICLE, QUANTITY, FINISHED
}