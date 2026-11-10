import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AccountApi } from '../services/account';

@Component({
  selector: 'app-account-form',
  imports: [FormsModule],
  templateUrl: './account-form.html',
  styleUrl: '../dialog-form/dialog-form.css',  
})
export class AccountForm {
  private readonly api = inject(AccountApi);
  private readonly router = inject(Router);

  username = '';
  password = '';
  confirm = '';
  error = signal('');

  save(): void {
    if (!this.username.trim() || !this.password) {
      this.error.set('Username and password are required.');
      return;
    }
    if (this.password !== this.confirm) {
      this.error.set('The passwords do not match.');
      return;
    }

    this.api.create(this.username.trim(), this.password).subscribe({
      next: () => this.router.navigate(['/accounts']),
      error: (err: HttpErrorResponse) =>
        this.error.set(err.error?.detail ?? err.error?.message ?? 'Could not create the account.'),
    });
  }

  cancel(): void {
    this.router.navigate(['/accounts']);
  }
}