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
  ngOnInit(): void {
    // 1. Read the id from the address: /dialogs/users -> "users"
    const id = this.route.snapshot.paramMap.get('id');

    // 2. Ask for the dialog file: users.json
    this.service.getDialog(id + '.json').subscribe((dialog: DialogConfig) => {
      this.dialog.set(dialog);

      // 3. Ask for the rows: entity "users" -> /api/users
      this.service.getRows(dialog.entity).subscribe((rows: any[]) => {
        this.rows.set(rows);
      });
    });
  }
}