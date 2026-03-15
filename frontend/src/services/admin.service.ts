import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { UserRole } from './auth.service';
import { environment } from '../enviroments/enviroment';

const API_BASE = environment.apiUrl.startsWith('http')
  ? environment.apiUrl
  : `https://${environment.apiUrl}`;

export interface AdminUser {
  id: number;
  email: string;
  role: UserRole;
  active: boolean;
  mustChangePassword: boolean;
  createdByEmail: string | null;
  createdAt: string | null;
  firstAdmin: boolean;
}

export interface CreateUserRequest {
  email: string;
  role: UserRole;
  temporaryPassword?: string;
}

export interface CreateUserResponse {
  id: number;
  email: string;
  role: UserRole;
  active: boolean;
  mustChangePassword: boolean;
  createdBy: number | null;
  createdAt: string | null;
  temporaryPassword: string;
}

export interface ResetPasswordResponse {
  id: number;
  temporaryPassword: string;
}

export interface MetaCell {
  id: number;
  municipioId: number;
  behavior: string;
  claseVehiculo: string;
  meta: number;
  activo: boolean;
}

export interface MetaUpdate {
  id: number;
  meta: number;
  activo: boolean;
}

@Injectable({ providedIn: 'root' })
export class AdminService {
  constructor(private http: HttpClient) {}

  listUsers(): Observable<AdminUser[]> {
    return this.http.get<AdminUser[]>(`${API_BASE}/admin/users`);
  }

  createUser(payload: CreateUserRequest): Observable<CreateUserResponse> {
    return this.http.post<CreateUserResponse>(`${API_BASE}/admin/users`, payload);
  }

  deactivateUser(id: number): Observable<{ ok: boolean }> {
    return this.http.patch<{ ok: boolean }>(`${API_BASE}/admin/users/${id}/deactivate`, {});
  }

  resetPassword(id: number, temporaryPassword?: string): Observable<ResetPasswordResponse> {
    return this.http.patch<ResetPasswordResponse>(
      `${API_BASE}/admin/users/${id}/reset-password`,
      { temporaryPassword },
    );
  }

  updateMeta(id: number, payload: { meta?: number; activo?: number }): Observable<{ ok: boolean }> {
    return this.http.patch<{ ok: boolean }>(`${API_BASE}/admin/metas/${id}`, payload);
  }

  getMetasByMunicipio(municipioId: number): Observable<MetaCell[]> {
    return this.http.get<MetaCell[]>(`${API_BASE}/metas?municipioId=${municipioId}`);
  }

  updateMetasBatch(payload: MetaUpdate[]): Observable<{ ok: boolean; updated: number }> {
    return this.http.put<{ ok: boolean; updated: number }>(`${API_BASE}/admin/metas/batch`, payload);
  }
}
