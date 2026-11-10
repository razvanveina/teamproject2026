import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Account, AccountApi } from '../services/account';

@Component({
  selector: 'app-accounts',
  imports: [RouterLink],
  templateUrl: './accounts.html',
  styleUrl: '../dialog-page/dialog-page.css',
})
export class Accounts implements OnInit {
  private readonly api = inject(AccountApi);

  accounts = signal<Account[]>([]);
  error = signal('');

  ngOnInit(): void {
    this.refresh();
  }

  refresh(): void {
    this.error.set('');
    this.api.list().subscribe({
      next: (list) => this.accounts.set(list),
      error: () => this.error.set('Could not load the accounts.'),
    });
  }

  deleteAccount(account: Account): void {
    if (!confirm('Delete account ' + account.username + '?')) {
      return;
    }

    this.error.set('');
    this.api.delete(account.id).subscribe({
      next: () => this.refresh(),
      error: (err) => this.error.set(err.error?.detail ?? err.error?.message ?? 'Delete failed (status ' + err.status + ')'),
    });
  }
}