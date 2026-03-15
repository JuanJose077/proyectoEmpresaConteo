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
  email = '';
  password = '';
  loading = false;
  error = '';

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
    this.error = '';
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
        this.error = err.error?.message || err.error?.error || 'Credenciales incorrectas';
      },
    });
  }
}
