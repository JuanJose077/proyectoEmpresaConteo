import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AdminService, AdminUser } from '../../services/admin.service';
import { HttpErrorResponse } from '@angular/common/http';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './admin-users.component.html',
  styleUrls: ['./admin-users.component.scss'],
})
export class AdminUsersComponent implements OnInit {
  users: AdminUser[] = [];
  loading = false;
  error = '';
  tempPasswords = new Map<number, string>();

  constructor(private adminService: AdminService) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.adminService.listUsers().subscribe({
      next: (users) => {
        this.users = users;
        this.loading = false;
      },
      error: (err: HttpErrorResponse) => {
        this.loading = false;
        this.error = err.error?.message || err.error?.error || 'No se pudieron cargar los usuarios';
      },
    });
  }

  deactivate(user: AdminUser): void {
    if (user.firstAdmin) {
      Swal.fire({
        icon: 'info',
        title: 'Acción no permitida',
        text: 'El primer administrador no puede ser eliminado.',
      });
      return;
    }

    Swal.fire({
      title: '¿Desactivar usuario?',
      text: 'Esta acción no se puede deshacer',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Desactivar',
      cancelButtonText: 'Cancelar',
    }).then((result) => {
      if (!result.isConfirmed) return;

      this.adminService.deactivateUser(user.id).subscribe({
        next: () => {
          user.active = false;
          Swal.fire({
            icon: 'success',
            title: 'Usuario desactivado',
            text: 'El usuario fue desactivado correctamente',
          });
        },
        error: (err: HttpErrorResponse) => {
          this.error = err.error?.message || err.error?.error || 'No se pudo desactivar el usuario';
          Swal.fire({
            icon: 'error',
            title: 'Error',
            text: this.error,
          });
        },
      });
    });
  }

  resetPassword(user: AdminUser): void {
    Swal.fire({
      title: '¿Restablecer contraseña?',
      text: `Se generará una contraseña temporal para ${user.email}`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Restablecer',
      cancelButtonText: 'Cancelar',
    }).then((result) => {
      if (!result.isConfirmed) return;

      this.adminService.resetPassword(user.id).subscribe({
        next: (res) => {
          this.tempPasswords.set(user.id, res.temporaryPassword);
          user.mustChangePassword = true;
          user.active = true;
          Swal.fire({
            icon: 'success',
            title: 'Contraseña restablecida',
            text: 'La contraseña temporal fue generada correctamente',
          });
        },
        error: (err: HttpErrorResponse) => {
          this.error = err.error?.message || err.error?.error || 'No se pudo restablecer la contraseña';
          Swal.fire({
            icon: 'error',
            title: 'Error',
            text: this.error,
          });
        },
      });
    });
  }
}
