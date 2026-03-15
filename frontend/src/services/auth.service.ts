import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../enviroments/enviroment';

const API_BASE = environment.apiUrl.startsWith('http')
  ? environment.apiUrl
  : `https://${environment.apiUrl}`;

export type UserRole = 'ADMIN' | 'USER';

export interface AuthUser {
  id: number;
  email: string;
  role: UserRole;
  mustChangePassword: boolean;
}

export interface AuthResponse {
  token: string;
  expiresAt: string;
  user: AuthUser;
}

interface JwtPayload {
  uid: number;
  role: UserRole;
  mustChangePassword: boolean;
  exp: number;
  sub: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly tokenKey = 'auth_token';

  constructor(private http: HttpClient) {}

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${API_BASE}/auth/login`, { email, password })
      .pipe(tap((res) => this.setToken(res.token)));
  }

  changePassword(currentPassword: string, newPassword: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${API_BASE}/auth/change-password`, { currentPassword, newPassword })
      .pipe(tap((res) => this.setToken(res.token)));
  }

  logout(): Observable<{ ok: boolean }> {
    return this.http.post<{ ok: boolean }>(`${API_BASE}/auth/logout`, {}).pipe(
      tap({
        next: () => this.clearToken(),
        error: () => this.clearToken(),
      }),
    );
  }

  getToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  isLoggedIn(): boolean {
    const payload = this.getPayload();
    if (!payload) return false;
    return payload.exp * 1000 > Date.now();
  }

  isAdmin(): boolean {
    const payload = this.getPayload();
    return payload?.role === 'ADMIN';
  }

  mustChangePassword(): boolean {
    const payload = this.getPayload();
    return payload?.mustChangePassword === true;
  }

  getEmail(): string | null {
    const payload = this.getPayload();
    return payload?.sub ?? null;
  }

  clearToken(): void {
    localStorage.removeItem(this.tokenKey);
  }

  private setToken(token: string): void {
    localStorage.setItem(this.tokenKey, token);
  }

  private getPayload(): JwtPayload | null {
    const token = this.getToken();
    if (!token) return null;

    const parts = token.split('.');
    if (parts.length !== 3) return null;

    try {
      const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
      const padded = base64.padEnd(base64.length + (4 - (base64.length % 4)) % 4, '=');
      const json = atob(padded);
      return JSON.parse(json) as JwtPayload;
    } catch {
      return null;
    }
  }
}
