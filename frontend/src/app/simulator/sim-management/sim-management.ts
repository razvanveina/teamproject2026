import { Component, inject } from '@angular/core';
import { API_BASE_URL } from '../../api.config';
import { EventService } from '../event-service';
import { HttpErrorResponse } from '@angular/common/http';
import { LogService } from '../../services/log';

@Component({
  imports: [],
  selector: 'app-sim-management',
  styleUrl: './sim-management.css',
  templateUrl: './sim-management.html',
})
export class SimManagement {
  private log = inject(LogService);
  private service = inject(EventService)
  private eventSource!: EventSource;

  ngOnInit() {
    this.eventSource = new EventSource(`${API_BASE_URL}/events/subscribe`);

    this.eventSource.onmessage = event => {
      console.log(event.data);
    }
  }

  ngOnDestroy() {
    this.eventSource?.close();
  }

  createLU(barcode: string) {
    this.service.triggerEvent(barcode).subscribe({
      error: (err: HttpErrorResponse) => {
        console.log(err)
      }
    });
  }

  createCeva(barcode: string) {

  }
}
