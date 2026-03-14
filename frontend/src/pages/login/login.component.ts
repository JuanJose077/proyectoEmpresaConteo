import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { CatalogService } from '../../services/catalog.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss'],
})
export class LoginComponent {
  username = '';
  password = '';
  loading = false;
  error = '';

  constructor(
    private router: Router,
    private catalogService: CatalogService,
  ) {
    this.catalogService.preloadDepartamentos();
    this.router.navigateByUrl('/metas-vs-conteo');
  }

  login() {
    this.error = '';
    this.loading = true;

    setTimeout(() => {
      this.loading = false;

      if (this.username.trim() && this.password.trim()) {
        this.router.navigateByUrl('/metas-vs-conteo');
      } else {
        this.error = 'Usuario y contraseña son obligatorios';
      }
    }, 400);
  }
}
