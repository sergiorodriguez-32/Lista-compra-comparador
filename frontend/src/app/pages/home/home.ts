import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { Auth } from '../../core/auth';

@Component({
  selector: 'app-home',
  imports: [MatButtonModule],
  templateUrl: './home.html',
  styleUrl: './home.scss',
})
export class Home {
  private auth = inject(Auth);
  private router = inject(Router);

  cerrarSesion() {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
