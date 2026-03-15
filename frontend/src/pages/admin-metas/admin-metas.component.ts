import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CatalogService, MunicipioItem } from '../../services/catalog.service';
import { AdminService, MetaCell, MetaUpdate } from '../../services/admin.service';
import { HttpErrorResponse } from '@angular/common/http';

const BEHAVIOR_LABELS: Record<string, string> = {
  SEATBELT: 'Uso del cinturón de seguridad',
  CHILD_RESTRAINT: 'Uso de sistemas de retención infantil',
  SPEED: 'Cumplimiento del límite de velocidad',
  LIGHTS: 'Uso del sistema de luces',
  HELMET: 'Uso del casco',
  REFLECTIVE: 'Uso de prendas reflectivas',
  OVEROCCUPANCY: 'Sobreocupación',
  DISTRACTORES: 'Actor vial con elementos distractores',
  MANEUVERS: 'Maniobras de riesgo en vía',
  PEDESTRIAN_CROSSING: 'Comportamiento de peatones en cruces',
};

const NO_APLICA: Record<string, string[]> = {
  'Uso del cinturón de seguridad': ['MOTOCICLETA', 'BICICLETA', 'PEATON'],
  'Uso de sistemas de retención infantil': ['MOTOCICLETA', 'BICICLETA', 'PEATON'],
  'Cumplimiento del límite de velocidad': ['PEATON'],
  'Uso del sistema de luces': ['PEATON'],
  'Uso del casco': ['AUTOMOVIL', 'CAMIONETA', 'CAMPERO', 'TAXI', 'PEATON'],
  'Uso de prendas reflectivas': ['AUTOMOVIL', 'CAMIONETA', 'CAMPERO', 'TAXI', 'PEATON'],
  Sobreocupación: ['PEATON'],
  'Maniobras de riesgo en vía': ['PEATON'],
  'Comportamiento de peatones en cruces': [
    'AUTOMOVIL',
    'CAMIONETA',
    'CAMPERO',
    'TAXI',
    'MOTOCICLETA',
    'BICICLETA',
  ],
};

interface CellState {
  id: number | null;
  behavior: string;
  vehicleLabel: string;
  vehicleKey: string;
  meta: number;
  activo: boolean;
  originalMeta: number;
  originalActivo: boolean;
  disabled: boolean;
}

@Component({
  selector: 'app-admin-metas',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-metas.component.html',
  styleUrls: ['./admin-metas.component.css'],
})
export class AdminMetasComponent implements OnInit {
  departamentos: string[] = [];
  municipios: MunicipioItem[] = [];

  departamentoSeleccionado = '';
  municipioSeleccionadoId: number | null = null;

  loadingCatalogo = false;
  loadingMetas = false;
  saving = false;
  error = '';
  success = '';

  readonly vehicleColumns: string[] = [
    'Automóvil',
    'Camioneta',
    'Campero',
    'Taxi',
    'Motocicleta',
    'Bicicleta',
    'Peatón',
  ];

  readonly behaviorLabels = BEHAVIOR_LABELS;
  readonly behaviors = Object.keys(BEHAVIOR_LABELS);

  private cells = new Map<string, CellState>();
  private updates = new Map<string, MetaUpdate>();

  constructor(
    private catalogService: CatalogService,
    private adminService: AdminService,
  ) {}

  ngOnInit(): void {
    this.cargarDepartamentos();
  }

  cargarDepartamentos(): void {
    this.loadingCatalogo = true;
    this.catalogService.getDepartamentos().subscribe({
      next: (res) => {
        this.departamentos = res.items || [];
        this.loadingCatalogo = false;
      },
      error: () => {
        this.error = 'No se pudo cargar la lista de departamentos';
        this.loadingCatalogo = false;
      },
    });
  }

  onDepartamentoChange(): void {
    this.error = '';
    this.success = '';
    this.municipioSeleccionadoId = null;
    this.municipios = [];
    this.cells.clear();
    this.updates.clear();

    if (!this.departamentoSeleccionado) return;

    this.loadingCatalogo = true;
    this.catalogService.getMunicipios(this.departamentoSeleccionado).subscribe({
      next: (res) => {
        this.municipios = res.items || [];
        this.loadingCatalogo = false;
      },
      error: () => {
        this.error = 'No se pudo cargar la lista de municipios';
        this.loadingCatalogo = false;
      },
    });
  }

