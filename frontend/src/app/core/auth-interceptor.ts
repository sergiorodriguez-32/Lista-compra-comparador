import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Auth } from './auth';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = inject(Auth).getToken();

  // sin token (login, registro) la petición sale tal cual
  if (!token) {
    return next(req);
  }

  // las peticiones son inmutables: hay que clonar para añadir la cabecera
  return next(req.clone({
    setHeaders: { Authorization: `Bearer ${token}` }
  }));
};
