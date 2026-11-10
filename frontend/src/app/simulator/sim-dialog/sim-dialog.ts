import { Component, ElementRef, inject, signal } from '@angular/core';
import { LogService } from '../../services/log';
import { RouterLink } from '@angular/router';

@Component({
  imports: [RouterLink],
  selector: 'app-sim-dialog',
  styleUrl: './sim-dialog.css',
  templateUrl: './sim-dialog.html',
  host: { '(document:click)': 'clickOutside($event)' },
})
export class SimulatorDialog {
  private log = inject(LogService);
  private element = inject(ElementRef);

  open = signal(false);
  dialogs = signal<DialogEntry[]>([
    { id: 1, routerLink: "/sim/management", title: "Management" },
    { id: 2, routerLink: "/sim/picking", title: "Picking" },
    { id: 3, routerLink: "/sim/visualization", title: "Visualization" }
  ]);

  toggle(): void {
    this.open.set(!this.open());
  }

  close(): void {
    this.open.set(false);
  }

  clickOutside(event: MouseEvent): void {
    if (!this.element.nativeElement.contains(event.target)) {
      this.close();
    }
  }

  onSelect(dialog: DialogEntry): void {
    this.close();
    this.log.action(`SIMULATOR ${dialog.title}`);
  }
}

interface DialogEntry {
  id: number;
  routerLink: string;
  title: string;
}