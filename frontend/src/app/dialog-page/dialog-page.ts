import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { DialogConfig } from '../models/dialog-config.model';
import { DialogConfigService } from '../services/dialog-config.service';

@Component({
  selector: 'app-dialog-page',
  templateUrl: './dialog-page.html',
  styleUrl: './dialog-page.css',
})
export class DialogPage implements OnInit {
  private route = inject(ActivatedRoute);
  private service = inject(DialogConfigService);

  // The dialog description (title, entity, columns). Empty until it arrives.
  dialog = signal<DialogConfig | null>(null);

  // The rows of the table. Empty until they arrive.
  rows = signal<any[]>([]);

  // Runs once, when the page opens
  // Runs every time the address changes: /dialogs/users -> /dialogs/articles
  ngOnInit(): void {
    this.route.paramMap.subscribe((params) => {
      // 1. Read the entity (class name) from the address, e.g. /dialogs/ReceivingOrder
      const entity = params.get('entity');
      this.rows.set([]);

      // 2. Ask for the dialog file, e.g. ReceivingOrder.json
      this.service.getDialog(entity + '.json').subscribe((dialog: DialogConfig) => {
        this.dialog.set(dialog);

        // 3. Ask for the rows, e.g. /api/data/article
        this.service.getRows(dialog.entity).subscribe((rows: any[]) => {
          this.rows.set(rows);
        });
      });
    });
  }
}