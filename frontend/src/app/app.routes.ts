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
import { Processes } from './processes/processes';
import { Stock } from './stock/stock';
import { Shipping } from './shipping/shipping';
import { PickingApp } from './picking-app/picking-app';
import { DecantingApp } from './decanting-app/decanting-app';
import { ScanApp } from './scan-app/scan-app';

export const routes: Routes = [
    { path: 'login', component: Login, title: 'Login' },
    {
        path: '',
        canActivate: [authGuard],
        children: [
            { path: 'home', component: Home, title: 'Home' },
            { path: 'users', component: UserTable, title: 'Users' },
            { path: 'users/add', component: UserDialog, title: 'Users - Add' },
            { path: 'users/edit/:id', component: UserDialog, title: 'Users - Edit' },
            { path: 'dialogs/:entity', component: DialogPage },
            { path: 'dialogs/:entity/add', component: DialogForm },
            { path: 'dialogs/:entity/edit/:id', component: DialogForm },
            { path: '', redirectTo: 'home', pathMatch: 'full' },
            { path: 'receiving', component: ReceivingApp, title: 'Receiving' },
            { path: 'shipping', component: Shipping, title: 'Shipping' },
            { path: 'transport', component: TransportApp, title: 'Transport' },
            { path: 'decanting', component: DecantingApp, title: 'Decanting' },
            { path: 'processes', component: Processes, title: 'Processes' },
            { path: 'stock', component: Stock, title: 'Stock' },
            { path: 'picking', component: PickingApp, title: 'Picking' },
            { path: 'scan', component: ScanApp, title: 'Scan'}
        ]
    },
    { path: '**', redirectTo: 'home' }
];