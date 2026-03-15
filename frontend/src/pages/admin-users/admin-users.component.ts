import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AdminService, AdminUser } from '../../services/admin.service';
import { HttpErrorResponse } from '@angular/common/http';

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
    if (!confirm(`Desactivar usuario ${user.email}?`)) return;

    this.adminService.deactivateUser(user.id).subscribe({
      next: () => {
        user.active = false;
      },
      error: (err: HttpErrorResponse) => {
        this.error = err.error?.message || err.error?.error || 'No se pudo desactivar el usuario';
      },
    });
  }

  resetPassword(user: AdminUser): void {
    if (!confirm(`Restablecer contraseña para ${user.email}?`)) return;

    this.adminService.resetPassword(user.id).subscribe({
      next: (res) => {
        this.tempPasswords.set(user.id, res.temporaryPassword);
        user.mustChangePassword = true;
        user.active = true;
      },
      error: (err: HttpErrorResponse) => {
        this.error = err.error?.message || err.error?.error || 'No se pudo restablecer la contraseña';
      },
    });
  }
}
