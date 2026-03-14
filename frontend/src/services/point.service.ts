import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Punto {
  id: number;
  nombre: string;
  latitud: number;
  longitud: number;
  municipio_id: number; // viene así desde MySQL / API
}

@Injectable({ providedIn: 'root' })
export class PointService {

  constructor(private http: HttpClient) {}

  getPuntos(municipioId?: number): Observable<Punto[]> {
    const params: any = {};
    if (municipioId) params.municipioId = municipioId;

    return this.http.get<Punto[]>('/api/puntos', { params });
  }
}