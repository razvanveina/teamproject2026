import { Component } from '@angular/core';
import { OrderLineStatus, OrderStatus } from '../models/order.model';

@Component({
  imports: [],
  selector: 'app-receiving-app',
  styleUrl: './receiving-app.css',
  templateUrl: './receiving-app.html',
})
export class ReceivingApp {
  instructions = "Scan barcode to start"
  barcode = ''
  orderId = ''
  orderStatus = ''

  onScan(barcode: string) {
    if(this.checkBarcode(barcode)){

    }
  }

  checkBarcode(barcode: string) {
    if(barcode == "123") {
      return {
        orderId: "123",
        status: OrderStatus.NEw,
        lines: [
          {article: "Mere", quantity: 10, receivedQuantity: 0, status: OrderLineStatus.NEw},
          {article: "Pere", quantity: 15, receivedQuantity: 0, status: OrderLineStatus.NEw}
        ]
      }
    } else if(barcode == "1234") {
      
    }
    return null; 
  }

  onCancel() {
    history.back();
  }
}