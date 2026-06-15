import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Page } from '../models/page';
import { LeadResumen } from '../models/lead-resumen';
import { AgenteDashboard } from '../models/agente-dashboard';

// Espejos de los DTOs de /inmobiliaria/** (solo accesibles con rol DUENO).

export interface AgenteEquipo {
  id: number;
  nombre: string;
  apellido: string;
  email: string;
  rol: 'DUENO' | 'AGENTE';
  activo: boolean;
  leadsActivos: number;
}

export interface CrearAgenteEquipoRequest {
  nombre: string;
  apellido: string;
  email: string;
  passwordTemporal: string;
}

export interface LeadEquipo {
  lead: LeadResumen;
  agenteId: number;
  agenteNombre: string;
  agenteApellido: string;
}

export interface AgenteStats {
  agenteId: number;
  nombre: string;
  apellido: string;
  stats: AgenteDashboard;
  tareasCompletadasMes: number;
  tareasTotalesMes: number;
}

export interface EquipoStats {
  totales: AgenteDashboard;
  porAgente: AgenteStats[];
}

@Injectable({
  providedIn: 'root',
})
export class EquipoService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/inmobiliaria`;

  listarEquipo(): Observable<AgenteEquipo[]> {
    return this.http.get<AgenteEquipo[]>(`${this.apiUrl}/agentes`);
  }

  crearAgente(request: CrearAgenteEquipoRequest): Observable<AgenteEquipo> {
    return this.http.post<AgenteEquipo>(`${this.apiUrl}/agentes`, request);
  }

  cambiarActivo(agenteId: number, activo: boolean): Observable<AgenteEquipo> {
    return this.http.patch<AgenteEquipo>(`${this.apiUrl}/agentes/${agenteId}/activo`, { activo });
  }

  getLeadsDelEquipo(page: number, size: number): Observable<Page<LeadEquipo>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<Page<LeadEquipo>>(`${this.apiUrl}/leads`, { params });
  }

  getStats(): Observable<EquipoStats> {
    return this.http.get<EquipoStats>(`${this.apiUrl}/stats`);
  }
}
