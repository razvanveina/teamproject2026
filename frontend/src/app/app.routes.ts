import { Routes } from '@angular/router';
import { Home } from './home/home';
import { UserTable } from './user-table/user-table';
import { UserDialog } from './user-dialog/user-dialog';
import { Login } from './login/login';
import { authGuard } from './guards/auth-guard';
import { DialogPage } from './dialog-page/dialog-page';
import { ReceivingApp } from './receiving-app/receiving-app';
import { DialogForm } from './dialog-form/dialog-form';
import { TransportApp } from './transport-app/transport-app';

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
            { path: 'dialogs/:entity', component: DialogPage },
            { path: 'dialogs/:entity/add', component: DialogForm },
            { path: '', redirectTo: 'home', pathMatch: 'full' },
            { path: 'receiving', component: ReceivingApp },
            { path: 'transport', component: TransportApp }
        ]
    },
    { path: '**', redirectTo: 'home' }
];