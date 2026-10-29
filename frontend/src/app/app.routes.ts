import { Routes } from '@angular/router';
import { UserTable } from './user-table/user-table';
import { UserDialog } from './user-dialog/user-dialog';

export const routes: Routes = [
    { path: 'users', component: UserTable },
    { path: 'users/add', component: UserDialog },
    { path: 'users/edit/:id', component: UserDialog },
    { path: '', redirectTo: 'users', pathMatch: 'full' }
];