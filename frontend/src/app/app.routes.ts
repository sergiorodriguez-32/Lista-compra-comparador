import { Routes } from '@angular/router';
import { authGuard } from './core/auth-guard';
import { Home } from './pages/home/home';
import { Login } from './pages/login/login';
import { ProductForm } from './pages/product-form/product-form';
import { ProductList } from './pages/product-list/product-list';
import { Register } from './pages/register/register';

export const routes: Routes = [
  { path: 'login', component: Login },
  { path: 'registro', component: Register },
  { path: '', component: Home, canActivate: [authGuard] },
  { path: 'productos', component: ProductList, canActivate: [authGuard] },
  { path: 'productos/nuevo', component: ProductForm, canActivate: [authGuard] },
  // :id es un parámetro: en /productos/7/editar, id vale 7
  { path: 'productos/:id/editar', component: ProductForm, canActivate: [authGuard] },
  { path: '**', redirectTo: '' },
];
