import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './services/auth';
import { DialogList } from './dialog-list/dialog-list';
import { LogService } from './services/log';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, DialogList],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  protected auth = inject(AuthService);
  private router = inject(Router);
  private log = inject(LogService);

  onNav(action: string): void {
    this.log.action(action);
  }

  logout(): void {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}