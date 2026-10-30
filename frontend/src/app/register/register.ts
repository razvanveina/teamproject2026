import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './register.html',
  styleUrl: '../login/login.css'
})
export class Register {
  private auth = inject(AuthService);
  private router = inject(Router);

  username = '';
  password = '';
  error = signal('');

  register(): void {
    this.auth.register(this.username.trim(), this.password).subscribe({
      next: () => this.router.navigate(['/login']),
      error: (err: HttpErrorResponse) => this.error.set(this.message(err))
    });
  }

  back(): void {
    this.router.navigate(['/login']);
  }

  private message(err: HttpErrorResponse): string {
    if (err.status === 409) {
      return 'That username is already taken.';
    }
    if (err.status === 400) {
      return 'Username: minimum 3 characters, only _ . - special characters allowed | Password: minimum 15 characters.';
    }
    return 'Registration failed. Please try again.';
  }
}