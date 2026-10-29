import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DialogSummary } from '../models/dialog-config.model';
import { DialogConfigService } from '../services/dialog-config.service';
import { httpErrorMessage } from '../services/http-error-message';

// Page for /dialogs: one link for every dialog configured on the backend.
@Component({
  selector: 'app-dialog-list',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './dialog-list.html',
  styleUrl: './dialog-list.css',
})
export class DialogList implements OnInit {
  private dialogConfigService = inject(DialogConfigService);

  dialogs = signal<DialogSummary[]>([]);
  loading = signal(true);
  error = signal('');

  ngOnInit(): void {
    this.dialogConfigService.getDialogs().subscribe({
      next: (dialogs) => {
        this.dialogs.set(dialogs);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(httpErrorMessage(err, 'Lista de dialoguri nu a fost găsită.'));
        this.loading.set(false);
      },
    });
  }
}
