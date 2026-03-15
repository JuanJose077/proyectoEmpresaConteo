import { Injectable } from '@angular/core';
import { DashboardResponse } from './report.service';

export type Jornada = 'GENERAL' | 'PICO' | 'VALLE';
export type DiaTipo = 'TODOS' | 'HABIL' | 'NO_HABIL';

export interface MetasVsConteoState {
  departamentoSeleccionado: string;
  municipioSeleccionadoId: number | null;
    puntoSeleccionadoId?: number | null;

  filters: {
    fechaInicio: string;
    fechaFin: string;
    jornada: Jornada;
    dia: DiaTipo;
  };

  data?: DashboardResponse;
}

const DEFAULT_STATE: MetasVsConteoState = {
  departamentoSeleccionado: '',
  municipioSeleccionadoId: null,
  filters: {
    fechaInicio: '',
    fechaFin: '',
    jornada: 'GENERAL',
    dia: 'TODOS',
  },
  data: undefined,
};

@Injectable({ providedIn: 'root' })
export class UiStateService {
  private metasVsConteoState: MetasVsConteoState = { ...DEFAULT_STATE };

  getMetasVsConteoState(): MetasVsConteoState {

    return {
      ...this.metasVsConteoState,
      filters: { ...this.metasVsConteoState.filters },
      data: this.metasVsConteoState.data,
    };
  }

  setMetasVsConteoState(patch: Partial<MetasVsConteoState>): void {
    this.metasVsConteoState = {
      ...this.metasVsConteoState,
      ...patch,
      filters: {
        ...this.metasVsConteoState.filters,
        ...(patch.filters ?? {}),
      },
    };
  }

  resetMetasVsConteoState(): void {
    this.metasVsConteoState = {
      ...DEFAULT_STATE,
      filters: { ...DEFAULT_STATE.filters },
    };
  }
}