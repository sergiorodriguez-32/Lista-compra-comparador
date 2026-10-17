import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Auth } from './auth';

// Protege las rutas privadas: sin sesión redirige a /login
export const authGuard: CanActivateFn = () => {
  if (inject(Auth).isLoggedIn()) {
    return true;
  }
  return inject(Router).createUrlTree(['/login']);
};
