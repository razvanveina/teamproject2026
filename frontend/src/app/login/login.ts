import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css'
})
export class Login {
  private auth = inject(AuthService);
  private router = inject(Router);

  username = '';
  password = '';
  error = signal('');

  login(): void {
    this.auth.login(this.username.trim(), this.password).subscribe({
      next: () => this.router.navigate(['/users']),
      error: () => this.error.set('Login failed. Check your credentials.')
    });
  }

  goToRegister(): void {
    this.router.navigate(['/register']);
  }
}