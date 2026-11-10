import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './services/auth';
import { DialogList } from './dialog-list/dialog-list';
import { LogService } from './services/log';
import { WorkerDialogList } from './worker-dialog-list/worker-dialog-list';
import { SimulatorDialog } from './simulator/sim-dialog/sim-dialog';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, DialogList, WorkerDialogList, SimulatorDialog],
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

  onPicking(): void {
    if (this.router.url === '/picking') {
      location.reload();
    }
  }

  logout(): void {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}