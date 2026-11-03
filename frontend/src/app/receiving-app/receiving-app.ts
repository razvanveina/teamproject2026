import { Component, inject, signal } from '@angular/core';
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

  instructions = "Scan barcode to start testing to "
  state = ScanState.ORDER

  orderBarcode: string = ''
  articleBarcode: string = ''
  // articleQuantity: number = 0

  displayTable = signal(false)
  error = signal(false);
  order = signal<ReceivingOrder>({
    orderId: '',
    status: "",
    lines: []
  })

  onScan(input: any) {
    if (this.state == ScanState.ORDER) {
      this.scanOrder(input)
    } else if (this.state == ScanState.ARTICLE) {
      this.articleBarcode = input
      this.state = ScanState.QUANTITY
      this.instructions = "Enter article quantity"
    } else if (this.state == ScanState.QUANTITY) {
      this.scanArticle(this.orderBarcode, this.articleBarcode, input)
    }else if(this.state == ScanState.FINISHED){
      this.instructions = "DONE"
    }
  }

  scanOrder(barcode: string) {
    this.receivingService.getReceivingOrder(barcode).subscribe({
      next: (order: ReceivingOrder) => {
        this.error.set(false)
        this.order.set(order)
        this.orderBarcode = order.orderId
        if (!this.displayTable())
          this.displayTable.set(true)
        this.instructions = "Scan article"
        this.state = ScanState.ARTICLE
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(true)
        if (this.displayTable())
          this.displayTable.set(false)
        this.instructions = err.error.detail
      }
    })
  }

  scanArticle(order: string, article: string, quantity: number) {
    this.receivingService.addArticleQuantity(order, article, quantity).subscribe({
      next: (order: ReceivingOrder) => {
        this.error.set(false)
        this.order.set(order)

        if (!this.displayTable())
          this.displayTable.set(true)
        this.instructions = "Scan article"
        this.state = ScanState.ARTICLE

        if(order.status == "FINISHED")
          this.state = ScanState.FINISHED
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(true)
        if (this.displayTable())
          this.displayTable.set(false)
        this.instructions = err.error.detail
      }
    })
  }

  onCancel() {
    history.back();
  }
}

// TODO handle error on too many items added

enum ScanState {
  ORDER, ARTICLE, QUANTITY, FINISHED
}



// scan article
// enter qunaty
// until green
// create load uint
// pe locatia IN
// gaseste loc liber in depo
// transportOrder catre deposit