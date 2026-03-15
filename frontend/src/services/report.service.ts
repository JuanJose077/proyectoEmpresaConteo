import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export type Jornada = 'GENERAL' | 'PICO' | 'VALLE';
export type DiaTipo = 'TODOS' | 'HABIL' | 'NO_HABIL';

export interface ConteoPorPuntoParams {
  municipioId?: number;
  puntoId?: number;
  fechaInicio?: string; // YYYY-MM-DD
  fechaFin?: string;    // YYYY-MM-DD
  jornada?: Jornada;
  dia?: DiaTipo;
  limite?: number;
}

export interface ConteoPorPuntoItem {
  puntoId: number;
  punto: string;
  cantidad: number;
}

export interface ActividadDiariaItem {
  fecha: string; // YYYY-MM-DD
  cantidad: number;
}

@Injectable({ providedIn: 'root' })
export class ReportService {
  constructor(private http: HttpClient) {}

  getConteoPorPunto(params: ConteoPorPuntoParams): Observable<ConteoPorPuntoItem[]> {
    let httpParams = new HttpParams();

    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        httpParams = httpParams.set(key, String(value));
      }
    });

    return this.http.get<ConteoPorPuntoItem[]>('/api/reportes/conteo-por-punto', { params: httpParams });
  }

  getActividadDiaria(params: ConteoPorPuntoParams): Observable<ActividadDiariaItem[]> {
    let httpParams = new HttpParams();

    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        httpParams = httpParams.set(key, String(value));
      }
    });

    return this.http.get<ActividadDiariaItem[]>('/api/reportes/conteo-por-punto/actividad-diaria', { params: httpParams });
  }
  getDashboard(params: {
  municipioId?: number;
  puntoId?: number;
  fechaInicio?: string;
  fechaFin?: string;
  jornada?: 'GENERAL' | 'PICO' | 'VALLE';
  dia?: 'TODOS' | 'HABIL' | 'NO_HABIL';
}) {
  return this.http.get<DashboardResponse>('/api/reportes/dashboard', { params: params as any });
}
}

export interface DashboardVehicleRow {
  claseVehiculo: string;
  total: number;
  cumple: number;
  noCumple: number;
}

export interface DashboardRow {
  behavior: string;
  label: string;
  total: number;
  cumple: number;
  noCumple: number;
  compliancePercent: number;
  targetPercent: number;
  byVehicle: DashboardVehicleRow[];

  metaByVehicle?: Record<string, number>;
  observByVehicle?: Record<string, number>;
  avanceByVehicle?: Record<string, number>;
  cumplimientoByVehicle?: Record<string, number>;
  cumpleMeta80ByVehicle?: Record<string, boolean>;
}

export interface DashboardResponse {
  table: DashboardRow[];
  metaInfo: {
    departamento: string | null;
    municipioId: number | null;
    puntoId: number | null;
    fechaInicio: string | null;
    fechaFin: string | null;
    jornada: string;
    dia: string;
  };

}

