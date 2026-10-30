import { Routes } from '@angular/router';
import { UserTable } from './user-table/user-table';
import { UserDialog } from './user-dialog/user-dialog';
import { Login } from './login/login';
import { Register } from './register/register';
import { authGuard } from './guards/auth-guard';

export const routes: Routes = [
    { path: 'login', component: Login },
    { path: 'register', component: Register },
    {
        path: '',
        canActivate: [authGuard],
        children: [
            { path: 'users', component: UserTable },
            { path: 'users/add', component: UserDialog },
            { path: 'users/edit/:id', component: UserDialog },
            { path: '', redirectTo: 'users', pathMatch: 'full' }
        ]
    },
    { path: '**', redirectTo: 'users' }
];