import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DialogConfig, DialogsFile } from '../models/dialog-config.model';
import { DialogConfigService } from '../services/dialog-config.service';

@Component({
  selector: 'app-dialog-list',
  imports: [RouterLink],
  templateUrl: './dialog-list.html',
  styleUrl: './dialog-list.css',
})
export class DialogList implements OnInit {
  private service = inject(DialogConfigService);

  // The dialogs shown on the page. Empty at the start.
  dialogs = signal<DialogConfig[]>([]);

  // Is the dropdown open? Closed at the start.
  open = signal(false);

  // Click on the button: open if closed, close if open
  toggle(): void {
    this.open.set(!this.open());
  }

  // Click on a link: close the dropdown
  close(): void {
    this.open.set(false);
  }

  // Runs once, when the page opens
  ngOnInit(): void {
    // 1. Ask for dialogs.json
    this.service.getDialogsFile().subscribe((dialogsFile: DialogsFile) => {
      // 2. For every file in the list, ask for that file
      for (const entry of dialogsFile.dialogs) {
        this.service.getDialog(entry.file).subscribe((dialog: DialogConfig) => {
          // 3. Add the dialog to the list shown on the page
          const list = this.dialogs().slice(); // a copy of the current list
          list.push(dialog);
          this.dialogs.set(list);
        });
      }
    });
  }
}