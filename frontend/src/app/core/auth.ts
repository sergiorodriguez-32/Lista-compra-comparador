import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { tap } from 'rxjs';

export interface RegisterRequest { email: string; password: string; displayName: string; }
export interface LoginRequest { email: string; password: string; }
export interface UserResponse { id: number; email: string; displayName: string; }

const TOKEN_KEY = 'token';

@Injectable({ providedIn: 'root' })
export class Auth {
  private http = inject(HttpClient);

  // signal con el token; se inicializa con el que haya guardado de una sesión anterior
  private token = signal<string | null>(localStorage.getItem(TOKEN_KEY));
  readonly isLoggedIn = computed(() => this.token() !== null);

  register(datos: RegisterRequest) {
    return this.http.post<UserResponse>('/api/auth/register', datos);
  }

  login(datos: LoginRequest) {
    return this.http.post<{ token: string }>('/api/auth/login', datos).pipe(
      tap(res => {
        localStorage.setItem(TOKEN_KEY, res.token);
        this.token.set(res.token);
      })
    );
  }

  logout() {
    localStorage.removeItem(TOKEN_KEY);
    this.token.set(null);
  }

  getToken() { return this.token(); }
}
