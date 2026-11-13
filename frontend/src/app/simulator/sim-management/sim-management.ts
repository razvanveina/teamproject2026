import { Component, inject, signal } from '@angular/core';
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
  private selectedEntity = '';
  entities = signal<string[]>([]);

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
    this.log.action(`User created LU with barcode: ${barcode} | On location: ${this.selectedEntity}`);
    this.service.triggerEvent(barcode).subscribe({
      error: (err: HttpErrorResponse) => {
        console.log(err)
      }
    });
  }
}
