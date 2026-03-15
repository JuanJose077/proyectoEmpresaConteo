import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AdminService, CreateUserResponse } from '../../services/admin.service';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-admin-create-user',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-create-user.component.html',
  styleUrls: ['./admin-create-user.component.scss'],
})
export class AdminCreateUserComponent {
  email = '';
  role: 'ADMIN' | 'USER' = 'USER';
  temporaryPassword = '';
  loading = false;
  error = '';
  created: CreateUserResponse | null = null;

  constructor(private adminService: AdminService, private router: Router) {}

  generateTemp(): void {
    const upper = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';
    const lower = 'abcdefghijklmnopqrstuvwxyz';
    const digits = '0123456789';
    const special = '!@#$%^&*()-_=+[]{}';
    const all = upper + lower + digits + special;

    const pick = (s: string) => s[Math.floor(Math.random() * s.length)];
    let pwd = pick(upper) + pick(digits) + pick(special) + pick(lower);

    while (pwd.length < 12) {
      pwd += pick(all);
    }

    this.temporaryPassword = pwd.split('').sort(() => 0.5 - Math.random()).join('');
  }

  submit(): void {
    this.error = '';
    this.created = null;
    this.loading = true;

    this.adminService
      .createUser({
        email: this.email,
        role: this.role,
        temporaryPassword: this.temporaryPassword || undefined,
      })
      .subscribe({
        next: (res) => {
          this.loading = false;
          this.created = res;
          this.temporaryPassword = res.temporaryPassword;
        },
        error: (err: HttpErrorResponse) => {
          this.loading = false;
          this.error = err.error?.message || err.error?.error || 'No se pudo crear el usuario';
        },
      });
  }

  back(): void {
    this.router.navigateByUrl('/admin/users');
  }
}
