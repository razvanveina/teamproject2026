import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth';
import { WORKPLACE_ROUTES } from '../workplaces';

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

  expired = this.auth.expired;
  username = '';
  password = '';
  error = signal('');

  login(): void {
    this.auth.login(this.username.trim(), this.password).subscribe({
      next: res => {
        const route = res.workplace ? WORKPLACE_ROUTES[res.workplace] : undefined;
        if (route) {
          this.router.navigate([route.path], { queryParams: route.queryParams });
        } else {
          this.router.navigate(['/home']);
        }
      },
      error: () => this.error.set('Login failed. Check your credentials.')
    });
  }
}