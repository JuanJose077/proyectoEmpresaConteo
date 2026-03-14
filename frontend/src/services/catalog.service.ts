import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, tap } from 'rxjs';

export interface DepartamentosResponse {
  items: string[];
}

export interface MunicipioItem {
  id: number;
  nombre: string;
}

export interface MunicipiosResponse {
  departamento: string;
  items: MunicipioItem[];
}
export interface PuntoItem {
  id: number;
  nombre: string;
  latitud?: number;
  longitud?: number;
  municipio_id?: number;
}
@Injectable({ providedIn: 'root' })
export class CatalogService {
  private departamentosCache: string[] | null = null;
  private municipiosCache = new Map<string, MunicipioItem[]>();
  private todosLosDepartamentosCache: string[] | null = null;

  constructor(private http: HttpClient) {}

  /** Departamentos (cache 1 vez) */
  getDepartamentos(force = false): Observable<DepartamentosResponse> {
    if (!force && this.departamentosCache) {
      return of({ items: this.departamentosCache });
    }

    return this.http
      .get<DepartamentosResponse>('/api/catalogo/departamentos')
      .pipe(
        tap((res) => {
          this.departamentosCache = res.items || [];
        }),
      );
  }

  /** Municipios por departamento (cache por departamento) */
  getMunicipios(
    departamento: string,
    force = false,
  ): Observable<MunicipiosResponse> {
    const key = departamento.trim();

    if (!force && this.municipiosCache.has(key)) {
      return of({ departamento: key, items: this.municipiosCache.get(key)! });
    }

    return this.http
      .get<MunicipiosResponse>('/api/catalogo/municipios', {
        params: { departamento: key },
      })
      .pipe(
        tap((res) => {
          this.municipiosCache.set(key, res.items || []);
        }),
      );
  }
  getPuntos(municipioId: number): Observable<PuntoItem[]> {
    return this.http.get<PuntoItem[]>(`/api/puntos?municipioId=${municipioId}`);
  }
  

  /** Para precargar al iniciar sesión */
  preloadDepartamentos(): void {
    this.getDepartamentos().subscribe({ error: () => {} });
  }

  getTodosLosDepartamentos(force = false): Observable<DepartamentosResponse> {
  if (!force && this.todosLosDepartamentosCache) {
    return of({ items: this.todosLosDepartamentosCache });
  }

  return this.http
    .get<DepartamentosResponse>('/api/catalogo/departamentos-todos')
    .pipe(
      tap((res) => {
        this.todosLosDepartamentosCache = res.items || [];
      }),
    );
}
}
