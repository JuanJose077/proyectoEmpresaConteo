import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
  ActividadDiariaItem,
  ConteoPorPuntoItem,
  DiaTipo,
  Jornada,
  ReportService,
} from '../../services/report.service';
import {
  CatalogService,
  MunicipioItem,
  PuntoItem,
} from '../../services/catalog.service';
import { NgFor, NgIf, DecimalPipe, NgClass } from '@angular/common';
import { forkJoin, of } from 'rxjs';

type ActivityLevel = 'none' | 'low' | 'high' | 'out';

interface CalendarDay {
  date: string;
  count: number;
  level: ActivityLevel;
  inRange: boolean;
}

export interface CalendarMonthBlock {
  label: string;   // e.g. "Mar 2024"
  weekCount: number;
}

export interface CalendarStats {
  totalConteos: number;
  diasConActividad: number;
  diaMasActivo: string;   // fecha formateada
  diaMasActivoCount: number;
  promedioDiario: number;
}

export interface JornadaStat {
  label: string;
  value: number;
  pct: number;
  color: string;
}

@Component({
  selector: 'app-conteo-por-punto',
  standalone: true,
  imports: [FormsModule, NgFor, NgIf, DecimalPipe, NgClass],
  templateUrl: './conteo-por-punto.component.html',
  styleUrl: './conteo-por-punto.component.scss'
})
export class ConteoPorPuntoComponent implements OnInit {

  /* ── Catálogo ──────────────────────────────────────────── */
  departamentos: string[] = [];
  municipios: MunicipioItem[] = [];
  puntos: PuntoItem[] = [];

  departamentoSeleccionado = '';
  municipioSeleccionadoId: number | null = null;
  puntoSeleccionadoId: number | null = null;

  /* ── Estado ────────────────────────────────────────────── */
  ranking: ConteoPorPuntoItem[] = [];
  actividad: ActividadDiariaItem[] = [];
  calendarWeeks: CalendarDay[][] = [];
  calendarMonthBlocks: CalendarMonthBlock[] = [];
  calendarStats: CalendarStats | null = null;
  totalConteoPunto: number | null = null;   // total para el punto seleccionado
  jornadaStats: JornadaStat[] = [];
  diaStats: JornadaStat[] = [];

  maxRanking = 0;
  maxActividad = 0;
  loading = false;
  loadingCatalogo = false;
  loadingJornada = false;
  error?: string;

  readonly weekdayLabels = ['Lu', 'Ma', 'Mi', 'Ju', 'Vi', 'Sa', 'Do'];
  readonly rankingLimit = 10;

  filters: {
    fechaInicio?: string;
    fechaFin?: string;
    jornada: Jornada;
    dia: DiaTipo;
  } = {
    fechaInicio: undefined,
    fechaFin: undefined,
    jornada: 'GENERAL',
    dia: 'TODOS',
  };

  constructor(
    private reportService: ReportService,
    private catalogService: CatalogService,
  ) {}

  ngOnInit(): void {
    this.cargarDepartamentos();
  }

  /* ── Carga de catálogo ─────────────────────────────────── */

  cargarDepartamentos() {
    this.loadingCatalogo = true;
    this.catalogService.getDepartamentos().subscribe({
      next: (res) => {
        this.departamentos = res.items || [];
        this.loadingCatalogo = false;
      },
      error: () => {
        this.error = 'No se pudo cargar la lista de departamentos.';
        this.loadingCatalogo = false;
      },
    });
  }

  private cargarMunicipios(departamento: string) {
    this.loadingCatalogo = true;
    this.catalogService.getMunicipios(departamento).subscribe({
      next: (res) => {
        this.municipios = res.items || [];
        this.loadingCatalogo = false;
      },
      error: () => {
        this.error = 'No se pudo cargar la lista de municipios.';
        this.loadingCatalogo = false;
      },
    });
  }

  private cargarPuntos(municipioId: number) {
    this.loadingCatalogo = true;
    this.catalogService.getPuntos(municipioId).subscribe({
      next: (res) => {
        this.puntos = res || [];
        this.loadingCatalogo = false;
      },
      error: () => {
        this.error = 'No se pudo cargar la lista de puntos.';
        this.puntos = [];
        this.loadingCatalogo = false;
      },
    });
  }

  /* ── Handlers de cambio ────────────────────────────────── */

