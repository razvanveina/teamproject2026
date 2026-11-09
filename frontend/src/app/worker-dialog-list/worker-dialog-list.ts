import { Component, ElementRef, inject, signal } from '@angular/core';
import { LogService } from '../services/log';
import { RouterLink } from '@angular/router';

@Component({
  imports: [RouterLink],
  selector: 'app-worker-dialog-list',
  styleUrl: './worker-dialog-list.css',
  templateUrl: './worker-dialog-list.html',
})
export class WorkerDialogList {
  // private service = inject(DialogConfigService);
  private log = inject(LogService);
  private element = inject(ElementRef);

  dialogs = signal<DialogEntry[]>([
    {id: 1, routerLink:"/transport", title:"Transport"},
    {id: 3, routerLink:"/receiving", title:"Receiving"},
    {id: 2, routerLink:"/decanting", title:"Decanting"},
    {id: 4, routerLink:"/shipping", title:"Shipping"},
    {id: 5, routerLink:"/toteTransport", title:"Tote Transport"},
    {id: 6, routerLink:"/scan", title:"Inventory"},
    {id: 7, routerLink:"/picking", title:"Picking"},
    {id: 8, routerLink:"/stock", title:"Stock"},
  ]);
  open = signal(false);

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
    this.log.action(`WORKER ${dialog.title}`);
  }
}

interface DialogEntry{
  id: number;
  routerLink: string;
  title: string;
}