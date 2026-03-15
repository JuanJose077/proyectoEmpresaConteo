import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../enviroments/enviroment';

const API_BASE = environment.apiUrl.startsWith('http')
  ? environment.apiUrl
  : `https://${environment.apiUrl}`;

export interface Municipio {
  id: number;
  nombre: string;
  departamento: string;
}

@Injectable({ providedIn: 'root' })
export class MunicipioService {
  constructor(private http: HttpClient) {}

  getMunicipios(departamento?: string): Observable<Municipio[]> {
    const params: any = {};
    if (departamento) params.departamento = departamento;
    return this.http.get<Municipio[]>(`${API_BASE}/api/municipios`, { params });
  }
}