  onDepartamentoChange() {
    this.error = undefined;
    this.municipioSeleccionadoId = null;
    this.puntoSeleccionadoId = null;
    this.municipios = [];
    this.puntos = [];

    if (this.departamentoSeleccionado) {
      this.cargarMunicipios(this.departamentoSeleccionado);
    }
  }

  onMunicipioChange() {
    this.error = undefined;
    this.puntoSeleccionadoId = null;
    this.puntos = [];

    if (this.municipioSeleccionadoId) {
      this.cargarPuntos(this.municipioSeleccionadoId);
    }
  }

  onPuntoChange() {
    this.error = undefined;
  }

  onFilterChange() {
    this.error = undefined;
  }

  /* ── Helpers de nombre ─────────────────────────────────── */

  getMunicipioNombre(): string {
    if (!this.municipioSeleccionadoId) return 'Todos';
    return this.municipios.find(m => m.id === this.municipioSeleccionadoId)?.nombre ?? '—';
  }

  getPuntoNombre(): string {
    if (!this.puntoSeleccionadoId) return 'Todos';
    return this.puntos.find(p => p.id === this.puntoSeleccionadoId)?.nombre ?? '—';
  }

  /* ── Carga de datos ────────────────────────────────────── */

  load() {
    if (!this.departamentoSeleccionado) return;

    this.loading = true;
    this.error = undefined;
    this.jornadaStats = [];
    this.diaStats = [];
    this.totalConteoPunto = null;

    const baseParams = this.buildParams();

    // Ranking: nunca filtra por punto individual
    const rankingParams = {
      departamento: baseParams.departamento,
      municipioId: baseParams.municipioId,
      fechaInicio: baseParams.fechaInicio,
      fechaFin: baseParams.fechaFin,
      jornada: baseParams.jornada,
      dia: baseParams.dia,
      limite: this.rankingLimit,
    };

    const actividad$ = this.puntoSeleccionadoId
      ? this.reportService.getActividadDiaria(baseParams)
      : of<ActividadDiariaItem[]>([]);

    // Total del punto seleccionado (sin filtro de jornada para mostrar el total real)
    const totalPunto$ = this.puntoSeleccionadoId
      ? this.reportService.getConteoPorPunto({
          ...rankingParams,
          puntoId: this.puntoSeleccionadoId,
          limite: 1,
        })
      : of<ConteoPorPuntoItem[]>([]);

    // Distribución por jornada: tres llamadas paralelas fijando cada jornada
    const jornadaBase = {
      departamento: baseParams.departamento,
      municipioId: baseParams.municipioId,
      puntoId: baseParams.puntoId,
      fechaInicio: baseParams.fechaInicio,
      fechaFin: baseParams.fechaFin,
      dia: baseParams.dia,
    };
    const jornada$ = forkJoin({
      general: this.reportService.getConteoPorPunto({ ...jornadaBase, jornada: 'GENERAL' as Jornada }),
      pico:    this.reportService.getConteoPorPunto({ ...jornadaBase, jornada: 'PICO'    as Jornada }),
      valle:   this.reportService.getConteoPorPunto({ ...jornadaBase, jornada: 'VALLE'   as Jornada }),
    });

    // Distribución por tipo de día: TODOS como total, HABIL y NO_HABIL como segmentos
    const diaBase = {
      departamento: baseParams.departamento,
      municipioId:  baseParams.municipioId,
      puntoId:      baseParams.puntoId,
      fechaInicio:  baseParams.fechaInicio,
      fechaFin:     baseParams.fechaFin,
      jornada:      baseParams.jornada,
    };
    const dia$ = forkJoin({
      todos:    this.reportService.getConteoPorPunto({ ...diaBase, dia: 'TODOS'    as DiaTipo }),
      habil:    this.reportService.getConteoPorPunto({ ...diaBase, dia: 'HABIL'    as DiaTipo }),
      noHabil:  this.reportService.getConteoPorPunto({ ...diaBase, dia: 'NO_HABIL' as DiaTipo }),
    });

    forkJoin({
      ranking:    this.reportService.getConteoPorPunto(rankingParams),
      actividad:  actividad$,
      totalPunto: totalPunto$,
      jornada:    jornada$,
      dia:        dia$,
    }).subscribe({
      next: ({ ranking, actividad, totalPunto, jornada, dia }) => {
        this.ranking     = ranking || [];
        this.actividad   = actividad || [];
        this.maxRanking  = this.ranking.reduce((max, item) => Math.max(max, item.cantidad), 0);

        // Total del punto seleccionado
        if (this.puntoSeleccionadoId && totalPunto?.length) {
          this.totalConteoPunto = totalPunto[0].cantidad;
        }

        // Distribución por jornada
        this.buildJornadaStats(jornada.general, jornada.pico, jornada.valle);

        // Distribución por tipo de día
        this.buildDiaStats(dia.todos, dia.habil, dia.noHabil);

        this.buildCalendar();
        this.loading = false;
      },
      error: (err) => {
        this.error = err?.message || 'No se pudo cargar la información.';
        this.ranking      = [];
        this.actividad    = [];
        this.calendarWeeks = [];
        this.maxRanking   = 0;
        this.maxActividad = 0;
        this.loading      = false;
      },
    });
  }

