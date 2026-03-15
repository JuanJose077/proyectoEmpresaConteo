import { Component, OnInit } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { forkJoin } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { HttpClientModule } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { CatalogService } from '../../services/catalog.service';
import { environment } from '../../enviroments/enviroment';

const API_BASE = environment.apiUrl.startsWith('http')
  ? environment.apiUrl
  : `https://${environment.apiUrl}`;

interface ResumenCobertura {
  totalMunicipios: number;
  municipiosConDatos: number;
  municipiosSinDatos: number;
  cobertura: number;
}

interface DetalleMunicipio {
  municipioId: number;
  municipio: string;
  tieneDatos: boolean;
  conteo: number;
  ultimaToma: string | null;
}

interface RankingMunicipio {
  municipioId: number;
  municipio: string;
  conteo: number;
}

interface FiltrosCobertura {
  departamento: string;
  fechaInicio: string;
  fechaFin: string;
  jornada: string;
  dia: string;
}

@Component({
  selector: 'app-metas-por-municipio',
  standalone: true,
  imports: [CommonModule, FormsModule, HttpClientModule],
  templateUrl: './metas-por-municipio.component.html',
  styleUrls: ['./metas-por-municipio.component.scss'],
})
export class MetasPorMunicipioComponent implements OnInit {
  departamentos: string[] = [];

  filters: FiltrosCobertura = {
    departamento: '',
    fechaInicio: '',
    fechaFin: '',
    jornada: 'GENERAL',
    dia: 'TODOS',
  };

  loading = false;
  error = '';

  resumen: ResumenCobertura | null = null;
  detalleMunicipios: DetalleMunicipio[] = [];
  municipiosSinDatos: DetalleMunicipio[] = [];
  rankingMas: RankingMunicipio[] = [];
  rankingMenos: RankingMunicipio[] = [];

  results: {
    total: number;
    conDatos: number;
    sinDatos: number;
  } | null = null;

  constructor(
    private http: HttpClient,
    private catalogService: CatalogService,
  ) {}

  ngOnInit(): void {
    this.cargarDepartamentos();
  }

  cargarDepartamentos(): void {
    this.catalogService.getTodosLosDepartamentos().subscribe({
      next: (res) => {
        this.error = '';
        this.departamentos = res.items || [];
      },
      error: (err) => {
        console.error('Error cargando departamentos', err);
        this.error = 'No se pudieron cargar los departamentos.';
      },
    });
  }

  consultar(): void {
    if (!this.filters.departamento) {
      this.error = 'Debes seleccionar un departamento.';
      return;
    }

    this.loading = true;
    this.error = '';

    let baseParams = new HttpParams().set(
      'departamento',
      this.filters.departamento,
    );

    if (this.filters.fechaInicio) {
      baseParams = baseParams.set('fechaInicio', this.filters.fechaInicio);
    }
    if (this.filters.fechaFin) {
      baseParams = baseParams.set('fechaFin', this.filters.fechaFin);
    }
    if (this.filters.jornada) {
      baseParams = baseParams.set('jornada', this.filters.jornada);
    }
    if (this.filters.dia) {
      baseParams = baseParams.set('dia', this.filters.dia);
    }

    forkJoin({
      resumen: this.http.get<ResumenCobertura>(`${API_BASE}/api/cobertura/resumen`, {
        params: baseParams,
      }),
      detalle: this.http.get<DetalleMunicipio[]>(`${API_BASE}/api/cobertura/detalle`, {
        params: baseParams,
      }),
      rankingMas: this.http.get<RankingMunicipio[]>(
        `${API_BASE}/api/cobertura/ranking-mas`,
        {
          params: baseParams.set('limite', '5'),
        },
      ),
      rankingMenos: this.http.get<RankingMunicipio[]>(
        `${API_BASE}/api/cobertura/ranking-menos`,
        {
          params: baseParams.set('limite', '5'),
        },
      ),
    }).subscribe({
      next: ({ resumen, detalle, rankingMas, rankingMenos }) => {
        this.resumen = resumen ?? null;
        this.detalleMunicipios = detalle ?? [];
        this.rankingMas = rankingMas ?? [];
        this.rankingMenos = rankingMenos ?? [];

        this.municipiosSinDatos = this.detalleMunicipios.filter(
          (m) => !m.tieneDatos,
        );

        this.results = {
          total: this.resumen?.totalMunicipios ?? 0,
          conDatos: this.resumen?.municipiosConDatos ?? 0,
          sinDatos: this.resumen?.municipiosSinDatos ?? 0,
        };

        this.loading = false;
      },
      error: (err) => {
        console.error('Error consultando cobertura', err);
        this.error = 'No se pudo consultar la información de cobertura.';
        this.limpiarResultados();
        this.loading = false;
      },
    });
  }

  limpiarFiltros(): void {
    this.filters = {
      departamento: '',
      fechaInicio: '',
      fechaFin: '',
      jornada: 'GENERAL',
      dia: 'TODOS',
    };

    this.limpiarResultados();
    this.error = '';
  }

  private limpiarResultados(): void {
    this.resumen = null;
    this.detalleMunicipios = [];
    this.municipiosSinDatos = [];
    this.rankingMas = [];
    this.rankingMenos = [];
    this.results = null;
  }
}
