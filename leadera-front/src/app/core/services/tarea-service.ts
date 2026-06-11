import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

// Espejo del TareaDTO del backend (Fase 2).
export interface TareaDTO {
  id: number;
  titulo: string;
  asignadoAId: number;
  asignadoANombre: string;
  creadoPorId: number;
  leadId: number | null;
  // null = tarea discreta (manual); con número = cuota automática.
  objetivo: number | null;
  // Leads distintos contactados hoy por el asistente (0 si la tarea es discreta).
  progreso: number;
  completada: boolean;
  fechaCompletada: string | null;
  fechaObjetivo: string;
}

// Espejo del AsistenteResumenDTO del backend.
export interface AsistenteResumen {
  id: number;
  nombre: string;
  apellido: string;
}

// Espejo del AsistenteStatsDTO del backend: fila de "Mi equipo" del agente.
// leadsActivos son los del supervisor (el asistente trabaja sobre esos leads).
export interface AsistenteStats {
  id: number;
  nombre: string;
  apellido: string;
  email: string;
  activo: boolean;
  leadsActivos: number;
  tareasCompletadasHoy: number;
}

// Espejo de CrearAgenteEquipoRequest (mismo DTO que usa el alta de agentes del dueño).
export interface CrearAsistenteRequest {
  nombre: string;
  apellido: string;
  email: string;
  passwordTemporal: string;
}

// Respuesta del alta de asistente: el backend reutiliza AgenteEquipoDTO
// (trae rol en lugar de tareasCompletadasHoy).
export interface AsistenteCreado {
  id: number;
  nombre: string;
  apellido: string;
  email: string;
  rol: string;
  activo: boolean;
  leadsActivos: number;
}

// Espejo de CrearTareaRequest del backend.
export interface CrearTareaRequest {
  titulo: string;
  asignadoAId: number;
  leadId?: number | null;
  // null/ausente = tarea discreta; con número = cuota automática.
  objetivo?: number | null;
  // LocalDateTime ISO (ej: "2026-06-11T23:59:59").
  fechaObjetivo: string;
}

@Injectable({
  providedIn: 'root',
})
export class TareaService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/tareas`;
  private asistentesUrl = `${environment.apiUrl}/agente/asistentes`;

  // Tareas que el supervisor le creó a uno de sus asistentes.
  obtenerTareasDeAsistente(asistenteId: number): Observable<TareaDTO[]> {
    return this.http.get<TareaDTO[]>(`${this.apiUrl}/asistente/${asistenteId}`);
  }

  // Asistentes del actor autenticado (lista vacía si no tiene).
  obtenerMisAsistentes(): Observable<AsistenteResumen[]> {
    return this.http.get<AsistenteResumen[]>(`${this.apiUrl}/mis-asistentes`);
  }

  // Marca a mano una tarea discreta (acción del asistente, no del supervisor).
  completarTarea(tareaId: number): Observable<TareaDTO> {
    return this.http.patch<TareaDTO>(`${this.apiUrl}/${tareaId}/completar`, {});
  }

  // Tareas de HOY del actor autenticado (vista del asistente en /gestion-del-dia).
  obtenerTareasDelDia(): Observable<TareaDTO[]> {
    return this.http.get<TareaDTO[]>(`${this.apiUrl}/hoy`);
  }

  // Crea una tarea (AGENTE/DUENO para su asistente; un asistente solo se autoasigna).
  crearTarea(request: CrearTareaRequest): Observable<TareaDTO> {
    return this.http.post<TareaDTO>(this.apiUrl, request);
  }

  // ---- Gestión de asistentes (vista "Mi equipo" del agente) ----

  // Asistentes del actor con métricas del día (reemplaza a obtenerMisAsistentes
  // en la página; el sidebar sigue usando la versión liviana).
  listarAsistentes(): Observable<AsistenteStats[]> {
    return this.http.get<AsistenteStats[]>(this.asistentesUrl);
  }

  crearAsistente(request: CrearAsistenteRequest): Observable<AsistenteCreado> {
    return this.http.post<AsistenteCreado>(this.asistentesUrl, request);
  }

  cambiarActivoAsistente(asistenteId: number, activo: boolean): Observable<AsistenteStats> {
    return this.http.patch<AsistenteStats>(`${this.asistentesUrl}/${asistenteId}/activo`, { activo });
  }
}