  private buildDiaStats(
    todos:   ConteoPorPuntoItem[],
    habil:   ConteoPorPuntoItem[],
    noHabil: ConteoPorPuntoItem[],
  ) {
    const sum = (items: ConteoPorPuntoItem[]) =>
      items.reduce((acc, i) => acc + i.cantidad, 0);

    const total   = sum(todos);
    const vHabil  = sum(habil);
    const vNoHabil = sum(noHabil);

    if (total === 0) { this.diaStats = []; return; }

    const pct = (v: number) => Math.round((v / total) * 100);

    this.diaStats = [
      { label: 'Hábil',    value: vHabil,   pct: pct(vHabil),   color: 'var(--indigo)' },
      { label: 'No hábil', value: vNoHabil, pct: pct(vNoHabil), color: '#e879f9' },
    ];
  }

  /** Calcula el stroke-dashoffset acumulado para cada segmento del donut */
  getDonutOffset(index: number, stats: JornadaStat[]): number {
    const circ = 276.46; // 2*PI*44
    let offset = 0;
    for (let i = 0; i < index; i++) {
      offset += stats[i].pct * (circ / 100);
    }
    return circ - offset;
  }

  barWidth(value: number): number {
    if (!this.maxRanking) return 0;
    return Math.round((value / this.maxRanking) * 100);
  }

