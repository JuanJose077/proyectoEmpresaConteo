import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-change-password',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './change-password.component.html',
  styleUrls: ['./change-password.component.scss'],
})
export class ChangePasswordComponent {
  currentPassword = '';
  newPassword     = '';
  confirmPassword = '';
  loading         = false;
  error           = '';

  showCurrent = false;
  showNew     = false;
  showConfirm = false;

  constructor(private authService: AuthService, private router: Router) {}

  /** 1-4: débil → muy fuerte */
  get strength(): number {
    const p = this.newPassword;
    if (!p) return 0;
    let score = 0;
    if (p.length >= 8)                          score++;
    if (p.length >= 12)                         score++;
    if (/[A-Z]/.test(p) && /[a-z]/.test(p))    score++;
    if (/[0-9]/.test(p) && /[^A-Za-z0-9]/.test(p)) score++;
    return Math.max(1, score);
  }

  get strengthLabel(): string {
    return ['', 'Débil', 'Regular', 'Fuerte', 'Muy fuerte'][this.strength] ?? '';
  }

  submit(): void {
    this.error = '';

    if (this.newPassword !== this.confirmPassword) {
      this.error = 'Las contraseñas no coinciden.';
      return;
    }

    this.loading = true;
    this.authService.changePassword(this.currentPassword, this.newPassword).subscribe({
      next: () => {
        this.loading = false;
        this.router.navigateByUrl('/metas-vs-conteo');
      },
      error: (err: HttpErrorResponse) => {
        this.loading = false;
        const msg = err.error?.message || err.error?.error || '';

        if (err.status === 401 || msg.toLowerCase().includes('incorrect') || msg.toLowerCase().includes('incorrecta') || msg.toLowerCase().includes('actual')) {
          this.error = 'La contraseña actual es incorrecta.';
        } else if (err.status === 400 && msg.toLowerCase().includes('igual')) {
          this.error = 'La nueva contraseña no puede ser igual a la actual.';
        } else if (err.status === 0 || err.status >= 500) {
          this.error = 'No se pudo conectar con el servidor. Intenta más tarde.';
        } else if (msg) {
          this.error = msg;
        } else {
          this.error = 'No se pudo cambiar la contraseña. Intenta de nuevo.';
        }
      },
    });
  }
}