import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../enviroments/enviroment';

const API_BASE = environment.apiUrl.startsWith('http')
  ? environment.apiUrl
  : `https://${environment.apiUrl}`;

export interface Punto {
  id: number;
  nombre: string;
  latitud: number;
  longitud: number;
  municipio_id: number; 
}

@Injectable({ providedIn: 'root' })
export class PointService {

  constructor(private http: HttpClient) {}

  getPuntos(municipioId?: number): Observable<Punto[]> {
    const params: any = {};
    if (municipioId) params.municipioId = municipioId;

    return this.http.get<Punto[]>(`${API_BASE}/api/puntos`, { params });
  }
}
