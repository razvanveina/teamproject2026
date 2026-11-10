import { Component, ElementRef, signal, ViewChild } from '@angular/core';

@Component({
  imports: [],
  selector: 'app-sim-picking',
  styleUrl: './sim-picking.css',
  templateUrl: './sim-picking.html',
})
export class SimPicking {
  @ViewChild('barcodeInput')
  barcodeInput!: ElementRef<HTMLInputElement>;

  instructions = signal("Scan order barcode to start")
  displayTable = signal(true);
  error = signal(false);

  order = signal({ orderId: "", status: "" })

  onScan(input: any) {
    this.barcodeInput.nativeElement.value = '';

    alert("not implemented");
  }

  onConfirm() {
    alert("not implemented");
  }

  onCancel() {
    history.back();
  }
}