import { HttpErrorResponse } from '@angular/common/http';
import { Component, ElementRef, inject, signal, ViewChild } from '@angular/core';
import { ShippingService } from '../services/shipping-out-service';
import { LoadUnit } from '../models/load-unit-model';

@Component({
  imports: [],
  selector: 'app-shipping',
  styleUrl: './shipping.css',
  templateUrl: './shipping.html',
})
export class Shipping {
  shippingService = inject(ShippingService)

  @ViewChild('barcodeInput')
  barcodeInput!: ElementRef<HTMLInputElement>;

  instructions = "Scan packaging barcode to start"

  displayTable = signal(false)
  confirmDisabled = signal(true)
  error = signal(false);
  loadUnit = signal<LoadUnit>({
    barcode: '',
    loadUnitType: '',
    location: '',
    items: []
  })

  onScan(barcode: string) {
    this.barcodeInput.nativeElement.value = '';

    this.shippingService.getLoadUnit(barcode).subscribe({
      next: (loadUnit: any) => {
        this.error.set(false)
        this.loadUnit.set(loadUnit)
        this.confirmDisabled.set(false)
        this.instructions = "Confirm the status of the package and press confirm"
        // TODO check location

        if (!this.displayTable()) {
          this.displayTable.set(true)
        }
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(true)
        this.instructions = err.error.detail
        if (this.displayTable()){
          this.displayTable.set(false)
        }
        this.confirmDisabled.set(true)
      }
    })
  }

  onConfirm() {
    this.shippingService.finishOrder(this.loadUnit().barcode).subscribe({
      next: (loadUnit: LoadUnit) => {
        this.error.set(false)
        this.instructions = "Scan packaging barcode to start"
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