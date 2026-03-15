import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { UserRole } from './auth.service';

export interface AdminUser {
  id: number;
  email: string;
  role: UserRole;
  active: boolean;
  mustChangePassword: boolean;
  createdBy: number | null;
  createdAt: string | null;
}

export interface CreateUserRequest {
  email: string;
  role: UserRole;
  temporaryPassword?: string;
}

export interface CreateUserResponse extends AdminUser {
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
    return this.http.get<AdminUser[]>('/admin/users');
  }

  createUser(payload: CreateUserRequest): Observable<CreateUserResponse> {
    return this.http.post<CreateUserResponse>('/admin/users', payload);
  }

  deactivateUser(id: number): Observable<{ ok: boolean }> {
    return this.http.patch<{ ok: boolean }>(`/admin/users/${id}/deactivate`, {});
  }

  resetPassword(id: number, temporaryPassword?: string): Observable<ResetPasswordResponse> {
    return this.http.patch<ResetPasswordResponse>(`/admin/users/${id}/reset-password`, { temporaryPassword });
  }

  updateMeta(id: number, payload: { meta?: number; activo?: number }): Observable<{ ok: boolean }> {
    return this.http.patch<{ ok: boolean }>(`/admin/metas/${id}`, payload);
  }

  getMetasByMunicipio(municipioId: number): Observable<MetaCell[]> {
    return this.http.get<MetaCell[]>(`/metas?municipioId=${municipioId}`);
  }

  updateMetasBatch(payload: MetaUpdate[]): Observable<{ ok: boolean; updated: number }> {
    return this.http.put<{ ok: boolean; updated: number }>(`/admin/metas/batch`, payload);
  }
}
