import { Routes } from '@angular/router';
import { authGuard } from './core/auth-guard';
import { Home } from './pages/home/home';
import { Login } from './pages/login/login';
import { Register } from './pages/register/register';

export const routes: Routes = [
  { path: 'login', component: Login },
  { path: 'registro', component: Register },
  { path: '', component: Home, canActivate: [authGuard] },
  { path: '**', redirectTo: '' },
];
