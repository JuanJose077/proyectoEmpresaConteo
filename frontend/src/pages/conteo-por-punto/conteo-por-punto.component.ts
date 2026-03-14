import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DiaTipo, Jornada, ReportService } from '../../services/report.service';
import { PointService, Punto } from '../../services/point.service';
import { NgFor, NgIf, DecimalPipe } from '@angular/common';
@Component({
  selector: 'app-conteo-por-punto',
  standalone: true,
imports: [FormsModule, NgFor, NgIf, DecimalPipe],
  templateUrl: './conteo-por-punto.component.html',
  styleUrl: './conteo-por-punto.component.scss'
})
export class ConteoPorPuntoComponent implements OnInit {

  data: any;
  puntos: Punto[] = [];

  filters: {
    municipioId?: number;
    puntoId?: number;
    fechaInicio?: string;
    fechaFin?: string;
    jornada: Jornada;
    dia: DiaTipo;
  } = {
    municipioId: 44560,
    puntoId: undefined,
    fechaInicio: '2025-01-01',
    fechaFin: '2025-12-31',
    jornada: 'PICO',
    dia: 'HABIL'
  };

  constructor(
    private reportService: ReportService,
    private pointService: PointService
  ) {}

  ngOnInit(): void {
    this.loadPuntos();
    this.load();
  }

  loadPuntos() {
    if (!this.filters.municipioId) {
      this.puntos = [];
      return;
    }

    this.pointService.getPuntos(this.filters.municipioId)
      .subscribe(p => this.puntos = p);
  }

  load() {
    const params = {
      ...this.filters,
      municipioId: this.filters.municipioId || undefined,
      puntoId: this.filters.puntoId || undefined
    };

    this.reportService.getConteoPorPunto(params)
      .subscribe(res => this.data = res);
  }
}