  /** Convierte '2024-03-15' → 'Vie 15 Mar 2024' para el tooltip del calendario */
  formatDateLabel(dateStr: string): string {
    const date = new Date(`${dateStr}T00:00:00`);
    if (Number.isNaN(date.getTime())) return dateStr;
    const dias  = ['Dom', 'Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb'];
    const meses = ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun',
                   'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic'];
    return `${dias[date.getDay()]} ${date.getDate()} ${meses[date.getMonth()]} ${date.getFullYear()}`;
  }

  /* ── Params ────────────────────────────────────────────── */

  private buildParams() {
    return {
      departamento: this.departamentoSeleccionado || undefined,
      municipioId:  this.municipioSeleccionadoId ?? undefined,
      puntoId:      this.puntoSeleccionadoId ?? undefined,
      fechaInicio:  this.filters.fechaInicio?.trim() || undefined,
      fechaFin:     this.filters.fechaFin?.trim() || undefined,
      jornada:      this.filters.jornada,
      dia:          this.filters.dia,
    };
  }

  /* ── Distribución jornada ──────────────────────────────── */

  private buildJornadaStats(
    general: ConteoPorPuntoItem[],
    pico:    ConteoPorPuntoItem[],
    valle:   ConteoPorPuntoItem[],
  ) {
    const sum = (items: ConteoPorPuntoItem[]) =>
      items.reduce((acc, i) => acc + i.cantidad, 0);

    // General es la suma total (Valle + Pico), se usa como denominador
    const total  = sum(general);
    const vPico  = sum(pico);
    const vValle = sum(valle);

    if (total === 0) { this.jornadaStats = []; return; }

    const pct = (v: number) => Math.round((v / total) * 100);

    this.jornadaStats = [
      { label: 'Pico',  value: vPico,  pct: pct(vPico),  color: '#f59e0b' },
      { label: 'Valle', value: vValle, pct: pct(vValle),  color: '#10b981' },
    ];
  }

  /* ── Calendario ────────────────────────────────────────── */

  private buildCalendar() {
    if (!this.puntoSeleccionadoId) {
      this.calendarWeeks      = [];
      this.calendarMonthBlocks = [];
      this.calendarStats      = null;
      this.maxActividad       = 0;
      return;
    }

    const dataMap = new Map<string, number>();
    this.actividad.forEach(item => dataMap.set(item.fecha, item.cantidad));

    const range = this.resolveRange(dataMap);
    if (!range) {
      this.calendarWeeks      = [];
      this.calendarMonthBlocks = [];
      this.calendarStats      = null;
      this.maxActividad       = 0;
      return;
    }

    const { start, end } = range;
    const startCalendar = this.startOfWeek(start);
    const endCalendar   = this.endOfWeek(end);

    const maxValue = Array.from(dataMap.values()).reduce((max, v) => Math.max(max, v), 0);
    this.maxActividad = maxValue;
    const lowThreshold = Math.max(1, Math.round(maxValue * 0.35));

    // Construir semanas
    const weeks: CalendarDay[][] = [];
    let current = new Date(startCalendar);
    let week: CalendarDay[] = [];

    while (current <= endCalendar) {
      const key     = this.formatDate(current);
      const inRange = current >= start && current <= end;
      const count   = inRange ? (dataMap.get(key) || 0) : 0;
      const level: ActivityLevel = inRange
        ? (count === 0 ? 'none' : count <= lowThreshold ? 'low' : 'high')
        : 'out';

      week.push({ date: key, count, level, inRange });

      if (week.length === 7) { weeks.push(week); week = []; }
      current = this.addDays(current, 1);
    }
    if (week.length) weeks.push(week);
    this.calendarWeeks = weeks;

    // Bloques de mes: cuántas semanas cae cada mes
    this.calendarMonthBlocks = this.buildMonthBlocks(weeks);

    // Estadísticas del calendario
    this.calendarStats = this.buildCalendarStats(dataMap);
  }

  private buildMonthBlocks(weeks: CalendarDay[][]): CalendarMonthBlock[] {
    const meses = ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun',
                   'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic'];
    const blocks: CalendarMonthBlock[] = [];

    for (const week of weeks) {
      // El mes representativo de la semana = mes del miércoles (día 3, índice 2)
      const mid   = week[Math.min(2, week.length - 1)];
      const date  = new Date(`${mid.date}T00:00:00`);
      const label = `${meses[date.getMonth()]} ${date.getFullYear()}`;

      if (blocks.length && blocks[blocks.length - 1].label === label) {
        blocks[blocks.length - 1].weekCount++;
      } else {
        blocks.push({ label, weekCount: 1 });
      }
    }
    return blocks;
  }

  private buildCalendarStats(dataMap: Map<string, number>): CalendarStats {
    const meses = ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun',
                   'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic'];

    let totalConteos      = 0;
    let diasConActividad  = 0;
    let diaMasActivo      = '';
    let diaMasActivoCount = 0;

    dataMap.forEach((count, fecha) => {
      totalConteos += count;
      if (count > 0) diasConActividad++;
      if (count > diaMasActivoCount) {
        diaMasActivoCount = count;
        diaMasActivo = fecha;
      }
    });

    const promedioDiario = diasConActividad > 0
      ? Math.round(totalConteos / diasConActividad)
      : 0;

    // Formatear diaMasActivo
    let diaMasActivoLabel = '—';
    if (diaMasActivo) {
      const d = new Date(`${diaMasActivo}T00:00:00`);
      diaMasActivoLabel = `${d.getDate()} ${meses[d.getMonth()]} ${d.getFullYear()}`;
    }

    return { totalConteos, diasConActividad, diaMasActivo: diaMasActivoLabel, diaMasActivoCount, promedioDiario };
  }

  /* ── Utilidades de fecha ───────────────────────────────── */

  private resolveRange(dataMap: Map<string, number>) {
    const start = this.parseDate(this.filters.fechaInicio);
    const end   = this.parseDate(this.filters.fechaFin);
    if (start && end) return { start, end };
    if (dataMap.size === 0) return null;
    const dates   = Array.from(dataMap.keys()).sort();
    const minDate = this.parseDate(dates[0]);
    const maxDate = this.parseDate(dates[dates.length - 1]);
    if (!minDate || !maxDate) return null;
    return { start: minDate, end: maxDate };
  }

  private parseDate(value?: string): Date | null {
    if (!value) return null;
    const date = new Date(`${value}T00:00:00`);
    return Number.isNaN(date.getTime()) ? null : date;
  }

  private formatDate(date: Date): string {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
  }

  private addDays(date: Date, days: number): Date {
    const next = new Date(date);
    next.setDate(next.getDate() + days);
    return next;
  }

  private startOfWeek(date: Date): Date {
    return this.addDays(date, -((date.getDay() + 6) % 7));
  }

  private endOfWeek(date: Date): Date {
    return this.addDays(date, 6 - ((date.getDay() + 6) % 7));
  }
}