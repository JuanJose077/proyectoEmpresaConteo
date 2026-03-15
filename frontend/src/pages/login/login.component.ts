import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { CatalogService } from '../../services/catalog.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss'],
})
export class LoginComponent {
  email    = '';
  password = '';
  loading  = false;
  error    = '';
  showPassword = false;

  constructor(
    private router: Router,
    private catalogService: CatalogService,
    private authService: AuthService,
  ) {
    if (this.authService.isLoggedIn()) {
      if (this.authService.mustChangePassword()) {
        this.router.navigateByUrl('/change-password');
      } else {
        this.router.navigateByUrl('/metas-vs-conteo');
      }
    }
  }

  login() {
    this.error   = '';
    this.loading = true;

    this.authService.login(this.email, this.password).subscribe({
      next: (res) => {
        this.loading = false;
        this.catalogService.preloadDepartamentos();
        if (res.user.mustChangePassword) {
          this.router.navigateByUrl('/change-password');
        } else {
          this.router.navigateByUrl('/metas-vs-conteo');
        }
      },
      error: (err: HttpErrorResponse) => {
        this.loading = false;
        const msg = err.error?.message || err.error?.error || '';

        // Mensajes amigables según código/texto del servidor
        if (err.status === 401 || msg.toLowerCase().includes('credencial') || msg.toLowerCase().includes('invalid') || msg.toLowerCase().includes('incorrect')) {
          this.error = 'Correo o contraseña incorrectos. Verifica tus datos e intenta de nuevo.';
        } else if (err.status === 404 || msg.toLowerCase().includes('no encontrado') || msg.toLowerCase().includes('not found')) {
          this.error = 'No existe una cuenta con ese correo electrónico.';
        } else if (err.status === 429) {
          this.error = 'Demasiados intentos fallidos. Espera unos minutos antes de intentar de nuevo.';
        } else if (err.status === 0 || err.status >= 500) {
          this.error = 'No se pudo conectar con el servidor. Intenta más tarde.';
        } else if (msg) {
          this.error = msg;
        } else {
          this.error = 'Ocurrió un error inesperado. Intenta de nuevo.';
        }
      },
    });
  }
}