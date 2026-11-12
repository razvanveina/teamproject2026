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
  carton = signal('');
  loadUnit = signal<LoadUnit>({
    barcode: '',
    loadUnitType: '',
    location: '',
    items: []
  })

  onScan(barcode: string) {
    this.barcodeInput.nativeElement.value = '';
    const current = this.loadUnit()
    if (current.loadUnitType === 'TOTE' && !this.carton()) {
      this.scanCarton(current.barcode, barcode)
      return
    }

    this.shippingService.getLoadUnit(barcode).subscribe({
      next: (loadUnit: any) => {
        this.error.set(false)
        this.loadUnit.set(loadUnit)
        this.carton.set('')
        if (loadUnit.loadUnitType === 'TOTE') {
          this.confirmDisabled.set(true)
          this.instructions = "Scan an empty carton or a new carton barcode"
        } else {
          this.confirmDisabled.set(false)
          this.instructions = "Confirm the status of the package and press confirm"
        }
        // TODO check location

        if (!this.displayTable()) {
          this.displayTable.set(true)
        }
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(true)
        this.instructions = err.error?.detail ?? err.error?.message ?? 'Something went wrong. Please try again.'
        if (this.displayTable()) {
          this.displayTable.set(false)
        }
        this.confirmDisabled.set(true)
      }
    })
  }

  onConfirm() {
    this.shippingService.finishOrder(this.loadUnit().barcode, this.carton() || undefined).subscribe({
      next: (loadUnit: LoadUnit) => {
        this.error.set(false)
        this.instructions = "Scan packaging barcode to start"
        this.confirmDisabled.set(true)
        this.carton.set('')
        this.loadUnit.set({ barcode: '', loadUnitType: '', location: '', items: [] })
        if (this.displayTable()) {
          this.displayTable.set(false)
        }
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(true)
        if (!this.displayTable())
          this.displayTable.set(true)
        this.instructions = err.error?.detail ?? err.error?.message ?? 'Something went wrong. Please try again.'
      }
    })
  }

  onCancel() {
    const barcode = this.loadUnit().barcode
    if (barcode) {
      this.shippingService.releaseLoadUnit(barcode).subscribe({
        next: () => history.back(),
        error: () => history.back(),
      })
    } else {
      history.back()
    }
  }
  scanCarton(toteBarcode: string, cartonBarcode: string) {
    this.shippingService.chooseCarton(toteBarcode, cartonBarcode).subscribe({
      next: (carton: LoadUnit) => {
        this.error.set(false)
        this.carton.set(carton.barcode)
        this.confirmDisabled.set(false)
        this.instructions = `Move all items from tote ${toteBarcode} into carton ${carton.barcode}, then press Confirm`
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(true)
        this.instructions = err.error?.detail ?? err.error?.message ?? 'This carton cannot be used. Scan another one.'
      }
    })
  }
}