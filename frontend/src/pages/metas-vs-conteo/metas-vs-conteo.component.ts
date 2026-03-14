import { Component, OnInit, ViewChild, ElementRef } from '@angular/core';
import { CommonModule, JsonPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  ReportService,
  DashboardResponse,
  DashboardRow,
} from '../../services/report.service';
import {
  CatalogService,
  MunicipioItem,
  PuntoItem,
} from '../../services/catalog.service';
import { UiStateService } from '../../services/ui-state.service';

import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartData } from 'chart.js';
import jsPDF from 'jspdf';
import autoTable from 'jspdf-autotable';
import html2canvas from 'html2canvas';

type Jornada = 'GENERAL' | 'PICO' | 'VALLE';
type DiaTipo = 'TODOS' | 'HABIL' | 'NO_HABIL';

@Component({
  selector: 'app-metas-vs-conteo',
  standalone: true,
  imports: [CommonModule, FormsModule, JsonPipe, BaseChartDirective],
  templateUrl: './metas-vs-conteo.component.html',
  styleUrls: ['./metas-vs-conteo.component.scss'],
})
export class MetasVsConteoComponent implements OnInit {
  @ViewChild(BaseChartDirective) chart?: BaseChartDirective;
  @ViewChild('chartCanvas') chartCanvasRef?: ElementRef<HTMLCanvasElement>;
  @ViewChild('chartCard') chartCardRef?: ElementRef<HTMLDivElement>;
  @ViewChild('tableWrap') tableWrapRef?: ElementRef<HTMLDivElement>;
  data?: DashboardResponse;

  loading = false;
  loadingCatalogo = false;
  error?: string;

  departamentos: string[] = [];
  municipios: MunicipioItem[] = [];
  puntos: PuntoItem[] = [];

  departamentoSeleccionado = '';
  municipioSeleccionadoId: number | null = null;
  puntoSeleccionadoId: number | null = null;

  readonly vehicleColors: Record<string, { bg: string; border: string }> = {
    Automóvil: {
      bg: 'rgba(59, 130, 246, 0.75)',
      border: 'rgba(59, 130, 246, 1)',
    },
    Camioneta: {
      bg: 'rgba(16, 185, 129, 0.75)',
      border: 'rgba(16, 185, 129, 1)',
    },
    Campero: {
      bg: 'rgba(245, 158, 11, 0.75)',
      border: 'rgba(245, 158, 11, 1)',
    },
    Taxi: { bg: 'rgba(234, 179, 8, 0.75)', border: 'rgba(234, 179, 8, 1)' },
    Motocicleta: {
      bg: 'rgba(239, 68, 68, 0.75)',
      border: 'rgba(239, 68, 68, 1)',
    },
    Bicicleta: {
      bg: 'rgba(168, 85, 247, 0.75)',
      border: 'rgba(168, 85, 247, 1)',
    },
    Peatón: { bg: 'rgba(20, 184, 166, 0.75)', border: 'rgba(20, 184, 166, 1)' },
  };

  showChart = false;
  private readonly NO_APLICA: Record<string, string[]> = {
    'Uso del cinturón de seguridad': ['MOTOCICLETA', 'BICICLETA', 'PEATÓN'],
    'Uso de sistemas de retención infantil': [
      'MOTOCICLETA',
      'BICICLETA',
      'PEATÓN',
    ],
    'Cumplimiento del límite de velocidad': ['PEATÓN'],
    'Uso del sistema de luces': ['PEATÓN'],
    'Uso del casco': ['AUTOMÓVIL', 'CAMIONETA', 'CAMPERO', 'TAXI', 'PEATÓN'],
    'Uso de prendas reflectivas': [
      'AUTOMÓVIL',
      'CAMIONETA',
      'CAMPERO',
      'TAXI',
      'PEATÓN',
    ],
    Sobreocupación: ['PEATÓN'],
    'Maniobras de riesgo en vía': ['PEATÓN'],
    'Comportamiento de peatones en cruces': [
      'AUTOMÓVIL',
      'CAMIONETA',
      'CAMPERO',
      'TAXI',
      'MOTOCICLETA',
      'BICICLETA',
    ],
  };

  aplica(label: string, vehiculo: string): boolean {
    const key = Object.keys(this.NO_APLICA).find((k) =>
      label.toLowerCase().includes(k.toLowerCase()),
    );
    if (!key) return true;
    return !this.NO_APLICA[key]
      .map((v) => v.toLowerCase())
      .includes(vehiculo.toLowerCase());
  }

