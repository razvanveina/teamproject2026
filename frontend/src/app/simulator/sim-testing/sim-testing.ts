import { Component, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { SimulatorService } from '../simulator-service';
import { LogService } from '../../services/log';

@Component({
  selector: 'app-sim-testing',
  styleUrl: './sim-testing.css',
  templateUrl: './sim-testing.html',
})
export class SimTesting {
  private log = inject(LogService);
  private service = inject(SimulatorService);

  statusMessage = signal('');
  isError = signal(false);

  createReceivingOrder(): void {
    this.log.action('User created a test receiving order file');
    this.service.createReceivingOrderFile().subscribe({
      next: response => this.showSuccess(`${response.fileName} created in host/in`),
      error: (err: HttpErrorResponse) => this.showError(err)
    });
  }

  createRetrievalOrder(): void {
    this.log.action('User created a test retrieval order file');
    this.service.createRetrievalOrderFile().subscribe({
      next: response => this.showSuccess(`${response.fileName} created in host/out`),
      error: (err: HttpErrorResponse) => this.showError(err)
    });
  }

  private showSuccess(message: string): void {
    this.isError.set(false);
    this.statusMessage.set(message);
  }

  private showError(err: HttpErrorResponse): void {
    this.isError.set(true);
    this.statusMessage.set(err.error?.message ?? 'File could not be created');
  }
}
