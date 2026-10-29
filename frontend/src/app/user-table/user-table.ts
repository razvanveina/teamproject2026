
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { User } from '../models/user.model';
import { UserTableService } from '../services/user-table';

@Component({
  selector: 'app-user-table',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './user-table.html',
  styleUrl: './user-table.css'
})
export class UserTable implements OnInit {
  private userTableService = inject(UserTableService);

  users = signal<User[]>([]);
  isLoading = signal(false);
  loadError = signal(false);

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.isLoading.set(true);
    this.loadError.set(false);

    this.userTableService.getUsers().subscribe({
      next: users => {
        this.users.set(users);
        this.isLoading.set(false);
      },
      error: err => {
        console.error('Error loading users', err);
        this.isLoading.set(false);
        this.loadError.set(true);
      }
    });
  }

  deleteUser(id: number): void {
    this.userTableService.deleteUser(id).subscribe({
      next: () => this.loadUsers(),
      error: err => console.error('Error deleting user', err)
    });
  }
}
