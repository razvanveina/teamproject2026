import { Component, inject, signal } from '@angular/core';
import { SimDataService } from '../sim-data-service';
import { SimEntity } from '../models/sim-entity-model';

@Component({
  imports: [],
  selector: 'app-sim-visualization',
  styleUrl: './sim-visualization.css',
  templateUrl: './sim-visualization.html',
})
export class SimVisualization {
  private simDataService = inject(SimDataService);
  private checker: ReturnType<typeof setInterval> | undefined;

  points = signal<SimEntity[]>(
    [
      { id: 1, name: "test", loadUnits: ["lu1", "lu2"] },
      { id: 2, name: "point", loadUnits: ["lu1", "lu2", "lu3"] }
    ]
  );

  ngOnInit(): void {
    this.getVizData();
    this.checker = setInterval(() => this.getVizData(), 1_000);
  }

  getVizData() {
    this.simDataService.getVizData().subscribe({
      next: (response: SimEntity[]) => {
        this.points.set(response);
      },
      error: (err) => {
        alert(err.error?.detail ?? 'Somethinig went wrong ¯\\_(ツ)_/¯');
      },
    });
  }
}
