import { Component, ElementRef, inject, signal, ViewChild } from '@angular/core';
import { API_BASE_URL } from '../../api.config';
import { SimulatorService } from '../simulator-service';
import { HttpErrorResponse } from '@angular/common/http';
import { LogService } from '../../services/log';
import { FormsModule } from '@angular/forms';

@Component({
  imports: [FormsModule],
  selector: 'app-sim-management',
  styleUrl: './sim-management.css',
  templateUrl: './sim-management.html',
})
export class SimManagement {
  private log = inject(LogService);
  private service = inject(SimulatorService);
  private eventSource!: EventSource;
  selectedEntity = '';
  readonly loadUnitTypes = ['TOTE', 'CARTON'];
  selectedType = 'TOTE';
  entities = signal<string[]>([]);
  message = signal('');
  isError = signal(false);

  @ViewChild('barcode')
  barcodeInput!: ElementRef<HTMLInputElement>;

  ngOnInit() {
    this.eventSource = new EventSource(`${API_BASE_URL}/events/subscribe`);

    this.eventSource.onmessage = event => {
      console.log(event.data);
    }

    this.service.getEntities().subscribe({
      next: (response: string[]) => {
        this.entities.set(response);
      }
    })
  }

  ngOnDestroy() {
    this.eventSource?.close();
  }

  createLU(barcode: string) {
    if (this.selectedEntity === '') {
      this.showMessage('Please select a location', true);
      return;
    }

    if (barcode === '') {
      this.showMessage('Please input a barcode', true);
      return;
    }

    this.log.action(`User created ${this.selectedType} with barcode: ${barcode} | On location: ${this.selectedEntity}`);
    this.service.createLU(barcode, this.selectedEntity, this.selectedType).subscribe({
      next: () => {
        this.barcodeInput.nativeElement.value = '';
        this.showMessage(`${this.selectedType} ${barcode} created on ${this.selectedEntity}`, false);
      },
      error: (err: HttpErrorResponse) => {
        this.showMessage(err.error?.detail ?? err.error?.message ?? 'The load unit cannot be created', true);
      }
    });
  }

  private showMessage(text: string, isError: boolean) {
    this.message.set(text);
    this.isError.set(isError);
  }
}