  readonly vehicleColumns: string[] = [
    'Automóvil',
    'Camioneta',
    'Campero',
    'Taxi',
    'Motocicleta',
    'Bicicleta',
    'Peatón',
  ];

  filters: {
    fechaInicio: string;
    fechaFin: string;
    jornada: Jornada;
    dia: DiaTipo;
  } = {
    fechaInicio: '',
    fechaFin: '',
    jornada: 'GENERAL',
    dia: 'TODOS',
  };

  barChartType: 'bar' = 'bar';

  barChartData: ChartData<'bar' | 'line'> = {
    labels: [],
    datasets: [],
  };

  barChartOptions: ChartConfiguration<'bar' | 'line'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    scales: {
      x: {
        ticks: {
          maxRotation: 45,
          minRotation: 45,
        },
      },
      y: {
        beginAtZero: true,
        suggestedMax: 300,
        ticks: {
          callback: (value) => `${value}%`,
        },
      },
    },
    plugins: {
      legend: {
        display: true,
        position: 'bottom',
      },
      tooltip: {
        callbacks: {
          label: (context) => `${context.dataset.label}: ${context.parsed.y}%`,
        },
      },
    },
  };

  constructor(
    private reportService: ReportService,
    private catalogService: CatalogService,
    private uiState: UiStateService,
  ) {}

  ngOnInit(): void {
    const saved = this.uiState.getMetasVsConteoState();
    this.departamentoSeleccionado = saved.departamentoSeleccionado ?? '';
    this.municipioSeleccionadoId = saved.municipioSeleccionadoId ?? null;
    this.puntoSeleccionadoId = saved.puntoSeleccionadoId ?? null;
    this.filters = {
      ...this.filters,
      ...(saved.filters ?? {}),
    };
    this.data = saved.data;

    this.cargarDepartamentos();

    if (this.data && this.showChart) {
      this.buildChart();
    }
  }

  private persistState() {
    this.uiState.setMetasVsConteoState({
      departamentoSeleccionado: this.departamentoSeleccionado,
      municipioSeleccionadoId: this.municipioSeleccionadoId,
      puntoSeleccionadoId: this.puntoSeleccionadoId,
      filters: { ...this.filters },
      data: this.data,
    });
  }

  cargarDepartamentos() {
    this.loadingCatalogo = true;
    this.catalogService.getDepartamentos().subscribe({
      next: (res) => {
        this.error = undefined;
        this.departamentos = res.items || [];
        this.loadingCatalogo = false;

        if (this.departamentoSeleccionado) {
          this.cargarMunicipios(this.departamentoSeleccionado);
        }
      },
      error: () => {
        this.error = 'No se pudo cargar la lista de departamentos';
        this.loadingCatalogo = false;
      },
    });
  }

  private cargarMunicipios(departamento: string) {
    this.loadingCatalogo = true;
    this.catalogService.getMunicipios(departamento).subscribe({
      next: (res) => {
        this.error = undefined;
        this.municipios = res.items || [];
        this.loadingCatalogo = false;

        if (this.municipioSeleccionadoId) {
          this.cargarPuntos(this.municipioSeleccionadoId);
        }
      },
      error: () => {
        this.error = 'No se pudo cargar la lista de municipios';
        this.loadingCatalogo = false;
      },
    });
  }

  getMunicipioNombre(): string {
    if (!this.municipioSeleccionadoId) return 'Todos';
    const found = this.municipios.find(
      (m) => m.id === this.municipioSeleccionadoId,
    );
    return found?.nombre ?? '—';
  }
  getPuntoNombre(): string {
    if (!this.puntoSeleccionadoId) return 'Todos';
    const found = this.puntos.find((p) => p.id === this.puntoSeleccionadoId);
    return found?.nombre ?? '—';
  }
  private cargarPuntos(municipioId: number) {
    this.loadingCatalogo = true;
    this.catalogService.getPuntos(municipioId).subscribe({
      next: (res) => {
        this.error = undefined;
        this.puntos = res || [];
        this.loadingCatalogo = false;
      },
      error: () => {
        this.error = 'No se pudo cargar la lista de puntos';
        this.puntos = [];
        this.loadingCatalogo = false;
      },
    });
  }

  onDepartamentoChange() {
    this.error = undefined;
    this.municipioSeleccionadoId = null;
    this.puntoSeleccionadoId = null;
    this.municipios = [];
    this.puntos = [];
    this.data = undefined;

    if (this.showChart) {
      this.barChartData = { labels: [], datasets: [] };
    }

    this.persistState();

    if (!this.departamentoSeleccionado) return;

    this.cargarMunicipios(this.departamentoSeleccionado);
  }

  onMunicipioChange() {
    this.puntoSeleccionadoId = null;
    this.puntos = [];
    this.data = undefined;

    if (this.showChart) {
      this.barChartData = { labels: [], datasets: [] };
    }

    if (this.municipioSeleccionadoId) {
      this.cargarPuntos(this.municipioSeleccionadoId);
    }

    this.persistState();
  }
  onPuntoChange() {
    this.persistState();
  }

  onFilterChange() {
    this.persistState();
  }

  load() {
    this.error = undefined;

    if (!this.departamentoSeleccionado) {
      this.error = 'Selecciona un departamento.';
      return;
    }

    this.loading = true;

    const params: any = {
      departamento: this.departamentoSeleccionado,
      jornada: this.filters.jornada,
      dia: this.filters.dia,
    };

    if (this.municipioSeleccionadoId) {
      params.municipioId = this.municipioSeleccionadoId;
    }
    if (this.puntoSeleccionadoId) {
      params.puntoId = this.puntoSeleccionadoId;
    }

    if (this.filters.fechaInicio?.trim()) {
      params.fechaInicio = this.filters.fechaInicio.trim();
    }

    if (this.filters.fechaFin?.trim()) {
      params.fechaFin = this.filters.fechaFin.trim();
    }

    this.reportService.getDashboard(params).subscribe({
      next: (res) => {
        this.data = res;
        this.loading = false;

        if (this.showChart) {
          this.buildChart();
        }

        this.persistState();
      },
      error: (err) => {
        this.error = err?.message || 'Error cargando dashboard';
        this.loading = false;
      },
    });
  }

  fmtPct(n: number | undefined | null): string {
    if (n === undefined || n === null) return '0.00';
    return Number(n).toFixed(2);
  }

  valueOf(
    map: Record<string, number> | undefined,
    key: string,
    fallback = 0,
  ): number {
    if (!map) return fallback;
    return map[key] ?? fallback;
  }

  statusOf(map: Record<string, boolean> | undefined, key: string): boolean {
    if (!map) return false;
    return !!map[key];
  }

  cumpleMeta(row: DashboardRow | any): boolean {
    return (row?.compliancePercent ?? 0) >= (row?.targetPercent ?? 0);
  }

  getAvanceClass(value: number): string {
    if (value >= 80) return 'avance-green';
    if (value >= 50) return 'avance-yellow';
    return 'avance-red';
  }


  toggleChart() {
    this.showChart = !this.showChart;

    if (this.showChart) {
      this.buildChart();
    }
  }

  private getMetaResumen(): string {
    if (!this.data) return '';

    return [
      `Departamento: ${this.departamentoSeleccionado || 'Todos'}`,
      `Municipio: ${this.getMunicipioNombre()}`,
      `Punto: ${this.getPuntoNombre()}`,
      `Rango: ${this.data.metaInfo.fechaInicio ?? '—'} a ${this.data.metaInfo.fechaFin ?? '—'}`,
      `Jornada: ${this.data.metaInfo.jornada}`,
      `Día: ${this.data.metaInfo.dia}`,
    ].join(' | ');
  }

  async exportToPdf(): Promise<void> {
    if (!this.data) return;

    const wasChartVisible = this.showChart;

    try {
      if (!this.showChart) {
        this.showChart = true;
        this.buildChart();
        await this.wait(400);
      }

      this.chart?.update();
      await this.wait(200);

      const doc = new jsPDF({
        orientation: 'landscape',
        unit: 'mm',
        format: 'a4',
      });

      const pageWidth = doc.internal.pageSize.getWidth();
      const pageHeight = doc.internal.pageSize.getHeight();

      doc.setFontSize(18);
      doc.text('Reporte Metas vs Conteo', 14, 14);

      doc.setFontSize(9);
      const subtitleLines = doc.splitTextToSize(
        this.getReportSubtitle(),
        pageWidth - 28,
      );
      doc.text(subtitleLines, 14, 22);

      let currentY = 22 + subtitleLines.length * 5 + 6;

      const chartInstance: any = this.chart?.chart;
      if (chartInstance) {
        const imgData = chartInstance.toBase64Image();

        doc.setFontSize(12);
        doc.text('Gráfica', 14, currentY);
        currentY += 4;

        const imgWidth = pageWidth - 28;
        const imgHeight = 95;

        doc.addImage(
          imgData,
          'PNG',
          14,
          currentY,
          imgWidth,
          imgHeight,
          undefined,
          'FAST',
        );
        currentY += imgHeight + 10;
      }

      if (currentY > pageHeight - 60) {
        doc.addPage();
        currentY = 14;
      }

      doc.setFontSize(12);
      doc.text('Tabla', 14, currentY);
      currentY += 3;

      autoTable(doc, {
        startY: currentY,
        head: [
          ['Comportamiento vial observado', 'Dato', ...this.vehicleColumns],
        ],
        body: this.buildPdfTableRows(),
        theme: 'grid',
        margin: { left: 10, right: 10 },
        styles: {
          fontSize: 7.5,
          cellPadding: 2,
          overflow: 'linebreak',
          valign: 'middle',
          textColor: [31, 41, 55],
        },
        headStyles: {
          fillColor: [15, 23, 42],
          textColor: 255,
          fontStyle: 'bold',
        },
        alternateRowStyles: {
          fillColor: [248, 250, 252],
        },
        columnStyles: {
          0: { cellWidth: 58 },
          1: { cellWidth: 20 },
        },
        didParseCell: (data) => {
          if (data.section === 'body' && data.column.index === 1) {
            const value = String(data.cell.raw || '');
            if (value === 'Avance') {
              data.cell.styles.fillColor = [254, 242, 242];
              data.cell.styles.textColor = [185, 28, 28];
            }
            if (value === 'Cumplimiento') {
              data.cell.styles.fillColor = [239, 246, 255];
              data.cell.styles.textColor = [29, 78, 216];
            }
            if (value === 'Meta 80%') {
              data.cell.styles.fontStyle = 'bold';
            }
          }
        },
      });

      doc.save(this.getFileName('pdf'));
    } finally {
      if (!wasChartVisible) {
        this.showChart = false;
      }
    }
  }
  private csvEscape(value: any): string {
    const text = String(value ?? '');
    return `"${text.replace(/"/g, '""')}"`;
  }

  exportToCsv(): void {
    if (!this.data?.table?.length) return;

    const SEP = ';';
    const lines: string[] = [];

    lines.push(`sep=${SEP}`);

    lines.push(this.csvEscape('Reporte Metas vs Conteo'));
    lines.push(this.csvEscape(this.getReportSubtitle()));
    lines.push('');

    const headers = [
      'Comportamiento vial observado',
      'Dato',
      ...this.vehicleColumns,
    ];
    lines.push(headers.map((h) => this.csvEscape(h)).join(SEP));

    // Filas
    this.data.table.forEach((row, index) => {
      const nombre = `${index + 1}. ${row.label}`;

      const tiposFilas: Array<{
        dato: string;
        getValue: (v: string) => string;
      }> = [
        {
          dato: 'Meta',
          getValue: (v) =>
            this.aplica(row.label, v)
              ? String(this.valueOf(row.metaByVehicle, v, 0))
              : 'N/A',
        },
        {
          dato: 'Observ.',
          getValue: (v) =>
            this.aplica(row.label, v)
              ? String(this.valueOf(row.observByVehicle, v, 0))
              : 'N/A',
        },
        {
          dato: 'Avance',
          getValue: (v) =>
            this.aplica(row.label, v)
              ? `${this.fmtPct(this.valueOf(row.avanceByVehicle, v, 0))}%`
              : 'N/A',
        },
        {
          dato: 'Cumplimiento',
          getValue: (v) =>
            this.aplica(row.label, v)
              ? `${this.fmtPct(this.valueOf(row.cumplimientoByVehicle, v, 0))}%`
              : 'N/A',
        },
        {
          dato: 'Meta 80%',
          getValue: (v) =>
            this.aplica(row.label, v)
              ? this.statusOf(row.cumpleMeta80ByVehicle, v)
                ? 'Cumple'
                : 'No cumple'
              : 'N/A',
        },
      ];

      tiposFilas.forEach(({ dato, getValue }, i) => {
        const comportamiento = i === 0 ? nombre : '';
        lines.push(
          [comportamiento, dato, ...this.vehicleColumns.map(getValue)]
            .map((v) => this.csvEscape(v))
            .join(SEP),
        );
      });
    });

    const csvContent = '\uFEFF' + lines.join('\n');
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });

    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = this.getFileName('csv');
    a.click();
    window.URL.revokeObjectURL(url);
  }
  private wait(ms = 250): Promise<void> {
    return new Promise((resolve) => setTimeout(resolve, ms));
  }

  private getFileName(ext: 'pdf' | 'csv'): string {
    const dep = (this.departamentoSeleccionado || 'todos')
      .toLowerCase()
      .replace(/\s+/g, '-');
    const mun = (this.getMunicipioNombre() || 'todos')
      .toLowerCase()
      .replace(/\s+/g, '-');
    const fecha = new Date().toISOString().slice(0, 19).replace(/[:T]/g, '-');
    return `metas-vs-conteo_${dep}_${mun}_${fecha}.${ext}`;
  }

  private getReportSubtitle(): string {
    if (!this.data) return '';

    return [
      `Departamento: ${this.departamentoSeleccionado || 'Todos'}`,
      `Municipio: ${this.getMunicipioNombre() || 'Todos'}`,
      `Punto: ${this.getPuntoNombre() || 'Todos'}`,
      `Rango: ${this.data.metaInfo?.fechaInicio || '—'} a ${this.data.metaInfo?.fechaFin || '—'}`,
      `Jornada: ${this.data.metaInfo?.jornada || '—'}`,
      `Día: ${this.data.metaInfo?.dia || '—'}`,
    ].join(' | ');
  }

  private buildPdfTableRows(): string[][] {
    if (!this.data?.table?.length) return [];

    const rows: string[][] = [];

    this.data.table.forEach((row, index) => {
      const nombre = `${index + 1}. ${row.label}`;

      rows.push([
        nombre,
        'Meta',
        ...this.vehicleColumns.map((v) =>
          this.aplica(row.label, v)
            ? String(this.valueOf(row.metaByVehicle, v, 0))
            : 'N/A',
        ),
      ]);

      rows.push([
        '',
        'Observ.',
        ...this.vehicleColumns.map((v) =>
          this.aplica(row.label, v)
            ? String(this.valueOf(row.observByVehicle, v, 0))
            : 'N/A',
        ),
      ]);

      rows.push([
        '',
        'Avance',
        ...this.vehicleColumns.map((v) =>
          this.aplica(row.label, v)
            ? `${this.fmtPct(this.valueOf(row.avanceByVehicle, v, 0))}%`
            : 'N/A',
        ),
      ]);

      rows.push([
        '',
        'Cumplimiento',
        ...this.vehicleColumns.map((v) =>
          this.aplica(row.label, v)
            ? `${this.fmtPct(this.valueOf(row.cumplimientoByVehicle, v, 0))}%`
            : 'N/A',
        ),
      ]);

      rows.push([
        '',
        'Meta 80%',
        ...this.vehicleColumns.map((v) =>
          this.aplica(row.label, v)
            ? this.statusOf(row.cumpleMeta80ByVehicle, v)
              ? 'Cumple'
              : 'No cumple'
            : 'N/A',
        ),
      ]);
    });

    return rows;
  }

  buildChart() {
    if (!this.data?.table?.length) {
      this.barChartData = { labels: [], datasets: [] };
      return;
    }

    const labels = [
      '1. Cinturón',
      '2. Retención infantil',
      '3. Velocidad',
      '4. Luces',
      '5. Casco',
      '6. Reflectivas',
      '7. Sobreocupación',
      '8. Distractores',
      '9. Maniobras',
      '10. Peatones',
    ];

    const datasets = this.vehicleColumns.map((vehicle) => {
      const colors = this.vehicleColors[vehicle];

      return {
        type: 'bar' as const,
        label: vehicle,
        data: this.data!.table.map((row) =>
          this.valueOf(row.avanceByVehicle, vehicle, 0),
        ),
        backgroundColor: colors.bg,
        borderColor: colors.border,
        borderWidth: 1,
        borderRadius: 4,
      };
    });

    const targetLine = {
      type: 'line' as const,
      label: 'Meta 100%',
      data: this.data.table.map(() => 100),
      borderWidth: 2,
      pointRadius: 0,
      fill: false,
    };

    this.barChartData = {
      labels,
      datasets: [...datasets, targetLine],
    };
  }
}
