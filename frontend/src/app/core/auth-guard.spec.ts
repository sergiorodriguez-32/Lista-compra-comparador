import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, provideRouter, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { authGuard } from './auth-guard';

describe('authGuard', () => {
  const ejecutar = () =>
    TestBed.runInInjectionContext(() =>
      authGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot)
    );

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideRouter([])],
    });
  });

  afterEach(() => localStorage.clear());

  it('deja pasar si hay token', () => {
    localStorage.setItem('token', 'abc');
    expect(ejecutar()).toBe(true);
  });

  it('redirige a /login si no hay token', () => {
    const resultado = ejecutar() as UrlTree;
    const router = TestBed.inject(Router);
    expect(router.serializeUrl(resultado)).toBe('/login');
  });
});
