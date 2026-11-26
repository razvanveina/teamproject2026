import { HttpErrorResponse } from '@angular/common/http';
import { Component, ElementRef, inject, OnDestroy, signal, ViewChild } from '@angular/core';
import { ShippingService } from '../services/shipping-out-service';
import { LoadUnit } from '../models/load-unit-model';

@Component({
  imports: [],
  selector: 'app-shipping',
  styleUrl: './shipping.css',
  templateUrl: './shipping.html',
})
export class Shipping implements OnDestroy {
  shippingService = inject(ShippingService)

  @ViewChild('barcodeInput')
  barcodeInput!: ElementRef<HTMLInputElement>;

  instructions = signal("Scan packaging barcode to start")

  displayTable = signal(false)
  confirmDisabled = signal(true)
  error = signal(false);
  carton = signal('');
  shippingStep = signal<'items' | 'printing' | 'label'>('items')
  labelCartonBarcode = signal('')
  scannedLabel = signal('')
  loadUnit = signal<LoadUnit>({
    barcode: '',
    loadUnitType: '',
    location: '',
    items: []
  })

  private labelStatusPollTimer: ReturnType<typeof setInterval> | undefined

  ngOnDestroy(): void {
    this.stopLabelStatusPolling()
  }

  onScan(barcode: string) {
    this.barcodeInput.nativeElement.value = '';
    if (this.shippingStep() === 'printing') {
      return
    }
    if (this.shippingStep() === 'label') {
      this.scannedLabel.set(barcode.trim())
      this.error.set(false)
      this.confirmDisabled.set(false)
      this.instructions.set(`Barcode ${barcode.trim()} scanned. Press Confirm to send carton ${this.labelCartonBarcode()} to the gate`)
      return
    }

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
          this.instructions.set("Scan an empty carton or a new carton barcode")
        } else {
          this.confirmDisabled.set(false)
          this.instructions.set("Confirm the status of the package and press confirm")
        }
        // TODO check location

        if (!this.displayTable()) {
          this.displayTable.set(true)
        }
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(true)
        this.instructions.set(err.error?.detail ?? err.error?.message ?? 'Something went wrong. Please try again.')
        if (this.displayTable()) {
          this.displayTable.set(false)
        }
        this.confirmDisabled.set(true)
      }
    })
  }

  onConfirm() {
    if (this.shippingStep() === 'label') {
      this.confirmScannedLabel()
      return
    }

    const loadUnitBarcodeToFinish = this.labelCartonBarcode() || this.loadUnit().barcode
    const cartonBarcodeForTote = this.labelCartonBarcode() ? undefined : this.carton() || undefined
    this.shippingService.finishOrder(loadUnitBarcodeToFinish, cartonBarcodeForTote).subscribe({
      next: () => {
        this.error.set(false)
        if (!this.labelCartonBarcode()) {
          this.labelCartonBarcode.set(this.carton() || this.loadUnit().barcode)
        }
        this.startWaitingForLabelPrint()
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(true)
        if (!this.displayTable())
          this.displayTable.set(true)
        this.instructions.set(err.error?.detail ?? err.error?.message ?? 'Something went wrong. Please try again.')
      }
    })
  }

  onCancel() {
    this.stopLabelStatusPolling()
    const barcode = this.labelCartonBarcode() || this.loadUnit().barcode
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
        this.instructions.set(`Move all items from tote ${toteBarcode} into carton ${carton.barcode}, then press Confirm`)
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(true)
        this.instructions.set(err.error?.detail ?? err.error?.message ?? 'This carton cannot be used. Scan another one.')
      }
    })
  }

  onReprint() {
    this.shippingService.reprintLabel(this.labelCartonBarcode()).subscribe({
      next: () => {
        this.error.set(false)
        this.scannedLabel.set('')
        this.startWaitingForLabelPrint()
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(true)
        this.instructions.set(err.error?.detail ?? err.error?.message ?? 'The label cannot be printed again. Please try again.')
      }
    })
  }

  private startWaitingForLabelPrint() {
    this.shippingStep.set('printing')
    this.confirmDisabled.set(true)
    this.instructions.set(`Printing the shipping label for carton ${this.labelCartonBarcode()}, please wait...`)
    this.stopLabelStatusPolling()
    this.labelStatusPollTimer = setInterval(() => this.checkLabelPrintStatus(), 2000)
  }

  private checkLabelPrintStatus() {
    this.shippingService.getLabelStatus(this.labelCartonBarcode()).subscribe({
      next: (labelStatusResponse) => {
        if (labelStatusResponse.status === 'PRINTED') {
          this.stopLabelStatusPolling()
          this.shippingStep.set('label')
          this.error.set(false)
          this.instructions.set(`Label printed. Please scan the barcode from the printed PDF: ${labelStatusResponse.label}`)
        } else if (labelStatusResponse.status === 'ERROR') {
          this.showPrintingFailure('Printing the label failed. Press Confirm to print it again')
        }
      },
      error: (err: HttpErrorResponse) => {
        this.showPrintingFailure(err.error?.detail ?? err.error?.message ?? 'Cannot check the label. Press Confirm to try again')
      }
    })
  }

  private showPrintingFailure(message: string) {
    this.stopLabelStatusPolling()
    this.shippingStep.set('items')
    this.error.set(true)
    this.confirmDisabled.set(false)
    this.instructions.set(message)
  }

  private confirmScannedLabel() {
    this.shippingService.confirmLabel(this.labelCartonBarcode(), this.scannedLabel()).subscribe({
      next: () => this.resetToFirstScan(),
      error: (err: HttpErrorResponse) => {
        this.error.set(true)
        this.scannedLabel.set('')
        this.confirmDisabled.set(true)
        this.instructions.set(err.error?.detail ?? err.error?.message ?? 'Something went wrong. Scan the label again.')
      }
    })
  }

  private resetToFirstScan() {
    this.stopLabelStatusPolling()
    this.shippingStep.set('items')
    this.labelCartonBarcode.set('')
    this.scannedLabel.set('')
    this.error.set(false)
    this.instructions.set("Scan packaging barcode to start")
    this.confirmDisabled.set(true)
    this.carton.set('')
    this.loadUnit.set({ barcode: '', loadUnitType: '', location: '', items: [] })
    if (this.displayTable()) {
      this.displayTable.set(false)
    }
  }

  private stopLabelStatusPolling() {
    if (this.labelStatusPollTimer) {
      clearInterval(this.labelStatusPollTimer)
      this.labelStatusPollTimer = undefined
    }
  }
}
