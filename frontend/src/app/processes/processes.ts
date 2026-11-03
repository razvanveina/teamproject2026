import { Component, OnInit, inject, signal } from '@angular/core';
import { ProcessInfo, ProcessService } from '../services/process.service';

@Component({
  selector: 'app-processes',
  templateUrl: './processes.html',
  styleUrl: './processes.css',
})
export class Processes implements OnInit {
  private service = inject(ProcessService);

  processes = signal<ProcessInfo[]>([]);
  error = signal('');

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.service.getAll().subscribe({
      next: list => this.processes.set(list),
      error: err => {
        console.error('Could not load the processes', err);
        this.error.set('Could not load the processes.');
      }
    });
  }

  act(name: string, action: 'start' | 'pause' | 'stop'): void {
    this.error.set('');
    this.service.act(name, action).subscribe({
      next: () => this.load(),
      error: err => {
        console.error('Could not ' + action + ' ' + name, err);
        this.error.set('Could not ' + action + ' ' + name + '.');
      }
    });
  }
}