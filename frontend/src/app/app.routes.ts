import { Routes } from '@angular/router';
import { Home } from './home/home';
import { UserTable } from './user-table/user-table';
import { UserDialog } from './user-dialog/user-dialog';
import { Login } from './login/login';
import { authGuard } from './guards/auth-guard';
import { DialogList } from './dialog-list/dialog-list';
import { DialogPage } from './dialog-page/dialog-page';

export const routes: Routes = [
    { path: 'login', component: Login },
    {
        path: '',
        canActivate: [authGuard],
        children: [
            { path: 'home', component: Home },
            { path: 'users', component: UserTable },
            { path: 'users/add', component: UserDialog },
            { path: 'users/edit/:id', component: UserDialog },
            { path: 'dialogs', component: DialogList },
            { path: 'dialogs/:id', component: DialogPage },
            { path: '', redirectTo: 'home', pathMatch: 'full' }
        ]
    },
    { path: '**', redirectTo: 'home' }
];
