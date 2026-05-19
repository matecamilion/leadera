import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AgenteDashboard } from '../models/agente-dashboard';

export interface ActividadReciente {
  tipoInteraccion: string;
  detalle: string;
  fechaInteraccion: string;
  leadNombre: string;
  leadApellido: string;
  leadId: number;
}

export type DashboardPeriodo = '7d' | '30d' | '90d' | 'ano';

export interface DashboardSnapshot {
  activos: number;
  calientes: number;
  tibios: number;
  frios: number;
  ganadosMes: number;
  metaMensual: number;
  diasRestantesMes: number;
  funnel: DashboardFunnel;
}

export interface DashboardFunnel {
  contactados: number;
  calificados: number;
  visita: number;
  oferta: number;
  cerrado: number;
}

export interface DashboardKpi {
  actual: number;
  anterior: number;
  deltaPct: number;
  direccion: 'up' | 'down' | 'flat';
  sparkline: number[];
}

export interface DashboardKpis {
  leadsActivos: DashboardKpi;
  tasaConversion: DashboardKpi;
  tiempoRespuesta: DashboardKpi;
  ganados: DashboardKpi;
}

export interface DashboardEvolucion {
  fechas: string[];
  nuevos: number[];
  ganados: number[];
  perdidos: number[];
}

export interface DashboardOrigen {
  nombre: string;
  cantidad: number;
  porcentaje: number;
}

export interface DashboardData {
  periodo: DashboardPeriodo;
  diasPeriodo: number;
  snapshot: DashboardSnapshot;
  kpis: DashboardKpis;
  evolucion: DashboardEvolucion;
  origenes: DashboardOrigen[];
}

@Injectable({
  providedIn: 'root',
})
export class AgenteService {
   private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  getDashboard(agenteId: number, periodo: DashboardPeriodo): Observable<DashboardData> {
    const params = new HttpParams().set('periodo', periodo);
    return this.http.get<DashboardData>(
      `${this.apiUrl}/leads/agente/${agenteId}/dashboard`,
      { params }
    );
  }

  getActividadReciente(agenteId: number): Observable<ActividadReciente[]> {
    return this.http.get<ActividadReciente[]>(`${this.apiUrl}/leads/agente/${agenteId}/actividad-reciente`);
  }

  getDashboardStats(agenteId: number): Observable<AgenteDashboard> {
    return this.http.get<AgenteDashboard>(`${this.apiUrl}/leads/agente/${agenteId}/stats`);
  }
}
