
import { ChangeDetectorRef, Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';

import { User } from '../models/user.model';
import { UserTableService } from '../services/user-table';

@Component({
  selector: 'app-user-dialog',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './user-dialog.html',
  styleUrl: './user-dialog.css'
})
export class UserDialog implements OnInit {
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private userTableService = inject(UserTableService);
  private cdr = inject(ChangeDetectorRef);

  name = '';
  email = '';
  userId: number | null = null;
  errorMessage = '';

  get isEdit(): boolean {
    return this.userId !== null;
  }

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');

    if (idParam !== null) {
      this.userId = Number(idParam);

      this.userTableService.getUserById(this.userId).subscribe({
        next: user => {
          this.name = user.name;
          this.email = user.email;
          this.cdr.markForCheck();
        },
        error: err => {
          console.error('Error loading user', err);
          this.router.navigate(['/users']);
        }
      });
    }
  }

  addUser(): void {
    const name = this.name.trim();
    const email = this.email.trim();

    if (!name || !email) {
      this.errorMessage = 'Name and email are required.';
      return;
    }

    this.errorMessage = '';

    const userData = { name, email };

    const request = this.isEdit
      ? this.userTableService.updateUser(this.userId!, userData)
      : this.userTableService.addUser(userData);

    request.subscribe({
      next: () => this.router.navigate(['/users']),
      error: err => {
        console.error('Error saving user', err);
        this.errorMessage = 'Could not save user. Please try again.';
      }
    });
  }

  cancel(): void {
    this.router.navigate(['/users']);
  }
}
