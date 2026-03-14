import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

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
    return this.http.get<Municipio[]>('/api/municipios', { params });
  }
}