  onMunicipioChange(): void {
    this.error = '';
    this.success = '';
    this.cells.clear();
    this.updates.clear();

    if (!this.municipioSeleccionadoId) return;

    this.loadingMetas = true;
    this.adminService.getMetasByMunicipio(this.municipioSeleccionadoId).subscribe({
      next: (rows) => {
        this.buildCells(rows);
        this.loadingMetas = false;
      },
      error: (err: HttpErrorResponse) => {
        this.error = err.error?.message || err.error?.error || 'No se pudieron cargar las metas';
        this.loadingMetas = false;
      },
    });
  }

  buildCells(rows: MetaCell[]): void {
    const rowMap = new Map<string, MetaCell>();
    rows.forEach((row) => {
      const key = this.cellKey(row.behavior, this.vehicleKey(row.claseVehiculo));
      rowMap.set(key, row);
    });

    this.cells.clear();

    for (const behavior of this.behaviors) {
      for (const vehicleLabel of this.vehicleColumns) {
        const vKey = this.vehicleKey(vehicleLabel);
        const key = this.cellKey(behavior, vKey);
        const row = rowMap.get(key);
        const metaValue = row?.meta ?? 0;
        const activo = row?.activo ?? true;
        const disabled = this.isNoAplica(behavior, vKey);

        this.cells.set(key, {
          id: row?.id ?? null,
          behavior,
          vehicleLabel,
          vehicleKey: vKey,
          meta: metaValue,
          activo,
          originalMeta: metaValue,
          originalActivo: activo,
          disabled,
        });
      }
    }
  }

  onMetaChange(behavior: string, vehicleLabel: string, value: string | number): void {
    const key = this.cellKey(behavior, this.vehicleKey(vehicleLabel));
    const cell = this.cells.get(key);
    if (!cell) return;
    if (cell.disabled) return;

    const parsed = Number(value);
    cell.meta = Number.isFinite(parsed) ? parsed : 0;
    this.markUpdate(cell);
  }

  onActivoChange(behavior: string, vehicleLabel: string, value: boolean): void {
    const key = this.cellKey(behavior, this.vehicleKey(vehicleLabel));
    const cell = this.cells.get(key);
    if (!cell) return;
    if (cell.disabled) return;

    cell.activo = value;
    this.markUpdate(cell);
  }

  getCell(behavior: string, vehicleLabel: string): CellState {
    const key = this.cellKey(behavior, this.vehicleKey(vehicleLabel));
    return (
      this.cells.get(key) ?? {
        id: null,
        behavior,
        vehicleLabel,
        vehicleKey: this.vehicleKey(vehicleLabel),
        meta: 0,
        activo: true,
        originalMeta: 0,
        originalActivo: true,
        disabled: true,
      }
    );
  }

  hasChanges(): boolean {
    return this.updates.size > 0;
  }

  saveChanges(): void {
    this.error = '';
    this.success = '';

    const changes: MetaUpdate[] = [];
    for (const item of this.updates.values()) {
      changes.push(item);
    }

    if (!changes.length) {
      this.success = 'No hay cambios para guardar.';
      return;
    }

    this.saving = true;
    this.adminService.updateMetasBatch(changes).subscribe({
      next: (res) => {
        this.saving = false;
        this.success = `Cambios guardados: ${res.updated}`;
        for (const cell of this.cells.values()) {
          cell.originalMeta = cell.meta;
          cell.originalActivo = cell.activo;
        }
        this.updates.clear();
      },
      error: (err: HttpErrorResponse) => {
        this.saving = false;
        this.error = err.error?.message || err.error?.error || 'No se pudieron guardar los cambios';
      },
    });
  }

  labelFor(behavior: string): string {
    return this.behaviorLabels[behavior] || behavior;
  }

  isNoAplica(behavior: string, vehicleKey: string): boolean {
    const label = this.labelFor(behavior);
    const list = NO_APLICA[label] || [];
    return list.map((v) => v.toUpperCase()).includes(vehicleKey.toUpperCase());
  }

  private cellKey(behavior: string, vehicleKey: string): string {
    return `${behavior}__${vehicleKey}`;
  }

  private vehicleKey(label: string): string {
    return this.normalize(label).toUpperCase();
  }

  private normalize(text: string): string {
    return text
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/\s+/g, '')
      .trim();
  }

  private markUpdate(cell: CellState): void {
    if (!cell.id || cell.disabled) return;

    const hasDiff =
      cell.meta !== cell.originalMeta || cell.activo !== cell.originalActivo;
    const key = this.cellKey(cell.behavior, cell.vehicleKey);

    if (!hasDiff) {
      this.updates.delete(key);
      return;
    }

    this.updates.set(key, {
      id: cell.id,
      meta: cell.meta,
      activo: cell.activo,
    });
  }
}
